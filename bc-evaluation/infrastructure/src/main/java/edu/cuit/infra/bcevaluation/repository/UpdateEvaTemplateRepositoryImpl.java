package edu.cuit.infra.bcevaluation.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.cuit.bc.evaluation.application.model.UpdateEvaTemplateCommand;
import edu.cuit.bc.evaluation.application.port.UpdateEvaTemplateRepository;
import edu.cuit.bc.evaluation.domain.UpdateEvaTemplateUpdateException;
import edu.cuit.infra.enums.cache.EvaCacheConstants;
import edu.cuit.zhuyimeng.framework.cache.LocalCacheManager;
import edu.cuit.infra.dal.database.dataobject.eva.FormTemplateDO;
import edu.cuit.infra.dal.database.mapper.eva.FormTemplateMapper;
import edu.cuit.zhuyimeng.framework.logging.utils.LogUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * 修改评教模板端口适配器（保持历史行为不变：原样搬运旧 gateway 写流程）。
 */
@Component
@RequiredArgsConstructor
public class UpdateEvaTemplateRepositoryImpl implements UpdateEvaTemplateRepository {
    private final FormTemplateMapper formTemplateMapper;
    private final EvaCacheConstants evaCacheConstants;
    private final LocalCacheManager localCacheManager;

    @Override
    @Transactional
    public void update(UpdateEvaTemplateCommand command) {
        // 检验指标是否重复（保持旧逻辑：props 非空且 isNotBlank 才校验；不 trim，不过滤空串）
        if (command.props() != null && StringUtils.isNotBlank(command.props())) {
            List<String> props = Arrays.stream(command.props().split(",")).toList();
            long count = props.stream().distinct().count();
            if (props.size() != count) {
                throw new UpdateEvaTemplateUpdateException("由于你输入的指标中有重复数据，故不能修改");
            }
        }

        FormTemplateDO formTemplateDO = new FormTemplateDO();
        formTemplateDO.setDescription(command.description());
        formTemplateDO.setProps(command.props());
        formTemplateDO.setName(command.name());
        formTemplateMapper.update(formTemplateDO, new QueryWrapper<FormTemplateDO>().eq("id", command.id()));

        // 失效缓存（保持行为不变）
        localCacheManager.invalidateCache(evaCacheConstants.ONE_TEMPLATE, String.valueOf(command.id()));
        localCacheManager.invalidateCache(null, evaCacheConstants.TEMPLATE_LIST);

        LogUtils.logContent(formTemplateMapper.selectById(command.id()).getName() + " 评教模板");
    }
}
