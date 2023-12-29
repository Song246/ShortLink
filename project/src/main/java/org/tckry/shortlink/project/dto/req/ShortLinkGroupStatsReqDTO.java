package org.tckry.shortlink.project.dto.req;

import lombok.Data;

/**
 * 短链接分组监控请求参数
 * @program: shortlink
 * @description:
 * @author: lydms
 * @create: 2023-12-27 16:03
 **/
@Data
public class ShortLinkGroupStatsReqDTO {

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
