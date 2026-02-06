package edu.cuit.domain.entity.eva;

import com.alibaba.cola.domain.Entity;
import edu.cuit.domain.entity.course.CourseEntity;
import edu.cuit.domain.entity.course.SemesterEntity;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.function.Supplier;

/**
 * 课程评教情况domain entity
 */
@Entity
@Data
@RequiredArgsConstructor
public class CourOneEvaTemplateEntity {

    /**
     * id
     */
    private Integer id;

    /**
     * 学期
     */
    @Getter(AccessLevel.NONE)
    private Supplier<SemesterEntity> semester;

    private SemesterEntity sCache=null;

    public synchronized SemesterEntity getSemester(){
        if(sCache==null){
            sCache=semester.get();
        }
        return sCache;
    }

    /**
     * 该门课
     */
    @Getter(AccessLevel.NONE)
    private Supplier<CourseEntity> course;

    private CourseEntity cCache=null;

    public synchronized CourseEntity getCourse(){
        if(cCache==null){
            cCache=course.get();
        }
        return cCache;
    }

    /**
     * 用到的评教模板的信息，用JSON表示的对象，eg: { name: ”某模板“, description: "模板描述", props: ["指标1", "指标2"] }
     */
    private String formTemplate;

    /**
     * 该门课在这学期的统计数据，JSON表示的对象数组
     */
    private String courseStatistics;


}
