package org.tckry.shortlink.project.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.Week;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.text.StrBuilder;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tckry.shortlink.project.common.convention.exception.ClientException;
import org.tckry.shortlink.project.common.convention.exception.ServiceException;
import org.tckry.shortlink.project.common.database.BaseDO;
import org.tckry.shortlink.project.common.enums.VailDateTypeEnum;
import org.tckry.shortlink.project.config.GotoDomainWhiteListConfiguration;
import org.tckry.shortlink.project.dao.entity.*;
import org.tckry.shortlink.project.dao.mapper.*;
import org.tckry.shortlink.project.dto.biz.ShortLinkStatsRecordDTO;
import org.tckry.shortlink.project.dto.req.ShortLinkBatchCreateReqDTO;
import org.tckry.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import org.tckry.shortlink.project.dto.req.ShortLinkPageReqDTO;
import org.tckry.shortlink.project.dto.req.ShortLinkUpdateReqDTO;
import org.tckry.shortlink.project.dto.resp.*;
import org.tckry.shortlink.project.mq.producer.DelayShortLinkStatsProducer;

import org.tckry.shortlink.project.mq.producer.ShortLinkStatsSaveProducer;
import org.tckry.shortlink.project.service.LinkStatsTodayService;
import org.tckry.shortlink.project.service.ShortLinkService;
import org.tckry.shortlink.project.toolkit.HashUtil;
import org.tckry.shortlink.project.toolkit.LinkUtil;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.tckry.shortlink.project.common.constant.RedisKeyConstant.*;
import static org.tckry.shortlink.project.common.constant.ShortLinkConstant.AMAP_REMOTE_URL;
import static org.tckry.shortlink.project.toolkit.LinkUtil.getActualIp;

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
    private final LinkAccessStatsMapper linkAccessStatsMapper;
    private final LinkLocaleStatsMapper linkLocaleStatsMapper;
    private final LinkOsStatsMapper linkOsStatsMapper;
    private final LinkBrowserStatsMapper linkBrowserStatsMapper;
    private final LinkAccessLogsMapper linkAccessLogsMapper;
    private final LinkDeviceStatsMapper linkDeviceStatsMapper;
    private final LinkNetworkStatsMapper linkNetworkStatsMapper;
    private final LinkStatsTodayMapper linkStatsTodayMapper;
    private final LinkStatsTodayService linkStatsTodayService;
    private final ShortLinkStatsSaveProducer shortLinkStatsSaveProducer;
    private final GotoDomainWhiteListConfiguration gotoDomainWhiteListConfiguration;

    /**
     * 高德接口密钥
     */
    @Value("${short-link.stats.locale.amap-key}")
    private String statsLocaleAmapKey;

    /**
     * 默认域名
     */
    @Value("${short-link.domain.default}")
    private String createShortLinkDefaultDomain;

    /**
    * 创建短链接
    * @Param: [requestParam]
    * @return: org.tckry.shortlink.project.dto.resp.ShortLinkCreateRespDTO
    * @Date: 2023/12/20
    */
    @Override
    public ShortLinkCreateRespDTO createShortLink(ShortLinkCreateReqDTO requestParam) {
        verificationWhitelist(requestParam.getOriginUrl());
        // 1、根据原始连接生成短链接的后缀
        String shortLinkSuffix = generateSuffix(requestParam);
        String fullShortUrl = StrBuilder.create(createShortLinkDefaultDomain).append("/").append(shortLinkSuffix).toString();
        // String fullShortUrl = requestParam.getDomain()+"/" + shortLinkSuffix;
        ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                .domain(createShortLinkDefaultDomain)
                .originUrl(requestParam.getOriginUrl())
                .gid(requestParam.getGid())
                .createdType(requestParam.getCreatedType())
                .validDateType(requestParam.getValidDateType())
                .validDate(requestParam.getValidDate())
                .describe(requestParam.getDescribe())
                .shortUri(shortLinkSuffix)
                .enableStatus(0)
                .totalPv(0)
                .totalUv(0)
                .totalUip(0)
                .fullShortUrl(fullShortUrl)
                .favicon(getFavicon(requestParam.getOriginUrl()))
                .build();
        // 不同表的分片键不同，使用goto 路由表记录
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
            // 由于在产生后缀generateSufffix的那边判断了，所以这里不用再去查询数据库了
            throw new ServiceException(String.format("短链接:%s 重复生成",fullShortUrl));
        }
        // 缓存预热，把创建的短链接就加入缓存
        stringRedisTemplate.opsForValue().set(
                String.format(GOTO_SHORT_LINK_KEY,fullShortUrl),
                requestParam.getOriginUrl(),LinkUtil.getLinkCacheValidTime(requestParam.getValidDate()),
                TimeUnit.MILLISECONDS
        );


        shortUriCreateCachePenetrationBloomFilter.add(fullShortUrl);
        return ShortLinkCreateRespDTO.builder()
                .gid(requestParam.getGid())
                .originUrl(requestParam.getOriginUrl())
                .fullShortUrl("http://"+shortLinkDO.getFullShortUrl()).build();
    }

    @Override
    public IPage<ShortLinkPageRespDTO> pageShortLink(ShortLinkPageReqDTO requestParam) {
        IPage<ShortLinkDO> resulPage = baseMapper.pageLink(requestParam);   //ShortLinkPageReqDTO 继承了Page对象
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

//    @Override
//    @Transactional(rollbackFor=Exception.class )
//    public void updateShortLink(ShortLinkUpdateReqDTO requestParam) {
//        // 短链接按照分组分片的，调整短链接所在分片数据就找不到，所以删除操作先进行删除再插入
//        LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
//                .eq(ShortLinkDO::getGid, requestParam.getOriginGid())
//                .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
//                .eq(BaseDO::getDelFlag, 0)
//                .eq(ShortLinkDO::getEnableStatus, 0);
//        ShortLinkDO hasShortLinkDO = baseMapper.selectOne(queryWrapper);
//        if (hasShortLinkDO==null) {
//            throw new ClientException("短链接记录不存在");
//        }
//
//        ShortLinkDO shortLinkDO = ShortLinkDO.builder()
//                .domain(hasShortLinkDO.getDomain())
//                .shortUri(hasShortLinkDO.getShortUri())
//                .clickNum(hasShortLinkDO.getClickNum())
//                .favicon(hasShortLinkDO.getFavicon())
//                .createdType(hasShortLinkDO.getCreatedType())
//                .gid(requestParam.getGid())
//                .originUrl(requestParam.getOriginUrl())
//                .describe(requestParam.getDescribe())
//                .validDateType(requestParam.getValidDateType())
//                .validDate(requestParam.getValidDate())
//                .build();
//
//        // 分组变了就要进行重新删除，gid是分片键，如果记录的gid变更就查不到记录了，进行重新删除插入
//        if (Objects.equals(hasShortLinkDO.getGid(),requestParam.getGid())) {
//            LambdaUpdateWrapper<ShortLinkDO> updateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
//                    .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
//                    .eq(ShortLinkDO::getGid, requestParam.getGid()) // GID 一致
//                    .eq(BaseDO::getDelFlag, 0)
//                    .eq(ShortLinkDO::getEnableStatus, 0)
//                    .set(Objects.equals(requestParam.getValidDateType(), ValiDateTypeEnum.PERMANENT), ShortLinkDO::getValidDate, null);// 如果有效期类型为永久有效，时间设为null
//
//            baseMapper.update(shortLinkDO,updateWrapper);
//        }else {
//            // 不一致重新删除插入
//            LambdaUpdateWrapper<ShortLinkDO> updateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
//                    .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
//                    .eq(ShortLinkDO::getGid, hasShortLinkDO.getGid())   // GID 不一致，删除原GID对应记录
//                    .eq(BaseDO::getDelFlag, 0)
//                    .eq(ShortLinkDO::getEnableStatus, 0);
//
//            baseMapper.delete(updateWrapper);
//            shortLinkDO.setGid(requestParam.getGid());
//            baseMapper.insert(shortLinkDO);
//        }
//
//        // 1、修改有效时间或者类型，但是redis之中存在GOTO_SHORT_LINK_KEY缓存，也能跳转，所以删除redis缓存
//        if (Objects.equals(hasShortLinkDO.getValidDateType(),requestParam.getValidDateType())
//                || !Objects.equals(hasShortLinkDO.getValidDate(),requestParam.getValidDate())) {
//            stringRedisTemplate.delete(String.format(GOTO_SHORT_LINK_KEY, requestParam.getFullShortUrl()));
//
//            // 2、无效变为有效，redis中缓存的空对象GOTO_IS_NULL_SHORT_LINK_KEY还存在，也需要进行删除
//            // 原连接过期时间不等于空且原链接有效时间在当前时间前，即原链接已过期，但是redis中存在原链接的 null 缓存
//            if (hasShortLinkDO.getValidDate() != null && hasShortLinkDO.getValidDate().before(new Date())){
//                // 修改后的连接为永久有效或者过期时间不等于空且有效时间在当前时间之后，即当前处于有效时间内，删除之前的GOTO_IS_NULL_SHORT_LINK_KEY 缓存
//                if (Objects.equals(requestParam.getValidDateType(),ValiDateTypeEnum.PERMANENT.getType())
//                || requestParam.getValidDate().after(new Date())){
//                    stringRedisTemplate.delete(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, requestParam.getFullShortUrl()));
//                }
//            }
//        }
//    }



    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateShortLink(ShortLinkUpdateReqDTO requestParam) {
        verificationWhitelist(requestParam.getOriginUrl());
        LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                .eq(ShortLinkDO::getGid, requestParam.getOriginGid())   // 若修改gid，修改后的gid由于是分片键，查不到数据，使用原来的gid去查询
                .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                .eq(ShortLinkDO::getDelFlag, 0)
                .eq(ShortLinkDO::getEnableStatus, 0);
        ShortLinkDO hasShortLinkDO = baseMapper.selectOne(queryWrapper);
        if (hasShortLinkDO == null) {
            throw new ClientException("短链接记录不存在");
        }
        // 未修改短连接的gid直接修改对象
        if (Objects.equals(hasShortLinkDO.getGid(), requestParam.getGid())) {
            LambdaUpdateWrapper<ShortLinkDO> updateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                    .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                    .eq(ShortLinkDO::getGid, requestParam.getGid())
                    .eq(ShortLinkDO::getDelFlag, 0)
                    .eq(ShortLinkDO::getEnableStatus, 0)
                    .set(Objects.equals(requestParam.getValidDateType(), VailDateTypeEnum.PERMANENT.getType()), ShortLinkDO::getValidDate, null);
            ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                    .domain(hasShortLinkDO.getDomain())
                    .shortUri(hasShortLinkDO.getShortUri())
                    .favicon(hasShortLinkDO.getFavicon())
                    .createdType(hasShortLinkDO.getCreatedType())
                    .gid(requestParam.getGid())
                    .originUrl(requestParam.getOriginUrl())
                    .describe(requestParam.getDescribe())
                    .validDateType(requestParam.getValidDateType())
                    .validDate(requestParam.getValidDate())
                    .build();
            baseMapper.update(shortLinkDO, updateWrapper);
        } else {    // 修改GID，删除再重新插入；修改时别的用户访问，通过读写锁的方式，适合写操作少读操作多的场景；读取共享，写入独占；分布式锁不适用于并发读取，效率低
            // 统计短链接拿取到读锁，则当前没有正在访问；若是获取不到写锁，则
            RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(String.format(LOCK_GID_UPDATE_KEY, requestParam.getFullShortUrl()));
            RLock rLock = readWriteLock.writeLock();
            if (!rLock.tryLock()) {
                throw new ServiceException("短链接正在被访问，请稍后再试...");
            }
            // 修改操作成功获取到写锁，让短链接无法被访问；避免写操作一直阻塞读取操作，将读操作加入延迟队列，一定事件后再去读取，若还是阻塞再加入延迟队列
            try {
                LambdaUpdateWrapper<ShortLinkDO> linkUpdateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                        .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                        .eq(ShortLinkDO::getGid, hasShortLinkDO.getGid())
                        .eq(ShortLinkDO::getDelFlag, 0)
                        .eq(ShortLinkDO::getDelTime, 0L)
                        .eq(ShortLinkDO::getEnableStatus, 0);
                ShortLinkDO delShortLinkDO = ShortLinkDO.builder()
                        .delTime(System.currentTimeMillis())
                        .build();
                delShortLinkDO.setDelFlag(1);
                baseMapper.update(delShortLinkDO, linkUpdateWrapper);
                ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                        .domain(createShortLinkDefaultDomain)
                        .originUrl(requestParam.getOriginUrl())
                        .gid(requestParam.getGid())
                        .createdType(hasShortLinkDO.getCreatedType())
                        .validDateType(requestParam.getValidDateType())
                        .validDate(requestParam.getValidDate())
                        .describe(requestParam.getDescribe())
                        .shortUri(hasShortLinkDO.getShortUri())
                        .enableStatus(hasShortLinkDO.getEnableStatus())
                        .totalPv(hasShortLinkDO.getTotalPv())
                        .totalUv(hasShortLinkDO.getTotalUv())
                        .totalUip(hasShortLinkDO.getTotalUip())
                        .fullShortUrl(hasShortLinkDO.getFullShortUrl())
                        .favicon(getFavicon(requestParam.getOriginUrl()))
                        .delTime(0L)
                        .build();
                baseMapper.insert(shortLinkDO);
                LambdaQueryWrapper<LinkStatsTodayDO> statsTodayQueryWrapper = Wrappers.lambdaQuery(LinkStatsTodayDO.class)
                        .eq(LinkStatsTodayDO::getFullShortUrl, requestParam.getFullShortUrl())
                        .eq(LinkStatsTodayDO::getGid, hasShortLinkDO.getGid())
                        .eq(LinkStatsTodayDO::getDelFlag, 0);
                List<LinkStatsTodayDO> linkStatsTodayDOList = linkStatsTodayMapper.selectList(statsTodayQueryWrapper);
                if (CollUtil.isNotEmpty(linkStatsTodayDOList)) {
                    linkStatsTodayMapper.deleteBatchIds(linkStatsTodayDOList.stream()
                            .map(LinkStatsTodayDO::getId)
                            .toList()
                    );
                    linkStatsTodayDOList.forEach(each -> each.setGid(requestParam.getGid()));
                    linkStatsTodayService.saveBatch(linkStatsTodayDOList);
                }
                LambdaQueryWrapper<ShortLinkGotoDO> linkGotoQueryWrapper = Wrappers.lambdaQuery(ShortLinkGotoDO.class)
                        .eq(ShortLinkGotoDO::getFullShortUrl, requestParam.getFullShortUrl())
                        .eq(ShortLinkGotoDO::getGid, hasShortLinkDO.getGid());
                ShortLinkGotoDO shortLinkGotoDO = shortLinkGotoMapper.selectOne(linkGotoQueryWrapper);
                shortLinkGotoMapper.deleteById(shortLinkGotoDO.getId());
                shortLinkGotoDO.setGid(requestParam.getGid());
                shortLinkGotoMapper.insert(shortLinkGotoDO);
                LambdaUpdateWrapper<LinkAccessStatsDO> linkAccessStatsUpdateWrapper = Wrappers.lambdaUpdate(LinkAccessStatsDO.class)
                        .eq(LinkAccessStatsDO::getFullShortUrl, requestParam.getFullShortUrl())
                        .eq(LinkAccessStatsDO::getGid, hasShortLinkDO.getGid())
                        .eq(LinkAccessStatsDO::getDelFlag, 0);
                LinkAccessStatsDO linkAccessStatsDO = LinkAccessStatsDO.builder()
                        .gid(requestParam.getGid())
                        .build();
                linkAccessStatsMapper.update(linkAccessStatsDO, linkAccessStatsUpdateWrapper);
                LambdaUpdateWrapper<LinkLocaleStatsDO> linkLocaleStatsUpdateWrapper = Wrappers.lambdaUpdate(LinkLocaleStatsDO.class)
                        .eq(LinkLocaleStatsDO::getFullShortUrl, requestParam.getFullShortUrl())
                        .eq(LinkLocaleStatsDO::getGid, hasShortLinkDO.getGid())
                        .eq(LinkLocaleStatsDO::getDelFlag, 0);
                LinkLocaleStatsDO linkLocaleStatsDO = LinkLocaleStatsDO.builder()
                        .gid(requestParam.getGid())
                        .build();
                linkLocaleStatsMapper.update(linkLocaleStatsDO, linkLocaleStatsUpdateWrapper);
                LambdaUpdateWrapper<LinkOsStatsDO> linkOsStatsUpdateWrapper = Wrappers.lambdaUpdate(LinkOsStatsDO.class)
                        .eq(LinkOsStatsDO::getFullShortUrl, requestParam.getFullShortUrl())
                        .eq(LinkOsStatsDO::getGid, hasShortLinkDO.getGid())
                        .eq(LinkOsStatsDO::getDelFlag, 0);
                LinkOsStatsDO linkOsStatsDO = LinkOsStatsDO.builder()
                        .gid(requestParam.getGid())
                        .build();
                linkOsStatsMapper.update(linkOsStatsDO, linkOsStatsUpdateWrapper);
                LambdaUpdateWrapper<LinkBrowserStatsDO> linkBrowserStatsUpdateWrapper = Wrappers.lambdaUpdate(LinkBrowserStatsDO.class)
                        .eq(LinkBrowserStatsDO::getFullShortUrl, requestParam.getFullShortUrl())
                        .eq(LinkBrowserStatsDO::getGid, hasShortLinkDO.getGid())
                        .eq(LinkBrowserStatsDO::getDelFlag, 0);
                LinkBrowserStatsDO linkBrowserStatsDO = LinkBrowserStatsDO.builder()
                        .gid(requestParam.getGid())
                        .build();
                linkBrowserStatsMapper.update(linkBrowserStatsDO, linkBrowserStatsUpdateWrapper);
                LambdaUpdateWrapper<LinkDeviceStatsDO> linkDeviceStatsUpdateWrapper = Wrappers.lambdaUpdate(LinkDeviceStatsDO.class)
                        .eq(LinkDeviceStatsDO::getFullShortUrl, requestParam.getFullShortUrl())
                        .eq(LinkDeviceStatsDO::getGid, hasShortLinkDO.getGid())
                        .eq(LinkDeviceStatsDO::getDelFlag, 0);
                LinkDeviceStatsDO linkDeviceStatsDO = LinkDeviceStatsDO.builder()
                        .gid(requestParam.getGid())
                        .build();
                linkDeviceStatsMapper.update(linkDeviceStatsDO, linkDeviceStatsUpdateWrapper);
                LambdaUpdateWrapper<LinkNetworkStatsDO> linkNetworkStatsUpdateWrapper = Wrappers.lambdaUpdate(LinkNetworkStatsDO.class)
                        .eq(LinkNetworkStatsDO::getFullShortUrl, requestParam.getFullShortUrl())
                        .eq(LinkNetworkStatsDO::getGid, hasShortLinkDO.getGid())
                        .eq(LinkNetworkStatsDO::getDelFlag, 0);
                LinkNetworkStatsDO linkNetworkStatsDO = LinkNetworkStatsDO.builder()
                        .gid(requestParam.getGid())
                        .build();
                linkNetworkStatsMapper.update(linkNetworkStatsDO, linkNetworkStatsUpdateWrapper);
                LambdaUpdateWrapper<LinkAccessLogsDO> linkAccessLogsUpdateWrapper = Wrappers.lambdaUpdate(LinkAccessLogsDO.class)
                        .eq(LinkAccessLogsDO::getFullShortUrl, requestParam.getFullShortUrl())
                        .eq(LinkAccessLogsDO::getGid, hasShortLinkDO.getGid())
                        .eq(LinkAccessLogsDO::getDelFlag, 0);
                LinkAccessLogsDO linkAccessLogsDO = LinkAccessLogsDO.builder()
                        .gid(requestParam.getGid())
                        .build();
                linkAccessLogsMapper.update(linkAccessLogsDO, linkAccessLogsUpdateWrapper);
            } finally {
                rLock.unlock();
            }
        }
        if (!Objects.equals(hasShortLinkDO.getValidDateType(), requestParam.getValidDateType())
                || !Objects.equals(hasShortLinkDO.getValidDate(), requestParam.getValidDate())) {
            stringRedisTemplate.delete(String.format(GOTO_SHORT_LINK_KEY, requestParam.getFullShortUrl()));
            if (hasShortLinkDO.getValidDate() != null && hasShortLinkDO.getValidDate().before(new Date())) {
                if (Objects.equals(requestParam.getValidDateType(), VailDateTypeEnum.PERMANENT.getType()) || requestParam.getValidDate().after(new Date())) {
                    stringRedisTemplate.delete(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, requestParam.getFullShortUrl()));
                }
            }
        }
    }


    /**
    * 构建短链接统计请求参数
    * @Param: [fullShortUrl, request, response]
    * @return: org.tckry.shortlink.project.dto.biz.ShortLinkStatsRecordDTO
    * @Date: 2024/1/2
    */
    private ShortLinkStatsRecordDTO buildLinkStatsRecordAndSetUser(String fullShortUrl, ServletRequest request, ServletResponse response) {
        AtomicBoolean uvFirstFlag = new AtomicBoolean();    // lambda 中不能用普通Boolean，报错未初始化
        //        Boolean uvFirstFlag;
        Cookie[] cookies = ((HttpServletRequest) request).getCookies();
        AtomicReference<String> uv = new AtomicReference<>();
        Runnable addResponseCookieTask = () -> {
            // 通过cookie获取uv，同一用户访问不加次数，首次访问添加cookie并加1
            uv.set(UUID.fastUUID().toString());
            Cookie uvCookie = new Cookie("uv", uv.get());
            uvCookie.setMaxAge(60 * 60 * 24 * 30);
            uvCookie.setPath(StrUtil.sub(fullShortUrl, fullShortUrl.indexOf("/"), fullShortUrl.length()));
            ((HttpServletResponse) response).addCookie(uvCookie);
            uvFirstFlag.set(Boolean.TRUE);
            stringRedisTemplate.opsForSet().add(SHORT_LINK_STATS_UV_KEY + fullShortUrl, uv.get());
        };
        if (ArrayUtil.isNotEmpty(cookies)) {
            Arrays.stream(cookies)
                    .filter(each -> Objects.equals(each.getName(), "uv"))
                    .findFirst()
                    .map(Cookie::getValue)
                    .ifPresentOrElse(each -> {
                        uv.set(each);
                        Long uvAdded = stringRedisTemplate.opsForSet().add(SHORT_LINK_STATS_UV_KEY + fullShortUrl, each);
                        uvFirstFlag.set(uvAdded != null && uvAdded > 0L);
                    }, addResponseCookieTask);
        } else {    // cookie 不存在，首次访问添加cookie
            addResponseCookieTask.run();
        }
        String remoteAddr = LinkUtil.getActualIp(((HttpServletRequest) request));
        String os = LinkUtil.getOs(((HttpServletRequest) request));
        String browser = LinkUtil.getBrowser(((HttpServletRequest) request));
        String device = LinkUtil.getDevice(((HttpServletRequest) request));
        String network = LinkUtil.getNetwork(((HttpServletRequest) request));
        Long uipAdded = stringRedisTemplate.opsForSet().add(SHORT_LINK_STATS_UIP_KEY + fullShortUrl, remoteAddr);
        // set结构，若是ip已存在加不进去
        boolean uipFirstFlag = uipAdded != null && uipAdded > 0L;
        return ShortLinkStatsRecordDTO.builder()
                .fullShortUrl(fullShortUrl)
                .uv(uv.get())
                .uvFirstFlag(uvFirstFlag.get())
                .uipFirstFlag(uipFirstFlag)
                .remoteAddr(remoteAddr)
                .os(os)
                .browser(browser)
                .device(device)
                .network(network)
                .build();
    }


    @SneakyThrows   // 不用在方法后面声明Throws
    @Override
    public void restoreUrl(String shortUri, ServletRequest request, ServletResponse response) {
        // 传入的短链接，先通过短链接去获取gid，再通过gid获取完整连接名跳转
        String serverName = request.getServerName();    // 域名，不含端口
        String serverPort = Optional.of(request.getServerPort())
                .filter(each -> !Objects.equals(each, "80"))
                .map(String::valueOf)   // request.getServerPort() 是int，转string
                .map(each -> ":" + each)
                .orElse("");

        String fullShortUrl = serverName + serverPort + "/" + shortUri;
        String originalLink = stringRedisTemplate.opsForValue().get(String.format(GOTO_SHORT_LINK_KEY, fullShortUrl));
        if (StrUtil.isNotBlank(originalLink)){  // 缓存中存在原始连接直接跳转
            ShortLinkStatsRecordDTO statsRecord = buildLinkStatsRecordAndSetUser(fullShortUrl, request, response);
            shortLinkStats(fullShortUrl,null,statsRecord);
            // shortLinkStats(fullShortUrl,null,request,response);
            ((HttpServletResponse)response).sendRedirect(originalLink);
            return;
        }
        // 缓存中不存在去布隆过滤器判断
        boolean contained = shortUriCreateCachePenetrationBloomFilter.contains(fullShortUrl);
        if (!contained) {  // 布隆过滤器不存在
            ((HttpServletResponse)response).sendRedirect("/page/notfound");
            return;
        }
        // 布隆过滤器存在，仍可能存在误判问题,查询Key是否存在空值（查询的是数据库不存在的值），存在空值则代表在缓存中存在但是在数据库不存在，也避免去查询数据库
        String gotoIsNullShortLink = stringRedisTemplate.opsForValue().get(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, fullShortUrl));
        // 布隆过滤器查询存在，但在redis 中空值key查询到了，说明是误判或者查询的不存在数据库的值，直接return
        if (StrUtil.isNotBlank(gotoIsNullShortLink)) {

            ((HttpServletResponse)response).sendRedirect("/page/notfound");
            return;
        }
        // 布隆过滤器和缓存中都不存在，大量请求到达数据库请求同一数据，使用分布式锁，因为请求都是针对同一个数据，缓存击问题
        // 第五步通过分布式锁仅让一个请求到达数据库
        // 缓存中不存在，避免缓存击穿：Redis中数据没有大量请求去数据库
        // redis中没有缓存,缓存击穿，跳转的url在Redis不存在缓存，大量请求涌入数据库，防止大量请求涌入使用分布式锁+双重锁机制
        RLock lock = redissonClient.getLock(String.format(LOCK_GOTO_SHORT_LINK_KEY, fullShortUrl));
        lock.lock();    // 分布式锁解决缓存击穿，redis缓存过期，导致大量请求去请求数据库，只有一个请求数据获取到锁去数据库查数据
        try {
            // 双重判定锁，第一个锁拿到数据并加入缓存后，后续的999个请求就没必要再去获取锁再解锁
            originalLink = stringRedisTemplate.opsForValue().get(String.format(GOTO_SHORT_LINK_KEY, fullShortUrl));
            if (StrUtil.isNotBlank(originalLink)){  // 双重判定锁，第一次获取锁期间某个请求去数据库获取数据并加入了缓存
                ShortLinkStatsRecordDTO statsRecord = buildLinkStatsRecordAndSetUser(fullShortUrl, request, response);
                shortLinkStats(fullShortUrl,null,statsRecord);
                // shortLinkStats(fullShortUrl,null,request,response);
                ((HttpServletResponse)response).sendRedirect(originalLink);
                return;

            }
            LambdaQueryWrapper<ShortLinkGotoDO> linkGotoQueryWrapper = Wrappers.lambdaQuery(ShortLinkGotoDO.class)
                    .eq(ShortLinkGotoDO::getFullShortUrl, fullShortUrl);

            ShortLinkGotoDO shortLinkGotoDO = shortLinkGotoMapper.selectOne(linkGotoQueryWrapper);
            if (shortLinkGotoDO==null) {
                // 数据库不存在的null 值，加入Redis，设置缓存时间，
                // 6、通过数据库加载数据并将数据加载进缓存，数据不存在设置有效期
                stringRedisTemplate.opsForValue().set(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, fullShortUrl),"-",30, TimeUnit.MINUTES);
                ((HttpServletResponse)response).sendRedirect("/page/notfound");
                return;
            }

            LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                    .eq(ShortLinkDO::getGid, shortLinkGotoDO.getGid()) // 用户传的shortUri，拿不到link表分片的gid，通过路由表的方式
                    .eq(ShortLinkDO::getFullShortUrl, fullShortUrl)
                    .eq(BaseDO::getDelFlag, 0)
                    .eq(ShortLinkDO::getEnableStatus, 0);
            ShortLinkDO shortLinkDO = baseMapper.selectOne(queryWrapper);
            // 1000个相同请求数据x redis不存在，去请求数据库，最先获取到锁的把x加入缓存， 后面999个就不会去数据库了
            // 第一个不存在的数据加载到缓存，后续数据就不会获取锁了
            // 不为空通过gid查到完整连接进行跳转
            if (shortLinkDO ==null || (shortLinkDO.getValidDate()!=null&&shortLinkDO.getValidDate().before(new Date()))){ // 数据库没有数据或者数据库有数据但是已过有效期，跳转notfound界面
                // 数据库的记录有效期以过期，相当于无
                stringRedisTemplate.opsForValue().set(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, fullShortUrl),"-",30, TimeUnit.MINUTES);
                ((HttpServletResponse)response).sendRedirect("/page/notfound");
                return;

            }
            // Redis中没有缓存，数据库有数据，数据加入缓存
            // 6、通过数据库加载数据并将数据加载进缓存，数据存在根据有效时间字段设置设置有效期
            stringRedisTemplate.opsForValue().set(
                    String.format(GOTO_SHORT_LINK_KEY, fullShortUrl),
                    shortLinkDO.getOriginUrl(),
                    LinkUtil.getLinkCacheValidTime(shortLinkDO.getValidDate()),TimeUnit.MILLISECONDS);
            ShortLinkStatsRecordDTO statsRecord = buildLinkStatsRecordAndSetUser(fullShortUrl, request, response);
            shortLinkStats(fullShortUrl,shortLinkDO.getGid(),statsRecord);
            // shortLinkStats(fullShortUrl,shortLinkDO.getGid(),request,response);
            ((HttpServletResponse)response).sendRedirect(shortLinkDO.getOriginUrl());

        } finally {  // 最后解锁
            lock.unlock();
        }


    }

    @Override
    public ShortLinkBatchCreateRespDTO batchCreateShortLink(ShortLinkBatchCreateReqDTO requestParam) {
        List<String> originUrls = requestParam.getOriginUrls();
        List<String> describes = requestParam.getDescribes();
        List<ShortLinkBaseInfoRespDTO> result = new ArrayList<>();
        for (int i = 0; i < originUrls.size(); i++) {
            ShortLinkCreateReqDTO shortLinkCreateReqDTO = BeanUtil.toBean(requestParam, ShortLinkCreateReqDTO.class);
            shortLinkCreateReqDTO.setOriginUrl(originUrls.get(i));
            shortLinkCreateReqDTO.setDescribe(describes.get(i));
            try {
                ShortLinkCreateRespDTO shortLink = createShortLink(shortLinkCreateReqDTO);  // 调用创建单个短链接的函数
                ShortLinkBaseInfoRespDTO linkBaseInfoRespDTO = ShortLinkBaseInfoRespDTO.builder()
                        .fullShortUrl(shortLink.getFullShortUrl())
                        .originUrl(shortLink.getOriginUrl())
                        .describe(describes.get(i))
                        .build();
                result.add(linkBaseInfoRespDTO);
            } catch (Throwable ex) {
                log.error("批量创建短链接失败，原始参数：{}", originUrls.get(i));
            }
        }
        return ShortLinkBatchCreateRespDTO.builder()
                .total(result.size())
                .baseLinkInfos(result)
                .build();
    }


    /**
    * 短链接跳转时进行监控
    * @Param: [fullShortUrl, gid, statsRecord]
    * @return: void
    * @Date: 2024/1/2
    */
    @Override
    public void shortLinkStats(String fullShortUrl, String gid, ShortLinkStatsRecordDTO statsRecord) {
//        fullShortUrl = Optional.ofNullable(fullShortUrl).orElse(statsRecord.getFullShortUrl());
//        // 获取读锁，避免在修改时进行读取
//        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(String.format(LOCK_GID_UPDATE_KEY, fullShortUrl));
//        // 获取读锁，多个并发请请求能获取到数据；修改操作使用写锁
//        RLock rLock = readWriteLock.readLock();
//        // 获取不到读锁，此时修改短链接加了写锁，将消息加入延迟队列，等修改完再去查询
//        if (!rLock.tryLock()) {
//            delayShortLinkStatsProducer.send(statsRecord);
//            return;
//        }
//        // 获取到读锁，进行统计
//        try {
//            if (StrUtil.isBlank(gid)) {
//                LambdaQueryWrapper<ShortLinkGotoDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkGotoDO.class)
//                        .eq(ShortLinkGotoDO::getFullShortUrl, fullShortUrl);
//                ShortLinkGotoDO shortLinkGotoDO = shortLinkGotoMapper.selectOne(queryWrapper);
//                gid = shortLinkGotoDO.getGid();
//            }
//            int hour = DateUtil.hour(new Date(), true);
//            Week week = DateUtil.dayOfWeekEnum(new Date());
//            int weekValue = week.getIso8601Value();
//            LinkAccessStatsDO linkAccessStatsDO = LinkAccessStatsDO.builder()
//                    .pv(1)
//                    .uv(statsRecord.getUvFirstFlag() ? 1 : 0)
//                    .uip(statsRecord.getUipFirstFlag() ? 1 : 0)
//                    .hour(hour)
//                    .weekday(weekValue)
//                    .fullShortUrl(fullShortUrl)
//                    .gid(gid)
//                    .date(new Date())
//                    .build();
//            linkAccessStatsMapper.shortLinkStats(linkAccessStatsDO);
//            Map<String, Object> localeParamMap = new HashMap<>();
//            localeParamMap.put("key", statsLocaleAmapKey);
//            localeParamMap.put("ip", statsRecord.getRemoteAddr());
//            String localeResultStr = HttpUtil.get(AMAP_REMOTE_URL, localeParamMap);
//            JSONObject localeResultObj = JSON.parseObject(localeResultStr);
//            String infoCode = localeResultObj.getString("infocode");
//            String actualProvince = "未知";
//            String actualCity = "未知";
//            if (StrUtil.isNotBlank(infoCode) && StrUtil.equals(infoCode, "10000")) {
//                String province = localeResultObj.getString("province");
//                boolean unknownFlag = StrUtil.equals(province, "[]");
//                LinkLocaleStatsDO linkLocaleStatsDO = LinkLocaleStatsDO.builder()
//                        .province(actualProvince = unknownFlag ? actualProvince : province)
//                        .city(actualCity = unknownFlag ? actualCity : localeResultObj.getString("city"))
//                        .adcode(unknownFlag ? "未知" : localeResultObj.getString("adcode"))
//                        .cnt(1)
//                        .fullShortUrl(fullShortUrl)
//                        .country("中国")
//                        .gid(gid)
//                        .date(new Date())
//                        .build();
//                linkLocaleStatsMapper.shortLinkLocaleState(linkLocaleStatsDO);
//            }
//            LinkOsStatsDO linkOsStatsDO = LinkOsStatsDO.builder()
//                    .os(statsRecord.getOs())
//                    .cnt(1)
//                    .gid(gid)
//                    .fullShortUrl(fullShortUrl)
//                    .date(new Date())
//                    .build();
//            linkOsStatsMapper.shortLinkOsState(linkOsStatsDO);
//            LinkBrowserStatsDO linkBrowserStatsDO = LinkBrowserStatsDO.builder()
//                    .browser(statsRecord.getBrowser())
//                    .cnt(1)
//                    .gid(gid)
//                    .fullShortUrl(fullShortUrl)
//                    .date(new Date())
//                    .build();
//            linkBrowserStatsMapper.shortLinkBrowserState(linkBrowserStatsDO);
//            LinkDeviceStatsDO linkDeviceStatsDO = LinkDeviceStatsDO.builder()
//                    .device(statsRecord.getDevice())
//                    .cnt(1)
//                    .gid(gid)
//                    .fullShortUrl(fullShortUrl)
//                    .date(new Date())
//                    .build();
//            linkDeviceStatsMapper.shortLinkDeviceState(linkDeviceStatsDO);
//            LinkNetworkStatsDO linkNetworkStatsDO = LinkNetworkStatsDO.builder()
//                    .network(statsRecord.getNetwork())
//                    .cnt(1)
//                    .gid(gid)
//                    .fullShortUrl(fullShortUrl)
//                    .date(new Date())
//                    .build();
//            linkNetworkStatsMapper.shortLinkNetworkState(linkNetworkStatsDO);
//            LinkAccessLogsDO linkAccessLogsDO = LinkAccessLogsDO.builder()
//                    .user(statsRecord.getUv())
//                    .ip(statsRecord.getRemoteAddr())
//                    .browser(statsRecord.getBrowser())
//                    .os(statsRecord.getOs())
//                    .network(statsRecord.getNetwork())
//                    .device(statsRecord.getDevice())
//                    .locale(StrUtil.join("-", "中国", actualProvince, actualCity))
//                    .gid(gid)
//                    .fullShortUrl(fullShortUrl)
//                    .build();
//            linkAccessLogsMapper.insert(linkAccessLogsDO);
//            baseMapper.incrementStats(gid, fullShortUrl, 1, statsRecord.getUvFirstFlag() ? 1 : 0, statsRecord.getUipFirstFlag() ? 1 : 0);
//            LinkStatsTodayDO linkStatsTodayDO = LinkStatsTodayDO.builder()
//                    .todayPv(1)
//                    .todayUv(statsRecord.getUvFirstFlag() ? 1 : 0)
//                    .todayUip(statsRecord.getUipFirstFlag() ? 1 : 0)
//                    .gid(gid)
//                    .fullShortUrl(fullShortUrl)
//                    .date(new Date())
//                    .build();
//            linkStatsTodayMapper.shortLinkTodayState(linkStatsTodayDO);
//        } catch (Throwable ex) {
//            log.error("短链接访问量统计异常", ex);
//        } finally {
//            rLock.unlock();
//        }

        //将上面代码重构为消息队列，只要参数发送给生产者就好，消费者负责插入数据库
        Map<String, String> producerMap = new HashMap<>();
        producerMap.put("fullShortUrl", fullShortUrl);
        producerMap.put("gid", gid);
        producerMap.put("statsRecord", JSON.toJSONString(statsRecord));
        shortLinkStatsSaveProducer.send(producerMap);
    }


