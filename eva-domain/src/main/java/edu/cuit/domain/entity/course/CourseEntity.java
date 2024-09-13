package edu.cuit.domain.entity.course;

import com.alibaba.cola.domain.Entity;
import edu.cuit.domain.entity.user.biz.UserEntity;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

/**
 * 一门课程的domain entity
 */
@Entity
@Data
@RequiredArgsConstructor
public class CourseEntity {

    /**
     * 课程id
     */
    private Integer id;

    /**
     * 科目
     */
    private SubjectVO subject;

    /**
     * 教学老师
     */
    private UserEntity teacher;

    /**
     * JSON形式存教室数组
     */
    private String classroom;

    /**
     * 学期
     */
    private SemesterVO semester;

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
