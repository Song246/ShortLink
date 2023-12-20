package org.tckry.shortlink.project.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.tckry.shortlink.project.dao.entity.ShortLinkDO;
import org.tckry.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import org.tckry.shortlink.project.dto.req.ShortLinkPageReqDTO;
import org.tckry.shortlink.project.dto.resp.ShortLinkCreateRespDTO;
import org.tckry.shortlink.project.dto.resp.ShortLinkPageRespDTO;

/**
 * 短链接接口层
 * @program: shortlink
 * @description:
 * @author: lydms
 * @create: 2023-12-20 16:36
 **/
public interface ShortLinkService extends IService<ShortLinkDO> {

    /**
    * 创建短链接
    * @Param: [requestParam]
    * @return: org.tckry.shortlink.project.dto.resp.ShortLinkCreateRespDTO
    * @Date: 2023/12/20
    */
    ShortLinkCreateRespDTO createShortLink(ShortLinkCreateReqDTO requestParam);

    /**
    * 分页查询短链接
    * @Param: [requestParam]
    * @return:
    * @Date: 2023/12/20
    */

    IPage<ShortLinkPageRespDTO> pageShortLink(ShortLinkPageReqDTO requestParam);
}
