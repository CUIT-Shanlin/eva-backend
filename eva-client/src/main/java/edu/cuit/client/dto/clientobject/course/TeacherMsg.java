package edu.cuit.client.dto.clientobject.course;

import com.alibaba.cola.dto.ClientObject;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 教师基础信息
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class TeacherMsg extends ClientObject {

    /**
     * 教师id
     */
    private Integer id;

    /**
     * 教师姓名
     */
    private String name;

    /**
     * 学院
     */
    private String department;

}
