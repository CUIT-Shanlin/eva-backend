package edu.cuit.domain.gateway;

import edu.cuit.client.dto.clientobject.SemesterCO;
import edu.cuit.zhuyimeng.framework.common.result.CommonResult;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;


import java.util.List;

/**
 * 学期相关数据门户接口
 */
@Component
public interface SemesterGateway {
    /**
     * 获取所有已有的学期
     * @return List<SemesterCO>
     */
    List<SemesterCO> getAll();

    /**
     * 获取当前学期的信息
     * @return SemesterCO
     */
    SemesterCO getNow();

    /**
     * 获取一个学期的信息
     * @param id 学期id
     */
    SemesterCO getSemesterInfo( Integer id) ;

}
