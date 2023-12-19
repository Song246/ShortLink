package org.tckry.shortlink.admin.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.tckry.shortlink.admin.common.database.BaseDO;


/**
 * 短链接分组实体
 * @program: shortlink
 * @description:
 * @author: lydms
 * @create: 2023-12-19 15:08
 **/
@Data
@TableName("t_group")
@Builder    // BUild方法构建对象，下面三个注解都要全加，否则报错
@NoArgsConstructor
@AllArgsConstructor
public class GroupDO extends BaseDO {
    /**
     * id
     */
    private Long id;

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
