package edu.cuit.client.dto.cmd.eva;

import com.alibaba.cola.dto.Command;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class NewEvaTaskCmd extends Command {
    /**
     *teacherId
     */
    @NotNull(message = "评教老师不能为空")
    private Integer teacherId;
    /**
     *courInfId
     */
    @NotNull(message = "课程详情不能为空")
    private Integer courInfId;

}
