package org.tckry.shortlink.admin.dto.resp;

import lombok.Data;

/**
 * 返回短链接分组实体对象
 * @program: shortlink
 * @description:
 * @author: lydms
 * @create: 2023-12-19 16:40
 **/
@Data
public class ShortLinkGroupRespDTO {
    /**
     * 分组标识
     */
    private String gid;

    /**
     * 分组名称
     */
    private String name;

    /**
     * 创建分组用户名
     */
    private String username;

    /**
     * 分组排序
     */
    private Integer sortOrder;
}
