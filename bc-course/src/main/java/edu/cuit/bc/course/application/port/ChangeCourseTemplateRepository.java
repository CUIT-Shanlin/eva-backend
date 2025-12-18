package edu.cuit.bc.course.application.port;

import java.util.List;

/**
 * 批量切换课程模板持久化端口。
 *
 * <p>说明：单体阶段可以在实现中顺带做日志与缓存失效；未来拆分时再进一步分离。</p>
 */
public interface ChangeCourseTemplateRepository {
    void changeTemplate(Integer semesterId, Integer templateId, List<Integer> courseIdList);
}

