package org.tckry.shortlink.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.tckry.shortlink.admin.dao.entity.GroupDO;
import org.tckry.shortlink.admin.dao.mapper.GroupMapper;
import org.tckry.shortlink.admin.service.GroupService;
import org.tckry.shortlink.admin.toolkit.RandomGenerator;

/**
 * 短链接分组接口实现曾
 **/
@Slf4j
@Service
public class GroupServiceImpl extends ServiceImpl<GroupMapper, GroupDO> implements GroupService {

    @Override
    public void saveGroup(String groupName) {
        String gid;
        while (true){
            gid = RandomGenerator.generateRandomString();   // 自动生成的gid万一重复，先判断生成的gid是否在数据库存在，存在的话重新生成
            if(hasGid(gid)){
                break;  // gid不存在，生成的id可用，直接break
            }
        }

        GroupDO groupDO = GroupDO.builder()
                .name(groupName)
                .gid(gid)
                .build();

        baseMapper.insert(groupDO);
    }

    private boolean hasGid(String gid){
        LambdaQueryWrapper<GroupDO> queryWrapper = Wrappers.lambdaQuery(GroupDO.class)
                .eq(GroupDO::getDelFlag,0)
                .eq(GroupDO::getGid, gid)
                // TODO 设置用户名
                .eq(GroupDO::getUsername, null);
        GroupDO hasGroupFlag = baseMapper.selectOne(queryWrapper);
        return hasGroupFlag ==null;
    }
}
