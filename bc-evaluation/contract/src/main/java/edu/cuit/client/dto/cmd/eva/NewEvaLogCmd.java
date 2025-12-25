package edu.cuit.client.dto.cmd.eva;
import com.alibaba.cola.dto.Command;
import edu.cuit.client.dto.clientobject.FormPropCO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class NewEvaLogCmd extends Command {
    /**
     * 任务id
     */
    @NotNull(message = "评教任务不能为空")
    private Integer taskId;
    /**
     * 评教的文本信息
     */
    @NotBlank(message = "文本信息不能为空")
    private String textValue;
    /**
     *评教的指标及其分数
     */
    private List<FormPropCO> formPropsValues;
}
