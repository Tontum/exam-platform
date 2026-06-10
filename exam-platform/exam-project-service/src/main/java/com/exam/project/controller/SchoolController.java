package com.exam.project.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.common.common.Result;
import com.exam.common.entity.School;
import com.exam.project.mapper.SchoolMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 学校控制器 — 查询学校列表（用于注册时选择学校）
 */
@RestController
@RequestMapping("/api/school")
@RequiredArgsConstructor
public class SchoolController {

    private final SchoolMapper schoolMapper;

    /**
     * 查询学校列表（支持按省市区筛选）
     * 
     * @param province 省份（可选）
     * @param city     城市（可选）
     * @param county   区县（可选）
     * @return 学校列表
     */
    @GetMapping("/list")
    public Result<List<School>> listSchools(
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String county) {
        
        LambdaQueryWrapper<School> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(School::getStatus, 1); // 只查启用的学校
        
        if (StringUtils.hasText(province)) {
            wrapper.eq(School::getProvince, province);
        }
        if (StringUtils.hasText(city)) {
            wrapper.eq(School::getCity, city);
        }
        if (StringUtils.hasText(county)) {
            wrapper.eq(School::getCounty, county);
        }
        
        wrapper.orderByAsc(School::getName);
        return Result.ok(schoolMapper.selectList(wrapper));
    }

    /**
     * 查询所有省份
     */
    @GetMapping("/provinces")
    public Result<List<String>> listProvinces() {
        List<School> schools = schoolMapper.selectList(
            new LambdaQueryWrapper<School>()
                .select(School::getProvince)
                .groupBy(School::getProvince)
                .orderByAsc(School::getProvince)
        );
        return Result.ok(schools.stream().map(School::getProvince).toList());
    }

    /**
     * 查询指定省下的城市
     */
    @GetMapping("/cities")
    public Result<List<String>> listCities(@RequestParam String province) {
        List<School> schools = schoolMapper.selectList(
            new LambdaQueryWrapper<School>()
                .select(School::getCity)
                .eq(School::getProvince, province)
                .groupBy(School::getCity)
                .orderByAsc(School::getCity)
        );
        return Result.ok(schools.stream().map(School::getCity).toList());
    }

    /**
     * 查询指定市下的区县
     */
    @GetMapping("/counties")
    public Result<List<String>> listCounties(@RequestParam String province, @RequestParam String city) {
        List<School> schools = schoolMapper.selectList(
            new LambdaQueryWrapper<School>()
                .select(School::getCounty)
                .eq(School::getProvince, province)
                .eq(School::getCity, city)
                .groupBy(School::getCounty)
                .orderByAsc(School::getCounty)
        );
        return Result.ok(schools.stream().map(School::getCounty).filter(c -> c != null).toList());
    }

    /**
     * 查询学校详情
     */
    @GetMapping("/{id}")
    public Result<School> getSchool(@PathVariable Long id) {
        School school = schoolMapper.selectById(id);
        if (school == null) {
            return Result.fail(404, "学校不存在");
        }
        return Result.ok(school);
    }
}
