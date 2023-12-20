package org.tckry.shortlink.project.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.tckry.shortlink.project.common.convention.result.Result;
import org.tckry.shortlink.project.common.convention.result.Results;
import org.tckry.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import org.tckry.shortlink.project.dto.resp.ShortLinkCreateRespDTO;
import org.tckry.shortlink.project.service.ShortLinkService;


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
}
