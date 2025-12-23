package edu.cuit.infra.dal.database.dataobject.eva;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * 课程评教模板的快照，一条数据表示该学期这门课的模板及其统计信息
 * @TableName cour_one_eva_template
 */
@TableName(value ="cour_one_eva_template")
@Data
public class CourOneEvaTemplateDO implements Serializable {

    /**
     * id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 学期id
     */
    @TableField(value = "semester_id")
    private Integer semesterId;

    /**
     * 该门课的id
     */
    @TableField(value = "course_id")
    private Integer courseId;

    /**
     * 用到的评教模板的信息，用JSON表示的对象，eg: { name: ”某模板“, description: "模板描述", props: ["指标1", "指标2"] }
     */
    @TableField(value = "form_template")
    private String formTemplate;

    /**
     * 该门课在这学期的统计数据，JSON表示的对象数组
     */
    @TableField(value = "course_statistics")
    private String courseStatistics;

    @Serial
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}