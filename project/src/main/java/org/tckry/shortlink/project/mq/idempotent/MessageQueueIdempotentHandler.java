package org.tckry.shortlink.project.mq.idempotent;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 消息队列幂等处理器
 * @program: shortlink
 * @description:
 * @author: lydms
 * @create: 2024-01-21 16:18
 **/
@Component
@RequiredArgsConstructor
public class MessageQueueIdempotentHandler {

    private final StringRedisTemplate stringRedisTemplate;
    private final String IDEMPOTENT_KEY_PREFIX = "short-link:idempotent";


    /**
    * 判断当前消息是否消费过，
    * @Param: [messageId] 消息唯一标识
    * @return: boolean 消息是否消费过
    * @Date: 2024/1/21
    */
    public boolean isMessageProcessed(String messageId) {
        String key = IDEMPOTENT_KEY_PREFIX + messageId;
        // 存在就返回false，不存在才设置k、v     使用 Boolean.TRUE.equals( 是因为返回boolean内部拆包可能存在空指针
        // 不存在设置预占标识，val设置值
        // 2 分钟原因：幂等的概率低，有效时间设置久了redis存储的压力就大了      2分钟经验
        return Boolean.TRUE.equals(stringRedisTemplate.opsForValue().setIfAbsent(key,"0",2, TimeUnit.MINUTES));    // 0代表执行中，1代表已完成
    }
    
    /** 
    * 判断消息的消费流程是否执行完成
    * @Param: [messageId]
    * @return: boolean
    * @Date: 2024/1/23
    */
    public boolean isAccomplish(String messageId) {
        String key = IDEMPOTENT_KEY_PREFIX + messageId;
        return Objects.equals(stringRedisTemplate.opsForValue().get(key),"1");
    }

    /**
    * 设置消息执行流程完成
    * @Param: [messageId]
    * @return: void
    * @Date: 2024/1/23
    */
    public void setAccomplish(String messageId) {
        String key = IDEMPOTENT_KEY_PREFIX + messageId;
        stringRedisTemplate.opsForValue().set(key, "1",2,TimeUnit.MINUTES);
    }

    /** 
    * 如果消息处理遇到异常情况，删除消息队列中的预占标识（幂等标识）
    * @Param: [messageId]
    * @return: void
    * @Date: 2024/1/21
    */
    public void delMessageProcessed(String messageId) {
        String key = IDEMPOTENT_KEY_PREFIX + messageId;
        stringRedisTemplate.delete(key);
    }
}
