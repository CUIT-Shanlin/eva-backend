package edu.cuit.infra.bcevaluation.query;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.GenericConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;
import edu.cuit.domain.entity.eva.EvaTemplateEntity;
import edu.cuit.infra.convertor.PaginationConverter;
import edu.cuit.infra.convertor.eva.EvaConvertor;
import edu.cuit.infra.dal.database.dataobject.course.CourInfDO;
import edu.cuit.infra.dal.database.dataobject.course.CourseDO;
import edu.cuit.infra.dal.database.dataobject.eva.CourOneEvaTemplateDO;
import edu.cuit.infra.dal.database.dataobject.eva.EvaTaskDO;
import edu.cuit.infra.dal.database.dataobject.eva.FormTemplateDO;
import edu.cuit.infra.dal.database.mapper.course.CourInfMapper;
import edu.cuit.infra.dal.database.mapper.course.CourseMapper;
import edu.cuit.infra.dal.database.mapper.eva.CourOneEvaTemplateMapper;
import edu.cuit.infra.dal.database.mapper.eva.EvaTaskMapper;
import edu.cuit.infra.dal.database.mapper.eva.FormTemplateMapper;
import edu.cuit.infra.enums.cache.EvaCacheConstants;
import edu.cuit.infra.gateway.impl.course.operate.CourseFormat;
import edu.cuit.infra.util.QueryUtils;
import edu.cuit.zhuyimeng.framework.cache.LocalCacheManager;
import edu.cuit.zhuyimeng.framework.common.exception.QueryException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 评教模板读侧 QueryRepo 实现（从 {@link EvaQueryRepository} 渐进式拆分出来）。
 *
 * <p>保持行为不变：仅搬运实现与依赖归属，不调整查询口径与异常文案。</p>
 */
@Primary
@Component
@RequiredArgsConstructor
public class EvaTemplateQueryRepository implements EvaTemplateQueryRepo {
    private final CourseMapper courseMapper;
    private final EvaTaskMapper evaTaskMapper;
    private final EvaConvertor evaConvertor;
    private final PaginationConverter paginationConverter;
    private final CourInfMapper courInfMapper;
    private final FormTemplateMapper formTemplateMapper;
    private final CourOneEvaTemplateMapper courOneEvaTemplateMapper;
    private final EvaCacheConstants evaCacheConstants;
    private final LocalCacheManager localCacheManager;

    @Override
    public PaginationResultEntity<EvaTemplateEntity> pageEvaTemplate(Integer semId, PagingQuery<GenericConditionalQuery> query) {

        Page<FormTemplateDO> page =new Page<>(query.getPage(),query.getSize());
        QueryWrapper<FormTemplateDO> queryWrapper = new QueryWrapper<>();
        QueryUtils.fileTimeQuery(queryWrapper,query.getQueryObj());
        if(query.getQueryObj().getKeyword()!=null&&StringUtils.isNotBlank(query.getQueryObj().getKeyword())){
            queryWrapper.like(query.getQueryObj().getKeyword()!=null,"name",query.getQueryObj().getKeyword());
        }
        queryWrapper.orderByDesc("create_time");
        Page<FormTemplateDO> formTemplateDOPage = formTemplateMapper.selectPage(page, queryWrapper);
        if(CollectionUtil.isEmpty(formTemplateDOPage.getRecords())){
            List list=new ArrayList();
            return paginationConverter.toPaginationEntity(page,list);
        }

        List<EvaTemplateEntity> evaTemplateEntities=formTemplateDOPage.getRecords().stream().map(pageEvaTemplateDO -> evaConvertor.ToEvaTemplateEntity(pageEvaTemplateDO)).toList();
        return paginationConverter.toPaginationEntity(page,evaTemplateEntities);

    }

    @Override
    public List<EvaTemplateEntity> getAllTemplate() {
        List<FormTemplateDO> getCached=localCacheManager.getCache(null,evaCacheConstants.TEMPLATE_LIST);
        if(CollectionUtil.isEmpty(getCached)) {
            List<FormTemplateDO> formTemplateDOS = formTemplateMapper.selectList(null);
            localCacheManager.putCache(null,evaCacheConstants.TEMPLATE_LIST,formTemplateDOS);
            getCached=localCacheManager.getCache(null,evaCacheConstants.TEMPLATE_LIST);
            if (CollectionUtil.isEmpty(formTemplateDOS)) {
                List list = new ArrayList();
                return list;
            }
            List<EvaTemplateEntity> evaTemplateEntities = formTemplateDOS.stream().map(formTemplateDO -> evaConvertor.ToEvaTemplateEntity(formTemplateDO)).toList();
            return evaTemplateEntities;
        }else {
            return getCached.stream().map(formTemplateDO -> evaConvertor.ToEvaTemplateEntity(formTemplateDO)).toList();
        }
    }

