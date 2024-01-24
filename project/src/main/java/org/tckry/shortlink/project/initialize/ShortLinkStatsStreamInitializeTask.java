package org.tckry.shortlink.project.initialize;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import static org.tckry.shortlink.project.common.constant.RedisKeyConstant.SHORT_LINK_STATS_STREAM_GROUP_KEY;
import static org.tckry.shortlink.project.common.constant.RedisKeyConstant.SHORT_LINK_STATS_STREAM_TOPIC_KEY;

/**
 * 初始化短链接监控消息队列消费者组
 * @program: shortlink
 * @description:
 * @author: lydms
 * @create: 2024-01-24 16:05
 **/
@Component
@RequiredArgsConstructor
public class ShortLinkStatsStreamInitializeTask implements InitializingBean {
    private StringRedisTemplate stringRedisTemplate;


    @Override
    public void afterPropertiesSet() throws Exception {
        Boolean hasKey = stringRedisTemplate.hasKey(SHORT_LINK_STATS_STREAM_TOPIC_KEY);
        // 已存在再创建会报错，所以先判断是否存在
        if (hasKey==null || !hasKey) {
            stringRedisTemplate.opsForStream().createGroup(SHORT_LINK_STATS_STREAM_TOPIC_KEY,SHORT_LINK_STATS_STREAM_GROUP_KEY);
        }

    }
}
