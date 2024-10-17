package edu.cuit.domain.entity.course;

import com.alibaba.cola.domain.Entity;
import edu.cuit.domain.entity.user.biz.RoleEntity;
import edu.cuit.domain.entity.user.biz.UserEntity;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;

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
    private Supplier<SubjectEntity> subject;

    /**
     * 教学老师
     */
    @Getter(AccessLevel.NONE)
    private Supplier<UserEntity> teacher;

    /**
     * 学期
     */
    @Getter(AccessLevel.NONE)
    private Supplier<SemesterEntity> semester;

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

    @Getter(AccessLevel.NONE)
    private SubjectEntity subjectCache = null;
    public synchronized SubjectEntity getSubjectEntity() {
        if (subjectCache == null) {
            subjectCache = subject.get();
        }
        return subjectCache;
    }



    @Getter(AccessLevel.NONE)
    private UserEntity userCache = null;
    public synchronized UserEntity getUserEntity() {
        if (userCache == null) {
            userCache = teacher.get();
        }
        return userCache;
    }

    @Getter(AccessLevel.NONE)
    private SemesterEntity semesterCache = null;
    public synchronized SemesterEntity getSemesterEntity() {
        if (semesterCache == null) {
            semesterCache = semester.get();
        }
        return semesterCache;
    }

}
