package org.tckry.shortlink.project.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 短链接不存在跳转控制器
 * @program: shortlink
 * @description:
 * @author: lydms
 * @create: 2023-12-24 19:36
 **/
@Controller // 自定义Controller，返回的会先去视图（前端页面）里面匹配，若是RestController会返回的数据转为json
public class ShortLinkNotfoundController {

    /**
    * 短链接不存在跳转页面
    * @Param: []
    * @return: java.lang.String
    * @Date: 2023/12/24
    */
    @RequestMapping("/page/notfound")
    public String notfound(){
        return "notfound";
    }
}
