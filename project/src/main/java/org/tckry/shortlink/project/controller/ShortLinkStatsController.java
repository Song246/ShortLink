package org.tckry.shortlink.project.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.tckry.shortlink.project.common.convention.result.Result;
import org.tckry.shortlink.project.common.convention.result.Results;
import org.tckry.shortlink.project.dto.req.ShortLinkGroupStatsReqDTO;
import org.tckry.shortlink.project.dto.req.ShortLinkStatsAccessRecordReqDTO;
import org.tckry.shortlink.project.dto.req.ShortLinkStatsReqDTO;
import org.tckry.shortlink.project.dto.resp.ShortLinkStatsAccessRecordRespDTO;
import org.tckry.shortlink.project.dto.resp.ShortLinkStatsRespDTO;
import org.tckry.shortlink.project.service.ShortLinkStatsService;

/**
 * 短链接监控统计控制层
 * @program: shortlink
 * @description:
 * @author: lydms
 * @create: 2023-12-27 16:01
 **/
@RestController
@RequiredArgsConstructor
public class ShortLinkStatsController {

    private final ShortLinkStatsService shortLinkStatsService;

    /**
     * 访问单个短链接指定时间内监控数据
     */
    @GetMapping("/api/short-link/v1/stats")
    public Result<ShortLinkStatsRespDTO> shortLinkStats(ShortLinkStatsReqDTO requestParam) {
        return Results.success(shortLinkStatsService.oneShortLinkStats(requestParam));
    }

    /**
    * 访问分组短链接指定时间内监控数据
    * @Param: [requestParam]
    * @return: org.tckry.shortlink.project.common.convention.result.Result<org.tckry.shortlink.project.dto.resp.ShortLinkStatsRespDTO>
    * @Date: 2023/12/29
    */
    @GetMapping("/api/short-link/v1/stats/group")
    public Result<ShortLinkStatsRespDTO> groupShortLinkStats(ShortLinkGroupStatsReqDTO requestParam) {
        return Results.success(shortLinkStatsService.groupShortLinkStats(requestParam));
    }

    /**
    * 访问单个短链接指定时间内访问记录监控数据
    * @Param: [requestParam]
    * @return: LinkStatsAccessRecordRespDTO>>
    * @Date: 2023/12/27
    */
    @GetMapping("/api/short-link/v1/stats/access-record")
    public Result<IPage<ShortLinkStatsAccessRecordRespDTO>> shortLinkStatsAccessRecord(ShortLinkStatsAccessRecordReqDTO requestParam) {
        return Results.success(shortLinkStatsService.shortLinkStatsAccessRecord(requestParam));
    }


}
