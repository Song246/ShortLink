package org.tckry.shortlink.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.tckry.shortlink.admin.dao.entity.UserDO;
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
}
