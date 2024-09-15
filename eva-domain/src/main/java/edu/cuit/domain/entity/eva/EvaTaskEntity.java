package edu.cuit.domain.entity.eva;

import com.alibaba.cola.domain.Entity;
import edu.cuit.domain.entity.course.SingleCourseEntity;
import edu.cuit.domain.entity.user.biz.UserEntity;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

/**
 * 评教任务domain entity
 */
@Entity
@Data
@RequiredArgsConstructor
public class EvaTaskEntity {

    /**
     * id
     */
    private Integer id;

    /**
     * 评教老师
     */
    private UserEntity teacher;

    /**
     * 被评教的那节课
     */
    private SingleCourseEntity courInf;

    /**
     * 评教需要使用的快照模板
     */
    private CourOneEvaTemplateEntity evaTemplate;

    /**
     * 任务状态（0：待执行，1：已执行）
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 实现逻辑删除（0:不可用 1:可用）
     */
    private Integer isDeleted;

}
