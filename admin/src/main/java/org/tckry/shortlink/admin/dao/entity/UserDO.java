package org.tckry.shortlink.admin.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.tckry.shortlink.admin.common.database.BaseDO;


/**
 * 用户持久层实体,DO 不能往前端返
 * @program: shortlink
 * @description:
 * @author: lydms
 * @create: 2023-12-19 15:08
 **/
@Data
@TableName(value = "t_user")
public class UserDO extends BaseDO {


    /**
     * id
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮箱
     */
    private String mail;

    /**
     * 注销时间戳
     */
    private Long deletionTime;


}
