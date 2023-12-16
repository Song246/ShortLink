package org.tckry.shortlink.admin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.tckry.shortlink.admin.common.convention.result.Result;
import org.tckry.shortlink.admin.common.convention.result.Results;
import org.tckry.shortlink.admin.common.enums.UserErrorCodeEnum;
import org.tckry.shortlink.admin.dto.resp.UserRespDTO;
import org.tckry.shortlink.admin.service.UserService;

/**
 * 用户管理控制层
 */
@RestController
@RequiredArgsConstructor    // 采用lombok的构造器方式注入
public class UserController {
//    不采用下面的方式，需要两行代码，且不太美观。或者采用@Resource注解替换Autowired自动注入
//    @Autowired
//    private UserService userService;

    private final UserService userService;  // 配合上面RequiredArgsConstructor以构造器的方式注入

    /**
     * 根据用户名查询应用户信息
     */
    @GetMapping("/api/shortlink/v1/user/{username}")
    public Result<UserRespDTO> getUserByUsername(@PathVariable("username") String username) {

        // 查出空的话报错
        // 1、一般不太可能在result里面做各种封装   2、controller里禁止写业务代码，
        // 所以采用统一封装Result
        UserRespDTO result = userService.getUserByUsername(username);
//        if(result==null){
//            // return new Result<UserRespDTO>().setCode(UserErrorCodeEnum.USER_NULL.code()).setMessage(UserErrorCodeEnum.USER_NULL.message());
//            return new Result<UserRespDTO>().setCode(UserErrorCodeEnum.USER_NULL.code()).setMessage(UserErrorCodeEnum.USER_NULL.message());
//        }else {
//            // return new Result<UserRespDTO>().setCode("0").setData(result);  // Results封装Result，避免每次new
//            return Results.success(result);
//        }

        // 失败的情况已经被全局异常拦截器处理了
        return Results.success(result);

    }

}
