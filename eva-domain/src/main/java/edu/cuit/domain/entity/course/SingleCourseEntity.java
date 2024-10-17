package edu.cuit.domain.entity.course;

import com.alibaba.cola.domain.Entity;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.function.Supplier;

@RequiredArgsConstructor
@Data
@Entity
public class SingleCourseEntity {

    /**
     * id
     */
    private Integer id;

    /**
     * 课程
     */
    @Getter(AccessLevel.NONE)
    private Supplier<CourseEntity> course;

    /**
     * 上课周时间
     */
    private Integer week;

    /**
     * 开始时间（第几节开始）
     */
    private Integer startTime;

    /**
     * 结束时间（第几节结束）
     */
    private Integer endTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 逻辑删除
     */
    private Integer isDeleted;

    /**
     * 地点信息
     */
    private String location;

    /**
     * 星期几
     */
    private Integer day;
    @Getter(AccessLevel.NONE)
    private CourseEntity courseCache=null;
    private synchronized CourseEntity getCourse(){
        if(courseCache==null){
            courseCache=course.get();
        }
        return courseCache;
    }

}
