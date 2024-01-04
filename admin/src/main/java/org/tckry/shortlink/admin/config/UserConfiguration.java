package org.tckry.shortlink.admin.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.tckry.shortlink.admin.common.biz.user.UserFlowRiskControlFilter;
import org.tckry.shortlink.admin.common.biz.user.UserTransmitFilter;

/**
 * 用户配置自动装配，Bean注册相关过滤器
 */
@Configuration
public class UserConfiguration {

    /**
     * 用户信息传递过滤器，Bean注册过滤器
     * UserTransmitFilter对象说明
     */
    @Bean
    public FilterRegistrationBean<UserTransmitFilter> globalUserTransmitFilter(StringRedisTemplate stringRedisTemplate) {
        FilterRegistrationBean<UserTransmitFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new UserTransmitFilter(stringRedisTemplate));
        registration.addUrlPatterns("/*");
        registration.setOrder(0);   // 多个过滤器，通过顺序进行先后处理，
        return registration;
    }

    /**
     * 用户操作流量风控过滤器，Bean注册过滤器
     */
    @Bean
    @ConditionalOnProperty(name = "short-link.flow-limit.enable", havingValue = "true") // 条件注解判断，如果enable为true才去进行注册流量风控过滤器
    public FilterRegistrationBean<UserFlowRiskControlFilter> globalUserFlowRiskControlFilter(
            StringRedisTemplate stringRedisTemplate,
            UserFlowRiskControlConfiguration userFlowRiskControlConfiguration) {
        FilterRegistrationBean<UserFlowRiskControlFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new UserFlowRiskControlFilter(stringRedisTemplate, userFlowRiskControlConfiguration));
        registration.addUrlPatterns("/*");
        registration.setOrder(10);  // 多个过滤器，通过顺序进行先后处理，跨度为10方便中间扩充
        return registration;
    }
}
