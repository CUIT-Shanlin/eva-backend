package edu.cuit.client.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EvaProp {
    /**
     * 评教指标
     */
    private String prop;

    /**
     * 对应分数
     */
    private Integer score;
}
