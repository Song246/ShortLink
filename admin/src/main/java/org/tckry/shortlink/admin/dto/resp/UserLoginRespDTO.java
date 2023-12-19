package org.tckry.shortlink.admin.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 返回用户登录参数响应
 */
@Data
@AllArgsConstructor
public class UserLoginRespDTO {
    /*
    * 用户token
    * */
    private String token;
}
