package org.tckry.shortlink.project.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.tckry.shortlink.project.dao.entity.LinkStatsTodayDO;
import org.tckry.shortlink.project.dao.mapper.LinkStatsTodayMapper;
import org.tckry.shortlink.project.service.LinkStatsTodayService;

/**
 * 短链接今日统计接口实现层
 * @program: shortlink
 * @description:
 * @author: lydms
 * @create: 2024-01-02 19:22
 **/
@Service
public class LinkStatsTodayServiceImpl extends ServiceImpl<LinkStatsTodayMapper, LinkStatsTodayDO> implements LinkStatsTodayService {
}