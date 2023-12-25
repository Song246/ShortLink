package org.tckry.shortlink.project.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.tckry.shortlink.project.dao.entity.ShortLinkDO;
import org.tckry.shortlink.project.dao.mapper.ShortLinkMapper;
import org.tckry.shortlink.project.dto.req.*;
import org.tckry.shortlink.project.dto.resp.ShortLinkPageRespDTO;
import org.tckry.shortlink.project.service.RecycleBinService;

import java.sql.Wrapper;

import static org.tckry.shortlink.project.common.constant.RedisKeyConstant.GOTO_IS_NULL_SHORT_LINK_KEY;
import static org.tckry.shortlink.project.common.constant.RedisKeyConstant.GOTO_SHORT_LINK_KEY;

/**
 * 回收站管理接口实现层
 * @program: shortlink
 * @description:
 * @author: lydms
 * @create: 2023-12-25 15:03
 **/
@Service
@RequiredArgsConstructor
public class RecycleBinServiceImpl extends ServiceImpl<ShortLinkMapper, ShortLinkDO> implements RecycleBinService {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void saveRecycleBin(RecycleBinSaveReqDTO requestParam) {
        LambdaUpdateWrapper<ShortLinkDO> updateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                .eq(ShortLinkDO::getGid, requestParam.getGid())
                .eq(ShortLinkDO::getEnableStatus,0)
                .eq(ShortLinkDO::getDelFlag,0);
        ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                .enableStatus(1)
                .build();
        baseMapper.update(shortLinkDO,updateWrapper);
        // 移到回收站，删除对应缓存
        stringRedisTemplate.delete(String.format(GOTO_IS_NULL_SHORT_LINK_KEY,requestParam.getFullShortUrl()));



    }

    @Override
    public IPage<ShortLinkPageRespDTO> pageShortLink(ShortLinkRecycleBinPageReqDTO requestParam) {
        LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                .in(ShortLinkDO::getGid, requestParam.getGidList())
                .eq(ShortLinkDO::getEnableStatus, 1)
                .eq(ShortLinkDO::getDelFlag, 0)
                .orderByDesc(ShortLinkDO::getUpdateTime);
        IPage<ShortLinkDO> resulPage = baseMapper.selectPage(requestParam,queryWrapper);   //ShortLinkPageReqDTO 继承了Page对象
        // ShortLinkDO 转 ShortLinkPageRespDTO
        return resulPage.convert(each-> {
                    ShortLinkPageRespDTO result = BeanUtil.toBean(each, ShortLinkPageRespDTO.class);
                    result.setDomain("http://"+result.getDomain());
                    return result;
                }
        );
    }

    @Override
    public void recoverRecycleBin(RecycleBinRecoverReqDTO requestParam) {
        LambdaUpdateWrapper<ShortLinkDO> updateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                .eq(ShortLinkDO::getGid, requestParam.getGid())
                .eq(ShortLinkDO::getEnableStatus,1)
                .eq(ShortLinkDO::getDelFlag,0);
        ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                .enableStatus(0)
                .build();
        baseMapper.update(shortLinkDO,updateWrapper);
        // 恢复，不做缓存预热，只把删除时在缓存中的 null 删除
        stringRedisTemplate.delete(String.format(GOTO_IS_NULL_SHORT_LINK_KEY,requestParam.getFullShortUrl()));

    }

    @Override
    public void removeRecycleBin(RecycleBinRemoveReqDTO requestParam) {
        LambdaUpdateWrapper<ShortLinkDO> updateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                .eq(ShortLinkDO::getGid, requestParam.getGid())
                .eq(ShortLinkDO::getEnableStatus,1)
                .eq(ShortLinkDO::getDelFlag,0);
        //        ShortLinkDO shortLinkDO = ShortLinkDO.builder()
        //                .enableStatus(1)
        //                .build();
        baseMapper.delete(updateWrapper);
    }
}
