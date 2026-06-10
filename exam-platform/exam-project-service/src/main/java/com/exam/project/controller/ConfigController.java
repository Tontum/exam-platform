package com.exam.project.controller;

import com.exam.common.common.Result;
import com.exam.project.model.dto.ConfigUpdateDTO;
import com.exam.project.model.vo.ConfigVO;
import com.exam.project.model.vo.ToolVO;
import com.exam.project.service.ConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 配置控制器 — 管理项目下各角色对工具的配置
 */
@RestController
@RequestMapping("/api/project/{projectId}/config")
@RequiredArgsConstructor
public class ConfigController {

    private final ConfigService configService;

    /**
     * 查询项目下某角色的工具配置列表（管理后台用，返回完整配置）
     *
     * @param projectId 项目 ID
     * @param role      角色：1=管理员、2=校长、3=老师
     */
    @GetMapping
    public Result<List<ConfigVO>> listConfigs(@PathVariable Long projectId,
                                               @RequestParam Integer role) {
        return Result.ok(configService.listConfigs(projectId, role));
    }

    /**
     * 查询项目下某角色可见的工具列表（学员端用，只返回已启用的工具）
     *
     * @param projectId 项目 ID
     * @param role      角色：1=管理员、2=校长、3=老师
     */
    @GetMapping("/tools")
    public Result<List<ToolVO>> listEnabledTools(@PathVariable Long projectId,
                                                  @RequestParam Integer role) {
        return Result.ok(configService.listEnabledTools(projectId, role));
    }

    /**
     * 更新单条配置
     */
    @PutMapping
    public Result<Void> updateConfig(@PathVariable Long projectId,
                                      @Valid @RequestBody ConfigUpdateDTO dto) {
        configService.updateConfig(dto);
        return Result.ok();
    }

    /**
     * 批量更新配置
     */
    @PutMapping("/batch")
    public Result<Void> batchUpdateConfigs(@PathVariable Long projectId,
                                            @Valid @RequestBody List<ConfigUpdateDTO> configs) {
        configService.batchUpdateConfigs(configs);
        return Result.ok();
    }
}
