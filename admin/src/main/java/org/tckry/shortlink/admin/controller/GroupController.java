package org.tckry.shortlink.admin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.tckry.shortlink.admin.common.convention.result.Result;
import org.tckry.shortlink.admin.common.convention.result.Results;
import org.tckry.shortlink.admin.dto.req.ShortLinkGroupSaveReqDTO;
import org.tckry.shortlink.admin.dto.req.ShortLinkGroupUpdateReqDTO;
import org.tckry.shortlink.admin.dto.resp.ShortLinkGroupRespDTO;
import org.tckry.shortlink.admin.service.GroupService;

import java.util.List;

/**
 * 短链接分组控制层
 **/
@RestController
@RequiredArgsConstructor
public class GroupController {
    private final GroupService groupService;

    /** 
    * 新增短连接分组
    * @Param: [requestParam]
    * @return: org.tckry.shortlink.admin.common.convention.result.Result<java.lang.Void>
    * @Date: 2023/12/19
    */
    @PostMapping("/api/short-link/v1/group")
    public Result<Void> saveGroup(@RequestBody ShortLinkGroupSaveReqDTO requestParam) {
        groupService.saveGroup(requestParam.getName());
        return Results.success();
    }

    /**
    * 查询用户短链接分组集合
    * @Param: []
    * @return:
    * @Date: 2023/12/19
    */
    @GetMapping("/api/short-link/v1/group")
    public Result<List<ShortLinkGroupRespDTO>> listGroup(){
        return Results.success(groupService.listGroup());
    }

    /** 
    * 修改短链接分组名称
    * @Param: [requestParam]
    * @return:
    * @Date: 2023/12/19
    */
    @PutMapping("/api/short-link/v1/group")
    public Result<Void> updateGroup(@RequestBody ShortLinkGroupUpdateReqDTO requestParam){
        groupService.updateGroup(requestParam);
        return Results.success();
    }

    /**
    * 删除短链接分组
    * @Param: [gid]
    * @return: org.tckry.shortlink.admin.common.convention.result.Result<java.lang.Void>
    * @Date: 2023/12/19
    */
    @DeleteMapping("/api/short-link/v1/group")
    public Result<Void> deleteGroup(@RequestParam("gid") String gid){
        groupService.deleteGroup(gid);
        return Results.success();
    }
}
