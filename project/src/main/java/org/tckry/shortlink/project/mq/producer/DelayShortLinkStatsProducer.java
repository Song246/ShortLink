package org.tckry.shortlink.project.mq.producer;

import cn.hutool.core.lang.UUID;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBlockingDeque;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.tckry.shortlink.project.dto.biz.ShortLinkStatsRecordDTO;

import java.util.concurrent.TimeUnit;

import static org.tckry.shortlink.project.common.constant.RedisKeyConstant.DELAY_QUEUE_STATS_KEY;

/**
 * 延迟消费短链接统计发送者，redis实现消息队列
 * @program: shortlink
 * @description:
 * @author: lydms
 * @create: 2024-01-02 16:30
 **/
@Component
@RequiredArgsConstructor
public class DelayShortLinkStatsProducer {

    private final RedissonClient redissonClient;

    /**
    * 短链接统计时先获取读锁，获取不到读锁就先加入延迟队列
    * @Param: [statsRecord]
    * @return: void
    * @Date: 2024/1/24
    */

    public void send(ShortLinkStatsRecordDTO statsRecord){
        // 延迟队列
        statsRecord.setKeys(UUID.fastUUID().toString());
        RBlockingDeque<ShortLinkStatsRecordDTO> blockingDeque = redissonClient.getBlockingDeque(DELAY_QUEUE_STATS_KEY);
        RDelayedQueue<ShortLinkStatsRecordDTO> delayedQueue = redissonClient.getDelayedQueue(blockingDeque);
        delayedQueue.offer(statsRecord,5, TimeUnit.SECONDS);
    }
}

