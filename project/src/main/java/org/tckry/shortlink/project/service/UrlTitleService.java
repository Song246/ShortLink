package org.tckry.shortlink.project.service;


/**
 * URL 标题接口层
 */
public interface UrlTitleService {

    /**
    * 根据URL获取标题
    * @Param: [url] 目标网站地址
    * @return: java.lang.String 网站标题
    * @Date: 2023/12/24
    */
    String getTitleByUrl(String url);
}
