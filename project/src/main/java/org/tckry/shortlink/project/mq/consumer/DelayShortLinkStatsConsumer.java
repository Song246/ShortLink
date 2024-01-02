package org.tckry.shortlink.project.mq.consumer;

/**
 * @program: shortlink
 * @description:
 * @author: lydms
 * @create: 2024-01-02 17:08
 **/

import lombok.RequiredArgsConstructor;
import org.redisson.api.RBlockingDeque;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import org.tckry.shortlink.project.dto.biz.ShortLinkStatsRecordDTO;
import org.tckry.shortlink.project.service.ShortLinkService;

import java.util.concurrent.Executors;
import java.util.concurrent.locks.LockSupport;

import static org.tckry.shortlink.project.common.constant.RedisKeyConstant.DELAY_QUEUE_STATS_KEY;

/**
 * 延迟记录短链接统计组件，redis实现消息队列
 */
@Component
@RequiredArgsConstructor
public class DelayShortLinkStatsConsumer implements InitializingBean {  // 实现InitializingBean接口，在启动spring的阶段，会初始化一个任务，执行一个线程
    private final RedissonClient redissonClient;
    private final ShortLinkService shortLinkService;

    public void onMessage() {
        Executors.newSingleThreadExecutor(
                        runnable -> {
                            Thread thread = new Thread(runnable);
                            thread.setName("delay_short-link_stats_consumer");
                            thread.setDaemon(Boolean.TRUE);
                            return thread;
                        })
                .execute(() -> {
                    RBlockingDeque<ShortLinkStatsRecordDTO> blockingDeque = redissonClient.getBlockingDeque(DELAY_QUEUE_STATS_KEY);
                    RDelayedQueue<ShortLinkStatsRecordDTO> delayedQueue = redissonClient.getDelayedQueue(blockingDeque);
                    // 死循环去消息队列拿任务
                    for (; ; ) {
                        try {
                            ShortLinkStatsRecordDTO statsRecord = delayedQueue.poll();
                            if (statsRecord != null) {
                                shortLinkService.shortLinkStats(null, null, statsRecord);
                                continue;
                            }
                            // 空数据睡眠500 ms
                            LockSupport.parkUntil(500);
                        } catch (Throwable ignored) {
                        }
                    }
                });
    }

    /**
     * 在 spring启动的时候，把数据onMessage，把消息队列启动
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        onMessage();
    }
}
