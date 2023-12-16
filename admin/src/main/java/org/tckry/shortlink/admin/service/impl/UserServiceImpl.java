package org.tckry.shortlink.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.tckry.shortlink.admin.common.convention.exception.ClientException;
import org.tckry.shortlink.admin.common.enums.UserErrorCodeEnum;
import org.tckry.shortlink.admin.dao.entity.UserDO;
import org.tckry.shortlink.admin.dao.mapper.UserMapper;
import org.tckry.shortlink.admin.dto.resp.UserRespDTO;
import org.tckry.shortlink.admin.service.UserService;

/**
 * 用户接口实现层
 */
@Service    // 标记是Spring的一个Bean
public class UserServiceImpl extends ServiceImpl<UserMapper, UserDO> implements UserService {
    // 不用再引mapper，ServiceImpl里面注入了
    @Override
    public UserRespDTO getUserByUsername(String username) {
        LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getUsername, username);
        UserDO userDO = baseMapper.selectOne(queryWrapper);// baseMapper 在 ServiceImpl中定义了
        if (userDO==null){
            throw new ClientException(UserErrorCodeEnum.USER_NULL); // 为空交给全局异常拦截器处理
        }
        UserRespDTO result = new UserRespDTO();
        BeanUtils.copyProperties(userDO, result);   // spring框架的BeanUtils 有问题，后面再改
        return result;
    }
}
