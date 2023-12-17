package org.tckry.shortlink.admin.dto.resp;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;
import org.tckry.shortlink.admin.common.serialize.PhoneDesensitizationSerializer;

/**
 * 返回无脱敏用户参数响应
 */
@Data
public class UserActualRespDTO {

    /**
     * id
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 手机号
     */
//    @JsonSerialize(using = PhoneDesensitizationSerializer.class)    // 在对PhoneDesensitizationSerializer这个对象序列化的时候去读取这个字段进行反解析处理，脱敏
    private String phone;

    /**
     * 邮箱
     */
    private String mail;
}
