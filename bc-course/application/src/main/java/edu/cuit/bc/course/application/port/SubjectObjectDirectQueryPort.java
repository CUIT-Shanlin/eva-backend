package edu.cuit.bc.course.application.port;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import edu.cuit.infra.dal.database.dataobject.course.SubjectDO;

import java.util.List;

/**
 * 课程读侧：科目基础数据对象的直查端口（用于跨 BC 去课程域 DAL（SubjectMapper）编译期直连）。
 *
 * <p><b>保持行为不变（重要）</b>：该端口用于替代“跨 BC 直连课程域 DAL（SubjectMapper）”的用法。
 * 端口适配器应沿用调用方旧实现语义（查询条件、结果顺序、空值表现与异常触发时机不变），且不应引入新的缓存/日志副作用。</p>
 */
public interface SubjectObjectDirectQueryPort {

    /**
     * 按条件查询科目列表（沿用旧实现语义；若无命中则返回空列表）。
     */
    List<SubjectDO> findSubjectList(Wrapper<SubjectDO> queryWrapper);

    /**
     * 按科目ID查询科目对象（沿用旧实现语义；若无命中则返回 null）。
     */
    SubjectDO findSubjectById(Integer subjectId);
}

