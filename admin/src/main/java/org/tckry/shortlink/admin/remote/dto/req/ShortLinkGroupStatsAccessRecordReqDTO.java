package org.tckry.shortlink.admin.remote.dto.req;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;


/**
 * 分组短链接监控访问记录请求参数
 * @program: shortlink
 * @description:
 * @author: lydms
 * @create: 2023-12-27 20:28
 **/
@Data
public class ShortLinkGroupStatsAccessRecordReqDTO extends Page {


    /**
     * 分组标识
     */
    private String gid;

    /**
     * 开始日期
     */
    private String startDate;

    /**
     * 结束日期
     */
    private String endDate;
}
