package org.tckry.shortlink.project.dao.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import groovy.transform.Field;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.tckry.shortlink.project.common.database.BaseDO;

import java.util.Date;

/**
 * 短链接实体
 * @program: shortlink
 * @description:
 * @author: lydms
 * @create: 2023-12-20 16:32
 **/
@Data
@TableName(value = "t_link")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ShortLinkDO extends BaseDO {

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
     * 点击量
     */
    private Integer clickNum;

    /**
     * 分组标识
     */
    private String gid;

    /**
     * 启用标识 0：启用 1：未启用
     */
    private Integer enableStatus;

    /**
     * 创建类型 0：控制台 1：接口
     */
    private Integer createdType;

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

    /**
     * 历史PV
     */
    private Integer totalPv;
    /**
     * 历史UV
     */
    private Integer totalUv;
    /**
     * 历史UIP
     */
    private Integer totalUip;

    /**
     * 今日PV,数据库表没有的字段，@TableField(exist = false)
     */
    @TableField(exist = false)
    private Integer todayPv;
    /**
     * 今日UV
     */
    @TableField(exist = false)
    private Integer todayUv;
    /**
     * 今日UIP
     */
    @TableField(exist = false)
    private Integer todayUip;

    /**
     * 删除时间，短链接可能复用， 将这个和gid作为联合索引
     */
    private Long delTime;

}
