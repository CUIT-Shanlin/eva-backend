package edu.cuit.infra.gateway.impl.eva;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import edu.cuit.bc.evaluation.application.model.PostEvaTaskCommand;
import edu.cuit.bc.evaluation.application.port.PostEvaTaskRepository;
import edu.cuit.bc.evaluation.domain.PostEvaTaskQueryException;
import edu.cuit.bc.evaluation.domain.PostEvaTaskUpdateException;
import edu.cuit.client.dto.cmd.eva.EvaTemplateCmd;
import edu.cuit.client.dto.cmd.eva.NewEvaLogCmd;
import edu.cuit.client.dto.cmd.eva.NewEvaTaskCmd;
import edu.cuit.client.dto.cmd.eva.NewEvaTemplateCmd;
import edu.cuit.domain.gateway.eva.EvaUpdateGateway;
import edu.cuit.infra.dal.database.dataobject.course.CourInfDO;
import edu.cuit.infra.dal.database.dataobject.course.CourseDO;
import edu.cuit.infra.dal.database.dataobject.eva.*;
import edu.cuit.infra.dal.database.dataobject.user.SysUserDO;
import edu.cuit.infra.dal.database.mapper.course.CourInfMapper;
import edu.cuit.infra.dal.database.mapper.course.CourseMapper;
import edu.cuit.infra.dal.database.mapper.course.SemesterMapper;
import edu.cuit.infra.dal.database.mapper.eva.*;
import edu.cuit.infra.dal.database.mapper.user.SysUserMapper;
import edu.cuit.infra.enums.cache.EvaCacheConstants;
import edu.cuit.zhuyimeng.framework.cache.LocalCacheManager;
import edu.cuit.zhuyimeng.framework.cache.aspect.annotation.local.LocalCacheInvalidate;
import edu.cuit.zhuyimeng.framework.cache.aspect.annotation.local.LocalCacheInvalidateContainer;
import edu.cuit.zhuyimeng.framework.common.exception.QueryException;
import edu.cuit.zhuyimeng.framework.common.exception.UpdateException;
import edu.cuit.zhuyimeng.framework.common.result.CommonResult;
import edu.cuit.zhuyimeng.framework.logging.utils.LogUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class EvaUpdateGatewayImpl implements EvaUpdateGateway {
    private final FormTemplateMapper formTemplateMapper;
    private final EvaTaskMapper evaTaskMapper;
    private final FormRecordMapper formRecordMapper;
    private final CourInfMapper courInfMapper;
    private final CourseMapper courseMapper;
    private final CourOneEvaTemplateMapper courOneEvaTemplateMapper;
    private final SemesterMapper semesterMapper;
    private final SysUserMapper sysUserMapper;
    private final EvaCacheConstants evaCacheConstants;
    private final LocalCacheManager localCacheManager;
    private final PostEvaTaskRepository postEvaTaskRepository;
    @Override
    @Transactional
    @LocalCacheInvalidate(area="#{@evaCacheConstants.ONE_TEMPLATE}",key= "#cmd.getId()")
    public Void updateEvaTemplate(EvaTemplateCmd cmd) {
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        //检验是否那个模板prop有重复
        if(cmd.getProps()!=null&&StringUtils.isNotBlank(cmd.getProps())) {
            List<String> props= Arrays.stream(cmd.getProps().split(",")).toList();
            long count = props.stream().distinct().count();
            if (props.size() != count) {
                throw new UpdateException("由于你输入的指标中有重复数据，故不能修改");
            }
        }

        FormTemplateDO formTemplateDO=new FormTemplateDO();
        formTemplateDO.setDescription(cmd.getDescription());
        formTemplateDO.setProps(cmd.getProps());
        formTemplateDO.setName(cmd.getName());
        formTemplateMapper.update(formTemplateDO, new QueryWrapper<FormTemplateDO>().eq("id", cmd.getId()));
        localCacheManager.invalidateCache(null,evaCacheConstants.TEMPLATE_LIST);
        LogUtils.logContent(formTemplateMapper.selectById(cmd.getId()).getName() +" 评教模板");
        return null;
    }
    @Override
    @Transactional
    @LocalCacheInvalidate(area="#{@evaCacheConstants.ONE_TASK}",key="#cmd.getTaskId()")
    public Void putEvaTemplate(NewEvaLogCmd cmd) {
        EvaTaskDO evaTaskDO=evaTaskMapper.selectById(cmd.getTaskId());
        CourInfDO courInfDO=courInfMapper.selectById(evaTaskDO.getCourInfId());
        CourseDO courseDO=courseMapper.selectById(courInfDO.getCourseId());

        CourOneEvaTemplateDO courOneEvaTemplateDO=courOneEvaTemplateMapper.selectOne(new QueryWrapper<CourOneEvaTemplateDO>().eq("course_id",courseDO.getId()));
        FormTemplateDO formTemplateDO=formTemplateMapper.selectById(courseDO.getTemplateId());
        //把评教的具体数据传进去给评教记录
        FormRecordDO formRecordDO=new FormRecordDO();
        formRecordDO.setTaskId(cmd.getTaskId());
        formRecordDO.setTextValue(cmd.getTextValue());

        //判断是不是任务已经取消了
        if(evaTaskDO==null){
            throw new UpdateException("该任务不存在");
        }
        if(evaTaskDO.getStatus()==1||evaTaskDO.getStatus()==2){
            throw new UpdateException("该任务已经被取消或删去,不能提交");
        }
        if(courInfDO==null){
            throw new UpdateException("该任务对应的课程信息不存在，不能提交哦");
        }

        formRecordDO.setFormPropsValues(JSONUtil.toJsonStr(cmd.getFormPropsValues()));
        formRecordMapper.insert(formRecordDO);
        //加缓存
        localCacheManager.invalidateCache(null,evaCacheConstants.LOG_LIST);

        //通过任务id把任务状态改了
        evaTaskDO.setStatus(1);
        evaTaskDO.setUpdateTime(LocalDateTime.now());
        evaTaskMapper.update(evaTaskDO,new QueryWrapper<EvaTaskDO>().eq("id",cmd.getTaskId()));
        localCacheManager.invalidateCache(evaCacheConstants.TASK_LIST_BY_TEACH,sysUserMapper.selectById(evaTaskDO.getTeacherId()).getName());
        //检验是否有快照模板，没有就建一个
        if(courOneEvaTemplateDO==null){
            CourOneEvaTemplateDO courOneEvaTemplateDO1=new CourOneEvaTemplateDO();
            courOneEvaTemplateDO1.setCourseId(courseDO.getId());
            courOneEvaTemplateDO1.setSemesterId(courseDO.getSemesterId());
            String s="{\"name\":\""+formTemplateDO.getName()+"\",\"description\":\""+formTemplateDO.getDescription()+"\",\"props\":\""+formTemplateDO.getProps()+"\"}";
            courOneEvaTemplateDO1.setFormTemplate(s);
            courOneEvaTemplateMapper.insert(courOneEvaTemplateDO1);
        }
        return null;
    }

    @Override
    @Transactional
    public Integer postEvaTask(NewEvaTaskCmd cmd,Integer maxNum) {
        try {
            return postEvaTaskRepository.create(new PostEvaTaskCommand(cmd.getCourInfId(), cmd.getTeacherId()), maxNum);
        } catch (PostEvaTaskUpdateException e) {
            throw new UpdateException(e.getMessage());
        } catch (PostEvaTaskQueryException e) {
            throw new QueryException(e.getMessage());
        }
    }

    @Override
    @Transactional
    public Void addEvaTemplate(NewEvaTemplateCmd cmd) throws ParseException {
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        //判断指标重复
        if(cmd.getProps()!=null) {
            List<String> props= Arrays.stream(cmd.getProps().split(",")).toList();
            long count = props.stream().distinct().count();
            if (props.size() != count) {
                throw new UpdateException("由于你输入的指标中有重复数据，故不能增加");
            }
        }

        FormTemplateDO formTemplateDO=new FormTemplateDO();
        formTemplateDO.setDescription(cmd.getDescription());
        formTemplateDO.setProps(cmd.getProps());
        formTemplateDO.setName(cmd.getName());
        formTemplateMapper.insert(formTemplateDO);
        //加缓存
        localCacheManager.invalidateCache(null,evaCacheConstants.TEMPLATE_LIST);
        LogUtils.logContent(cmd.getName() +" 评教模板");
        return null;
    }

    @Override
    @Transactional
    @LocalCacheInvalidate(area="#{@evaCacheConstants.ONE_TASK}", key="#id")
    public Void cancelEvaTaskById(Integer id){
        //取消相应的评教任务
        UpdateWrapper<EvaTaskDO> evaTaskWrapper=new UpdateWrapper<>();
        evaTaskWrapper.eq("id",id);
        EvaTaskDO evaTaskDO=evaTaskMapper.selectById(id);
        evaTaskDO.setStatus(2);
        evaTaskMapper.update(evaTaskDO,evaTaskWrapper);
        localCacheManager.invalidateCache(evaCacheConstants.TASK_LIST_BY_SEM, String.valueOf(courseMapper.selectById(courInfMapper.selectById(evaTaskDO.getCourInfId()).getCourseId()).getSemesterId()));
        localCacheManager.invalidateCache(evaCacheConstants.TASK_LIST_BY_TEACH,sysUserMapper.selectById(evaTaskDO.getTeacherId()).getName());
        return null;
    }
}
