package org.tckry.shortlink.project.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 短链接跳转实体
 * @program: shortlink
 * @description:
 * @author: lydms
 * @create: 2023-12-23 15:43
 **/
@Data
@TableName(value = "t_link_goto")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ShortLinkGotoDO {

    /**
     * id
     */
    private Long id;

    /**
     * 分组标识
     */
    private String gid;

    /**
     * 完整短链接
     */
    private String fullShortUrl;

}
