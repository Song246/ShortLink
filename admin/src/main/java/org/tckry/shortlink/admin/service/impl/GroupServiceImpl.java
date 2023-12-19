package org.tckry.shortlink.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.tckry.shortlink.admin.common.biz.user.UserContext;
import org.tckry.shortlink.admin.dao.entity.GroupDO;
import org.tckry.shortlink.admin.dao.mapper.GroupMapper;
import org.tckry.shortlink.admin.dto.resp.ShortLinkGroupRespDTO;
import org.tckry.shortlink.admin.service.GroupService;
import org.tckry.shortlink.admin.toolkit.RandomGenerator;

import java.util.List;

/**
 * 短链接分组接口实现曾
 **/
@Slf4j
@Service
public class GroupServiceImpl extends ServiceImpl<GroupMapper, GroupDO> implements GroupService {

    @Override
    public void saveGroup(String groupName) {
        String gid;
        do {
            gid = RandomGenerator.generateRandomString();   // 自动生成的gid万一重复，先判断生成的gid是否在数据库存在，存在的话重新生成
        }while (!hasGid(gid));

        GroupDO groupDO = GroupDO.builder()
                .name(groupName)
                .gid(gid)
                .sortOrder(0)
                .build();

        baseMapper.insert(groupDO);
    }

    /**
    * 查询用户短链接分组集合
    * @Param: []
    * @return: 短链接分组集合
    * @Date: 2023/12/19
    */

    @Override
    public List<ShortLinkGroupRespDTO> listGroup() {
        // TODO 从当前请求里面获取用户名，由网关管理，这里先不做
        LambdaQueryWrapper<GroupDO> queryWrapper = Wrappers.lambdaQuery(GroupDO.class)
                .eq(GroupDO::getDelFlag,0)
                .eq(GroupDO::getUsername, "mading")
                .eq(GroupDO::getUsername,UserContext.getUsername())
                .orderByDesc(GroupDO::getSortOrder, GroupDO::getUpdateTime);

        List<GroupDO> groupDOList = baseMapper.selectList(queryWrapper);
        return BeanUtil.copyToList(groupDOList, ShortLinkGroupRespDTO.class);
    }

    private boolean hasGid(String gid){
        LambdaQueryWrapper<GroupDO> queryWrapper = Wrappers.lambdaQuery(GroupDO.class)
                .eq(GroupDO::getDelFlag,0)
                .eq(GroupDO::getGid, gid)
                // TODO 设置用户名
                .eq(GroupDO::getUsername,UserContext.getUsername())
                .eq(GroupDO::getUsername, null);
        GroupDO hasGroupFlag = baseMapper.selectOne(queryWrapper);
        return hasGroupFlag ==null;
    }
}
