package org.tckry.shortlink.admin.common.convention.result;

/**
 * @program: shortlink
 * @description:
 * @author: lydms
 * @create: 2023-12-16 16:17
 **/
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * 全局返回对象
 */
@Data
@Accessors(chain = true)
public class Result<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 5679018624309023727L;

    /**
     * 正确返回码
     */
    public static final String SUCCESS_CODE = "0";

    /**
     * 返回码
     */
    private String code;

    /**
     * 返回消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 请求ID
     */
    private String requestId;

    public boolean isSuccess() {    // is 开头会序列化成字段success
        return SUCCESS_CODE.equals(code);
    }
}