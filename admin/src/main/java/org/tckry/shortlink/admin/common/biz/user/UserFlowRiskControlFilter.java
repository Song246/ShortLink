package org.tckry.shortlink.admin.common.biz.user;

import com.alibaba.fastjson2.JSON;
import com.google.common.collect.Lists;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.tckry.shortlink.admin.common.convention.exception.ClientException;
import org.tckry.shortlink.admin.common.convention.result.Results;
import org.tckry.shortlink.admin.config.UserFlowRiskControlConfiguration;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Optional;

import static org.tckry.shortlink.admin.common.convention.errorcode.BaseErrorCode.FLOW_LIMIT_ERROR;

/**
 * 用户操作流量风控过滤器
 * @program: shortlink
 * @description:
 * @author: lydms
 * @create: 2024-01-04 13:35
 **/
@Slf4j
@RequiredArgsConstructor
public class UserFlowRiskControlFilter implements Filter {

    private final StringRedisTemplate stringRedisTemplate;
    private final UserFlowRiskControlConfiguration userFlowRiskControlConfiguration;

    private static final String USER_FLOW_RISK_CONTROL_LUA_SCRIPT_PATH = "lua/user_flow_risk_control.lua";

    /**
    *  用户操作流量风控过滤
    * @Param: [request, response, filterChain]
    * @return: void
    * @Date: 2024/1/4
    */
    @SneakyThrows
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        // 获取到 redis 的脚本,返回类型为Long
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource(USER_FLOW_RISK_CONTROL_LUA_SCRIPT_PATH)));
        redisScript.setResultType(Long.class);  // 设置Long， lua 脚本默认返回long的数据类型
        String username = Optional.ofNullable(UserContext.getUsername()).orElse("other");   // 获取到上一个过滤器设置的用户名上下文
        Long result = null;
        try {
            result = stringRedisTemplate.execute(redisScript, Lists.newArrayList(username), userFlowRiskControlConfiguration.getTimeWindow());  //stringRedisTemplate，参数不用String序列化报错
        } catch (Throwable ex) {    // （过滤器抛异常全局异常拦截器拿不到）
            log.error("执行用户请求流量限制LUA脚本出错", ex);
            returnJson((HttpServletResponse) response, JSON.toJSONString(Results.failure(new ClientException(FLOW_LIMIT_ERROR))));
        }
        if (result == null || result > userFlowRiskControlConfiguration.getMaxAccessCount()) {
            returnJson((HttpServletResponse) response, JSON.toJSONString(Results.failure(new ClientException(FLOW_LIMIT_ERROR))));
        }
        filterChain.doFilter(request, response);
    }


    /**
     * 过滤器有问题把信息返回给前端（过滤器抛异常全局异常拦截器拿不到）
     * @param response
     * @param json
     * @throws Exception
     */
    private void returnJson(HttpServletResponse response, String json) throws Exception {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/html; charset=utf-8");
        try (PrintWriter writer = response.getWriter()) {
            writer.print(json);
        }
    }

}