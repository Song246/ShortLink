package org.tckry.shortlink.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import org.springframework.web.bind.annotation.*;
import org.tckry.shortlink.admin.common.convention.result.Result;
import org.tckry.shortlink.admin.common.convention.result.Results;
import org.tckry.shortlink.admin.remote.dto.req.ShortLinkBatchCreateReqDTO;
import org.tckry.shortlink.admin.remote.dto.req.ShortLinkUpdateReqDTO;
import org.tckry.shortlink.admin.remote.ShortLinkRemoteService;
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
public class ShortLinkController {

    // TODO 后续重构为Spring Cloud　Feign 调用
    ShortLinkRemoteService shortLinkRemoteService = new ShortLinkRemoteService(){};

    /**
    * 创建短链接
    * @Param: [requestParam]
    * @return:
    * @Date: 2023/12/21
    */
    @PostMapping("/api/short-link/admin/v1/create")
    public Result<ShortLinkCreateRespDTO> createShortLink(@RequestBody ShortLinkCreateReqDTO requestParam){
        return shortLinkRemoteService.createShortLink(requestParam);
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
        Result<ShortLinkBatchCreateRespDTO> shortLinkBatchCreateRespDTOResult = shortLinkRemoteService.batchCreateShortLink(requestParam);
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
        shortLinkRemoteService.updateShortLink(requestParam);
        return Results.success();
    }

    /**
    * 分页查询短链接
    * @Param: [requestParam]
    * @return:
    * @Date: 2023/12/21
    */
    @GetMapping("/api/short-link/admin/v1/page")
    public Result<IPage<ShortLinkPageRespDTO>> pageShortLink(ShortLinkPageReqDTO requestParam){
        return shortLinkRemoteService.pageShortLink(requestParam);

    }

}
