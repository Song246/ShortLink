package org.tckry.shortlink.admin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.tckry.shortlink.admin.common.convention.result.Result;
import org.tckry.shortlink.admin.remote.ShortLinkActualRemoteService;


/**
 * URL标题控制层
 * @program: shortlink
 * @description:
 * @author: lydms
 * @create: 2023-12-24 19:53
 **/
@RestController
@RequiredArgsConstructor
public class UrlTitleController {

    private final ShortLinkActualRemoteService shortLinkActualRemoteService;

    /**
    * 根据URL获取对应网站标题
    * @Param: [url]
    * @return: org.tckry.shortlink.project.common.convention.result.Result<java.lang.String>
    * @Date: 2023/12/24
    */
    @GetMapping("/api/short-link/admin/v1/title")
    public Result<String> getTitleByUrl(@RequestParam("url") String url){
        return shortLinkActualRemoteService.getTitleByUrl(url);
    }


}
