package org.tckry.shortlink.project.mq.producer;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 短链接监控状态保存消息队列生产者
 * @program: shortlink
 * @description:
 * @author: lydms
 * @create: 2024-01-21 16:37
 **/
@Component
@RequiredArgsConstructor
public class ShortLinkStatsSaveProducer {

    private final StringRedisTemplate stringRedisTemplate;

    @Value("short_link:stats-stream")
    private String topic;

    /**
    * 发送延迟消费短链接统计
    * @Param: [producerMap]
    * @return: void
    * @Date: 2024/1/21
    */
    public void send(Map<String, String> producerMap) {
        // 向指定topic发送消息
        stringRedisTemplate.opsForStream().add(topic,producerMap);
    }
}
