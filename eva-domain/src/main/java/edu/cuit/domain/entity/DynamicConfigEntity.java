package edu.cuit.domain.entity;

import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.cola.domain.Entity;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Entity
@Data
@RequiredArgsConstructor
public class DynamicConfigEntity implements Cloneable{

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

    @Override
    public DynamicConfigEntity clone() {
        DynamicConfigEntity config = SpringUtil.getBean(DynamicConfigEntity.class);
        config.setMaxBeEvaNum(maxBeEvaNum);
        config.setMinEvaNum(minEvaNum);
        config.setMinBeEvaNum(minBeEvaNum);
        return config;
    }

}
