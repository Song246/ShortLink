package org.tckry.shortlink.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.tckry.shortlink.admin.dao.entity.UserDO;
import org.tckry.shortlink.admin.dto.req.UserRegisterReqDTO;
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
}
