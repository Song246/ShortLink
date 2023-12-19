package org.tckry.shortlink.admin.dto.req;


import lombok.Data;

/**
 * 短链接分组修改参数
 * @program: shortlink
 * @description:
 * @author: lydms
 * @create: 2023-12-19 16:17
 **/
@Data
public class ShortLinkGroupUpdateReqDTO {

    /*
    * 分组标识
    * */
    private String gid;

    /*
    * 分组名
    * */
    private String name;
}
