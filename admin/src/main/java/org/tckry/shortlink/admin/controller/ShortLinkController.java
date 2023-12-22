package org.tckry.shortlink.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.web.bind.annotation.*;
import org.tckry.shortlink.admin.common.convention.result.Result;
import org.tckry.shortlink.admin.common.convention.result.Results;
import org.tckry.shortlink.admin.dto.req.ShortLinkUpdateReqDTO;
import org.tckry.shortlink.admin.remote.ShortLinkRemoteService;
import org.tckry.shortlink.admin.remote.dto.req.ShortLinkCreateReqDTO;
import org.tckry.shortlink.admin.remote.dto.req.ShortLinkPageReqDTO;
import org.tckry.shortlink.admin.remote.dto.resp.ShortLinkCreateRespDTO;
import org.tckry.shortlink.admin.remote.dto.resp.ShortLinkPageRespDTO;

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
     * 修改短链接
     * @Param: [requestParam]
     * @return: org.tckry.shortlink.project.common.convention.result.Result<java.lang.Void>
     * @Date: 2023/12/22
     */
    @PutMapping("/api/short-link/admin/v1/update")
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
