package com.helmx.tutorial.configuration;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;

/**
 * MyBatis-Plus元数据对象处理器
 * 用于自动填充实体类中的创建时间和更新时间字段
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    private static final String FIELD_CREATED_AT = "createdAt";
    private static final String FIELD_UPDATED_AT = "updatedAt";

    /**
     * 插入数据时的字段填充处理
     * 自动填充createdAt和updatedAt字段，只有当字段值为null时才会进行填充
     *
     * @param metaObject 元数据对象，包含要填充的字段信息
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        Timestamp now = new Timestamp(System.currentTimeMillis());

        Object createdAt = getFieldValByName(FIELD_CREATED_AT, metaObject);
        if (createdAt == null) {
            this.strictInsertFill(metaObject, FIELD_CREATED_AT, Timestamp.class, now);
        }

        Object updatedAt = getFieldValByName(FIELD_UPDATED_AT, metaObject);
        if (updatedAt == null) {
            this.strictInsertFill(metaObject, FIELD_UPDATED_AT, Timestamp.class, now);
        }
    }

    /**
     * 更新数据时的字段填充处理
     * 自动填充updatedAt字段，每次更新都会重新设置该字段的值
     *
     * @param metaObject 元数据对象，包含要填充的字段信息
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        // 更新时只填充 updatedAt 字段
        this.strictUpdateFill(metaObject, FIELD_UPDATED_AT, Timestamp.class, new Timestamp(System.currentTimeMillis()));
    }
}