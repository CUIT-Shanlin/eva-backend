package edu.cuit.client.dto.clientobject.eva;

import com.alibaba.cola.dto.ClientObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import java.time.LocalDateTime;

/**
 * 评教信息(评教信息)
 * 一条数据是一个评教任务的信息，包括评教老师信息和被评教的那节课的信息
 */

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class EvaInfoCO extends ClientObject {
    /**
     * id
     */
    private long id;
    /**
     * 任务状态（0：待执行，1：已执行）
     */
    private long status;
    /**
     * 评教老师的id
     */
    private long teacherId;
    /**
     * 被评教的那节课的id
     */
    private long courInfId;
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
