package org.tckry.shortlink.admin.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.tckry.shortlink.admin.common.biz.user.UserContext;
import org.tckry.shortlink.admin.common.convention.exception.ServiceException;
import org.tckry.shortlink.admin.common.convention.result.Result;
import org.tckry.shortlink.admin.dao.entity.GroupDO;
import org.tckry.shortlink.admin.dao.mapper.GroupMapper;
import org.tckry.shortlink.admin.remote.ShortLinkRemoteService;
import org.tckry.shortlink.admin.remote.dto.req.ShortLinkPageReqDTO;
import org.tckry.shortlink.admin.remote.dto.req.ShortLinkRecycleBinPageReqDTO;
import org.tckry.shortlink.admin.remote.dto.resp.ShortLinkPageRespDTO;
import org.tckry.shortlink.admin.service.RecycleBinService;

import java.util.List;
import java.util.stream.Collectors;

/**
 * URL　回收站接口实现层
 * @program: shortlink
 * @description:
 * @author: lydms
 * @create: 2023-12-25 16:22
 **/
@Service
@RequiredArgsConstructor
public class RecycleBinServiceImpl implements RecycleBinService {

    private final GroupMapper groupMapper;

    // TODO 后续重构为Spring Cloud　Feign 调用
    ShortLinkRemoteService shortLinkRemoteService = new ShortLinkRemoteService(){};

    @Override
    public Result<IPage<ShortLinkPageRespDTO>> pageRecycleBinShortLink(ShortLinkRecycleBinPageReqDTO requestParam) {
        // 查当前用户下的所有分组赋值
        LambdaQueryWrapper<GroupDO> queryWrapper = Wrappers.lambdaQuery(GroupDO.class)
                .eq(GroupDO::getUsername, UserContext.getUsername())
                .eq(GroupDO::getDelFlag, 0);
        List<GroupDO> groupDOList = groupMapper.selectList(queryWrapper);
        if (CollUtil.isEmpty(groupDOList)) {
            throw new ServiceException("用户无分组信息");
        }
        requestParam.setGidList(groupDOList.stream().map(GroupDO::getGid).toList());
        return shortLinkRemoteService.pageRecycleBinShortLink(requestParam);
    }
}
