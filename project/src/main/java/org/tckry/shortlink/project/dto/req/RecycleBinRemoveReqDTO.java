package org.tckry.shortlink.project.dto.req;

import lombok.Data;

/**
 * 回收站短链接删除删除请求参数
 * @program: shortlink
 * @description:
 * @author: lydms
 * @create: 2023-12-25 14:58
 **/
@Data
public class RecycleBinRemoveReqDTO {

    /**
     * 分组标识
     */
    private String gid;

    /**
     * 完整短链接
     */
    private String fullShortUrl;
}
