package edu.cuit.bc.course.application.port;

import edu.cuit.client.dto.data.Term;

/**
 * 课程导入状态查询端口（读侧外部依赖）。
 *
 * <p>保持行为不变：查询条件与边界语义由端口适配器原样搬运旧实现。</p>
 */
public interface CourseImportedQueryPort {

    /**
     * 判断某学期是否已经导入过课表文件（按课程性质判断）。
     *
     * @param type 用于确定是导入实验课表还是理论课表，0：理论课，1：实验课
     * @param term 学期类
     */
    Boolean isImported(Integer type, Term term);
}

