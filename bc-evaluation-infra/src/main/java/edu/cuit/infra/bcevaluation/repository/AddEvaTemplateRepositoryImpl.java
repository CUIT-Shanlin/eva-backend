package edu.cuit.infra.bcevaluation.repository;

import edu.cuit.bc.evaluation.application.model.AddEvaTemplateCommand;
import edu.cuit.bc.evaluation.application.port.AddEvaTemplateRepository;
import edu.cuit.bc.evaluation.domain.AddEvaTemplateUpdateException;
import edu.cuit.infra.enums.cache.EvaCacheConstants;
import edu.cuit.zhuyimeng.framework.cache.LocalCacheManager;
import edu.cuit.infra.dal.database.dataobject.eva.FormTemplateDO;
import edu.cuit.infra.dal.database.mapper.eva.FormTemplateMapper;
import edu.cuit.zhuyimeng.framework.logging.utils.LogUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * 新增评教模板端口适配器（保持历史行为不变：原样搬运旧 gateway 写流程）。
 */
@Component
@RequiredArgsConstructor
public class AddEvaTemplateRepositoryImpl implements AddEvaTemplateRepository {
    private final FormTemplateMapper formTemplateMapper;
    private final EvaCacheConstants evaCacheConstants;
    private final LocalCacheManager localCacheManager;

    @Override
    @Transactional
    public void add(AddEvaTemplateCommand command) {
        // 判断指标重复（保持旧逻辑：props != null 才校验；不 trim，不过滤空串）
        if (command.props() != null) {
            List<String> props = Arrays.stream(command.props().split(",")).toList();
            long count = props.stream().distinct().count();
            if (props.size() != count) {
                throw new AddEvaTemplateUpdateException("由于你输入的指标中有重复数据，故不能增加");
            }
        }

        FormTemplateDO formTemplateDO = new FormTemplateDO();
        formTemplateDO.setDescription(command.description());
        formTemplateDO.setProps(command.props());
        formTemplateDO.setName(command.name());
        formTemplateMapper.insert(formTemplateDO);

        // 失效缓存（保持行为不变）
        localCacheManager.invalidateCache(null, evaCacheConstants.TEMPLATE_LIST);

        LogUtils.logContent(command.name() + " 评教模板");
    }
}
