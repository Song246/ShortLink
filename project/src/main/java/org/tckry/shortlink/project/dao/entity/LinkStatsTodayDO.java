package org.tckry.shortlink.project.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.tckry.shortlink.project.common.database.BaseDO;

import java.util.Date;

/**
 * 短链接今日统计实体
 * @program: shortlink
 * @description:
 * @author: lydms
 * @create: 2023-12-28 15:36
 **/
@TableName("t_link_stats_today")
@Data
public class LinkStatsTodayDO extends BaseDO {

    /**
     * id
     */
    private Long id;

    /**
     * 分组标识
     */
    private String gid;

    /**
     * 短链接
     */
    private String fullShortUrl;

    /**
     * 日期
     */
    private Date date;

    /**
     * 今日pv
     */
    private Integer todayPv;

    /**
     * 今日uv
     */
    private Integer todayUv;

    /**
     * 今日ip数
     */
    private Integer todayUip;


}