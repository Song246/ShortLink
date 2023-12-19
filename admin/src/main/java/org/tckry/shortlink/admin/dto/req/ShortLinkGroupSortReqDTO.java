package org.tckry.shortlink.admin.dto.req;


import lombok.Data;

/**
 * 短链接分组排序参数
 * @program: shortlink
 * @description:
 * @author: lydms
 * @create: 2023-12-19 16:17
 **/
@Data
public class ShortLinkGroupSortReqDTO {

    /*
    * 分组ID
    * */
    private String gid;

    /*
    * 排序
    * */
    private Integer sortOrder;
}
