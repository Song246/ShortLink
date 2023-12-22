package org.tckry.shortlink.project.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.tckry.shortlink.project.common.convention.result.Result;
import org.tckry.shortlink.project.common.convention.result.Results;
import org.tckry.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import org.tckry.shortlink.project.dto.req.ShortLinkPageReqDTO;
import org.tckry.shortlink.project.dto.req.ShortLinkUpdateReqDTO;
import org.tckry.shortlink.project.dto.resp.ShortLinkCreateRespDTO;
import org.tckry.shortlink.project.dto.resp.ShortLinkGroupCountQueryRespDTO;
import org.tckry.shortlink.project.dto.resp.ShortLinkPageRespDTO;
import org.tckry.shortlink.project.service.ShortLinkService;

import java.util.List;


/**
 * @program: shortlink
 * @description:
 * @author: lydms
 * @create: 2023-12-20 16:51
 **/
@RestController
@RequiredArgsConstructor
@RequestMapping
public class ShortLinkController {

    private final ShortLinkService shortLinkService;

    /** 
    * 创建短链接
    * @Param: []
    * @return: org.tckry.shortlink.project.common.convention.result.Result<java.lang.Void>
    * @Date: 2023/12/20
    */
    @PostMapping("/api/short-link/v1/create")
    public Result<ShortLinkCreateRespDTO> createShortLink(@RequestBody ShortLinkCreateReqDTO requestParam){
        return Results.success(shortLinkService.createShortLink(requestParam));
    }

    /**
    * 修改短链接
    * @Param: [requestParam]
    * @return: org.tckry.shortlink.project.common.convention.result.Result<java.lang.Void>
    * @Date: 2023/12/22
    */
    @PostMapping("/api/short-link/v1/update")
    public Result<Void> updateShortLink(@RequestBody ShortLinkUpdateReqDTO  requestParam) {
        shortLinkService.updateShortLink(requestParam);
        return Results.success();
    }

    /**
    * 分页查询短链接
    * @Param: [requestParam]
    * @return:
    * @Date: 2023/12/20
    */
    @GetMapping("/api/short-link/v1/page")
    public Result<IPage<ShortLinkPageRespDTO>> pageShortLink(ShortLinkPageReqDTO requestParam){ // GET 请求，body不带数据，这个get请求，它传递的是param（接在url后面），所以不拿requestbody接（请求体），不用@RequestBody
        return Results.success(shortLinkService.pageShortLink(requestParam));
    }

    /**
    * 查询短链接分组内数量
    * @Param: []
    * @return:
    * @Date: 2023/12/21
    */
    @GetMapping("/api/short-link/v1/count")
    public Result<List<ShortLinkGroupCountQueryRespDTO>> listGroupShortLinkCount(@RequestParam List<String> requestParam) {
        return Results.success(shortLinkService.listGroupShortLinkCount(requestParam));
    }


}
