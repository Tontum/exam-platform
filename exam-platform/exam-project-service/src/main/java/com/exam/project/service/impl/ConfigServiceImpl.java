package com.exam.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.common.common.BusinessException;
import com.exam.common.entity.Config;
import com.exam.common.entity.Tool;
import com.exam.project.mapper.ConfigMapper;
import com.exam.project.mapper.ToolMapper;
import com.exam.project.model.dto.ConfigUpdateDTO;
import com.exam.project.model.vo.ConfigVO;
import com.exam.project.model.vo.ToolVO;
import com.exam.project.service.ConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 配置服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigServiceImpl implements ConfigService {

    private final ConfigMapper configMapper;
    private final ToolMapper toolMapper;

    @Override
    public List<ConfigVO> listConfigs(Long projectId, Integer role) {
        // 查询配置
        LambdaQueryWrapper<Config> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Config::getProjectId, projectId)
                .eq(Config::getRole, role);
        List<Config> configs = configMapper.selectList(wrapper);

        // 查询工具名称映射
        List<Tool> tools = toolMapper.selectList(null);
        Map<Long, Tool> toolMap = tools.stream()
                .collect(Collectors.toMap(Tool::getId, t -> t));

        // 组装 VO
        return configs.stream().map(config -> {
            ConfigVO vo = new ConfigVO();
            vo.setId(config.getId());
            vo.setToolId(config.getToolId());
            vo.setRole(config.getRole());
            vo.setIsEnabled(config.getIsEnabled());
            vo.setAllowPublish(config.getAllowPublish());
            vo.setAllowDelete(config.getAllowDelete());
            vo.setAllowReview(config.getAllowReview());
            vo.setRequirePassScore(config.getRequirePassScore());
            vo.setAutoScore(config.getAutoScore());
            vo.setPublishTimeStart(config.getPublishTimeStart());
            vo.setPublishTimeEnd(config.getPublishTimeEnd());
            vo.setScorePerSubmit(config.getScorePerSubmit());

            // 填充工具信息
            Tool tool = toolMap.get(config.getToolId());
            if (tool != null) {
                vo.setToolCode(tool.getToolCode());
                vo.setToolName(tool.getToolName());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<ToolVO> listEnabledTools(Long projectId, Integer role) {
        // 查询该角色在该项目下已启用的配置
        LambdaQueryWrapper<Config> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Config::getProjectId, projectId)
                .eq(Config::getRole, role)
                .eq(Config::getIsEnabled, 1);
        List<Config> enabledConfigs = configMapper.selectList(wrapper);

        // 查询所有工具
        List<Tool> tools = toolMapper.selectList(null);
        Map<Long, Tool> toolMap = tools.stream()
                .collect(Collectors.toMap(Tool::getId, t -> t));

        // 组装 VO（只返回已启用的工具）
        return enabledConfigs.stream().map(config -> {
            ToolVO vo = new ToolVO();
            Tool tool = toolMap.get(config.getToolId());
            if (tool != null) {
                vo.setCode(tool.getToolCode());
                vo.setName(tool.getToolName());
                vo.setDescription(tool.getDescription());
            }
            vo.setIsEnabled(true);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateConfig(ConfigUpdateDTO dto) {
        Config config = configMapper.selectById(dto.getId());
        if (config == null) {
            throw BusinessException.notFound("配置不存在");
        }

        // 只更新传入的非空字段
        if (dto.getIsEnabled() != null) {
            config.setIsEnabled(dto.getIsEnabled());
        }
        if (dto.getAllowPublish() != null) {
            config.setAllowPublish(dto.getAllowPublish());
        }
        if (dto.getAllowDelete() != null) {
            config.setAllowDelete(dto.getAllowDelete());
        }
        if (dto.getAllowReview() != null) {
            config.setAllowReview(dto.getAllowReview());
        }
        if (dto.getRequirePassScore() != null) {
            config.setRequirePassScore(dto.getRequirePassScore());
        }
        if (dto.getAutoScore() != null) {
            config.setAutoScore(dto.getAutoScore());
        }
        if (dto.getPublishTimeStart() != null) {
            config.setPublishTimeStart(dto.getPublishTimeStart());
        }
        if (dto.getPublishTimeEnd() != null) {
            config.setPublishTimeEnd(dto.getPublishTimeEnd());
        }
        if (dto.getScorePerSubmit() != null) {
            config.setScorePerSubmit(dto.getScorePerSubmit());
        }

        configMapper.updateById(config);
        log.info("更新配置成功: id={}", dto.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateConfigs(List<ConfigUpdateDTO> configs) {
        for (ConfigUpdateDTO dto : configs) {
            updateConfig(dto);
        }
    }
}
