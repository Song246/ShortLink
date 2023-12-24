package org.tckry.shortlink.project.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.text.StrBuilder;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tckry.shortlink.project.common.convention.exception.ClientException;
import org.tckry.shortlink.project.common.convention.exception.ServiceException;
import org.tckry.shortlink.project.common.database.BaseDO;
import org.tckry.shortlink.project.common.enums.ValiDateTypeEnum;
import org.tckry.shortlink.project.dao.entity.ShortLinkDO;
import org.tckry.shortlink.project.dao.entity.ShortLinkGotoDO;
import org.tckry.shortlink.project.dao.mapper.ShortLinkGotoMapper;
import org.tckry.shortlink.project.dao.mapper.ShortLinkMapper;
import org.tckry.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import org.tckry.shortlink.project.dto.req.ShortLinkPageReqDTO;
import org.tckry.shortlink.project.dto.req.ShortLinkUpdateReqDTO;
import org.tckry.shortlink.project.dto.resp.ShortLinkCreateRespDTO;
import org.tckry.shortlink.project.dto.resp.ShortLinkGroupCountQueryRespDTO;
import org.tckry.shortlink.project.dto.resp.ShortLinkPageRespDTO;
import org.tckry.shortlink.project.service.ShortLinkService;
import org.tckry.shortlink.project.toolkit.HashUtil;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.tckry.shortlink.project.common.constant.RedisKeyConstant.*;

/**
 * 短链接接口实现层
 * @program: shortlink
 * @description:
 * @author: lydms
 * @create: 2023-12-20 16:38
 **/
@Slf4j
@Service
@RequiredArgsConstructor
public class ShortLinkServiceImpl extends ServiceImpl<ShortLinkMapper, ShortLinkDO> implements ShortLinkService {

    private final RBloomFilter<String> shortUriCreateCachePenetrationBloomFilter;
    private final ShortLinkGotoMapper shortLinkGotoMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;

    /**
    * 创建短链接
    * @Param: [requestParam]
    * @return: org.tckry.shortlink.project.dto.resp.ShortLinkCreateRespDTO
    * @Date: 2023/12/20
    */
    @Override
    public ShortLinkCreateRespDTO createShortLink(ShortLinkCreateReqDTO requestParam) {
        // 1、根据原始连接生成短链接的后缀
        String shortLinkSuffix = generateSuffix(requestParam);
        String fullShortUrl = StrBuilder.create(requestParam.getDomain()).append("/").append(shortLinkSuffix).toString();
        // String fullShortUrl = requestParam.getDomain()+"/" + shortLinkSuffix;
        ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                .domain(requestParam.getDomain())
                .originUrl(requestParam.getOriginUrl())
                .gid(requestParam.getGid())
                .createdType(requestParam.getCreatedType())
                .validDateType(requestParam.getValidDateType())
                .validDate(requestParam.getValidDate())
                .describe(requestParam.getDescribe())
                .shortUri(shortLinkSuffix)
                .enableStatus(0)
                .fullShortUrl(fullShortUrl)
                .build();
        ShortLinkGotoDO shortLinkGotoDO = ShortLinkGotoDO.builder()
                .fullShortUrl(fullShortUrl)
                .gid(requestParam.getGid()).build();

        // 布隆过滤器存在误判，数据库设置唯一索引作为兜底策略，如果插入误判的值，会报索引重复的错误
        try {
            baseMapper.insert(shortLinkDO);
            // 创建短链接的时候去路由表中创建完整短链接到GID的映射记录；
            shortLinkGotoMapper.insert(shortLinkGotoDO);

        }catch (DuplicateKeyException ex){
            // 已经误判的短链接如何处理？ 去数据库查询一次，看是否真的存在
            // 布隆过滤器误判的概率低，在误判的情况下去攻击数据库的概率更低
            LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                    .eq(ShortLinkDO::getFullShortUrl, fullShortUrl);

            ShortLinkDO hasShortLinkDO = baseMapper.selectOne(queryWrapper);
            if (hasShortLinkDO!=null){  // 确实数据库存在，没有误判
                log.warn("短链接：{}重复入库",fullShortUrl);
                throw new ServiceException("短链接重复生成");
            }
        }
        shortUriCreateCachePenetrationBloomFilter.add(fullShortUrl);
        return ShortLinkCreateRespDTO.builder()
                .gid(requestParam.getGid())
                .originUrl(requestParam.getOriginUrl())
                .fullShortUrl("http://"+shortLinkDO.getFullShortUrl()).build();
    }

