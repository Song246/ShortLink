package org.tckry.shortlink.project.common.constant;

/**
 * Redis Key常量类
 * @program: shortlink
 * @description:
 * @author: lydms
 * @create: 2023-12-24 15:32
 **/
public class RedisKeyConstant {

    /**
     * 短链接跳转前缀key  %s后面连接
     */
    public static final String GOTO_SHORT_LINK_KEY = "short-link_goto_%s";

    /**
     * 短链接空值跳转前缀key
     */
    public static final String GOTO_IS_NULL_SHORT_LINK_KEY = "short-link_is_null_goto_%s";

    /**
     * 短链接跳转分布式锁前缀key
     */
    public static final String LOCK_GOTO_SHORT_LINK_KEY = "short-link_lock_goto_%s";

    /**
     * 短链接修改分组 ID　锁前缀ｋｅｙ
     */
    public static final String LOCK_GID_UPDATE_KEY = "short-link_lock_update-gid_%s";

    /**
     * 短链接延迟队列消费统计 key
     */
    public static final String DELAY_QUEUE_STATS_KEY = "short-link_delay_queue:stats";

}
