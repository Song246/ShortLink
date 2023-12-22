package org.tckry.shortlink.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.crypto.SecureUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.tckry.shortlink.admin.common.biz.user.UserContext;
import org.tckry.shortlink.admin.common.convention.exception.ClientException;
import org.tckry.shortlink.admin.common.enums.UserErrorCodeEnum;
import org.tckry.shortlink.admin.dao.entity.UserDO;
import org.tckry.shortlink.admin.dao.mapper.UserMapper;
import org.tckry.shortlink.admin.dto.req.UserLoginReqDTO;
import org.tckry.shortlink.admin.dto.req.UserRegisterReqDTO;
import org.tckry.shortlink.admin.dto.req.UserUpdateReqDTO;
import org.tckry.shortlink.admin.dto.resp.UserLoginRespDTO;
import org.tckry.shortlink.admin.dto.resp.UserRespDTO;
import org.tckry.shortlink.admin.service.GroupService;
import org.tckry.shortlink.admin.service.UserService;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.tckry.shortlink.admin.common.constant.RedisCacheConstant.LOCK_USER_REGISTER_KEY;
import static org.tckry.shortlink.admin.common.enums.UserErrorCodeEnum.*;

/**
 * 用户接口实现层
 */
@Service    // 标记是Spring的一个Bean
@RequiredArgsConstructor    // 构造器方式注入相关资源，下面private final ...
public class UserServiceImpl extends ServiceImpl<UserMapper, UserDO> implements UserService {
    // 不用再引mapper，ServiceImpl里面注入了

    private final RBloomFilter<String> userRegisterCachePenetrationBloomFilter;
    private final RedissonClient redissonClient;
    private final StringRedisTemplate stringRedisTemplate;  // redis 依赖
    private final GroupService groupService;

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

        // redissonClient 分布式锁机制防止同一时间多个未注册的name去请求数据库，防止恶意请求毫秒级触发大量请求去一个未注册的用户名
        RLock lock = redissonClient.getLock(LOCK_USER_REGISTER_KEY+requestParam.getUsername()); // 拼一个username，不然成全局锁
        try {
            if(lock.tryLock()) {    // 不用lock.lock（），使用tryLock（）；lock会一直等待上一个锁释放；tryLock只要有一个获取到就认为成功

                try {
                    int inserted = baseMapper.insert(BeanUtil.toBean(requestParam, UserDO.class));  //  BeanUtil.toBean方法,此处并不是传Bean对象,而是Bean类,Hutool会自动调用默认构造方法创建对象
                    if (inserted < 1){
                        throw new ClientException(USER_SAVE_ERROR); // 一般业务层用save，持久层采用insert
                    }
                }catch (DuplicateKeyException ex){  // 索引重复
                    throw new ClientException(USER_EXIST); // 一般业务层用save，持久层采用insert
                }

                userRegisterCachePenetrationBloomFilter.add(requestParam.getUsername());    // 新数据插入布隆过滤器，保证布隆过滤器和数据库一致
                // 注册用户后创建默认分组
                groupService.saveGroup(requestParam.getUsername(),"默认分组");
                return;
            }else {
                throw new ClientException(UserErrorCodeEnum.USER_NAME_EXIST);
            }
        } finally {
            lock.unlock();
        }



    }

    @Override
    public void update(UserUpdateReqDTO requestParam) {
        // TODO 验证当前用户名是否为登录用户，当前用户的token只能修改当前用户，避免张三账号知道李四接口，去修改李四；后面依赖网关，这里先TODO
        LambdaUpdateWrapper<UserDO> updateWrapper = Wrappers.lambdaUpdate(UserDO.class)
                .eq(UserDO::getUsername, requestParam.getUsername());
        baseMapper.update(BeanUtil.toBean(requestParam,UserDO.class),updateWrapper);
    }

    @Override
    public UserLoginRespDTO login(UserLoginReqDTO requestParam) {
        // 1、验证用户名和密码是否存在数据库
        LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getUsername, requestParam.getUsername())
                .eq(UserDO::getPassword, requestParam.getPassword())
                .eq(UserDO::getDelFlag, 0);// 未注销的用户，数据库软注销
        UserDO userDO = baseMapper.selectOne(queryWrapper);
        if (userDO==null) {
            throw new ClientException("用户不存在");
        }
        Boolean hasLogin = stringRedisTemplate.hasKey("login_"+requestParam.getUsername());
        if (hasLogin!=null&& hasLogin){
            throw new ClientException("用户已登录");
        }
        /*
        * Hash
        * key: login_用户名
        * Value:
        *   key: token标识(生成的uuid)
        *   value: JSON 字符串（用户信息）
        * */
        String uuid = UUID.randomUUID().toString();
        stringRedisTemplate.opsForHash().put("login_"+requestParam.getUsername(),uuid,JSON.toJSONString(userDO));
        // 设置过期时间30分钟
        stringRedisTemplate.expire("login_"+requestParam.getUsername(),30L,TimeUnit.DAYS);
        return new UserLoginRespDTO(uuid);
    }

    /** 
    * 检查用户是否登录
    * @Param: [token]
    * @return: java.lang.Boolean
    * @Date: 2023/12/18
    */
    @Override
    public Boolean checkLogin(String username, String token) {
        return stringRedisTemplate.opsForHash().get("login_" + username, token)!=null;
    }

    @Override
    public void logout(String username, String token) {
        if (checkLogin(username, token)){
            stringRedisTemplate.delete("login_"+username);
            return;
        }else {
            throw new ClientException("用户tokne不存在或者未登录");
        }

    }
}
