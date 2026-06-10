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
 * ProjectController 集成测试 — 连接 Docker MySQL 真实数据库
 * 测试所有项目管理接口的增删改查和加入/退出功能
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static Long createdProjectId;

    // ==================== 1. 分页查询项目列表 ====================

    @Test
    @Order(1)
    @DisplayName("分页查询项目列表 — 应返回 records 和 total")
    void testListProjects_ShouldReturnPageResult() throws Exception {
        mockMvc.perform(get("/api/project/list")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.records.length()").value(greaterThan(0)))
                .andExpect(jsonPath("$.data.total").value(greaterThan(0)))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(10));
    }

    @Test
    @Order(2)
    @DisplayName("按状态筛选项目 — status=1 应只返回进行中的项目")
    void testListProjects_WithStatusFilter() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/project/list")
                        .param("page", "1")
                        .param("size", "20")
                        .param("status", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        // 验证所有返回的项目 status 都是 1
        JsonNode records = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data").get("records");
        for (JsonNode record : records) {
            assertEquals(1, record.get("status").asInt(), "筛选 status=1，所有记录 status 应为 1");
        }
    }

    @Test
    @Order(3)
    @DisplayName("按关键词搜索项目 — keyword='河南' 应返回匹配结果")
    void testListProjects_WithKeywordSearch() throws Exception {
        mockMvc.perform(get("/api/project/list")
                        .param("page", "1")
                        .param("size", "10")
                        .param("keyword", "河南"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.total").value(greaterThan(0)));
    }

    // ==================== 2. 查询项目详情 ====================

    @Test
    @Order(4)
    @DisplayName("查询存在的项目详情 — 应返回完整信息")
    void testGetProject_ShouldReturnProjectDetail() throws Exception {
        mockMvc.perform(get("/api/project/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").isString())
                .andExpect(jsonPath("$.data.status").isNumber())
                .andExpect(jsonPath("$.data.createdAt").isString());
    }

    @Test
    @Order(5)
    @DisplayName("查询不存在的项目 — 应返回 404 业务异常")
    void testGetProject_NonExistent_ShouldReturn404() throws Exception {
        mockMvc.perform(get("/api/project/99999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("项目不存在"));
    }

    // ==================== 3. 创建项目 ====================

    @Test
    @Order(6)
    @DisplayName("创建项目 — 应返回创建的项目信息")
    void testCreateProject_ShouldSucceed() throws Exception {
        String json = objectMapper.writeValueAsString(new CreateProjectRequest(
                "集成测试项目", "通过单元测试创建的项目", "河南省", "郑州市"
        ));

        MvcResult result = mockMvc.perform(post("/api/project")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("集成测试项目"))
                .andExpect(jsonPath("$.data.status").value(0))
                .andReturn();

        // 保存创建的项目 ID，后续测试用到
        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        createdProjectId = data.get("id").asLong();
        assertNotNull(createdProjectId, "创建项目应返回有效 ID");
    }

    // ==================== 4. 编辑项目 ====================

    @Test
    @Order(7)
    @DisplayName("编辑项目 — 应更新项目名称")
    void testUpdateProject_ShouldSucceed() throws Exception {
        String json = objectMapper.writeValueAsString(new CreateProjectRequest(
                "集成测试项目（已更新）", null, null, null
        ));

        mockMvc.perform(put("/api/project/" + createdProjectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("集成测试项目（已更新）"));
    }

    // ==================== 5. 加入项目 ====================

    @Test
    @Order(8)
    @DisplayName("老师加入项目 — 应成功")
    void testJoinProject_ShouldSucceed() throws Exception {
        mockMvc.perform(post("/api/project/" + createdProjectId + "/join")
                        .header("X-User-Id", "12")) // teacher10
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @Order(9)
    @DisplayName("重复加入项目 — 应返回 400 错误")
    void testJoinProject_Duplicate_ShouldReturn400() throws Exception {
        mockMvc.perform(post("/api/project/" + createdProjectId + "/join")
                        .header("X-User-Id", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(containsString("已加入")));
    }

    // ==================== 6. 退出项目 ====================

    @Test
    @Order(10)
    @DisplayName("老师退出项目 — 应成功")
    void testLeaveProject_ShouldSucceed() throws Exception {
        mockMvc.perform(delete("/api/project/" + createdProjectId + "/leave")
                        .header("X-User-Id", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @Order(11)
    @DisplayName("退出未加入的项目 — 应返回 400 错误")
    void testLeaveProject_NotJoined_ShouldReturn400() throws Exception {
        mockMvc.perform(delete("/api/project/" + createdProjectId + "/leave")
                        .header("X-User-Id", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(containsString("未加入")));
    }

    /**
     * 创建/更新项目请求体
     */
    record CreateProjectRequest(String name, String description, String province, String city) {}
}
