package edu.cuit.domain.gateway;

import edu.cuit.zhuyimeng.framework.common.result.CommonResult;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;

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
