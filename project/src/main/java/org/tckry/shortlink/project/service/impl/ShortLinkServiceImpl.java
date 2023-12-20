package org.tckry.shortlink.project.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.text.StrBuilder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.tckry.shortlink.project.common.convention.exception.ServiceException;
import org.tckry.shortlink.project.config.RBloomFilterConfiguration;
import org.tckry.shortlink.project.dao.entity.ShortLinkDO;
import org.tckry.shortlink.project.dao.mapper.ShortLinkMapper;
import org.tckry.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import org.tckry.shortlink.project.dto.req.ShortLinkPageReqDTO;
import org.tckry.shortlink.project.dto.resp.ShortLinkCreateRespDTO;
import org.tckry.shortlink.project.dto.resp.ShortLinkPageRespDTO;
import org.tckry.shortlink.project.service.ShortLinkService;
import org.tckry.shortlink.project.toolkit.HashUtil;

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
        // 布隆过滤器存在误判，数据库设置唯一索引作为兜底策略，如果插入误判的值，会报索引重复的错误
        try {
            baseMapper.insert(shortLinkDO);
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
        shortUriCreateCachePenetrationBloomFilter.add(shortLinkSuffix);
        return ShortLinkCreateRespDTO.builder()
                .gid(requestParam.getGid())
                .originUrl(requestParam.getOriginUrl())
                .fullShortUrl(shortLinkDO.getFullShortUrl()).build();
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
        return resulPage.convert(each-> BeanUtil.toBean(each,ShortLinkPageRespDTO.class));
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
