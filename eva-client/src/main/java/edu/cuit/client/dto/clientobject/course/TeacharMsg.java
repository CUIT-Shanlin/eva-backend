package edu.cuit.client.dto.clientobject.course;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
/**
 * 教师基础信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TeacharMsg {
    /**
     * 教师id
     */
    @NotNull(message = "教室id不能为空")
    private Integer id;

    /**
     * 教师姓名
     */
    @NotNull(message = "教室姓名不能为空")
    private String name;

    /**
     * 学院
     */
    @NotNull(message = "教室所属院系不能为空")
    private String department;

}
