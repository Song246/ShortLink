package org.tckry.shortlink.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.tckry.shortlink.admin.dao.entity.UserDO;
import org.tckry.shortlink.admin.dto.req.UserLoginReqDTO;
import org.tckry.shortlink.admin.dto.req.UserRegisterReqDTO;
import org.tckry.shortlink.admin.dto.req.UserUpdateReqDTO;
import org.tckry.shortlink.admin.dto.resp.UserLoginRespDTO;
import org.tckry.shortlink.admin.dto.resp.UserRespDTO;

/**
 * 用户接口层
 */
public interface UserService extends IService<UserDO> {

    /** 
    * 根据用户名查询用户信息
    * @Param: [username] 用户名
    * @return: org.tckry.shortlink.admin.dto.resp.UserRespDTO 返回实体
    * @Date: 2023/12/16
    */
    UserRespDTO getUserByUsername(String username);

    /**
    * 查询用户名是否存在
    * @Param: [username] 用户名
    * @return: java.lang.Boolean 用户名存在返回True，不存在false
    * @Date: 2023/12/16
    */
    Boolean hasUsername(String username);


    /** 
    * 注册用户
    * @Param: [requestParam] 注册用户请求参数
    * @return: void
    * @Date: 2023/12/17
    */
    void register(UserRegisterReqDTO requestParam);

    /** 
    * 根据用户名修改用户
    * @Param: [requestParam]
    * @return: void
    * @Date: 2023/12/18
    */
    void update(UserUpdateReqDTO requestParam);

    /**
    * 用户登录
    * @Param: [requestParam]
    * @return: org.tckry.shortlink.admin.dto.resp.UserLoginRespDTO
    * @Date: 2023/12/18
    */
    UserLoginRespDTO login(UserLoginReqDTO requestParam);

    /**
    * 检查用户登录
    * @Param: [token] 用户登录token
    * @return: java.lang.Boolean
    * @Date: 2023/12/18
    */

    Boolean checkLogin(String username, String token);
}
