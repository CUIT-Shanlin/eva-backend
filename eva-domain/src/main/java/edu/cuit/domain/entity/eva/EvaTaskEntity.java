package edu.cuit.domain.entity.eva;

import com.alibaba.cola.domain.Entity;
import edu.cuit.domain.entity.course.SingleCourseEntity;
import edu.cuit.domain.entity.user.biz.UserEntity;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.function.Supplier;

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
    @Getter(AccessLevel.NONE)
    private Supplier<UserEntity> teacher;

    private UserEntity uCache=null;

    public synchronized UserEntity getTeacher(){
        if(uCache==null){
            uCache=teacher.get();
        }
        return uCache;
    }

    /**
     * 被评教的那节课
     */
    @Getter(AccessLevel.NONE)
    private Supplier<SingleCourseEntity> courInf;

    private SingleCourseEntity sCache=null;

    public synchronized SingleCourseEntity getCourInf(){
        if(sCache==null){
            sCache=courInf.get();
        }
        return sCache;
    }

    /**
     * 评教需要使用的快照模板
     */
    @Getter(AccessLevel.NONE)
    private Supplier<CourOneEvaTemplateEntity> evaTemplate;

    private CourOneEvaTemplateEntity cCache=null;

    public synchronized CourOneEvaTemplateEntity getEvaTemplate(){
        if(cCache==null){
            cCache=evaTemplate.get();
        }
        return cCache;
    }

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
