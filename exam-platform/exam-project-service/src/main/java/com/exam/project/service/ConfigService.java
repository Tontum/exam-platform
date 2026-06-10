package com.exam.project.service;

import com.exam.project.model.dto.ConfigUpdateDTO;
import com.exam.project.model.vo.ConfigVO;
import com.exam.project.model.vo.ToolVO;

import java.util.List;

/**
 * 配置服务接口 — 管理项目下各角色对工具的配置
 */
public interface ConfigService {

    /**
     * 查询项目下某角色的工具配置列表（管理后台用，返回完整配置）
     *
     * @param projectId 项目 ID
     * @param role      角色：1=管理员、2=校长、3=老师
     * @return 配置列表（含工具名称信息）
     */
    List<ConfigVO> listConfigs(Long projectId, Integer role);

    /**
     * 查询项目下某角色可见的工具列表（学员端用，只返回已启用的工具）
     *
     * @param projectId 项目 ID
     * @param role      角色：1=管理员、2=校长、3=老师
     * @return 工具列表（只包含 is_enabled=1 的工具）
     */
    List<ToolVO> listEnabledTools(Long projectId, Integer role);

    /**
     * 更新单条配置
     *
     * @param dto 配置内容
     */
    void updateConfig(ConfigUpdateDTO dto);

    /**
     * 批量更新配置
     *
     * @param configs 配置列表
     */
    void batchUpdateConfigs(List<ConfigUpdateDTO> configs);
}
