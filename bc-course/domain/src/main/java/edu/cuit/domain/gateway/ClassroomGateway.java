package edu.cuit.domain.gateway;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 教室相关数据门面
 */
@Component
public interface ClassroomGateway {

    /**
     * 获取所有教室
     */
    List<String> getAll();

}