    @Override
    public IPage<ShortLinkPageRespDTO> pageShortLink(ShortLinkPageReqDTO requestParam) {
        LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                .eq(ShortLinkDO::getGid, requestParam.getGid())
                .eq(ShortLinkDO::getEnableStatus, 1)
                .eq(ShortLinkDO::getDelFlag, 0)
                .orderByDesc(ShortLinkDO::getCreateTime);
        IPage<ShortLinkDO> resulPage = baseMapper.selectPage(requestParam,queryWrapper);   //ShortLinkPageReqDTO 继承了Page对象
        // ShortLinkDO 转 ShortLinkPageRespDTO
        return resulPage.convert(each-> {
                    ShortLinkPageRespDTO result = BeanUtil.toBean(each, ShortLinkPageRespDTO.class);
                    result.setDomain("http://"+result.getDomain());
                    return result;
                }
                );
    }

    /**
    * 查询短链接分组内数量
    * @Param: [requestParam]
    * @return:
    * @Date: 2023/12/21
    */
    @Override
    public List<ShortLinkGroupCountQueryRespDTO> listGroupShortLinkCount(List<String> requestParam) {
        // select gid,count(*) from t_link where enable_status=0 and gid= in (x,x,x) group by gid
        QueryWrapper<ShortLinkDO> queryWrapper = Wrappers.query(new ShortLinkDO())
                .select("gid", "count(*) as shortLinkCount")
                .in("gid", requestParam)
                .eq("enable_status", 0)
                .groupBy("gid");
        List<Map<String, Object>> shortLinkDOList = baseMapper.selectMaps(queryWrapper);    // selectMaps,查询多个字段的多条记录
        return BeanUtil.copyToList(shortLinkDOList,ShortLinkGroupCountQueryRespDTO.class);
    }

    @Override
    @Transactional(rollbackFor=Exception.class )
    public void updateShortLink(ShortLinkUpdateReqDTO requestParam) {
        // 短链接按照分组分片的，调整短链接所在分片数据就找不到，所以删除操作先进行删除再插入
        LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                .eq(ShortLinkDO::getGid, requestParam.getGid())
                .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                .eq(BaseDO::getDelFlag, 0)
                .eq(ShortLinkDO::getEnableStatus, 0);
        ShortLinkDO hasShortLinkDO = baseMapper.selectOne(queryWrapper);
        if (hasShortLinkDO==null) {
            throw new ClientException("短链接记录不存在");
        }

        ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                .domain(hasShortLinkDO.getDomain())
                .shortUri(hasShortLinkDO.getShortUri())
                .clickNum(hasShortLinkDO.getClickNum())
                .favicon(hasShortLinkDO.getFavicon())
                .createdType(hasShortLinkDO.getCreatedType())
                .gid(requestParam.getGid())
                .originUrl(requestParam.getOriginUrl())
                .describe(requestParam.getDescribe())
                .validDateType(requestParam.getValidDateType())
                .validDate(requestParam.getValidDate())
                .build();

        // 分组变了就要进行重新删除，gid是分片键，如果记录的gid变更就查不到记录了，进行重新删除插入
        if (Objects.equals(hasShortLinkDO.getGid(),requestParam.getGid())) {
            LambdaUpdateWrapper<ShortLinkDO> updateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                    .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                    .eq(ShortLinkDO::getGid, requestParam.getGid()) // GID 一致
                    .eq(BaseDO::getDelFlag, 0)
                    .eq(ShortLinkDO::getEnableStatus, 0)
                    .set(Objects.equals(requestParam.getValidDateType(), ValiDateTypeEnum.PERMANENT), ShortLinkDO::getValidDate, null);// 如果有效期类型为永久有效，时间设为null

            baseMapper.update(shortLinkDO,updateWrapper);
        }else {
            // 不一致重新删除插入
            LambdaUpdateWrapper<ShortLinkDO> updateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                    .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                    .eq(ShortLinkDO::getGid, hasShortLinkDO.getGid())   // GID 不一致，删除原GID对应记录
                    .eq(BaseDO::getDelFlag, 0)
                    .eq(ShortLinkDO::getEnableStatus, 0);

            baseMapper.delete(updateWrapper);
            shortLinkDO.setGid(requestParam.getGid());
            baseMapper.insert(shortLinkDO);

        }



    }