//    /**
//     * 短链接跳转时进行监控
//     * @param fullShortUrl
//     * @param gid
//     * @param request
//     * @param response
//     */
//    private void shortLinkStats(String fullShortUrl,String gid,ServletRequest request, ServletResponse response) {
//        AtomicBoolean uvFirstFlag = new AtomicBoolean();    // lambda 中不能用普通Boolean，报错未初始化
//        //        Boolean uvFirstFlag;
//        Cookie[] cookies = ((HttpServletRequest) request).getCookies(); // 获取cookie
//
//        try {
//
//            AtomicReference<String> uv = new AtomicReference<>();
//            // 定义添加cookie的Runnable
//            Runnable addResponseCookieTask = ()->{
//                // 通过cookie获取uv，同一用户访问不加次数，首次访问添加cookie并加1
//                // cookie 值用户标识
//                uv.set(UUID.fastUUID().toString());
//                Cookie uvCookie = new Cookie("uv",uv.get());
//                uvCookie.setMaxAge(60*60*24*30);   // cookie 过期时间,一个月
//                uvCookie.setPath(StrUtil.sub(fullShortUrl,fullShortUrl.indexOf("/"),fullShortUrl.length()));   // Cookie 作用域域名， /后缀
//                ((HttpServletResponse)response).addCookie(uvCookie);
//                uvFirstFlag.set(Boolean.TRUE);
//                stringRedisTemplate.opsForSet().add("short-link:stats:uv:" + fullShortUrl, uv.get());
//            };
//
//            if (ArrayUtil.isNotEmpty(cookies)){
//                Arrays.stream(cookies)
//                        .filter(each-> Objects.equals(each.getName(),"uv"))
//                        .findFirst()
//                        .map(Cookie::getValue)  // 获取cookie uv的值
//                        .ifPresentOrElse(each->{    // uv 的值非空
//                            uv.set(each);
//                            // set缓存中不存在的才添加，存在的话加不进去
//                            Long uvAdded = stringRedisTemplate.opsForSet().add("short-link:stats:uv:" + fullShortUrl, each);    // set 中已存在的话加不进去
//                            uvFirstFlag.set(uvAdded!=null&&uvAdded>0L);
//                        }, addResponseCookieTask);  // uv 的值空添加cookie
//            }else {
//                // cookies非空，不存在cookie，说明首次访问
//                addResponseCookieTask.run();
//            }
//            String remoteAddr = getActualIp((HttpServletRequest) request);
//            Long uipAdded = stringRedisTemplate.opsForSet().add("short-link:stats:uip:" + fullShortUrl, remoteAddr);
//            Boolean uipFirstFlag = uipAdded!=null&&uipAdded>0L;
//
//            if (StrUtil.isBlank(gid)) {
//                LambdaQueryWrapper<ShortLinkGotoDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkGotoDO.class)
//                        .eq(ShortLinkGotoDO::getFullShortUrl, fullShortUrl);
//                ShortLinkGotoDO shortLinkGotoDO = shortLinkGotoMapper.selectOne(queryWrapper);
//                gid = shortLinkGotoDO.getGid();
//            }
//            int hour = DateUtil.hour(new Date(), true);
//            Week week = DateUtil.dayOfWeekEnum(new Date());
//            int weekValue = week.getIso8601Value();
//            LinkAccessStatsDO linkAccessStatsDO = LinkAccessStatsDO.builder()
//                    .pv(1)
//                    .uv(uvFirstFlag.get() ? 1:0)
//                    .uip(uipFirstFlag ? 1:0)
//                    .hour(hour)
//                    .weekday(weekValue)
//                    .fullShortUrl(fullShortUrl)
//                    .gid(gid)
//                    .date(new Date())
//                    .build();
//            linkAccessStatsMapper.shortLinkStats(linkAccessStatsDO);
//            Map<String,Object> localeParamMap = new HashMap<>();
//            localeParamMap.put("key",statsLocaleAmapKey);
//            localeParamMap.put("ip",remoteAddr);
//            String localeRequestStr = HttpUtil.get(AMAP_REMOTE_URL,localeParamMap); // 请求高德接口，获取ip所在地区
//            JSONObject localeRequestObj = JSON.parseObject(localeRequestStr);
//            String infoCode = localeRequestObj.getString("infocode");
//            String actualProvince;
//            String actualCity;
//
//            if (StrUtil.isNotBlank(infoCode) && StrUtil.equals(infoCode,"1000")){   // 1000返回正常
//                String province = localeRequestObj.getString("province");
//                Boolean unknownFlag = StrUtil.equals(province,"[]");
//                LinkLocaleStatsDO linkLocaleStatsDO = LinkLocaleStatsDO.builder()
//                        .province(actualProvince = unknownFlag?"未知":province)
//                        .city(actualCity = unknownFlag?"未知":localeRequestObj.getString("city"))
//                        .adcode(unknownFlag?"未知":localeRequestObj.getString("adcode"))
//                        .cnt(1)
//                        .country("中国")
//                        .fullShortUrl(fullShortUrl)
//                        .gid(gid)
//                        .date(new Date())
//                        .build();
//                linkLocaleStatsMapper.shortLinkLocaleState(linkLocaleStatsDO);
//                String os = LinkUtil.getOs((HttpServletRequest) request);
//                LinkOsStatsDO linkOsStatsDO = LinkOsStatsDO.builder()
//                        .os(os)
//                        .cnt(1)
//                        .gid(gid)
//                        .fullShortUrl(fullShortUrl)
//                        .date(new Date())
//                        .build();
//                linkOsStatsMapper.shortLinkOsState(linkOsStatsDO);
//
//                String browser =  LinkUtil.getBrowser((HttpServletRequest)request);
//                LinkBrowserStatsDO linkBrowserStatsDO = LinkBrowserStatsDO.builder()
//                        .browser(browser)
//                        .cnt(1)
//                        .gid(gid)
//                        .fullShortUrl(fullShortUrl)
//                        .date(new Date())
//                        .build();
//                linkBrowserStatsMapper.shortLinkBrowserState(linkBrowserStatsDO);
//
//
//                String device = LinkUtil.getDevice((HttpServletRequest) request);
//                LinkDeviceStatsDO linkDeviceStatsDO = LinkDeviceStatsDO.builder()
//                        .device(LinkUtil.getDevice((HttpServletRequest) request))
//                        .cnt(1)
//                        .gid(gid)
//                        .fullShortUrl(fullShortUrl)
//                        .date(new Date())
//                        .build();
//                linkDeviceStatsMapper.shortLinkDeviceState(linkDeviceStatsDO);
//
//                String network = LinkUtil.getNetwork((HttpServletRequest) request);
//                LinkNetworkStatsDO linkNetworkStatsDO = LinkNetworkStatsDO.builder()
//                        .network(network)
//                        .cnt(1)
//                        .gid(gid)
//                        .fullShortUrl(fullShortUrl)
//                        .date(new Date())
//                        .build();
//
//                linkNetworkStatsMapper.shortLinkNetworkState(linkNetworkStatsDO);
//
//                LinkAccessLogsDO linkAccessLogsDO = LinkAccessLogsDO.builder()
//                        .user(uv.get())
//                        .ip(remoteAddr)
//                        .browser(browser)
//                        .os(os)
//                        .network(network)
//                        .device(device)
//                        .locale(StrUtil.join("-","中国",actualProvince,actualCity))
//                        .gid(gid)
//                        .fullShortUrl(fullShortUrl)
//                        .build();
//                linkAccessLogsMapper.insert(linkAccessLogsDO);
//
//                baseMapper.incrementStats(gid,fullShortUrl,1,uvFirstFlag.get()?1:0,uipFirstFlag?1:0);
//
//                LinkStatsTodayDO linkStatsTodayDO = LinkStatsTodayDO.builder()
//                        .gid(gid)
//                        .fullShortUrl(fullShortUrl)
//                        .date(new Date())
//                        .todayPv(1)
//                        .todayUv(uvFirstFlag.get()?1:0)
//                        .todayUip(uipFirstFlag?1:0)
//                        .build();
//                linkStatsTodayMapper.shortLinkTodayState(linkStatsTodayDO);
//            }
//
//        } catch (Throwable ex) {
//            log.error("短链接访问统计异常");
//        }
//
//
//    }


    private String generateSuffix(ShortLinkCreateReqDTO requestParam){
        // 取模生成的uri可能重复
        int customGenerateCount = 0;
        String shortUri;
        while (true){
            if (customGenerateCount>10) {
                throw new ServiceException("短链接频繁生成，请稍后再试");
            }
            String originUrl = requestParam.getOriginUrl();
            originUrl+=UUID.randomUUID().toString();  // url传入hash重新生成的还是原来的值，要改变url
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
            if (!shortUriCreateCachePenetrationBloomFilter.contains(createShortLinkDefaultDomain + "/" + shortUri)){    // 布隆过滤器不存在直接可以创建
                break;
            }
            // 布隆过滤器存在重新生成 suffix
            customGenerateCount++;

        }


        return shortUri;
    }

    /**
    * 根据url获取网站的图标
    * @Param: [url]
    * @return: java.lang.String
    * @Date: 2023/12/25
    */
    @SneakyThrows
    private String getFavicon(String url){
        URL targetUrl = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) targetUrl.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK){
            Document document = Jsoup.connect(url).get();
            Element faviconLink = document.select("Link[rel~=(?i)^(shortcut) ?icon]").first();
            if (faviconLink!=null){
                return faviconLink.attr("abs:href");
            }
        }
        return null;

    }

    /**
    * 验证短链接是否在白名单
    * @Param: [originUrl]
    * @return: void
    * @Date: 2024/1/2
    */

    private void verificationWhitelist(String originUrl) {
        Boolean enable = gotoDomainWhiteListConfiguration.getEnable();
        if (enable == null || !enable) {
            return;
        }
        String domain = LinkUtil.extractDomain(originUrl);
        if (StrUtil.isBlank(domain)) {
            throw new ClientException("跳转链接填写错误");
        }
        List<String> details = gotoDomainWhiteListConfiguration.getDetails();
        if (!details.contains(domain)) {
            throw new ClientException("演示环境为避免恶意攻击，请生成以下网站跳转链接：" + gotoDomainWhiteListConfiguration.getNames());
        }
    }

}
