package org.tckry.shortlink.admin.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 用户操作流量风控配置文件
 * @program: shortlink
 * @description:
 * @author: lydms
 * @create: 2024-01-04 15:00
 **/
@Data
@Component
@ConfigurationProperties(prefix = "short-link.flow-limit")
public class UserFlowRiskControlConfiguration {

    /**
     * 是否开启用户流量风控验证
     */
    private Boolean enable;

    /**
     * 流量风控时间窗口，单位：秒
     */
    private String timeWindow;

    /**
     * 流量风控时间窗口内可访问次数
     */
    private Long maxAccessCount;
}