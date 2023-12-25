package org.tckry.shortlink.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.tckry.shortlink.project.dao.entity.ShortLinkDO;
import org.tckry.shortlink.project.dto.req.RecycleBinSaveReqDTO;

/** 
* 回收站管理接口层
* @Param:
* @return: 
* @Date: 2023/12/25
*/

public interface RecycleBinService extends IService<ShortLinkDO>{

    /**
    * 保存回收站
    * @Param: [requestParam]
    * @return: void
    * @Date: 2023/12/25
    */

    void saveRecycleBin(RecycleBinSaveReqDTO requestParam);
}