    //zjok
    @Override
    public Optional<String> getTaskTemplate(Integer taskId, Integer semId) {
        //任务
        List<Integer> evaTaskIdS=getEvaTaskIdS(semId);
        if(CollectionUtil.isEmpty(evaTaskIdS)){
            throw new QueryException("并没有找到相关任务");
        }
        EvaTaskDO evaTaskDO=evaTaskMapper.selectOne(new QueryWrapper<EvaTaskDO>().in("id",evaTaskIdS).eq("id",taskId));
        if(evaTaskDO==null){
            throw new QueryException("无法找到该任务");
        }
        CourInfDO courInfDO=courInfMapper.selectById(evaTaskDO.getCourInfId());
        if(courInfDO==null){
            throw new QueryException("并没有找到相关课程详情");
        }
        //1.直接去快照那边拿到
        CourOneEvaTemplateDO courOneEvaTemplateDO=courOneEvaTemplateMapper.selectOne(new QueryWrapper<CourOneEvaTemplateDO>().eq("course_id",courInfDO.getCourseId()));
        //2.去课程那边拿到
        CourseDO courseDO=courseMapper.selectById(courInfDO.getCourseId());
        FormTemplateDO formTemplateDO=formTemplateMapper.selectOne(new QueryWrapper<FormTemplateDO>().eq("id",courseDO.getTemplateId()));

        if(courOneEvaTemplateDO==null&&formTemplateDO==null){
            throw new QueryException("快照模板和评教模板都没有相关数据");
        }

        if(courOneEvaTemplateDO!=null){
            if(courOneEvaTemplateDO.getFormTemplate()==null){
                return Optional.empty();
            }
            String s1 = CourseFormat.toFormat(courOneEvaTemplateDO.getFormTemplate());
            JSONObject jsonObject= new JSONObject(s1);
            String s=jsonObject.getStr("props");
            return Optional.of(s);
        }else {
            if(formTemplateDO.getProps()==null){
                return Optional.empty();
            }
            return Optional.of(formTemplateDO.getProps());
        }
    }

    //根据传来的学期id返回evaTaskIdS
    private List<Integer> getEvaTaskIdS(Integer semId){
        List<EvaTaskDO> getCached=localCacheManager.getCache(evaCacheConstants.TASK_LIST_BY_SEM, String.valueOf(semId));
        if(getCached==null) {
            if (semId == null) {
                List<EvaTaskDO> evaTaskDOS = evaTaskMapper.selectList(null);
                localCacheManager.putCache(evaCacheConstants.TASK_LIST_BY_SEM, String.valueOf(semId),evaTaskDOS);
                getCached=localCacheManager.getCache(evaCacheConstants.TASK_LIST_BY_SEM, String.valueOf(semId));
                if (CollectionUtil.isEmpty(evaTaskDOS)) {
                    return List.of();
                }
                List<Integer> evaTaskIdS = evaTaskDOS.stream().map(EvaTaskDO::getId).toList();
                return evaTaskIdS;
            } else {
                List<CourseDO> courseDOS = courseMapper.selectList(new QueryWrapper<CourseDO>().eq("semester_id", semId));

                if (CollectionUtil.isEmpty(courseDOS)) {
                    return List.of();
                }
                List<Integer> courseIdS = courseDOS.stream().map(CourseDO::getId).toList();

                if (CollectionUtil.isEmpty(courseIdS)) {
                    return List.of();
                }

                List<CourInfDO> courInfDOS = courInfMapper.selectList(new QueryWrapper<CourInfDO>().in("course_id", courseIdS));
                List<Integer> courInfoIdS = courInfDOS.stream().map(CourInfDO::getId).toList();
                if (CollectionUtil.isEmpty(courInfoIdS)) {
                    return List.of();
                }
                List<EvaTaskDO> evaTaskDOS = evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().in("cour_inf_id", courInfoIdS));

                if (CollectionUtil.isEmpty(evaTaskDOS)) {
                    return List.of();
                }
                localCacheManager.putCache(evaCacheConstants.TASK_LIST_BY_SEM, String.valueOf(semId),evaTaskDOS);
                getCached=localCacheManager.getCache(evaCacheConstants.TASK_LIST_BY_SEM, String.valueOf(semId));
                return getCached.stream().map(EvaTaskDO::getId).toList();
            }
        }else {
            return getCached.stream().map(EvaTaskDO::getId).toList();
        }
    }
}
