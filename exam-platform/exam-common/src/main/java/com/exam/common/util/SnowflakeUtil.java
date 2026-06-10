package com.exam.common.util;

import cn.hutool.core.util.IdUtil;

/**
 * 雪花算法分布式 ID 生成器
 * 基于 Hutool 的 Snowflake 实现，workerId 和 datacenterId 由 Hutool 自动管理
 */
public class SnowflakeUtil {

    private SnowflakeUtil() {
        // 工具类禁止实例化
    }

    /**
     * 生成全局唯一分布式 ID
     *
     * @return 19 位 Long 型唯一 ID
     */
    public static long nextId() {
        return IdUtil.getSnowflakeNextId();
    }

    /**
     * 生成全局唯一分布式 ID（字符串形式）
     *
     * @return ID 字符串
     */
    public static String nextIdStr() {
        return IdUtil.getSnowflakeNextIdStr();
    }
}
