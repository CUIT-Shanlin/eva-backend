package edu.cuit.infra.gateway.impl.eva;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import edu.cuit.client.dto.cmd.eva.EvaTemplateCmd;
import edu.cuit.client.dto.cmd.eva.NewEvaLogCmd;
import edu.cuit.client.dto.cmd.eva.NewEvaTaskCmd;
import edu.cuit.client.dto.cmd.eva.NewEvaTemplateCmd;
import edu.cuit.domain.gateway.eva.EvaUpdateGateway;
import edu.cuit.infra.dal.database.dataobject.course.CourInfDO;
import edu.cuit.infra.dal.database.dataobject.course.CourseDO;
import edu.cuit.infra.dal.database.dataobject.course.SemesterDO;
import edu.cuit.infra.dal.database.dataobject.eva.*;
import edu.cuit.infra.dal.database.dataobject.user.SysUserDO;
import edu.cuit.infra.dal.database.mapper.course.CourInfMapper;
import edu.cuit.infra.dal.database.mapper.course.CourseMapper;
import edu.cuit.infra.dal.database.mapper.course.SemesterMapper;
import edu.cuit.infra.dal.database.mapper.eva.*;
import edu.cuit.infra.dal.database.mapper.user.SysUserMapper;
import edu.cuit.infra.enums.cache.EvaCacheConstants;
import edu.cuit.infra.gateway.impl.eva.util.CalculateClassTime;
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
import java.time.LocalDate;
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

        //判断评价是否低于50个字符
        if(cmd.getTextValue().length()<50){
            throw new UpdateException("输入的评价文本过少，请用心评价此课程，故不能提交哦");
        }

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
        //同时发送该任务的评教待办消息;
        CourInfDO courInfDO=courInfMapper.selectById(cmd.getCourInfId());
        if(courInfDO==null){
            throw new UpdateException("并没有找到相关课程");
        }
        CourseDO courseDO=courseMapper.selectById(courInfDO.getCourseId());
        //选中的课程是否已经上完
        SemesterDO semesterDO = semesterMapper.selectById(courseDO.getSemesterId());
        LocalDate localDate = semesterDO.getStartDate().plusDays((courInfDO.getWeek()-1) * 7L + courInfDO.getDay() - 1);

        Integer f=2;//判断是不是课程快已经结束 1冲0无
        if(localDate.getYear()>=LocalDate.now().getYear()){
            if(localDate.getYear()==LocalDate.now().getYear()){
                if(localDate.getMonthValue()<LocalDate.now().getMonthValue()){
                    f=1;
                }else if(localDate.getMonthValue()>LocalDate.now().getMonthValue()){
                    f=0;
                }else {
                    if(localDate.getDayOfMonth()>LocalDate.now().getDayOfMonth()){
                        f=0;
                    }else {
                        if(localDate.getDayOfMonth()==LocalDate.now().getDayOfMonth()) {
                            String dateTime = localDate + " 00:00";//因为少了一个空格而不能满足格式而报错
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                            LocalDateTime localDateTime = LocalDateTime.parse(dateTime, formatter);
                            if (CalculateClassTime.calculateClassTime(localDateTime, courInfDO.getStartTime()).isBefore(LocalDateTime.now())) {
                                f = 1;
                            } else {
                                f = 0;
                            }
                        }
                        if(localDate.getDayOfMonth()<LocalDate.now().getDayOfMonth()) {
                            f = 1;
                        }
                    }
                }
            }else{
                f=0;
            }
        }else {
            f=1;
        }
        if(f==1){
            throw new UpdateException("课程已经开始了哦");
        }
        //看看是否和老师自己的课有冲突
        List<CourseDO> courseDOList=courseMapper.selectList(new QueryWrapper<CourseDO>().eq("teacher_id",cmd.getTeacherId()));
        List<Integer> courseIds=courseDOList.stream().map(CourseDO::getId).toList();
        if(CollectionUtil.isNotEmpty(courseIds)) {
            List<CourInfDO> courInfDOList = courInfMapper.selectList(new QueryWrapper<CourInfDO>().in("course_id", courseIds));
            for (int i = 0; i < courInfDOList.size(); i++) {
                if (courInfDO.getWeek().equals(courInfDOList.get(i).getWeek())) {
                    if (courInfDO.getDay().equals(courInfDOList.get(i).getDay())) {
                        if (((courInfDO.getStartTime() <= courInfDOList.get(i).getEndTime()) && (courInfDO.getEndTime() >= courInfDOList.get(i).getStartTime()))
                                || ((courInfDOList.get(i).getStartTime() <= courInfDO.getEndTime()) && (courInfDOList.get(i).getEndTime() >= courInfDO.getStartTime()))) {
                            throw new UpdateException("与你其他课程冲突");
                        }
                    }
                }
            }
        }
        //判定是否超过最大评教次数
        SysUserDO teacher=sysUserMapper.selectById(courseDO.getTeacherId());
        List<CourseDO> evaCourseDOS=courseMapper.selectList(new QueryWrapper<CourseDO>().eq("teacher_id",teacher.getId()));
        List<Integer> evaCourseIds=evaCourseDOS.stream().map(CourseDO::getId).toList();
        if(CollectionUtil.isNotEmpty(evaCourseIds)) {
            List<CourInfDO> evaCourInfoDOs = courInfMapper.selectList(new QueryWrapper<CourInfDO>().in("course_id", evaCourseIds));
            if(CollectionUtil.isNotEmpty(evaCourInfoDOs)) {
                List<Integer> evaCourInfoIds = evaCourInfoDOs.stream().map(CourInfDO::getId).toList();
                List<EvaTaskDO> evaTaskDOList1=evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>()
                        .in("cour_inf_id",evaCourInfoIds)
                        .eq("status",0)
                        .or()
                        .eq("status",1)
                        .in("cour_inf_id",evaCourInfoIds));
                if(evaTaskDOList1.size()>=maxNum){
                    throw new QueryException("任务发起失败，该老师本学期的被评教次数已达上限，不可再进行评教！");
                }
            }
        }

                //看看是否和老师其他评教任务有冲突
        List<EvaTaskDO> evaTaskDOList=evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().eq("teacher_id",cmd.getTeacherId()).eq("status",0));
        List<Integer> courInfoIds=evaTaskDOList.stream().map(EvaTaskDO::getCourInfId).toList();
        if(CollectionUtil.isNotEmpty(courInfoIds)) {
            List<CourInfDO> evaCourInfDOList = courInfMapper.selectList(new QueryWrapper<CourInfDO>().in("id", courInfoIds));
            for (int i = 0; i < evaCourInfDOList.size(); i++) {
                if (courInfDO.getWeek().equals(evaCourInfDOList.get(i).getWeek())) {
                    if (courInfDO.getDay().equals(evaCourInfDOList.get(i).getDay())) {
                        if ((courInfDO.getStartTime() <= evaCourInfDOList.get(i).getEndTime() && courInfDO.getEndTime() >= evaCourInfDOList.get(i).getStartTime())
                        ||(evaCourInfDOList.get(i).getStartTime()<=courInfDO.getEndTime()&&evaCourInfDOList.get(i).getEndTime()>=courInfDO.getStartTime())){
                            throw new UpdateException("与你其他任务所上课程冲突");
                        }
                    }
                }
            }
        }


        EvaTaskDO evaTaskDO=new EvaTaskDO();
        evaTaskDO.setCreateTime(LocalDateTime.now());
        evaTaskDO.setUpdateTime(LocalDateTime.now());
        evaTaskDO.setStatus(0);
        evaTaskDO.setCourInfId(cmd.getCourInfId());
        evaTaskDO.setTeacherId(cmd.getTeacherId());
        evaTaskMapper.insert(evaTaskDO);
        //加缓存
        localCacheManager.invalidateCache(evaCacheConstants.TASK_LIST_BY_SEM,String.valueOf(courseDO.getSemesterId()));
        localCacheManager.invalidateCache(evaCacheConstants.TASK_LIST_BY_TEACH,sysUserMapper.selectById(evaTaskDO.getTeacherId()).getName());
        Integer taskId=evaTaskMapper.selectOne(new QueryWrapper<EvaTaskDO>().eq("teacher_id",cmd.getTeacherId()).eq("cour_inf_id",cmd.getCourInfId()).eq("status",0)).getId();

        if(taskId==null){
            throw new QueryException("没有找到你的id");
        }
        return taskId;
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
