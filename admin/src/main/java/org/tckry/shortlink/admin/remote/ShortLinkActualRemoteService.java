package org.tckry.shortlink.admin.remote;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import org.tckry.shortlink.admin.common.convention.result.Result;
import org.tckry.shortlink.admin.dto.req.RecycleBinRecoverReqDTO;
import org.tckry.shortlink.admin.dto.req.RecycleBinRemoveReqDTO;
import org.tckry.shortlink.admin.dto.req.RecycleBinSaveReqDTO;
import org.tckry.shortlink.admin.remote.dto.req.*;
import org.tckry.shortlink.admin.remote.dto.resp.*;

import java.util.List;

/**
* 短链接中台远程调用服务(微服务feign)
* @Param: 
* @return: 
* @Date: 2024/2/1
*/

@FeignClient("short-link-project")  // 调用project
public interface ShortLinkActualRemoteService {

    /**
     * 创建短链接
     * @Param: [requestParam]
     * @return:
     * @Date: 2023/12/21
     */
    @PostMapping("/api/short-link/v1/create")
    Result<ShortLinkCreateRespDTO> createShortLink(@RequestBody  ShortLinkCreateReqDTO requestParam);

    /**
     * 批量创建短链接
     * @Param: [requestParam]
     * @return: org.tckry.shortlink.admin.common.convention.result.Result<org.tckry.shortlink.admin.remote.dto.resp.ShortLinkBatchCreateRespDTO>
     * @Date: 2023/12/30
     */
     @PostMapping("/api/short-link/v1/create/batch")
     Result<ShortLinkBatchCreateRespDTO> batchCreateShortLink(@RequestBody ShortLinkBatchCreateReqDTO requestParam);


    /**
     * 短链接修改
     * @Param: [requestParam]
     * @return: void
     * @Date: 2023/12/22
     */
    @PostMapping("/api/short-link/v1/update")
    void updateShortLink(@RequestBody ShortLinkUpdateReqDTO requestParam);

    /**
    * 分页查询短链接
    * @Param: [gid, orderTag, current, size]
    * @return:
    * @Date: 2024/2/1
    */
    @GetMapping("/api/short-link/v1/page")
    Result<Page<ShortLinkPageRespDTO>> pageShortLink(@RequestParam("gid") String gid,
                                                     @RequestParam("orderTag") String orderTag,
                                                     @RequestParam("current")Long current,
                                                     @RequestParam("size") Long size);


    /**
     * 查询分组短链接数量
     * @Param: [requestParam] 分组短链接数量请求参数
     * @return:  返回查询分组短链接数量
     * @Date: 2023/12/21
     */
    @GetMapping("/api/short-link/v1/count")
    Result<List<ShortLinkGroupCountQueryRespDTO>> listGroupShortLinkCount(@RequestParam List<String> requestParam);

    /**
     * 根据 URL 获取对应网站标题
     * @Param: [url]
     * @return: org.tckry.shortlink.admin.common.convention.result.Result<java.lang.String>
     * @Date: 2023/12/24
     */
    @GetMapping("/api/short-link/v1/title")
    Result<String> getTitleByUrl(@RequestParam("url") String url);

    /**
     * 保存回收站
     * @Param: [requestParam]
     * @return: org.tckry.shortlink.admin.common.convention.result.Result<java.lang.Void>
     * @Date: 2023/12/25
     */
     @PostMapping("/api/short-link/v1/recycle-bin/save")
     void saveRecycleBin(RecycleBinSaveReqDTO requestParam);

    /**
     * 分页查询回收站短链接
     * @Param: [requestParam] 分页短链接请求参数
     * @return:  查询短链接响应
     * @Date: 2023/12/21
     */
    @GetMapping("/api/short-link/v1/recycle-bin/page")
    Result<Page<ShortLinkPageRespDTO>> pageRecycleBinShortLink(@RequestParam List<String> gidList,
                                                               @RequestParam("current")Long current,
                                                               @RequestParam("size") Long size);
    /**
     * 恢复短链接
     * @Param: [requestParam]
     * @return:
     * @Date: 2023/12/25
     */
    @PostMapping("/api/short-link/v1/recycle-bin/recover")
    void recoverRecycleBin(@RequestBody RecycleBinRecoverReqDTO requestParam);

    /**
     * 移除短链接
     * @Param: [requestParam]
     * @return: void
     * @Date: 2023/12/25
     */
    @PostMapping("/api/short-link/v1/recycle-bin/remove")
    void removeRecycleBin(@RequestBody RecycleBinRemoveReqDTO requestParam);

    /**
     * 访问单个短链接指定时间内监控数据
     * @Param: [requestParam]
     * @return: org.tckry.shortlink.admin.common.convention.result.Result<org.tckry.shortlink.admin.remote.dto.resp.ShortLinkStatsRespDTO>
     * @Date: 2023/12/27
     */
    @GetMapping("/api/short-link/v1/stats")
    Result<ShortLinkStatsRespDTO> oneShortLinkStats(@RequestParam("fullShortUrl") String fullShortUrl,
                                                    @RequestParam("gid") String gid,
                                                    @RequestParam("startDate") String startDate,
                                                    @RequestParam("endDate") String endDate);

    /**
     * 访问分组短链接指定时间内监控数据
     * @Param: [requestParam]
     * @return:
     */
    @GetMapping("/api/short-link/v1/stats/group")
    Result<ShortLinkStatsRespDTO> groupShortLinkStats(@RequestParam("gid") String gid,
                                                      @RequestParam("startDate") String startDate,
                                                      @RequestParam("endDate") String endDate);

    /**
     * 访问单个短链接指定时间内监控访问记录数据
     * @Param: [requestParam]
     * @return:
     * @Date: 2023/12/27
     */
    @GetMapping("/api/short-link/v1/stats/access-record")
    Result<Page<ShortLinkStatsAccessRecordRespDTO>> shortLinkStatsAccessRecord(@RequestParam("fullShortUrl") String fullShortUrl,
                                                                                        @RequestParam("gid") String gid,
                                                                                        @RequestParam("startDate") String startDate,
                                                                                        @RequestParam("endDate") String endDate);

    /**
     * 访问分组短链接指定时间内监控访问记录数据
     * @Param: [requestParam]
     * @return: org.tckry.shortlink.admin.common.convention.result.Result<com.baomidou.mybatisplus.core.metadata.IPage<org.tckry.shortlink.admin.remote.dto.resp.ShortLinkStatsAccessRecordRespDTO>>
     * @Date: 2023/12/30
     */
    @GetMapping("/api/short-link/v1/stats/access-record/group")
    Result<Page<ShortLinkStatsAccessRecordRespDTO>> groupShortLinkStatsAccessRecord(@RequestParam("gid") String gid,
                                                                                             @RequestParam("startDate") String startDate,
                                                                                             @RequestParam("endDate") String endDate);
}
