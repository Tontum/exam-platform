package com.exam.project.controller;

import com.exam.common.utils.JwtUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AnswerController 集成测试 — 连接 Docker MySQL 真实数据库
 * 测试查看已批阅试卷成绩接口
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AnswerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /** 种子数据中 userId=3 对应的老师用户，有已批阅试卷 */
    private static final Long USER_ID = 3L;
    private static final String TOKEN = JwtUtils.generateToken(USER_ID, "teacher01", 3);

    // ==================== 1. 查看已批阅试卷成绩 ====================

    @Test
    @Order(1)
    @DisplayName("查看已批阅试卷成绩 — 应返回完整答卷数据")
    void testGetExamResult_ReviewedPaper_ShouldReturnFullResult() throws Exception {
        // 种子数据中 response(id=4, paperId=4, userId=3, status=3, score=78)
        MvcResult result = mockMvc.perform(get("/api/answer/4/result")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isNotEmpty())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");

        // 验证试卷级字段
        assertNotNull(data.get("paperName"), "应有试卷名称");
        assertNotNull(data.get("totalScore"), "应有试卷总分");
        assertNotNull(data.get("passScore"), "应有及格线");
        assertNotNull(data.get("userScore"), "应有用户得分");
        assertEquals(78, data.get("userScore").asInt(), "用户得分应为 78");
        assertNotNull(data.get("isPass"), "应有是否合格");
        assertTrue(data.get("isPass").asBoolean(), "用户应合格");
        assertNotNull(data.get("questions"), "应有题目列表");
        assertTrue(data.get("questions").isArray(), "题目列表应为数组");
    }

    @Test
    @Order(2)
    @DisplayName("查看未批阅试卷成绩 — 应返回 400 错误")
    void testGetExamResult_UnreviewedPaper_ShouldReturn400() throws Exception {
        // 种子数据中 response(id=1, paperId=1, userId=3, status=0) — 未答题
        mockMvc.perform(get("/api/answer/1/result")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("试卷尚未批阅完成，无法查看成绩"));
    }

    @Test
    @Order(3)
    @DisplayName("查看不存在的试卷成绩 — 应返回 404 错误")
    void testGetExamResult_NonExistent_ShouldReturn404() throws Exception {
        mockMvc.perform(get("/api/answer/99999/result")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }
}
