package org.tckry.shortlink.project.dto.resp;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.util.Date;

/**
 * 短链接分页请求返回参数
 * @program: shortlink
 * @description:
 * @author: lydms
 * @create: 2023-12-20 20:54
 **/
@Data
public class ShortLinkPageRespDTO {
    /**
     * id
     */
    private Long id;

    /**
     * 域名
     */
    private String domain;

    /**
     * 短链接
     */
    private String shortUri;

    /**
     * 完整短链接
     */
    private String fullShortUrl;

    /**
     * 原始链接
     */
    private String originUrl;


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
    private Date validDate;

    /**
     * 描述,describe为Mysql关键字，进行转换
     */
    @TableField(value = "`describe`")
    private String describe;

    /**
     * 网站标识
     */
    private String favicon;
}
