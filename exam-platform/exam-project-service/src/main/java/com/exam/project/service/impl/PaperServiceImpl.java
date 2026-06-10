package com.exam.project.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.common.common.BusinessException;
import com.exam.common.entity.*;
import com.exam.project.mapper.*;
import com.exam.common.vo.MyPaperVO;
import com.exam.project.model.dto.OptionCreateDTO;
import com.exam.project.model.dto.PaperCreateDTO;
import com.exam.project.model.dto.PaperUpdateDTO;
import com.exam.project.model.dto.QuestionCreateDTO;
import com.exam.project.model.vo.ExamOptionVO;
import com.exam.project.model.vo.ExamPaperVO;
import com.exam.project.model.vo.ExamQuestionVO;
import com.exam.project.model.vo.OptionVO;
import com.exam.project.model.vo.PaperDetailVO;
import com.exam.project.model.vo.QuestionVO;
import com.exam.project.service.PaperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 试卷服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaperServiceImpl implements PaperService {

    private final PaperMapper paperMapper;
    private final QuestionMapper questionMapper;
    private final OptionMapper optionMapper;
    private final ResponseMapper responseMapper;
    private final ProjectUserMapper projectUserMapper;
    private final UserMapper userMapper;
    private final SchoolMapper schoolMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaperDetailVO createPaper(PaperCreateDTO dto, Long creatorId) {
        Paper paper = new Paper();
        paper.setTitle(dto.getTitle());
        paper.setDescription(dto.getDescription());
        paper.setPaperType(dto.getPaperType() != null ? dto.getPaperType() : 1);
        paper.setTotalScore(dto.getTotalScore());
        paper.setPassScore(dto.getPassScore());
        paper.setQuestionCount(0);
        paper.setDurationMinutes(dto.getDurationMinutes());
        paper.setStatus(0);
        paper.setPublisherId(creatorId);
        paper.setProjectId(dto.getProjectId());

        User creator = userMapper.selectById(creatorId);
        if (creator != null && creator.getSchoolId() != null) {
            School school = schoolMapper.selectById(creator.getSchoolId());
            if (school != null) {
                paper.setProvince(school.getProvince());
                paper.setCity(school.getCity());
                paper.setCounty(school.getCounty());
                paper.setSchool(school.getName());
            }
        }

        paperMapper.insert(paper);

        // 如果有题目，一起创建
        if (dto.getQuestions() != null && !dto.getQuestions().isEmpty()) {
            for (QuestionCreateDTO questionDTO : dto.getQuestions()) {
                addQuestion(paper.getId(), questionDTO, creatorId);
            }
            paper.setQuestionCount(dto.getQuestions().size());
            paperMapper.updateById(paper);
        }

        log.info("创建试卷成功: paperId={}, title={}, projectId={}", paper.getId(), paper.getTitle(), paper.getProjectId());
        return getPaperDetail(paper.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaperDetailVO updatePaper(Long paperId, PaperUpdateDTO dto, Long userId) {
        Paper paper = paperMapper.selectById(paperId);
        if (paper == null) {
            throw BusinessException.notFound("试卷不存在");
        }
        if (paper.getStatus() != 0) {
            throw BusinessException.badRequest("只能编辑草稿状态的试卷");
        }

        if (dto.getTitle() != null) {
            paper.setTitle(dto.getTitle());
        }
        if (dto.getDescription() != null) {
            paper.setDescription(dto.getDescription());
        }
        if (dto.getPaperType() != null) {
            paper.setPaperType(dto.getPaperType());
        }
        if (dto.getTotalScore() != null) {
            paper.setTotalScore(dto.getTotalScore());
        }
        if (dto.getPassScore() != null) {
            paper.setPassScore(dto.getPassScore());
        }
        if (dto.getDurationMinutes() != null) {
            paper.setDurationMinutes(dto.getDurationMinutes());
        }

        paperMapper.updateById(paper);

        log.info("更新试卷成功: paperId={}", paperId);
        return getPaperDetail(paperId);
    }

    @Override
    public ExamPaperVO getExamPaper(Long paperId, Long userId) {
        Paper paper = paperMapper.selectById(paperId);
        if (paper == null) {
            throw BusinessException.notFound("试卷不存在");
        }
        if (paper.getStatus() != 1) {
            throw BusinessException.badRequest("试卷未发布，无法答题");
        }

        ExamPaperVO vo = new ExamPaperVO();
        vo.setPaperId(paper.getId());
        vo.setPaperName(paper.getTitle());
        vo.setTotalScore(paper.getTotalScore());
        vo.setPassScore(paper.getPassScore());
        vo.setQuestionCount(paper.getQuestionCount());
        vo.setDurationMinutes(paper.getDurationMinutes());

        LambdaQueryWrapper<Question> qw = new LambdaQueryWrapper<>();
        qw.eq(Question::getPaperId, paperId).orderByAsc(Question::getSortOrder);
        List<Question> questions = questionMapper.selectList(qw);

        LambdaQueryWrapper<Option> ow = new LambdaQueryWrapper<>();
        ow.eq(Option::getPaperId, paperId).orderByAsc(Option::getSortOrder);
        List<Option> allOptions = optionMapper.selectList(ow);
        var optionMap = allOptions.stream()
                .collect(Collectors.groupingBy(Option::getQuestionId));

        List<ExamQuestionVO> questionVOs = questions.stream().map(q -> {
            ExamQuestionVO qvo = new ExamQuestionVO();
            qvo.setQuestionId(q.getId());
            qvo.setStem(q.getTitle());
            qvo.setType(questionTypeToString(q.getQuestionType()));
            qvo.setScore(q.getScore());

            List<Option> opts = optionMap.getOrDefault(q.getId(), List.of());
            List<ExamOptionVO> optVOs = opts.stream().map(o -> {
                ExamOptionVO ovo = new ExamOptionVO();
                ovo.setOptionId(o.getId());
                ovo.setOptionKey(o.getOptionLabel());
                ovo.setContent(o.getOptionContent());
                return ovo;
            }).collect(Collectors.toList());

            qvo.setOptions(optVOs);
            return qvo;
        }).collect(Collectors.toList());

        vo.setQuestions(questionVOs);
        return vo;
    }

    private String questionTypeToString(Integer type) {
        if (type == null) return "single";
        return switch (type) {
            case 2 -> "multiple";
            case 3 -> "judge";
            case 4 -> "essay";
            default -> "single";
        };
    }

    @Override
    public List<MyPaperVO> listMyPapers(Long projectId, Long userId) {
        LambdaQueryWrapper<Response> respWrapper = new LambdaQueryWrapper<>();
        respWrapper.eq(Response::getUserId, userId);
        List<Response> responses = responseMapper.selectList(respWrapper);

        if (responses.isEmpty()) {
            return List.of();
        }

        List<Long> paperIds = responses.stream()
                .map(Response::getPaperId)
                .distinct()
                .collect(Collectors.toList());

        LambdaQueryWrapper<Paper> paperWrapper = new LambdaQueryWrapper<>();
        paperWrapper.in(Paper::getId, paperIds)
                .eq(Paper::getProjectId, projectId)
                .eq(Paper::getStatus, 1);
        List<Paper> papers = paperMapper.selectList(paperWrapper);

        if (papers.isEmpty()) {
            return List.of();
        }

        var paperMap = papers.stream()
                .collect(Collectors.toMap(Paper::getId, p -> p));

        var publisherIds = papers.stream()
                .map(Paper::getPublisherId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        var publisherMap = publisherIds.isEmpty() ? Map.<Long, String>of()
                : userMapper.selectBatchIds(publisherIds).stream()
                        .collect(Collectors.toMap(User::getId, User::getRealName));

        return responses.stream()
                .filter(r -> paperMap.containsKey(r.getPaperId()))
                .map(r -> {
                    Paper p = paperMap.get(r.getPaperId());
                    MyPaperVO vo = new MyPaperVO();
                    vo.setResponseId(r.getId());
                    vo.setPaperId(p.getId());
                    vo.setPaperTitle(p.getTitle());
                    vo.setPaperType(p.getPaperType());
                    vo.setTotalScore(p.getTotalScore());
                    vo.setPassScore(p.getPassScore());
                    vo.setQuestionCount(p.getQuestionCount());
                    vo.setDurationMinutes(p.getDurationMinutes());
                    vo.setPublisherName(publisherMap.getOrDefault(p.getPublisherId(), ""));
                    vo.setStatus(r.getStatus());
                    vo.setScore(r.getScore());
                    vo.setIsPass(r.getIsPass());
                    vo.setSubmitTime(r.getSubmitTime());
                    vo.setReviewTime(r.getReviewTime());
                    return vo;
                })
                .collect(Collectors.toList());
    }

    @Override
    public IPage<PaperDetailVO> listPapers(Long projectId, String name, Integer status, Integer page, Integer size) {
        LambdaQueryWrapper<Paper> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Paper::getProjectId, projectId)
                .eq(Paper::getDeleted, 0);

        if (name != null && !name.isBlank()) {
            wrapper.like(Paper::getTitle, name);
        }
        if (status != null) {
            wrapper.eq(Paper::getStatus, status);
        }

        wrapper.orderByDesc(Paper::getCreatedAt);

        IPage<Paper> paperPage = paperMapper.selectPage(new Page<>(page, size), wrapper);

        return paperPage.convert(this::toPaperDetailVO);
    }

    @Override
    public PaperDetailVO getPaperDetail(Long paperId) {
        Paper paper = paperMapper.selectById(paperId);
        if (paper == null) {
            throw BusinessException.notFound("试卷不存在");
        }
        return toPaperDetailVO(paper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addQuestion(Long paperId, QuestionCreateDTO dto, Long creatorId) {
        Paper paper = paperMapper.selectById(paperId);
        if (paper == null) {
            throw BusinessException.notFound("试卷不存在");
        }
        if (paper.getStatus() != 0) {
            throw BusinessException.badRequest("只能向草稿试卷添加题目");
        }

        Question question = new Question();
        question.setPaperId(paperId);
        question.setTitle(dto.getTitle());
        question.setQuestionType(dto.getQuestionType());
        question.setScore(dto.getScore());
        question.setIsRequired(dto.getIsRequired() != null ? dto.getIsRequired() : 1);
        question.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : getNextSortOrder(paperId));
        question.setAnalysis(dto.getAnalysis());
        questionMapper.insert(question);

        if (dto.getOptions() != null && !dto.getOptions().isEmpty()) {
            for (OptionCreateDTO optionDTO : dto.getOptions()) {
                Option option = new Option();
                option.setQuestionId(question.getId());
                option.setPaperId(paperId);
                option.setOptionLabel(optionDTO.getOptionLabel());
                option.setOptionContent(optionDTO.getOptionContent());
                option.setIsCorrect(optionDTO.getIsCorrect() != null ? optionDTO.getIsCorrect() : 0);
                option.setSortOrder(optionDTO.getSortOrder() != null ? optionDTO.getSortOrder() : 0);
                optionMapper.insert(option);
            }
        }

        updateQuestionCount(paperId);

        log.info("添加题目成功: paperId={}, questionId={}, type={}", paperId, question.getId(), dto.getQuestionType());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateQuestion(Long paperId, Long questionId, QuestionCreateDTO dto, Long userId) {
        Paper paper = paperMapper.selectById(paperId);
        if (paper == null) {
            throw BusinessException.notFound("试卷不存在");
        }
        if (paper.getStatus() != 0) {
            throw BusinessException.badRequest("只能编辑草稿试卷的题目");
        }

        Question question = questionMapper.selectById(questionId);
        if (question == null || !question.getPaperId().equals(paperId)) {
            throw BusinessException.notFound("题目不存在");
        }

        question.setTitle(dto.getTitle());
        question.setQuestionType(dto.getQuestionType());
        question.setScore(dto.getScore());
        question.setIsRequired(dto.getIsRequired() != null ? dto.getIsRequired() : 1);
        if (dto.getSortOrder() != null) {
            question.setSortOrder(dto.getSortOrder());
        }
        question.setAnalysis(dto.getAnalysis());
        questionMapper.updateById(question);

        // 删除旧选项，重新插入
        LambdaQueryWrapper<Option> optionWrapper = new LambdaQueryWrapper<>();
        optionWrapper.eq(Option::getQuestionId, questionId);
        optionMapper.delete(optionWrapper);

        if (dto.getOptions() != null && !dto.getOptions().isEmpty()) {
            for (OptionCreateDTO optionDTO : dto.getOptions()) {
                Option option = new Option();
                option.setQuestionId(questionId);
                option.setPaperId(paperId);
                option.setOptionLabel(optionDTO.getOptionLabel());
                option.setOptionContent(optionDTO.getOptionContent());
                option.setIsCorrect(optionDTO.getIsCorrect() != null ? optionDTO.getIsCorrect() : 0);
                option.setSortOrder(optionDTO.getSortOrder() != null ? optionDTO.getSortOrder() : 0);
                optionMapper.insert(option);
            }
        }

        log.info("更新题目成功: paperId={}, questionId={}", paperId, questionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteQuestion(Long paperId, Long questionId, Long userId) {
        Paper paper = paperMapper.selectById(paperId);
        if (paper == null) {
            throw BusinessException.notFound("试卷不存在");
        }
        if (paper.getStatus() != 0) {
            throw BusinessException.badRequest("只能删除草稿试卷的题目");
        }

        Question question = questionMapper.selectById(questionId);
        if (question == null || !question.getPaperId().equals(paperId)) {
            throw BusinessException.notFound("题目不存在");
        }

        LambdaQueryWrapper<Option> optionWrapper = new LambdaQueryWrapper<>();
        optionWrapper.eq(Option::getQuestionId, questionId);
        optionMapper.delete(optionWrapper);

        questionMapper.deleteById(questionId);

        updateQuestionCount(paperId);

        log.info("删除题目成功: paperId={}, questionId={}", paperId, questionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishPaper(Long paperId, Long publisherId) {
        Paper paper = paperMapper.selectById(paperId);
        if (paper == null) {
            throw BusinessException.notFound("试卷不存在");
        }
        if (paper.getStatus() != 0) {
            throw BusinessException.badRequest("试卷已发布，不能重复发布");
        }

        LambdaQueryWrapper<Question> questionWrapper = new LambdaQueryWrapper<>();
        questionWrapper.eq(Question::getPaperId, paperId);
        if (questionMapper.selectCount(questionWrapper) == 0) {
            throw BusinessException.badRequest("试卷没有题目，不能发布");
        }

        paper.setStatus(1);
        paperMapper.updateById(paper);

        LambdaQueryWrapper<ProjectUser> puWrapper = new LambdaQueryWrapper<>();
        puWrapper.eq(ProjectUser::getProjectId, paper.getProjectId());
        List<ProjectUser> projectUsers = projectUserMapper.selectList(puWrapper);

        List<Response> responses = new ArrayList<>();

        // 预加载所有相关学校信息（避免 N+1 查询）
        List<Long> schoolIds = projectUsers.stream()
                .map(pu -> userMapper.selectById(pu.getUserId()))
                .filter(Objects::nonNull)
                .map(User::getSchoolId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, School> schoolMap = Map.of();
        if (!schoolIds.isEmpty()) {
            List<School> schools = schoolMapper.selectBatchIds(schoolIds);
            schoolMap = schools.stream().collect(Collectors.toMap(School::getId, s -> s));
        }

        for (ProjectUser pu : projectUsers) {
            User user = userMapper.selectById(pu.getUserId());
            if (user == null) {
                continue;
            }

            // 只给老师角色（role=3）分发试卷，跳过校长(role=2)和管理员(role=1)
            if (user.getRole() == null || user.getRole() != 3) {
                continue;
            }

            Response response = new Response();
            response.setPaperId(paperId);
            response.setUserId(user.getId());
            response.setStatus(0);

            School school = user.getSchoolId() != null ? schoolMap.get(user.getSchoolId()) : null;
            if (school != null) {
                response.setProvince(school.getProvince());
                response.setCity(school.getCity());
                response.setCounty(school.getCounty());
                response.setSchool(school.getName());
            }

            responses.add(response);
        }

        if (!responses.isEmpty()) {
            for (Response response : responses) {
                responseMapper.insert(response);
            }
        }

        log.info("发布试卷成功: paperId={}, title={}, 分发人数={}", paperId, paper.getTitle(), responses.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closePaper(Long paperId, Long userId) {
        Paper paper = paperMapper.selectById(paperId);
        if (paper == null) {
            throw BusinessException.notFound("试卷不存在");
        }
        if (paper.getStatus() != 1) {
            throw BusinessException.badRequest("只能下线已发布状态的试卷");
        }

        paper.setStatus(2);
        paperMapper.updateById(paper);

        log.info("下线试卷成功: paperId={}", paperId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePaper(Long paperId, Long creatorId) {
        Paper paper = paperMapper.selectById(paperId);
        if (paper == null) {
            throw BusinessException.notFound("试卷不存在");
        }
        if (paper.getStatus() != 0) {
            throw BusinessException.badRequest("只能删除草稿状态的试卷");
        }

        LambdaQueryWrapper<Option> optionWrapper = new LambdaQueryWrapper<>();
        optionWrapper.eq(Option::getPaperId, paperId);
        optionMapper.delete(optionWrapper);

        LambdaQueryWrapper<Question> questionWrapper = new LambdaQueryWrapper<>();
        questionWrapper.eq(Question::getPaperId, paperId);
        questionMapper.delete(questionWrapper);

        paperMapper.deleteById(paperId);

        log.info("删除试卷成功: paperId={}", paperId);
    }

    /**
     * 获取下一个排序号
     */
    private Integer getNextSortOrder(Long paperId) {
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Question::getPaperId, paperId);
        wrapper.orderByDesc(Question::getSortOrder);
        wrapper.last("LIMIT 1");
        Question last = questionMapper.selectOne(wrapper);
        return last != null ? last.getSortOrder() + 1 : 1;
    }

    /**
     * 更新试卷的题目数量
     */
    private void updateQuestionCount(Long paperId) {
        LambdaQueryWrapper<Question> countWrapper = new LambdaQueryWrapper<>();
        countWrapper.eq(Question::getPaperId, paperId);
        Long count = questionMapper.selectCount(countWrapper);
        Paper paper = paperMapper.selectById(paperId);
        if (paper != null) {
            paper.setQuestionCount(count.intValue());
            paperMapper.updateById(paper);
        }
    }

    /**
     * Paper 实体转 VO
     */
    private PaperDetailVO toPaperDetailVO(Paper paper) {
        PaperDetailVO vo = new PaperDetailVO();
        BeanUtil.copyProperties(paper, vo);

        // 填充发布人姓名
        if (paper.getPublisherId() != null) {
            User publisher = userMapper.selectById(paper.getPublisherId());
            if (publisher != null) {
                vo.setPublisherName(publisher.getRealName());
            }
        }

        // 获取题目列表
        LambdaQueryWrapper<Question> questionWrapper = new LambdaQueryWrapper<>();
        questionWrapper.eq(Question::getPaperId, paper.getId());
        questionWrapper.orderByAsc(Question::getSortOrder);
        List<Question> questions = questionMapper.selectList(questionWrapper);

        List<QuestionVO> questionVOs = questions.stream().map(q -> {
            QuestionVO qvo = new QuestionVO();
            BeanUtil.copyProperties(q, qvo);

            LambdaQueryWrapper<Option> optionWrapper = new LambdaQueryWrapper<>();
            optionWrapper.eq(Option::getQuestionId, q.getId());
            optionWrapper.orderByAsc(Option::getSortOrder);
            List<Option> options = optionMapper.selectList(optionWrapper);

            List<OptionVO> optionVOs = options.stream().map(o -> {
                OptionVO ovo = new OptionVO();
                BeanUtil.copyProperties(o, ovo);
                return ovo;
            }).collect(Collectors.toList());

            qvo.setOptions(optionVOs);
            return qvo;
        }).collect(Collectors.toList());

        vo.setQuestions(questionVOs);
        return vo;
    }
}
