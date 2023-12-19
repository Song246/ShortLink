package org.tckry.shortlink.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.tckry.shortlink.admin.dao.entity.GroupDO;

/**
* 短链接分组接口层
*/

public interface GroupService extends IService<GroupDO> {
    /**
    * 新增短链接分组
    * @Param: [groupName] 短链接分组名
    * @return: void
    * @Date: 2023/12/19
    */

    void saveGroup(String groupName);
}
