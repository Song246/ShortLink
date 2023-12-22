package org.tckry.shortlink.project.dto.resp;

import lombok.Data;

/**
 * 短链接分组查询返回参数
 * @program: shortlink
 * @description:
 * @author: lydms
 * @create: 2023-12-21 19:13
 **/
@Data
public class ShortLinkGroupCountQueryRespDTO {

    /**
     * 分组标识
     */
    private String gid;

    /**
     * 短链接数
     */
    private Integer shortLinkCount;


}
