package edu.cuit.client.dto.clientobject.course;

import com.alibaba.cola.dto.ClientObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 一门科目(一门科目)
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class SubjectCO extends ClientObject {

    /**
     * 科目名称
     */
    private String name;

    /**
     * 科目性质(0:理论课,1:实验课,3:其他)
     */
    private Integer nature;

    /**
     * 科目创建时间
     */
    private LocalDateTime createTime;

    /**
     * 科目更新时间
     */
    private LocalDateTime updateTime;
}
