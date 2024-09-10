package edu.cuit.client.dto.cmd.course;

import com.alibaba.cola.dto.ClientObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

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
    private Integer id;

    /**
     * 评教老师的id集合
     */
    private Integer[] evaTeacherIdList;

}