    @Override
    public void restoreUrl(String shortUri, ServletRequest request, ServletResponse response) throws IOException {
        // 传入的短链接，先通过短链接去获取gid，再通过gid获取完整连接名跳转
        String serverName = request.getServerName();    // 域名
        String fullShortUrl = serverName + "/" + shortUri;
        String originalLink = stringRedisTemplate.opsForValue().get(String.format(GOTO_SHORT_LINK_KEY, fullShortUrl));
        if (StrUtil.isNotBlank(originalLink)){  // 缓存中存在直接跳转
            ((HttpServletResponse)response).sendRedirect(originalLink);
            return;
        }
        // 缓存中不存在，避免缓存穿透：去查一个数据库和Redis都不存在的短链接
        // redis中没有缓存,缓存穿透，跳转的url在Redis不存在缓存，大量请求涌入数据库，防止大量请求涌入使用分布式锁或者布隆过滤器
        RLock lock = redissonClient.getLock(String.format(LOCK_GOTO_SHORT_LINK_KEY, fullShortUrl));
        lock.lock();    // 分布式锁解决缓存穿透，大量数据库和redis不存在的数据去请求时只有一个数据获取到数据，只有一个数据能去查询
        try {
            // 双重判定锁，第一个锁拿到数据并加入缓存后，后续的999个请求就没必要再去获取锁再解锁
            originalLink = stringRedisTemplate.opsForValue().get(String.format(GOTO_SHORT_LINK_KEY, fullShortUrl));
            if (StrUtil.isNotBlank(originalLink)){  // 双重判定锁，第一次获取锁期间某个请求去数据库获取数据并加入了缓存
                ((HttpServletResponse)response).sendRedirect(originalLink);
                return;

            }else {
                LambdaQueryWrapper<ShortLinkGotoDO> linkGotoQueryWrapper = Wrappers.lambdaQuery(ShortLinkGotoDO.class)
                        .eq(ShortLinkGotoDO::getFullShortUrl, fullShortUrl);

                ShortLinkGotoDO shortLinkGotoDO = shortLinkGotoMapper.selectOne(linkGotoQueryWrapper);
                if (shortLinkGotoDO==null) {
                    // 严谨来说此处需要封控
                    return;
                }

                LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                        .eq(ShortLinkDO::getGid, shortLinkGotoDO.getGid()) // 用户传的shortUri，拿不到link表分片的gid，通过路由表的方式
                        .eq(ShortLinkDO::getFullShortUrl, fullShortUrl)
                        .eq(BaseDO::getDelFlag, 0)
                        .eq(ShortLinkDO::getEnableStatus, 0);
                ShortLinkDO shortLinkDO = baseMapper.selectOne(queryWrapper);
                // 1000个相同请求数据x在数据库和redis都不存在，最先获取到锁的把x加入缓存， 后面999个就不会去数据库了
                // 第一个不存在的数据加载到缓存，后续数据就不会获取锁了
                // 不为空通过gid查到完整连接进行跳转
                if (shortLinkDO!=null){ // Redis中没有缓存，数据库有数据，数据加入缓存
                    stringRedisTemplate.opsForValue().set(String.format(GOTO_SHORT_LINK_KEY, fullShortUrl),shortLinkDO.getOriginUrl());
                    ((HttpServletResponse)response).sendRedirect(shortLinkDO.getFullShortUrl());
                }
            }


        } finally {  // 最后解锁
            lock.unlock();
        }


    }


    private String generateSuffix(ShortLinkCreateReqDTO requestParam){
        // 取模生成的uri可能重复
        int customGenerateCount = 0;
        String shortUri;
        while (true){
            if (customGenerateCount>10) {
                throw new ServiceException("短链接频繁生成，请稍后再试");
            }
            String originUrl = requestParam.getOriginUrl();
            originUrl+=System.currentTimeMillis();  // url传入hash重新生成的还是原来的值，要改变url
            shortUri =  HashUtil.hashToBase62(originUrl);
            // 存在的问题：频繁去查询数据库，缓存穿透
//            LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
//                    .eq(ShortLinkDO::getFullShortUrl, requestParam.getDomain() + "/" + shortUri);
//
//            ShortLinkDO shortLinkDO = baseMapper.selectOne(queryWrapper);
//            if (shortLinkDO == null) {
//                break;
//            }
//            customGenerateCount++;
            // 布隆过滤器,避免大量不存在的直接访问数据库造成缓存穿透=====存在误判
            if (!shortUriCreateCachePenetrationBloomFilter.contains(requestParam.getDomain() + "/" + shortUri)){    // 布隆过滤器不存在直接可以创建
                break;
            }
            // 布隆过滤器存在重新生成 suffix
            customGenerateCount++;

        }


        return shortUri;
    }
}
