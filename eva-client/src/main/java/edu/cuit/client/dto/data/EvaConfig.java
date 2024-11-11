package edu.cuit.client.dto.data;

import com.alibaba.cola.dto.DTO;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 评教配置信息DTO
 */
@EqualsAndHashCode(callSuper = false)
@Data
@Accessors(chain = true)
public class EvaConfig extends DTO {

    /**
     * 最小评教次数
     */
    @NotNull(message = "最小评教次数不能为空")
    @Min(value = 0,message = "最小评教次数最小为0")
    private Integer minEvaNum;

    /**
     * 最小被评教次数
     */
    @NotNull(message = "最小被评教次数不能为空")
    @Min(value = 0,message = "最小被评教次数最小为0")
    private Integer minBeEvaNum;

    /**
     * 最大被评教次数
     */
    @NotNull(message = "最大被评教次数不能为空")
    @Min(value = 1,message = "最大被评教次数最小为1")
    private Integer maxBeEvaNum;

}
