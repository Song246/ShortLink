package org.tckry.shortlink.project.toolkit;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;

import java.util.Date;
import java.util.Optional;

import static org.tckry.shortlink.project.common.constant.ShortLinkConstant.DEFAULT_CACHE_VALID_TIME;

/**
 * 短链接工具类
 * @program: shortlink
 * @description:
 * @author: lydms
 * @create: 2023-12-24 17:07
 **/
public class LinkUtil {

    /** 
    * 获取短链接缓存有效期时间
    * @Param: [validDate] 短链接缓存有效期时间戳
    * @Date: 2023/12/24
    */
    
    public static long getLinkCacheValidTime(Date validDate) {
        return Optional.ofNullable(validDate)
                .map(each -> DateUtil.between(new Date(),each,DateUnit.MS)) // validDate非空的话使用当前时间和用户短链接的validDate进行比较，获取中间差值
                .orElse(DEFAULT_CACHE_VALID_TIME);
    }
}
