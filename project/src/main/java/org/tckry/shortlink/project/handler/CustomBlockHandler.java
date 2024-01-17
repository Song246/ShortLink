package org.tckry.shortlink.project.handler;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import org.tckry.shortlink.project.common.convention.result.Result;
import org.tckry.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import org.tckry.shortlink.project.dto.resp.ShortLinkCreateRespDTO;

/**
 * @program: shortlink
 * @description:
 * @author: lydms
 * @create: 2024-01-17 20:10
 **/
public class CustomBlockHandler {
    public static Result<ShortLinkCreateRespDTO> createShortLinkBlockHandlerMethod(ShortLinkCreateReqDTO requestParam, BlockException ex) {
        return new Result<ShortLinkCreateRespDTO>().setCode("B100000").setMessage("当前访问人数过多，请稍后重试");
    }
}
