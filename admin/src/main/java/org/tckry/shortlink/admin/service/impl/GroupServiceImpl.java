package org.tckry.shortlink.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.tckry.shortlink.admin.common.biz.user.UserContext;
import org.tckry.shortlink.admin.dao.entity.GroupDO;
import org.tckry.shortlink.admin.dao.mapper.GroupMapper;
import org.tckry.shortlink.admin.dto.req.ShortLinkGroupSortReqDTO;
import org.tckry.shortlink.admin.dto.req.ShortLinkGroupUpdateReqDTO;
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
                .username(UserContext.getUsername())
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
//                .eq(GroupDO::getUsername, "mading")
                .eq(GroupDO::getUsername,UserContext.getUsername())
                .orderByDesc(GroupDO::getSortOrder, GroupDO::getUpdateTime);

        List<GroupDO> groupDOList = baseMapper.selectList(queryWrapper);
        return BeanUtil.copyToList(groupDOList, ShortLinkGroupRespDTO.class);
    }

    /**
    * 修改短链接分组名称
    * @Param: [requestParam]
    * @return: java.lang.Void
    * @Date: 2023/12/19
    */

    @Override
    public void updateGroup(ShortLinkGroupUpdateReqDTO requestParam) {
        LambdaUpdateWrapper<GroupDO> updateWrapper = Wrappers.lambdaUpdate(GroupDO.class)
                .eq(GroupDO::getUsername, UserContext.getUsername())
                .eq(GroupDO::getGid, requestParam.getGid())
                .eq(GroupDO::getDelFlag, 0);
        GroupDO groupDO = new GroupDO();
        groupDO.setName(requestParam.getName());    // 修改分组名称
        baseMapper.update(groupDO, updateWrapper);
    }

    @Override
    public void deleteGroup(String gid) {
        //采用软删除的方式，update ，delflag设为1
        LambdaUpdateWrapper<GroupDO> updateWrapper = Wrappers.lambdaUpdate(GroupDO.class)
                .eq(GroupDO::getUsername, UserContext.getUsername())
                .eq(GroupDO::getGid, gid)
                .eq(GroupDO::getDelFlag, 0);
        GroupDO groupDO = new GroupDO();
        groupDO.setDelFlag(1);
        baseMapper.update(groupDO, updateWrapper);
    }

    @Override
    public void sortGroup(List<ShortLinkGroupSortReqDTO> requestParam) {
        requestParam.forEach(each->{
            GroupDO groupDO = GroupDO.builder()
                    .sortOrder(each.getSortOrder()) // 只修改 顺序，只build的这个值
                    .build();
            LambdaUpdateWrapper<GroupDO> updateWrapper = Wrappers.lambdaUpdate(GroupDO.class)
                    .eq(GroupDO::getUsername, UserContext.getUsername())
                    .eq(GroupDO::getGid, each.getGid())
                    .eq(GroupDO::getDelFlag, 0);
            baseMapper.update(groupDO,updateWrapper);
        });
    }

    private boolean hasGid(String gid){
        LambdaQueryWrapper<GroupDO> queryWrapper = Wrappers.lambdaQuery(GroupDO.class)
                .eq(GroupDO::getDelFlag,0)
                .eq(GroupDO::getGid, gid)
                //                .eq(GroupDO::getUsername, null);
                .eq(GroupDO::getUsername,UserContext.getUsername());
        GroupDO hasGroupFlag = baseMapper.selectOne(queryWrapper);
        return hasGroupFlag ==null;
    }
}
