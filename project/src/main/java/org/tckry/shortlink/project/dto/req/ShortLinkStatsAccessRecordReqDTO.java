package org.tckry.shortlink.project.dto.req;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;
import org.tckry.shortlink.project.dao.entity.LinkAccessLogsDO;

/**
 * 短链接监控访问记录请求参数
 * @program: shortlink
 * @description:
 * @author: lydms
 * @create: 2023-12-27 20:28
 **/
@Data
public class ShortLinkStatsAccessRecordReqDTO extends Page<LinkAccessLogsDO> {

    /**
     * 完整短链接
     */
    private String FullShortUrl;

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
