package org.tckry.shortlink.project.dto.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 短链接修改请求对象
 * @program: shortlink
 * @description:
 * @author: lydms
 * @create: 2023-12-22 20:06
 **/
@Data
public class ShortLinkUpdateReqDTO {

    /**
     * 原始链接
     */
    private String originUrl;

    /**
     * 完整短链接
     */
    private String FullShortUrl;

    /**
     * 原始gid,修改短链接gid后去查询数据库查不到,.eq(ShortLinkDO::getGid, requestParam.getGid())
     */
    private String originGid;

    /**
     * 分组标识
     */
    private String gid;

    /**
     * 有效期类型 0：永久有效 1：用户自定义
     */
    private Integer validDateType;

    /**
     * 有效期
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    private Date validDate;

    /**
     * 描述
     */
    private String describe;

}
