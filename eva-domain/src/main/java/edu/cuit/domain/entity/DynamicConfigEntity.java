package edu.cuit.domain.entity;

import com.alibaba.cola.domain.Entity;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Entity
@Data
@RequiredArgsConstructor
public class DynamicConfigEntity {

    /**
     * 最小评教次数
     */
    private Integer minEvaNum = 2;

    /**
     * 最小被评教次数
     */
    private Integer minBeEvaNum = 2;

    /**
     * 最大被评教次数
     */
    private Integer maxBeEvaNum = 8;

}
