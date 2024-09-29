package edu.cuit.infra.convertor;

import cn.hutool.extra.spring.SpringUtil;
import org.mapstruct.TargetType;

/**
 * Entity工厂类
 */
public class EntityFactory {

    public <T> T newPrototypeInstance(@TargetType Class<T> clazz) {
        return SpringUtil.getBean(clazz);
    }

}
