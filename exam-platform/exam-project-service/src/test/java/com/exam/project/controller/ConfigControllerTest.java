package com.exam.project.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ConfigController 集成测试 — 连接 Docker MySQL 真实数据库
 * 测试工具配置的查询、更新和批量更新
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ==================== 1. 查询配置列表 ====================

    @Test
    @Order(1)
    @DisplayName("查询校长角色配置 — 应返回非空列表")
    void testListConfigs_PrincipalRole_ShouldReturnConfigs() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/project/1/config")
                        .param("role", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(greaterThan(0)))
                .andReturn();

        // 验证返回的每条配置都有 toolCode 和 toolName
        JsonNode configs = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        for (JsonNode config : configs) {
            assertNotNull(config.get("toolCode"), "每条配置应有 toolCode");
            assertNotNull(config.get("toolName"), "每条配置应有 toolName");
            assertEquals(2, config.get("role").asInt(), "筛选 role=2，所有配置 role 应为 2");
        }
    }

    @Test
    @Order(2)
    @DisplayName("查询老师角色配置 — 应返回非空列表")
    void testListConfigs_TeacherRole_ShouldReturnConfigs() throws Exception {
        mockMvc.perform(get("/api/project/1/config")
                        .param("role", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(greaterThan(0)));
    }

    @Test
    @Order(3)
    @DisplayName("查询管理员角色配置 — 应返回非空列表")
    void testListConfigs_AdminRole_ShouldReturnConfigs() throws Exception {
        mockMvc.perform(get("/api/project/1/config")
                        .param("role", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    // ==================== 2. 更新单条配置 ====================

    @Test
    @Order(4)
    @DisplayName("更新配置 isEnabled — 应成功并生效")
    void testUpdateConfig_ShouldSucceed() throws Exception {
        // 先查出一条配置
        MvcResult listResult = mockMvc.perform(get("/api/project/1/config")
                        .param("role", "2"))
                .andReturn();
        JsonNode configs = objectMapper.readTree(listResult.getResponse().getContentAsString()).get("data");
        long configId = configs.get(0).get("id").asLong();
        int oldEnabled = configs.get(0).get("isEnabled").asInt();

        // 翻转 isEnabled
        int newEnabled = oldEnabled == 1 ? 0 : 1;
        String json = String.format("{\"id\":%d,\"isEnabled\":%d}", configId, newEnabled);

        mockMvc.perform(put("/api/project/1/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 重新查一遍，确认值已改变
        MvcResult verifyResult = mockMvc.perform(get("/api/project/1/config")
                        .param("role", "2"))
                .andReturn();
        JsonNode updatedConfigs = objectMapper.readTree(verifyResult.getResponse().getContentAsString()).get("data");
        for (JsonNode c : updatedConfigs) {
            if (c.get("id").asLong() == configId) {
                assertEquals(newEnabled, c.get("isEnabled").asInt(), "isEnabled 应已更新");
                break;
            }
        }
    }

    @Test
    @Order(5)
    @DisplayName("更新不存在的配置 — 应返回 404")
    void testUpdateConfig_NonExistent_ShouldReturn404() throws Exception {
        String json = "{\"id\":99999,\"isEnabled\":1}";

        mockMvc.perform(put("/api/project/1/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("配置不存在"));
    }

    // ==================== 3. 批量更新配置 ====================

    @Test
    @Order(6)
    @DisplayName("批量更新 scorePerSubmit — 应成功更新多条")
    void testBatchUpdateConfigs_ShouldSucceed() throws Exception {
        // 先查出老师角色的所有配置
        MvcResult listResult = mockMvc.perform(get("/api/project/1/config")
                        .param("role", "3"))
                .andReturn();
        JsonNode configs = objectMapper.readTree(listResult.getResponse().getContentAsString()).get("data");

        // 构造批量更新请求
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < configs.size() && i < 3; i++) {
            if (i > 0) sb.append(",");
            sb.append(String.format("{\"id\":%d,\"scorePerSubmit\":%d}",
                    configs.get(i).get("id").asLong(), 10 + i));
        }
        sb.append("]");

        mockMvc.perform(put("/api/project/1/config/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sb.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== 4. 查询已启用工具列表（学员端） ====================

    @Test
    @Order(7)
    @DisplayName("查询老师角色已启用工具 — 应返回 is_enabled=1 的工具")
    void testListEnabledTools_TeacherRole_ShouldReturnEnabledTools() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/project/1/config/tools")
                        .param("role", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andReturn();

        // 验证返回的工具都有 code 和 name
        JsonNode tools = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        for (JsonNode tool : tools) {
            assertNotNull(tool.get("code"), "工具应有 code 字段");
            assertNotNull(tool.get("name"), "工具应有 name 字段");
            assertTrue(tool.get("isEnabled").asBoolean(), "返回的工具应都是已启用的");
        }
    }

    @Test
    @Order(8)
    @DisplayName("查询校长角色已启用工具 — 应返回已启用的工具列表")
    void testListEnabledTools_PrincipalRole_ShouldReturnEnabledTools() throws Exception {
        mockMvc.perform(get("/api/project/1/config/tools")
                        .param("role", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @Order(9)
    @DisplayName("查询不存在的项目工具 — 应返回空列表")
    void testListEnabledTools_NonExistentProject_ShouldReturnEmptyList() throws Exception {
        mockMvc.perform(get("/api/project/99999/config/tools")
                        .param("role", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }
}
