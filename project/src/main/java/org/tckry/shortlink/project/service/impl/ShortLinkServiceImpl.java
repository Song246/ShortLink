package org.tckry.shortlink.project.service.impl;

import cn.hutool.core.bean.BeanUtil;
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
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tckry.shortlink.project.common.convention.exception.ClientException;
import org.tckry.shortlink.project.common.convention.exception.ServiceException;
import org.tckry.shortlink.project.common.database.BaseDO;
import org.tckry.shortlink.project.common.enums.ValiDateTypeEnum;
import org.tckry.shortlink.project.dao.entity.*;
import org.tckry.shortlink.project.dao.mapper.*;
import org.tckry.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import org.tckry.shortlink.project.dto.req.ShortLinkPageReqDTO;
import org.tckry.shortlink.project.dto.req.ShortLinkUpdateReqDTO;
import org.tckry.shortlink.project.dto.resp.ShortLinkCreateRespDTO;
import org.tckry.shortlink.project.dto.resp.ShortLinkGroupCountQueryRespDTO;
import org.tckry.shortlink.project.dto.resp.ShortLinkPageRespDTO;
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
    private LinkDeviceStatsMapper linkDeviceStatsMapper;


    @Value("${short-link.stats.locale.amap-key}")
    private String statsLocaleAmapKey;

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
                .favicon(getFavicon(requestParam.getOriginUrl()))
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
        LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                .eq(ShortLinkDO::getGid, requestParam.getGid())
                .eq(ShortLinkDO::getEnableStatus, 0)
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

    @SneakyThrows   //
    @Override
    public void restoreUrl(String shortUri, ServletRequest request, ServletResponse response) {
        // 传入的短链接，先通过短链接去获取gid，再通过gid获取完整连接名跳转
        String serverName = request.getServerName();    // 域名
        String fullShortUrl = serverName + "/" + shortUri;
        String originalLink = stringRedisTemplate.opsForValue().get(String.format(GOTO_SHORT_LINK_KEY, fullShortUrl));
        if (StrUtil.isNotBlank(originalLink)){  // 缓存中存在原始连接直接跳转
            shortLinkStats(fullShortUrl,null,request,response);
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
                shortLinkStats(fullShortUrl,null,request,response);
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
            // 1000个相同请求数据xredis不存在，去请求数据库，最先获取到锁的把x加入缓存， 后面999个就不会去数据库了
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
            shortLinkStats(fullShortUrl,shortLinkDO.getGid(),request,response);
            ((HttpServletResponse)response).sendRedirect(shortLinkDO.getFullShortUrl());

        } finally {  // 最后解锁
            lock.unlock();
        }


    }



    private void shortLinkStats(String fullShortUrl,String gid,ServletRequest request, ServletResponse response) {
        AtomicBoolean uvFirstFlag = new AtomicBoolean();    // lambda 中不能用普通Boolean，报错未初始化
        //        Boolean uvFirstFlag;
        Cookie[] cookies = ((HttpServletRequest) request).getCookies(); // 获取cookie

        try {

            AtomicReference<String> uv = new AtomicReference<>();
            // 定义添加cookie的Runnable
            Runnable addResponseCookieTask = ()->{
                // 通过cookie获取uv，同一用户访问不加次数，首次访问添加cookie并加1
                // cookie 值用户标识
                uv.set(UUID.fastUUID().toString());
                Cookie uvCookie = new Cookie("uv",uv.get());
                uvCookie.setMaxAge(60*60*24*30);   // cookie 过期时间,一个月
                uvCookie.setPath(StrUtil.sub(fullShortUrl,fullShortUrl.indexOf("/"),fullShortUrl.length()));   // Cookie 作用域域名， /后缀
                ((HttpServletResponse)response).addCookie(uvCookie);
                uvFirstFlag.set(Boolean.TRUE);
                stringRedisTemplate.opsForSet().add("short-link:stats:uv:" + fullShortUrl, uv.get());
            };

            if (ArrayUtil.isNotEmpty(cookies)){
                Arrays.stream(cookies)
                        .filter(each-> Objects.equals(each.getName(),"uv"))
                        .findFirst()
                        .map(Cookie::getValue)  // 获取cookie uv的值
                        .ifPresentOrElse(each->{    // uv 的值非空
                            uv.set(each);
                            // set缓存中不存在的才添加，存在的话加不进去
                            Long uvAdded = stringRedisTemplate.opsForSet().add("short-link:stats:uv:" + fullShortUrl, each);    // set 中已存在的话加不进去
                            uvFirstFlag.set(uvAdded!=null&&uvAdded>0L);
                        }, addResponseCookieTask);  // uv 的值空添加cookie
            }else {
                // cookies非空，不存在cookie，说明首次访问
                addResponseCookieTask.run();
            }
            String remoteAddr = getActualIp((HttpServletRequest) request);
            Long uipAdded = stringRedisTemplate.opsForSet().add("short-link:stats:uip:" + fullShortUrl, remoteAddr);
            Boolean uipFirstFlag = uipAdded!=null&&uipAdded>0L;

            if (StrUtil.isBlank(gid)) {
                LambdaQueryWrapper<ShortLinkGotoDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkGotoDO.class)
                        .eq(ShortLinkGotoDO::getFullShortUrl, fullShortUrl);
                ShortLinkGotoDO shortLinkGotoDO = shortLinkGotoMapper.selectOne(queryWrapper);
                gid = shortLinkGotoDO.getGid();
            }
            int hour = DateUtil.hour(new Date(), true);
            Week week = DateUtil.dayOfWeekEnum(new Date());
            int weekValue = week.getIso8601Value();
            LinkAccessStatsDO linkAccessStatsDO = LinkAccessStatsDO.builder()
                    .pv(1)
                    .uv(uvFirstFlag.get() ? 1:0)
                    .uip(uipFirstFlag ? 1:0)
                    .hour(hour)
                    .weekday(weekValue)
                    .fullShortUrl(fullShortUrl)
                    .gid(gid)
                    .date(new Date())
                    .build();
            linkAccessStatsMapper.shortLinkStats(linkAccessStatsDO);
            Map<String,Object> localeParamMap = new HashMap<>();
            localeParamMap.put("key",statsLocaleAmapKey);
            localeParamMap.put("ip",remoteAddr);
            String localeRequestStr = HttpUtil.get(AMAP_REMOTE_URL,localeParamMap); // 请求高德接口，获取ip所在地区
            JSONObject localeRequestObj = JSON.parseObject(localeRequestStr);
            String infoCode = localeRequestObj.getString("infocode");

            if (StrUtil.isNotBlank(infoCode) && StrUtil.equals(infoCode,"1000")){   // 1000返回正常
                String province = localeRequestObj.getString("province");
                Boolean unknownFlag = StrUtil.equals(province,"[]");
                LinkLocaleStatsDO linkLocaleStatsDO = LinkLocaleStatsDO.builder()
                        .province(unknownFlag?"未知":province)
                        .city(unknownFlag?"未知":localeRequestObj.getString("city"))
                        .adcode(unknownFlag?"未知":localeRequestObj.getString("adcode"))
                        .cnt(1)
                        .country("中国")
                        .fullShortUrl(fullShortUrl)
                        .gid(gid)
                        .date(new Date())
                        .build();
                linkLocaleStatsMapper.shortLinkLocaleState(linkLocaleStatsDO);
                String os = LinkUtil.getOs((HttpServletRequest) request);
                LinkOsStatsDO linkOsStatsDO = LinkOsStatsDO.builder()
                        .os(os)
                        .cnt(1)
                        .gid(gid)
                        .fullShortUrl(fullShortUrl)
                        .date(new Date())
                        .build();
                linkOsStatsMapper.shortLinkOsState(linkOsStatsDO);

                String browser =  LinkUtil.getBrowser((HttpServletRequest)request);
                LinkBrowserStatsDO linkBrowserStatsDO = LinkBrowserStatsDO.builder()
                        .browser(browser)
                        .cnt(1)
                        .gid(gid)
                        .fullShortUrl(fullShortUrl)
                        .date(new Date())
                        .build();
                linkBrowserStatsMapper.shortLinkBrowserState(linkBrowserStatsDO);

                LinkAccessLogsDO linkAccessLogsDO = LinkAccessLogsDO.builder()
                        .user(uv.get())
                        .ip(remoteAddr)
                        .browser(browser)
                        .os(os)
                        .gid(gid)
                        .fullShortUrl(fullShortUrl)
                        .build();
                linkAccessLogsMapper.insert(linkAccessLogsDO);

                LinkDeviceStatsDO linkDeviceStatsDO = LinkDeviceStatsDO.builder()
                        .device(LinkUtil.getDevice((HttpServletRequest) request))
                        .cnt(1)
                        .gid(gid)
                        .fullShortUrl(fullShortUrl)
                        .date(new Date())
                        .build();
                linkDeviceStatsMapper.shortLinkDeviceState(linkDeviceStatsDO);
            }




        } catch (Throwable ex) {
            log.error("短链接访问统计异常");
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
}
