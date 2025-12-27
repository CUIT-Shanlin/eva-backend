package edu.cuit.bc.course.application.port;

import java.util.List;
import java.util.Map;

/**
 * 分配听课/评教老师持久化端口。
 *
 * <p>说明：渐进式重构阶段，为保持行为不变，返回值沿用旧系统消息模型。</p>
 */
public interface AssignEvaTeachersRepository {
    Map<String, Map<Integer, Integer>> assign(Integer semesterId, Integer courInfId, List<Integer> evaTeacherIdList);
}

