package edu.cuit.client.dto.cmd.eva;

import com.alibaba.cola.dto.Command;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class EvaTemplateCmd extends Command {
    /**
     * 模版id
     */
    @NotNull(message = "模板id不能为空")
    private Integer id;

    /**
     *模板名称
     */
    @NotBlank(message = "模板名不能为空")
    private String name;

    /**
     *描述
     */
    private String description;

    /**
     * 表单评教指标，JSON表示的字符串形式
     */
    private String props;

}
