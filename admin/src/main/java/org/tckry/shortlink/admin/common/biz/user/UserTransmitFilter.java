package org.tckry.shortlink.admin.common.biz.user;
import com.alibaba.fastjson2.JSON;
import com.google.common.collect.Lists;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * 用户信息传输过滤器
 *
 */
@RequiredArgsConstructor
public class UserTransmitFilter implements Filter {
    private final StringRedisTemplate stringRedisTemplate;
    private static final List<String> IGNORE_URI = Lists.newArrayList("/api/short-link/admin/v1/user/login","/api/short-link/admin/v1/user/has-username");
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        String requestURI = httpServletRequest.getRequestURI(); // url 加上路径域名的整体， uri只含后面部分/api/short-link/admin/v1/user/login
        if (!Objects.equals(requestURI,"/api/short-link/v1/user/login")){
            String username = httpServletRequest.getHeader("username");
            String token = httpServletRequest.getHeader("token");
            Object userInfoJsonStr = stringRedisTemplate.opsForHash().get("login_" + username, token);
            if (userInfoJsonStr != null) {
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
}