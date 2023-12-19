package org.tckry.shortlink.admin.dto.req;

import lombok.Data;

/**
 * 用户登录请求参数
 **/
@Data
public class UserLoginReqDTO {

    /**
    * 用户名
    */
    private String  username;

    /**
     * 密码
     */
    private String password;

    // 一般还有一个验证码，这个项目验证码不是重点，所以不需要

}
