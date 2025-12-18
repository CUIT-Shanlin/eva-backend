package edu.cuit.domain.entity.eva;

import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.cola.domain.Entity;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Entity
@Data
@RequiredArgsConstructor
public class EvaConfigEntity implements Cloneable{

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

    /**
     * 高分阈值（>=该值计为“高分”）。
     */
    private Integer highScoreThreshold = 95;

    @Override
    public EvaConfigEntity clone() {
        EvaConfigEntity config = SpringUtil.getBean(EvaConfigEntity.class);
        config.setMaxBeEvaNum(maxBeEvaNum);
        config.setMinEvaNum(minEvaNum);
        config.setMinBeEvaNum(minBeEvaNum);
        config.setHighScoreThreshold(highScoreThreshold);
        return config;
    }

}
