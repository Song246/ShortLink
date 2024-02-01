package org.tckry.shortlink.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient  // nacos注册中心
@EnableFeignClients("org.tckry.shortlink.admin.remote")
@MapperScan("org.tckry.shortlink.admin.dao.mapper") // 扫描mapper包，最好具体到精确包，减少无用扫描开销
public class ShortLinkAdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(ShortLinkAdminApplication.class,args);
    }
}
