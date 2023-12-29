package org.tckry.shortlink.project.dto.req;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;
import org.tckry.shortlink.project.dao.entity.ShortLinkDO;


/**
 * 短链接分页请求参数
 * @program: shortlink
 * @description:
 * @author: lydms
 * @create: 2023-12-20 20:52
 **/
@Data
public class ShortLinkPageReqDTO extends Page<ShortLinkDO> {    // Page 内包含size、current、total等内容

    /**
     * 分组标识
     */
    private String gid;

    /**
     * 排序标识
     */
    private String orderTag;


}
