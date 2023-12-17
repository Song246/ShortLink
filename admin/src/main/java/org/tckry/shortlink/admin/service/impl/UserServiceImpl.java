package org.tckry.shortlink.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBloomFilter;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.tckry.shortlink.admin.common.convention.exception.ClientException;
import org.tckry.shortlink.admin.common.enums.UserErrorCodeEnum;
import org.tckry.shortlink.admin.dao.entity.UserDO;
import org.tckry.shortlink.admin.dao.mapper.UserMapper;
import org.tckry.shortlink.admin.dto.req.UserRegisterReqDTO;
import org.tckry.shortlink.admin.dto.resp.UserRespDTO;
import org.tckry.shortlink.admin.service.UserService;

import static org.tckry.shortlink.admin.common.enums.UserErrorCodeEnum.USER_NAME_EXIST;
import static org.tckry.shortlink.admin.common.enums.UserErrorCodeEnum.USER_SAVE_ERROR;

/**
 * 用户接口实现层
 */
@Service    // 标记是Spring的一个Bean
@RequiredArgsConstructor    // 构造器方式注入相关资源，下面private final ...
public class UserServiceImpl extends ServiceImpl<UserMapper, UserDO> implements UserService {
    // 不用再引mapper，ServiceImpl里面注入了

    private final RBloomFilter<String> userRegisterCachePenetrationBloomFilter;

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

    @Override
    public Boolean hasUsername(String username) {
//        LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.lambdaQuery(UserDO.class)
//                .eq(UserDO::getUsername, username);
//        UserDO userDO = baseMapper.selectOne(queryWrapper);
//        return userDO==null?false:true;

        // 将上面内容改为布隆过滤器；将注册的用户放入布隆过滤器
        return !userRegisterCachePenetrationBloomFilter.contains(username);
    }

    @Override
    public void register(UserRegisterReqDTO requestParam) {
        // 数据库再加一层兜底，username唯一索引，避免前面redis的错误
        if (!hasUsername(requestParam.getUsername())) {
            throw new ClientException(USER_NAME_EXIST);
        }
        int inserted = baseMapper.insert(BeanUtil.toBean(requestParam, UserDO.class));  //  BeanUtil.toBean方法,此处并不是传Bean对象,而是Bean类,Hutool会自动调用默认构造方法创建对象
        if (inserted < 1){
            throw new ClientException(USER_SAVE_ERROR); // 一般业务层用save，持久层采用insert
        }
        userRegisterCachePenetrationBloomFilter.add(requestParam.getUsername());    // 新数据插入布隆过滤器，保证布隆过滤器和数据库一致

    }
}
