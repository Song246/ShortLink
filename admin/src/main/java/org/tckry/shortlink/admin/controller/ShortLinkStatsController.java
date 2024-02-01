package org.tckry.shortlink.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.tckry.shortlink.admin.common.convention.result.Result;
import org.tckry.shortlink.admin.common.convention.result.Results;
import org.tckry.shortlink.admin.remote.ShortLinkActualRemoteService;
import org.tckry.shortlink.admin.remote.dto.req.ShortLinkGroupStatsAccessRecordReqDTO;
import org.tckry.shortlink.admin.remote.dto.req.ShortLinkGroupStatsReqDTO;
import org.tckry.shortlink.admin.remote.dto.req.ShortLinkStatsAccessRecordReqDTO;
import org.tckry.shortlink.admin.remote.dto.req.ShortLinkStatsReqDTO;
import org.tckry.shortlink.admin.remote.dto.resp.ShortLinkStatsAccessRecordRespDTO;
import org.tckry.shortlink.admin.remote.dto.resp.ShortLinkStatsRespDTO;

/**
 * 短链接监控控制层
 * @program: shortlink
 * @description:
 * @author: lydms
 * @create: 2023-12-27 16:21
 **/
@RestController
@RequiredArgsConstructor
public class ShortLinkStatsController {

    private final ShortLinkActualRemoteService shortLinkActualRemoteService;

    /**
    * 访问单个短链接指定时间内监控数据
    * @Param: [requestParam]
    * @return: org.tckry.shortlink.admin.common.convention.result.Result<org.tckry.shortlink.admin.remote.dto.resp.ShortLinkStatsRespDTO>
    * @Date: 2023/12/27
    */
    @GetMapping("/api/short-link/admin/v1/stats")
    public Result<ShortLinkStatsRespDTO> shortLinkStats(ShortLinkStatsReqDTO requestParam){
        return shortLinkActualRemoteService.oneShortLinkStats(requestParam.getFullShortUrl(),requestParam.getGid(),requestParam.getStartDate(),requestParam.getEndDate());
    }

    /**
     * 访问分组短链接指定时间内监控数据
     * @Param: [requestParam]
     * @return:
     * @Date: 2023/12/29
     */
    @GetMapping("/api/short-link/admin/v1/stats/group")
    public Result<ShortLinkStatsRespDTO> groupShortLinkStats(ShortLinkGroupStatsReqDTO requestParam) {
        return shortLinkActualRemoteService.groupShortLinkStats(requestParam.getGid(),requestParam.getStartDate(),requestParam.getEndDate());
    }

    /**
    * 访问单个短链接指定时间内监控访问记录数据
    * @Param: [requestParam]
    * @return:
    * @Date: 2023/12/29
    */
    @GetMapping("/api/short-link/admin/v1/stats/access-record")
    public Result<Page<ShortLinkStatsAccessRecordRespDTO>> shortLinkStatsAccessRecord(ShortLinkStatsAccessRecordReqDTO requestParam) {
        return shortLinkActualRemoteService.shortLinkStatsAccessRecord(requestParam.getFullShortUrl(),requestParam.getGid(),requestParam.getStartDate(),requestParam.getEndDate());
    }

    /**
     * 访问分组短链接指定时间内访问记录监控数据
     * @Param: [requestParam]
     * @return: LinkStatsAccessRecordRespDTO>>
     * @Date: 2023/12/27
     */
    @GetMapping("/api/short-link/admin/v1/stats/access-record/group")
    public Result<Page<ShortLinkStatsAccessRecordRespDTO>> groupShortLinkStatsAccessRecord(ShortLinkGroupStatsAccessRecordReqDTO requestParam) {
        return shortLinkActualRemoteService.groupShortLinkStatsAccessRecord(requestParam.getGid(),requestParam.getStartDate(),requestParam.getEndDate());
    }


}
