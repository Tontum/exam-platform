package com.exam.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.common.common.BusinessException;
import com.exam.common.entity.*;
import com.exam.project.mapper.*;
import com.exam.project.model.dto.AnswerSaveDTO;
import com.exam.project.model.vo.ExamResultOptionVO;
import com.exam.project.model.vo.ExamResultQuestionVO;
import com.exam.project.model.vo.ExamResultVO;
import com.exam.project.model.vo.SubmitResultVO;
import com.exam.project.service.AnswerService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 答题服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnswerServiceImpl implements AnswerService {

    private static final String REDIS_KEY_ANSWERS = "exam:answers:%d";
    private static final String REDIS_KEY_TIMER = "exam:timer:%d";
    private static final Duration REDIS_TTL = Duration.ofHours(2);

    private final ResponseMapper responseMapper;
    private final AnswerMapper answerMapper;
    private final QuestionMapper questionMapper;
    private final OptionMapper optionMapper;
    private final PaperMapper paperMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void startExam(Long paperId, Long userId) {
        Response response = findResponse(paperId, userId);
        if (response.getStatus() == 0) {
            response.setStatus(1);
            responseMapper.updateById(response);
            log.info("开始答题: responseId={}, userId={}", response.getId(), userId);
        }
    }

    @Override
    public void saveProgress(Long paperId, Long userId, AnswerSaveDTO dto) {
        Response response = findResponse(paperId, userId);

        // 保存答案到 Redis
        try {
            String answersJson = objectMapper.writeValueAsString(dto.getAnswers());
            redisTemplate.opsForValue().set(
                    String.format(REDIS_KEY_ANSWERS, response.getId()),
                    answersJson,
                    REDIS_TTL
            );

            if (dto.getRemainingSeconds() != null) {
                redisTemplate.opsForValue().set(
                        String.format(REDIS_KEY_TIMER, response.getId()),
                        String.valueOf(dto.getRemainingSeconds()),
                        REDIS_TTL
                );
            }
        } catch (Exception e) {
            log.error("保存答题进度失败: responseId={}", response.getId(), e);
            throw new RuntimeException("保存失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SubmitResultVO submitExam(Long paperId, Long userId) {
        Response response = findResponse(paperId, userId);
        if (response.getStatus() == 2 || response.getStatus() == 3) {
            throw BusinessException.badRequest("试卷已提交，不可重复提交");
        }

        // 从 Redis 读取答案
        Map<Long, String> answers = loadAnswersFromRedis(response.getId());

        // 查询试卷和题目
        Paper paper = paperMapper.selectById(paperId);
        LambdaQueryWrapper<Question> qw = new LambdaQueryWrapper<>();
        qw.eq(Question::getPaperId, paperId);
        List<Question> questions = questionMapper.selectList(qw);

        // 查询所有正确选项
        LambdaQueryWrapper<Option> ow = new LambdaQueryWrapper<>();
        ow.eq(Option::getPaperId, paperId).eq(Option::getIsCorrect, 1);
        List<Option> correctOptions = optionMapper.selectList(ow);
        Map<Long, List<String>> correctMap = correctOptions.stream()
                .collect(Collectors.groupingBy(
                        Option::getQuestionId,
                        Collectors.mapping(Option::getOptionLabel, Collectors.toList())
                ));

        // 写入答案到 MySQL 并自动批阅客观题
        BigDecimal objectiveScore = BigDecimal.ZERO;
        int correctCount = 0;
        int objectiveCount = 0;

        for (Question q : questions) {
            Answer answer = new Answer();
            answer.setResponseId(response.getId());
            answer.setQuestionId(q.getId());
            answer.setUserId(userId);

            String userAnswer = answers.getOrDefault(q.getId(), "");
            answer.setAnswerContent(userAnswer);

            // 客观题自动批阅
            if (q.getQuestionType() != null && q.getQuestionType() != 4) {
                objectiveCount++;
                List<String> correct = correctMap.getOrDefault(q.getId(), List.of());
                boolean isCorrect = judgeCorrect(q.getQuestionType(), userAnswer, correct);
                answer.setIsCorrect(isCorrect ? 1 : 0);

                if (isCorrect) {
                    answer.setScore(q.getScore());
                    objectiveScore = objectiveScore.add(q.getScore());
                    correctCount++;
                } else {
                    answer.setScore(BigDecimal.ZERO);
                }
            }

            answerMapper.insert(answer);
        }

        // 更新答题记录
        response.setStatus(2);
        response.setSubmitTime(LocalDateTime.now());

        // 如果全是客观题，直接完成批阅
        if (objectiveCount == questions.size()) {
            response.setStatus(3);
            response.setScore(objectiveScore);
            response.setIsPass(objectiveScore.compareTo(paper.getPassScore()) >= 0 ? 1 : 0);
            response.setReviewTime(LocalDateTime.now());
        }

        responseMapper.updateById(response);

        // 清理 Redis
        redisTemplate.delete(String.format(REDIS_KEY_ANSWERS, response.getId()));
        redisTemplate.delete(String.format(REDIS_KEY_TIMER, response.getId()));

        SubmitResultVO result = new SubmitResultVO();
        result.setTotalScore(paper.getTotalScore());
        result.setObjectiveScore(objectiveScore);
        result.setCorrectCount(correctCount);
        result.setTotalCount(objectiveCount);
        result.setIsPass(response.getIsPass());
        return result;
    }

    /**
     * 根据 paperId + userId 查找 response
     */
    private Response findResponse(Long paperId, Long userId) {
        LambdaQueryWrapper<Response> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Response::getPaperId, paperId)
                .eq(Response::getUserId, userId);
        Response response = responseMapper.selectOne(wrapper);
        if (response == null) {
            throw BusinessException.notFound("未找到答题记录，请先确认试卷已发布");
        }
        return response;
    }

    @Override
    public ExamResultVO getExamResult(Long paperId, Long userId) {
        // 1. 查询答题记录，必须已批阅
        Response response = findResponse(paperId, userId);
        if (response.getStatus() != 3) {
            throw BusinessException.badRequest("试卷尚未批阅完成，无法查看成绩");
        }

        // 2. 查询试卷信息
        Paper paper = paperMapper.selectById(paperId);

        // 3. 查询所有题目
        LambdaQueryWrapper<Question> qw = new LambdaQueryWrapper<>();
        qw.eq(Question::getPaperId, paperId).orderByAsc(Question::getSortOrder);
        List<Question> questions = questionMapper.selectList(qw);

        // 4. 查询所有选项
        LambdaQueryWrapper<Option> ow = new LambdaQueryWrapper<>();
        ow.eq(Option::getPaperId, paperId).orderByAsc(Option::getSortOrder);
        List<Option> allOptions = optionMapper.selectList(ow);
        Map<Long, List<Option>> optionsByQuestion = allOptions.stream()
                .collect(Collectors.groupingBy(Option::getQuestionId));

        // 5. 查询用户所有答案
        LambdaQueryWrapper<Answer> aw = new LambdaQueryWrapper<>();
        aw.eq(Answer::getResponseId, response.getId());
        List<Answer> answers = answerMapper.selectList(aw);
        Map<Long, Answer> answerByQuestion = answers.stream()
                .collect(Collectors.toMap(Answer::getQuestionId, a -> a, (a, b) -> a));

        // 6. 组装逐题结果
        List<ExamResultQuestionVO> questionVOs = new ArrayList<>();
        int correctCount = 0;
        int objectiveCount = 0;

        for (Question q : questions) {
            ExamResultQuestionVO qvo = new ExamResultQuestionVO();
            qvo.setQuestionId(q.getId());
            qvo.setStem(q.getTitle());
            qvo.setQuestionType(q.getQuestionType());
            qvo.setScore(q.getScore());
            qvo.setSortOrder(q.getSortOrder());

            Answer answer = answerByQuestion.get(q.getId());
            if (answer != null) {
                qvo.setUserAnswer(answer.getAnswerContent());
                qvo.setGotScore(answer.getScore());
                qvo.setIsCorrect(answer.getIsCorrect() != null ? answer.getIsCorrect() == 1 : null);
                qvo.setReviewComment(answer.getReviewComment());
            }

            // 客观题统计
            if (q.getQuestionType() != null && q.getQuestionType() != 4) {
                objectiveCount++;
                if (answer != null && answer.getIsCorrect() != null && answer.getIsCorrect() == 1) {
                    correctCount++;
                }
            }

            // 组装选项（客观题）
            List<Option> opts = optionsByQuestion.getOrDefault(q.getId(), List.of());
            List<ExamResultOptionVO> optionVOs = opts.stream().map(o -> {
                ExamResultOptionVO ovo = new ExamResultOptionVO();
                ovo.setOptionId(o.getId());
                ovo.setOptionLabel(o.getOptionLabel());
                ovo.setOptionContent(o.getOptionContent());
                ovo.setIsCorrect(o.getIsCorrect() == 1);
                return ovo;
            }).toList();
            qvo.setOptions(optionVOs);

            questionVOs.add(qvo);
        }

        // 7. 组装总结果
        ExamResultVO result = new ExamResultVO();
        result.setPaperName(paper.getTitle());
        result.setTotalScore(paper.getTotalScore());
        result.setPassScore(paper.getPassScore());
        result.setUserScore(response.getScore());
        result.setIsPass(response.getIsPass() != null && response.getIsPass() == 1);
        result.setQuestionCount(questions.size());
        result.setCorrectCount(correctCount);
        result.setObjectiveCount(objectiveCount);
        result.setSubmitTime(response.getSubmitTime() != null ? response.getSubmitTime().toString() : null);
        result.setReviewTime(response.getReviewTime() != null ? response.getReviewTime().toString() : null);
        result.setQuestions(questionVOs);

        return result;
    }

    /**
     * 从 Redis 加载答案
     */
    private Map<Long, String> loadAnswersFromRedis(Long responseId) {
        try {
            String json = redisTemplate.opsForValue().get(String.format(REDIS_KEY_ANSWERS, responseId));
            if (json == null || json.isEmpty()) {
                return Map.of();
            }
            return objectMapper.readValue(json, new TypeReference<Map<Long, String>>() {});
        } catch (Exception e) {
            log.warn("从 Redis 加载答案失败: responseId={}", responseId, e);
            return Map.of();
        }
    }

    /**
     * 判断客观题是否正确
     */
    private boolean judgeCorrect(Integer questionType, String userAnswer, List<String> correctLabels) {
        if (userAnswer == null || userAnswer.isBlank()) return false;
        if (correctLabels.isEmpty()) return false;

        return switch (questionType) {
            case 1 -> { // 单选题：精确匹配
                yield userAnswer.trim().equals(correctLabels.get(0));
            }
            case 2 -> { // 多选题：选项集合相等
                Set<String> userSet = Arrays.stream(userAnswer.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toSet());
                Set<String> correctSet = new HashSet<>(correctLabels);
                yield userSet.equals(correctSet);
            }
            case 3 -> { // 判断题：精确匹配
                yield userAnswer.trim().equals(correctLabels.get(0));
            }
            default -> false;
        };
    }
}
