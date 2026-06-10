package com.exam.common.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页响应体 — 分页查询统一返回此格式
 *
 * @param <T> 数据记录类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    /** 当前页数据列表 */
    private List<T> records;

    /** 总记录数 */
    private Long total;

    /** 当前页码 */
    private Integer page;

    /** 每页条数 */
    private Integer size;

    /**
     * 从 MyBatis-Plus 分页对象构建分页响应
     *
     * @param page MyBatis-Plus IPage 分页结果
     * @param <T>  记录类型
     * @return 统一分页响应体
     */
    public static <T> PageResult<T> of(IPage<T> page) {
        return new PageResult<>(
                page.getRecords(),
                page.getTotal(),
                (int) page.getCurrent(),
                (int) page.getSize()
        );
    }
}
