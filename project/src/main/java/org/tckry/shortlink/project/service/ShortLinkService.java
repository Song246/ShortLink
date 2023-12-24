package org.tckry.shortlink.project.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import org.tckry.shortlink.project.dao.entity.ShortLinkDO;
import org.tckry.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import org.tckry.shortlink.project.dto.req.ShortLinkPageReqDTO;
import org.tckry.shortlink.project.dto.req.ShortLinkUpdateReqDTO;
import org.tckry.shortlink.project.dto.resp.ShortLinkCreateRespDTO;
import org.tckry.shortlink.project.dto.resp.ShortLinkGroupCountQueryRespDTO;
import org.tckry.shortlink.project.dto.resp.ShortLinkPageRespDTO;

import java.io.IOException;
import java.util.List;

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

    /**
    * 查询短链接分组内数量
    * @Param: [requestParam]
    * @return: java.util.List<org.tckry.shortlink.project.dto.resp.ShortLinkGroupCountQueryRespDTO>
    * @Date: 2023/12/21
    */
    List<ShortLinkGroupCountQueryRespDTO> listGroupShortLinkCount(List<String> requestParam);

    /**
    * 修改短链接
    * @Param: [requestParam]
    * @return: void
    * @Date: 2023/12/22
    */
    void updateShortLink(ShortLinkUpdateReqDTO requestParam);

    /**
    * 短链接跳转
    * @Param: [shortUri, request, response] 短链接后缀、请求、响应
    * @return: void
    * @Date: 2023/12/23
    */
    void restoreUrl(String shortUri,ServletRequest request, ServletResponse response) throws IOException;
}
