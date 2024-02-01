package org.tckry.shortlink.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.web.bind.annotation.*;
import org.tckry.shortlink.admin.common.convention.result.Result;
import org.tckry.shortlink.admin.common.convention.result.Results;
import org.tckry.shortlink.admin.remote.ShortLinkActualRemoteService;
import org.tckry.shortlink.admin.remote.dto.req.ShortLinkBatchCreateReqDTO;
import org.tckry.shortlink.admin.remote.dto.req.ShortLinkUpdateReqDTO;
import org.tckry.shortlink.admin.remote.dto.req.ShortLinkCreateReqDTO;
import org.tckry.shortlink.admin.remote.dto.req.ShortLinkPageReqDTO;
import org.tckry.shortlink.admin.remote.dto.resp.ShortLinkBaseInfoRespDTO;
import org.tckry.shortlink.admin.remote.dto.resp.ShortLinkBatchCreateRespDTO;
import org.tckry.shortlink.admin.remote.dto.resp.ShortLinkCreateRespDTO;
import org.tckry.shortlink.admin.remote.dto.resp.ShortLinkPageRespDTO;
import org.tckry.shortlink.admin.toolkit.EasyExcelWebUtil;

import java.util.List;

/**
 * 短链接后管控制层,调用Project中台
 * @program: shortlink
 * @description:
 * @author: lydms
 * @create: 2023-12-21 15:12
 **/
@RestController
@RequiredArgsConstructor
public class ShortLinkController {

    private final ShortLinkActualRemoteService shortLinkActualRemoteService;

    /**
    * 创建短链接
    * @Param: [requestParam]
    * @return:
    * @Date: 2023/12/21
    */
    @PostMapping("/api/short-link/admin/v1/create")
    public Result<ShortLinkCreateRespDTO> createShortLink(@RequestBody ShortLinkCreateReqDTO requestParam){
        return shortLinkActualRemoteService.createShortLink(requestParam);
    }

    /**
     * 批量创建短链接
     * @Param: [requestParam]
     * @return: org.tckry.shortlink.project.common.convention.result.Result<ShortLinkBatchCreateRespDTO>
     * @Date: 2023/12/30
     */
    @SneakyThrows
    @PostMapping("/api/short-link/admin/v1/create/batch")
    public void batchCreateShortLink(@RequestBody ShortLinkBatchCreateReqDTO requestParam, HttpServletResponse response) {
        Result<ShortLinkBatchCreateRespDTO> shortLinkBatchCreateRespDTOResult = shortLinkActualRemoteService.batchCreateShortLink(requestParam);
        if (shortLinkBatchCreateRespDTOResult.isSuccess()) {
            List<ShortLinkBaseInfoRespDTO> baseLinkInfos = shortLinkBatchCreateRespDTOResult.getData().getBaseLinkInfos();
            EasyExcelWebUtil.write(response, "批量创建短链接-SaaS短链接系统", ShortLinkBaseInfoRespDTO.class, baseLinkInfos);   // 将批量创建的短链接返回给前端excel形式下载
        }
    }
    /**
     * 修改短链接
     * @Param: [requestParam]
     * @return: org.tckry.shortlink.project.common.convention.result.Result<java.lang.Void>
     * @Date: 2023/12/22
     */
    @PostMapping("/api/short-link/admin/v1/update")
    public Result<Void> updateShortLink(@RequestBody ShortLinkUpdateReqDTO requestParam) {
        shortLinkActualRemoteService.updateShortLink(requestParam);
        return Results.success();
    }

    /**
    * 分页查询短链接
    * @Param: [requestParam]
    * @return:
    * @Date: 2023/12/21
    */
    @GetMapping("/api/short-link/admin/v1/page")
    public Result<Page<ShortLinkPageRespDTO>> pageShortLink(ShortLinkPageReqDTO requestParam){
        return shortLinkActualRemoteService.pageShortLink(requestParam.getGid(),requestParam.getOrderTag(),requestParam.getCurrent(),requestParam.getSize());

    }

}
