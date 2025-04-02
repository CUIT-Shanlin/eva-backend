package edu.cuit.domain.entity.eva;

import com.alibaba.cola.domain.Entity;
import edu.cuit.domain.entity.course.SemesterEntity;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.function.Supplier;

/**
 * 评教记录domain entity
 */
@Entity
@Data
@RequiredArgsConstructor
public class EvaRecordEntity {

    /**
     * id
     */
    private Integer id;

    /**
     * 评教任务
     */
    @Getter(AccessLevel.NONE)
    private Supplier<EvaTaskEntity> task;
    private EvaTaskEntity tCache=null;

    public synchronized EvaTaskEntity getTask(){
        if(tCache==null){
            tCache=task.get();
        }
        return tCache;
    }

    /**
     * 详细的文字评价
     */
    private String textValue;

    /**
     * 表单评教指标以及对应的分值，JSON字符串格式的对象数组
     */
    private String formPropsValues;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 实现逻辑删除（0:不可用 1:可用）
     */
    private Integer isDeleted;

    /**
     * 那节课的课程主题
     */
    private String topic;

}
