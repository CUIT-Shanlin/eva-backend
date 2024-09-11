package edu.cuit.client.api;

import edu.cuit.client.dto.clientobject.SemesterCO;

import java.util.List;

/**
 * 学期相关业务接口
 */
public interface ISemesterService {

    /**
     * 获取所有已有的学期
     */
    List<SemesterCO> all();

    /**
     * 获取当前学期的信息
     */
    SemesterCO now();
}
