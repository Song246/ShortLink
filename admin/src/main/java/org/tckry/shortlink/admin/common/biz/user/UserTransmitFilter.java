package org.tckry.shortlink.admin.common.biz.user;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.google.common.collect.Lists;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.tckry.shortlink.admin.common.convention.exception.ClientException;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Objects;

import static org.tckry.shortlink.admin.common.enums.UserErrorCodeEnum.USER_TOKEN_FAIL;

/**
 * 用户信息传输过滤器
 *
 */
@RequiredArgsConstructor
public class UserTransmitFilter implements Filter {
    private final StringRedisTemplate stringRedisTemplate;
    private static final List<String> IGNORE_URI = Lists.newArrayList("/api/short-link/admin/v1/user/login"
            , "/api/short-link/admin/v1/user/has-username");
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        String requestURI = httpServletRequest.getRequestURI(); // url 加上路径域名的整体， uri只含后面部分/api/short-link/admin/v1/user/login
        if (!IGNORE_URI.contains(requestURI)){
            String method = httpServletRequest.getMethod();
            // restful 风格，/api/short-link/admin/v1/user在增加、修改、删除都用到了，只有POST是注册不需要去验证
            if (!(Objects.equals(requestURI,"/api/short-link/admin/v1/user")&&Objects.equals(method, "POST"))){
                String username = httpServletRequest.getHeader("username");
                String token = httpServletRequest.getHeader("token");
                // 这里要使用isAllNotBlank，概述比较全部，isEmpty不全面
                if (!StrUtil.isAllNotBlank(username,token)) { // username、token万一为空
                    // throw new ClientException(USER_TOKEN_FAIL); 直接return捕获不到错误？？
                    returnJson((HttpServletResponse) servletResponse,JSON.toJSONString(new ClientException(USER_TOKEN_FAIL)));
                    return;
                }
                Object userInfoJsonStr;
                try {
                    userInfoJsonStr = stringRedisTemplate.opsForHash().get("login_" + username, token);
                    if (userInfoJsonStr == null) {
                        // throw new ClientException(USER_TOKEN_FAIL); 直接return捕获不到错误？？
                        returnJson((HttpServletResponse) servletResponse,JSON.toJSONString(new ClientException(USER_TOKEN_FAIL)));
                        return;
                    }
                }catch (Exception ex){
                    // throw new ClientException(USER_TOKEN_FAIL); 直接return捕获不到错误？？
                    returnJson((HttpServletResponse) servletResponse,JSON.toJSONString(new ClientException(USER_TOKEN_FAIL)));
                    return;
                }

                UserInfoDTO userInfoDTO = JSON.parseObject(userInfoJsonStr.toString(), UserInfoDTO.class);
                UserContext.setUser(userInfoDTO);   // 将当前对象放到上下文

            }

        }
        try {
            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            UserContext.removeUser();
        }
    }

    public void returnJson(HttpServletResponse response,String json) throws IOException {
        PrintWriter writer = null;
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/html;charset=UTF-8");
        try{
            writer = response.getWriter();
            writer.print(json);
        }catch (IOException e){
        }finally {
            if (writer != null)writer.close();
        }

    }
}