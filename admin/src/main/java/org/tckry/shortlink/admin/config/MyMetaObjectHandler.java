package org.tckry.shortlink.admin.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * @program: shortlink
 * @description:
 * @author: lydms
 * @create: 2023-12-17 15:04
 **/
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {
    // mybatisplus 官网文档说明
    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", Date::new, Date.class); // 起始版本 3.3.3(推荐)
        this.strictInsertFill(metaObject, "updateTime", Date::new, Date.class); // 起始版本 3.3.3(推荐)
        this.strictInsertFill(metaObject, "delFlag", ()->0, Integer.class); // 起始版本 3.3.3(推荐)
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime::now, LocalDateTime.class); // 起始版本 3.3.3(推荐)
    }
}
