package org.tckry.shortlink.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.tckry.shortlink.project.dao.entity.LinkOsStatsDO;
import org.tckry.shortlink.project.dao.entity.LinkStatsTodayDO;

/**
* 短链接今日统计持久层
* @Param:
* @return:
* @Date: 2023/12/28
*/

public interface LinkStatsTodayMapper extends BaseMapper<LinkStatsTodayDO> {

    /**
     * 记录今日统计监控数据
     */
    @Insert("INSERT INTO t_link_stats_today (full_short_url, gid, date, today_pv,today_uv, today_uip,create_time, update_time, del_flag) " +
            "VALUES( #{linkTodayStats.fullShortUrl}, #{linkTodayStats.gid}, #{linkTodayStats.date}, #{linkTodayStats.todayPv},  #{linkTodayStats.todayUv}, #{linkTodayStats.todayUip},NOW(), NOW(), 0) " +
            "ON DUPLICATE KEY UPDATE today_uv = today_uv +  #{linkTodayStats.todayUv},today_pv = today_pv +  #{linkTodayStats.todayPv},today_uip = today_uip +  #{linkTodayStats.todayUip};")
    void shortLinkTodayState(@Param("linkTodayStats") LinkStatsTodayDO linkStatsTodayDO);


}
