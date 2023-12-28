package org.tckry.shortlink.project.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.tckry.shortlink.project.dto.req.ShortLinkStatsAccessRecordReqDTO;
import org.tckry.shortlink.project.dto.req.ShortLinkStatsReqDTO;
import org.tckry.shortlink.project.dto.resp.ShortLinkStatsAccessRecordRespDTO;
import org.tckry.shortlink.project.dto.resp.ShortLinkStatsRespDTO;

/**
* 短链接监控统计接口层
* @Param:
* @return:
* @Date: 2023/12/28
*/

public interface ShortLinkStatsService {
    /**
     * 获取单个短链接监控数据
     *
     * @param requestParam 获取短链接监控数据入参
     * @return 短链接监控数据
     */
    ShortLinkStatsRespDTO oneShortLinkStats(ShortLinkStatsReqDTO requestParam);


    /**
    * 指定时间内访问记录监控数据
    * @Param: [requestParam]
    * @return: org.tckry.shortlink.project.dto.resp.ShortLinkStatsAccessRecordRespDTO
    * @Date: 2023/12/27
    */
    IPage<ShortLinkStatsAccessRecordRespDTO> shortLinkStatsAccessRecord(ShortLinkStatsAccessRecordReqDTO requestParam);
}
