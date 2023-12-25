package org.tckry.shortlink.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.tckry.shortlink.admin.common.convention.result.Result;
import org.tckry.shortlink.admin.remote.dto.req.ShortLinkPageReqDTO;
import org.tckry.shortlink.admin.remote.dto.req.ShortLinkRecycleBinPageReqDTO;
import org.tckry.shortlink.admin.remote.dto.resp.ShortLinkPageRespDTO;

/**
 * URL　回收站接口层
 * @program: shortlink
 * @description:
 * @author: lydms
 * @create: 2023-12-25 16:22
 **/
public interface RecycleBinService {

    /**
    * 分页查询回收站连接
    * @Param: [requestParam]
    * @return: org.tckry.shortlink.admin.common.convention.result.Result<com.baomidou.mybatisplus.core.metadata.IPage<org.tckry.shortlink.admin.remote.dto.resp.ShortLinkPageRespDTO>>
    * @Date: 2023/12/25
    */

    Result<IPage<ShortLinkPageRespDTO>> pageRecycleBinShortLink(ShortLinkRecycleBinPageReqDTO requestParam);
}
