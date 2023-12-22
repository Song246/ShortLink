package org.tckry.shortlink.admin.remote;

import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.web.bind.annotation.GetMapping;
import org.tckry.shortlink.admin.common.convention.result.Result;
import org.tckry.shortlink.admin.remote.dto.req.ShortLinkCreateReqDTO;
import org.tckry.shortlink.admin.remote.dto.req.ShortLinkPageReqDTO;
import org.tckry.shortlink.admin.remote.dto.resp.ShortLinkCreateRespDTO;
import org.tckry.shortlink.admin.remote.dto.resp.ShortLinkPageRespDTO;

import java.util.HashMap;
import java.util.Map;

/**
 * 短链接中台远程调用服务
 */
public interface ShortLinkRemoteService {

    /**
    * 创建短链接
    * @Param: [requestParam]
    * @return:
    * @Date: 2023/12/21
    */
    default Result<ShortLinkCreateRespDTO> createShortLink(ShortLinkCreateReqDTO requestParam){
        String resultBodyStr = HttpUtil.post("http://localhost:8001/api/short-link/v1/create",JSON.toJSONString(requestParam));
        return JSON.parseObject(resultBodyStr,new TypeReference<>() {   //  new TypeReference<>()作用：Result内含泛型对象，进行反序列化时不知道具体类型，帮助类型转换进行反序列化
        });
    }

    
    /** 
    * 分页查询短链接
    * @Param: [requestParam] 分页短链接请求参数
    * @return:  查询短链接响应
    * @Date: 2023/12/21
    */
    default Result<IPage<ShortLinkPageRespDTO>> pageShortLink(ShortLinkPageReqDTO requestParam){    // 接口内方法含方法体，default方法
        Map<String,Object> requestMap = new HashMap<>();    // 请求GetMapping方式，传入的json数据通过Map进行放入自动解析
        requestMap.put("gid",requestParam.getGid());
        requestMap.put("current",requestParam.getCurrent());
        requestMap.put("size",requestParam.getSize());
        String resultPageStr = HttpUtil.get("http://localhost:8001/api/short-link/v1/page",requestMap);
        return JSON.parseObject(resultPageStr, new TypeReference<>() {   //  new TypeReference<>()作用：Result内含泛型对象，进行反序列化时不知道具体类型，帮助类型转换进行反序列化
        });
    }
}
