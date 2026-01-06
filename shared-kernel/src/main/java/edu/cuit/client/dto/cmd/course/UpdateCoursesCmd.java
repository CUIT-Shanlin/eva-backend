package edu.cuit.client.dto.cmd.course;

import com.alibaba.cola.dto.ClientObject;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 批量修改课程对应模板的模型
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class UpdateCoursesCmd extends ClientObject {
    /**
     * 课程id数组
      */
    @NotNull(message = "课程id数组不能为空")
    private List<Integer> courseIdList;

    /**
     *待改成的模版id
     */
    @NotNull(message = "模板id不能为空")
    private Integer templateId;
}
