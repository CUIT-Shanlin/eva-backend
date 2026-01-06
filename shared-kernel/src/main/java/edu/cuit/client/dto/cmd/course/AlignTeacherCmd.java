package edu.cuit.client.dto.cmd.course;

import com.alibaba.cola.dto.ClientObject;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 分配听课/评教老师模型
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class AlignTeacherCmd extends ClientObject {
    /**
     * 课程详情id
     */
    @NotNull(message = "课程详情id不能为null")
    private Integer id;

    /**
     * 评教老师的id集合
     */
    private List<Integer> evaTeacherIdList;

}
