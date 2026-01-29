package edu.cuit.domain.gateway;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 院系相关数据门户接口
 */
@Component
public interface DepartmentGateway {

    /**
     * 获取所有学院名称
     * @return List<CourseEntity>
     */
    List<String> getAll();
}
