package edu.cuit.bc.iam.application.contract.api.department;

import java.util.List;

/**
 * 学院信息相关业务接口
 */
public interface IDepartmentService {

    /**
     * 获取所有学院名称
     */
    List<String> all();
}

