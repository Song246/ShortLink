package org.tckry.shortlink.project.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 跳转域名白名单配置文件
 * @program: shortlink
 * @description:
 * @author: lydms
 * @create: 2024-01-02 20:34
 **/
@Data
@Component
@ConfigurationProperties(prefix = "short-link.goto-domain.white-list")  // 获取 yml 配置文件中的熟悉值，字段名称一一对应
public class GotoDomainWhiteListConfiguration {

    /**
     * 是否开启跳转原始链接域名白名单验证
     */
    private Boolean enable;

    /**
     * 跳转原始域名白名单网站名称集合
     */
    private String names;

    /**
     * 可跳转的原始链接域名
     */
    private List<String> details;
}