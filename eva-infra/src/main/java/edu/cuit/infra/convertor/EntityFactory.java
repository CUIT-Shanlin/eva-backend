package edu.cuit.infra.convertor;

import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.cola.exception.SysException;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.Named;
import org.mapstruct.TargetType;
import org.springframework.beans.BeansException;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

/**
 * Entity工厂类
 */
@Slf4j
public class EntityFactory {

    public <T> T newPrototypeInstance(@TargetType Class<T> clazz) {
        T result;
        try {
            result = SpringUtil.getBean(clazz);
        } catch (BeansException e) {
            try {
                result = clazz.getDeclaredConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                     NoSuchMethodException ex) {
                log.error("创建Entity对象失败：没有空构造器，请联系管理员",ex);
                throw new SysException("创建Entity对象失败：没有空构造器，请联系管理员");
            }
        }
        return result;
    }

    @Named("toOptional")
    public <T> Optional<?> toOptional(T value) {
        if (value instanceof Optional<?> optional) {
            return optional;
        }
        return Optional.of(value);
    }

}
