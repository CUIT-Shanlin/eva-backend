package edu.cuit.infra.bcevaluation.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.cuit.bc.evaluation.application.port.DeleteEvaTemplateRepository;
import edu.cuit.bc.evaluation.domain.DeleteEvaTemplateQueryException;
import edu.cuit.bc.evaluation.domain.DeleteEvaTemplateUpdateException;
import edu.cuit.infra.dal.database.dataobject.course.CourseDO;
import edu.cuit.infra.dal.database.dataobject.eva.FormTemplateDO;
import edu.cuit.infra.dal.database.mapper.course.CourseMapper;
import edu.cuit.infra.dal.database.mapper.eva.FormTemplateMapper;
import edu.cuit.infra.enums.cache.EvaCacheConstants;
import edu.cuit.zhuyimeng.framework.cache.LocalCacheManager;
import edu.cuit.zhuyimeng.framework.logging.utils.LogUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 删除评教模板端口适配器（保持历史行为不变：原样搬运旧 gateway 写流程）。
 */
@Component
@RequiredArgsConstructor
public class DeleteEvaTemplateRepositoryImpl implements DeleteEvaTemplateRepository {
    private final FormTemplateMapper formTemplateMapper;
    private final CourseMapper courseMapper;
    private final EvaCacheConstants evaCacheConstants;
    private final LocalCacheManager localCacheManager;

    @Override
    @Transactional
    public void delete(List<Integer> ids) {
        for (Integer id : ids) {
            //是否是默认数据
            FormTemplateDO formTemplateDO = formTemplateMapper.selectById(id);
            if (formTemplateDO.getIsDefault() == 1 || formTemplateDO.getIsDefault() == 0) {
                throw new DeleteEvaTemplateUpdateException("默认数据不允许删除");
            }
            //没有分配在课程中
            QueryWrapper<CourseDO> courWrapper = new QueryWrapper<>();
            courWrapper.eq("templateId", id);
            CourseDO courseDO = courseMapper.selectOne(courWrapper);
            //获取对应课程id
            if (courseDO == null) {
                QueryWrapper<FormTemplateDO> formTemplateWrapper = new QueryWrapper<>();
                formTemplateWrapper.eq("id", id);
                if (formTemplateMapper.selectOne(formTemplateWrapper) == null) {
                    throw new DeleteEvaTemplateQueryException("并未找到找到相应模板");
                } else {
                    //删除模板
                    LogUtils.logContent(formTemplateMapper.selectById(id).getName() + " 评教模板");
                    formTemplateMapper.delete(formTemplateWrapper);
                    //删除缓存
                    localCacheManager.invalidateCache(evaCacheConstants.ONE_TEMPLATE, String.valueOf(id));
                    localCacheManager.invalidateCache(null, evaCacheConstants.TEMPLATE_LIST);
                }
            } else {
                throw new DeleteEvaTemplateUpdateException("该模板已经被课程分配，无法再进行删除");
            }
        }
    }
}

