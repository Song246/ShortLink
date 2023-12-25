package org.tckry.shortlink.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.tckry.shortlink.admin.common.convention.result.Result;
import org.tckry.shortlink.admin.common.convention.result.Results;
import org.tckry.shortlink.admin.dto.req.RecycleBinRecoverReqDTO;
import org.tckry.shortlink.admin.dto.req.RecycleBinRemoveReqDTO;
import org.tckry.shortlink.admin.dto.req.RecycleBinSaveReqDTO;
import org.tckry.shortlink.admin.remote.ShortLinkRemoteService;
import org.tckry.shortlink.admin.remote.dto.req.ShortLinkPageReqDTO;
import org.tckry.shortlink.admin.remote.dto.req.ShortLinkRecycleBinPageReqDTO;
import org.tckry.shortlink.admin.remote.dto.resp.ShortLinkPageRespDTO;
import org.tckry.shortlink.admin.service.RecycleBinService;

/**
 * 回收站控制层
 * @program: shortlink
 * @description:
 * @author: lydms
 * @create: 2023-12-25 14:54
 **/
@RestController
@RequiredArgsConstructor
public class RecycleBinController {

    private RecycleBinService recycleBinService;

    // TODO 后续重构为Spring Cloud　Feign 调用
    ShortLinkRemoteService shortLinkRemoteService = new ShortLinkRemoteService(){};

    /**
    * 保存回收站
    * @Param: [requestParam]
    * @return: org.tckry.shortlink.admin.common.convention.result.Result<java.lang.Void>
    * @Date: 2023/12/25
    */
    @PostMapping("/api/short-link/admin/v1/recycle-bin/save")
    public Result<Void> saveRecycleBin(@RequestBody RecycleBinSaveReqDTO requestParam) {
        shortLinkRemoteService.saveRecycleBin(requestParam);
        return Results.success();
    }

    /**
     * 分页查询回收站短链接
     * @Param: [requestParam]
     * @return:
     * @Date: 2023/12/21
     */
    @GetMapping("/api/short-link/admin/v1/recycle-bin/page")
    public Result<IPage<ShortLinkPageRespDTO>> pageShortLink(@RequestBody ShortLinkRecycleBinPageReqDTO requestParam){
        return recycleBinService.pageRecycleBinShortLink(requestParam);

    }

    /**
     * 恢复短链接
     * @Param: [requestParam]
     * @return: org.tckry.shortlink.project.common.convention.result.Result<java.lang.Void>
     * @Date: 2023/12/25
     */
    @PostMapping("/api/short-link/admin/v1/recycle-bin/recover")
    public Result<Void> recoverRecycleBin(@RequestBody RecycleBinRecoverReqDTO requestParam) {
        shortLinkRemoteService.recoverRecycleBin(requestParam);
        return Results.success();
    }

    /**
     * 移除短链接
     * @Param: [requestParam]
     * @return: org.tckry.shortlink.project.common.convention.result.Result<java.lang.Void>
     * @Date: 2023/12/25
     */
    @PostMapping("/api/short-link/admin/v1/recycle-bin/remove")
    public Result<Void> removeRecycleBin(@RequestBody RecycleBinRemoveReqDTO requestParam) {
        shortLinkRemoteService.removeRecycleBin(requestParam);
        return Results.success();
    }
}
