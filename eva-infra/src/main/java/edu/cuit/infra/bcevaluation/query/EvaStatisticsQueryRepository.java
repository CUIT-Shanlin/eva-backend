package edu.cuit.infra.bcevaluation.query;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONConfig;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.alibaba.cola.exception.SysException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.cuit.client.dto.clientobject.DateEvaNumCO;
import edu.cuit.client.dto.clientobject.MoreDateEvaNumCO;
import edu.cuit.client.dto.clientobject.SimplePercentCO;
import edu.cuit.client.dto.clientobject.SimpleEvaPercentCO;
import edu.cuit.client.dto.clientobject.eva.EvaScoreInfoCO;
import edu.cuit.client.dto.clientobject.eva.EvaSituationCO;
import edu.cuit.client.dto.clientobject.eva.EvaWeekAddCO;
import edu.cuit.client.dto.clientobject.eva.PastTimeEvaDetailCO;
import edu.cuit.client.dto.clientobject.eva.ScoreRangeCourseCO;
import edu.cuit.client.dto.clientobject.user.UnqualifiedUserInfoCO;
import edu.cuit.client.dto.clientobject.user.UnqualifiedUserResultCO;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.UnqualifiedUserConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;
import edu.cuit.infra.convertor.PaginationConverter;
import edu.cuit.infra.dal.database.dataobject.course.CourInfDO;
import edu.cuit.infra.dal.database.dataobject.course.CourseDO;
import edu.cuit.infra.dal.database.dataobject.eva.EvaTaskDO;
import edu.cuit.infra.dal.database.dataobject.eva.FormRecordDO;
import edu.cuit.infra.dal.database.dataobject.user.SysUserDO;
import edu.cuit.infra.dal.database.mapper.course.CourInfMapper;
import edu.cuit.infra.dal.database.mapper.course.CourseMapper;
import edu.cuit.infra.dal.database.mapper.eva.EvaTaskMapper;
import edu.cuit.infra.dal.database.mapper.eva.FormRecordMapper;
import edu.cuit.infra.dal.database.mapper.user.SysUserMapper;
import edu.cuit.infra.enums.cache.EvaCacheConstants;
import edu.cuit.infra.enums.cache.UserCacheConstants;
import edu.cuit.zhuyimeng.framework.cache.LocalCacheManager;
import edu.cuit.zhuyimeng.framework.common.exception.QueryException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * 评教统计读侧 QueryRepo 实现（从 {@link EvaQueryRepository} 渐进式拆分出来）。
 *
 * <p>保持行为不变：仅搬运实现与依赖归属，不调整统计口径与异常文案。</p>
 */
@Primary
@Component
@RequiredArgsConstructor
public class EvaStatisticsQueryRepository implements EvaStatisticsQueryRepo {
    private final CourseMapper courseMapper;
    private final EvaTaskMapper evaTaskMapper;
    private final CourInfMapper courInfMapper;
    private final FormRecordMapper formRecordMapper;
    private final SysUserMapper sysUserMapper;
    private final PaginationConverter paginationConverter;
    private final EvaCacheConstants evaCacheConstants;
    private final LocalCacheManager localCacheManager;
    private final UserCacheConstants userCacheConstants;

    //zjok
    @Override
    public Optional<EvaScoreInfoCO> evaScoreStatisticsInfo(Integer semId, Number score) {
        //格式小数，日期
        DecimalFormat df = new DecimalFormat("0.0");
        //学期id->找到课程-》找到课程详情-》评教任务详情-》评教表单记录里面
        List<Integer> evaTaskIdS=getEvaTaskIdS(semId);
        List<FormRecordDO> nowFormRecordDOS;
        if(CollectionUtil.isEmpty(evaTaskIdS)){
            return Optional.empty();
        }else {
            nowFormRecordDOS = formRecordMapper.selectList(new QueryWrapper<FormRecordDO>().in("task_id", evaTaskIdS));
        }
        Integer totalNum;
        if(CollectionUtil.isEmpty(nowFormRecordDOS)){
            totalNum=0;
        }else {
            //总评教数
            totalNum = nowFormRecordDOS.size();
        }
        //根据他们的form_props_values得到对应的数值
        List<String> strings=nowFormRecordDOS.stream().map(FormRecordDO::getFormPropsValues).toList();
        List<Double> numbers =new ArrayList<>();
        //低于 指定分数的数目
        double aScore = ((BigDecimal) score).doubleValue();
        Integer lowerNum=0;
        Integer higherNum=0;
        for(int i=0;i<strings.size();i++){
            //整个方法把单个text整到平均分
            numbers.add(i, stringToSumAver(strings.get(i)));
            if(aScore>numbers.get(i)){
                lowerNum++;
            }
            if(aScore<numbers.get(i)){
                higherNum++;
            }
        }
        Double percent;
        if(totalNum!=0){
            percent=Double.parseDouble(df.format((higherNum/(double)totalNum)*100.0));
        }else {
            percent = Double.parseDouble(df.format(100));
        }
        //整个方法把以前的数据拿出来
        List<FormRecordDO> last1FormRecordDOS=formRecordMapper.selectList(new QueryWrapper<FormRecordDO>().in("task_id",evaTaskIdS).gt("create_time",LocalDateTime.now().minusDays(1)));
        Integer totalNum1;
        if(CollectionUtil.isEmpty(last1FormRecordDOS)){
            totalNum1=0;
        }else {
            totalNum1=last1FormRecordDOS.size();
        }
        //根据他们的form_props_values得到对应的数值
        List<String> strings1=last1FormRecordDOS.stream().map(FormRecordDO::getFormPropsValues).toList();
        List<Double> numbers1=new ArrayList<>();
        //低于 指定分数的数目
        Integer lowerNum1=0;
        Integer higherNum1=0;
        for(int i=0;i<strings1.size();i++){
            //整个方法把单个text整到平均分
            numbers1.add(i, stringToSumAver(strings1.get(i)));
            if(aScore>numbers1.get(i)){
                lowerNum1++;
            }
            if(aScore<numbers1.get(i)){
                higherNum1++;
            }
        }
        Double percent1;
        if(totalNum1!=0) {
            percent1 = Double.parseDouble(df.format((higherNum1 / (double) totalNum1) * 100.0));
        }else {
            percent1 = Double.parseDouble(df.format(100));
        }
        //7日内 percent 的值
        List<SimplePercentCO> percentArr=new ArrayList<>();
        for(int i=0;i<7;i++){
            percentArr.add(getSimplePercent(i,evaTaskIdS,aScore));
        }

        //构建EvaScoreInfoCO对象返回
        EvaScoreInfoCO evaScoreInfoCO=new EvaScoreInfoCO();
        evaScoreInfoCO.setLowerNum(lowerNum);
        evaScoreInfoCO.setTotalNum(totalNum);
        evaScoreInfoCO.setPercent(percent);
        evaScoreInfoCO.setMoreNum(lowerNum-lowerNum1);
        evaScoreInfoCO.setMorePercent(percent-percent1);
        evaScoreInfoCO.setPercentArr(percentArr);
        return Optional.of(evaScoreInfoCO);
    }

    //zjok
    @Override
    public Optional<EvaSituationCO> evaTemplateSituation(Integer semId) {
        //日期格式
        SimpleDateFormat sf=new SimpleDateFormat("YYYY-MM-DD");
        List<EvaTaskDO> evaTaskDOS=new ArrayList<>();
        if(semId!=null){
            List<Integer> evaTaskIds=getEvaTaskIdS(semId);
            if(CollectionUtil.isEmpty(evaTaskIds)){
                return Optional.empty();
            }
            evaTaskDOS=evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().in("id",evaTaskIds));
        }else{
            evaTaskDOS=evaTaskMapper.selectList(null);
            if(CollectionUtil.isEmpty(evaTaskDOS)){
                return Optional.empty();
            }
        }
        Integer totalNum=0;
        Integer evaNum=0;
        for(int i=0;i<evaTaskDOS.size();i++){
            if(evaTaskDOS.get(i).getStatus()==0){
                totalNum++;
            }
            if(evaTaskDOS.get(i).getStatus()==1){
                evaNum++;
            }
        }
        LocalDateTime end=LocalDateTime.now();
        LocalDateTime start=LocalDateTime.of(end.getYear(),end.getMonthValue(),end.getDayOfMonth(),0,0);

        List<EvaTaskDO> lastEvaTaskDOS=new ArrayList<>();
        if(semId!=null){
            List<Integer> evaTaskIds=getEvaTaskIdS(semId);
            if(CollectionUtil.isEmpty(evaTaskIds)){
                return Optional.empty();
            }
            lastEvaTaskDOS=evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().in("id",evaTaskIds).between("create_time",start,end));
        }else{
            lastEvaTaskDOS=evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().between("create_time",start,end));
            if(CollectionUtil.isEmpty(evaTaskDOS)){
                return Optional.empty();
            }
        }

        Integer unTotalNum=0;
        for(int i=0;i<lastEvaTaskDOS.size();i++){
            if(lastEvaTaskDOS.get(i).getStatus()==0){
                unTotalNum++;
            }
        }
        List<DateEvaNumCO> list=new ArrayList<>();
        for(int i=0;i<7;i++){
            DateEvaNumCO dateEvaNumCO=new DateEvaNumCO();
            dateEvaNumCO.setDate(LocalDate.now().minusDays(i));
            dateEvaNumCO.setValue(getEvaNumByDate(i,semId));
            list.add(dateEvaNumCO);
        }

        EvaSituationCO evaSituationCO=new EvaSituationCO();
        evaSituationCO.setEvaNum(evaNum);
        evaSituationCO.setTotalNum(totalNum);
        evaSituationCO.setMoreNum(getEvaNumByDate(0,semId));
        evaSituationCO.setMoreEvaNum(unTotalNum);
        evaSituationCO.setEvaNumArr(list);

        return Optional.of(evaSituationCO);
    }

    //zjok
    @Override
    public List<Integer> getMonthEvaNUmber(Integer semId) {
        Integer nowYear=LocalDateTime.now().getYear();
        Integer lastYear=LocalDateTime.now().minusMonths(1).getYear();
        //得到这个月
        Integer nowMonth=LocalDateTime.now().getMonthValue();
        Integer lastMonth=LocalDateTime.now().minusMonths(1).getMonthValue();

        LocalDateTime nowStart=LocalDateTime.of(nowYear,nowMonth,1,0,0,0);
        LocalDateTime nowEnd=LocalDateTime.now();
        LocalDateTime lastStart=LocalDateTime.of(lastYear,lastMonth,1,0,0,0);
        LocalDateTime lastEnd=lastStart.plusMonths(1);

        //学期id->课程-》详情-》任务-》记录
        List<Integer> evaTaskIdS=getEvaTaskIdS(semId);

        if(CollectionUtil.isEmpty(evaTaskIdS)){
            List<Integer> list=new ArrayList<>();
            list.add(0);
            list.add(0);
            return list;
        }

        List<FormRecordDO> nowFormRecordDOS=formRecordMapper.selectList(new QueryWrapper<FormRecordDO>().in("task_id",evaTaskIdS).between("create_time",nowStart,nowEnd));
        List<FormRecordDO> lastFormRecordDOS=formRecordMapper.selectList(new QueryWrapper<FormRecordDO>().in("task_id",evaTaskIdS).between("create_time",lastStart,lastEnd));

        //获取上个月和本月的评教数目，以有两个整数的List形式返回，data[0]：上个月评教数目；data[1]：本月评教数目
        List<Integer> list=new ArrayList<>();
        list.add(lastFormRecordDOS.size());
        list.add(nowFormRecordDOS.size());
        return list;
    }

    //zjok
    @Override
    public Optional<EvaWeekAddCO> evaWeekAdd(Integer week,Integer semId) {
        //得到前week周的数据
        LocalDate start=LocalDate.now().minusWeeks(week).with(DayOfWeek.MONDAY);
        LocalDateTime st=LocalDateTime.of(start.getYear(),start.getMonth(),start.getDayOfMonth(),0,0);
        LocalDate end=start.plusDays(7);
        LocalDateTime et=LocalDateTime.of(end.getYear(),end.getMonth(),end.getDayOfMonth(),0,0);

        List<Integer> taskIds=getEvaTaskIdS(semId);
        if(CollectionUtil.isEmpty(taskIds)){
            return Optional.empty();
        }
        Integer moreNum=0;
        List<FormRecordDO> recordDOS=formRecordMapper.selectList(new QueryWrapper<FormRecordDO>().in("task_id",taskIds).between("create_time",st,et));
        if(CollectionUtil.isNotEmpty(recordDOS)){
            moreNum=recordDOS.size();
        }

        Integer lastMoreNum=0;
        List<FormRecordDO> lastRecordDOS=formRecordMapper.selectList(new QueryWrapper<FormRecordDO>().in("task_id",taskIds).between("create_time",st.minusDays(7),et.minusDays(7)));
        if(CollectionUtil.isNotEmpty(lastRecordDOS)){
            lastMoreNum=lastRecordDOS.size();
        }

        Double percent=0.0;
        if(moreNum!=0&&lastMoreNum!=0) {//TODO
            percent = (moreNum - lastMoreNum) / Double.valueOf(lastMoreNum) * 100;
        }
        List<Integer> weekAdd=new ArrayList<>();
        for(int i=0;i<7;i++){
            LocalDateTime s=st.plusDays(i);
            LocalDateTime e=st.plusDays(i+1);

            Integer num=0;
            List<FormRecordDO> recordDO=formRecordMapper.selectList(new QueryWrapper<FormRecordDO>().in("task_id",taskIds).between("create_time",s,e));
            if(CollectionUtil.isNotEmpty(recordDO)){
                num=recordDO.size();
            }
            weekAdd.add(i,num);
        }
        return Optional.of(new EvaWeekAddCO().setMoreNum(moreNum).setMorePercent(percent).setEvaNumArr(weekAdd));
    }

    //zjok
    @Override
    public Optional<PastTimeEvaDetailCO> getEvaData(Integer semId, Integer num, Integer target, Integer evaTarget) {
        DecimalFormat ds=new DecimalFormat("0.0");
        SimpleDateFormat sf=new SimpleDateFormat("YY-MM-DD");
        //根据semId找到
        List<Integer> evaTaskIdS=getEvaTaskIdS(semId);

        LocalDateTime timeEnd=LocalDateTime.now();
        LocalDate timeStart=LocalDate.now().minusDays((long)num);

        LocalDate lastStart=LocalDate.now().minusDays((long)2*num);
        LocalDate lastEnd=LocalDate.now().minusDays(num);
        if(CollectionUtil.isEmpty(evaTaskIdS)){
            return Optional.empty();
        }
        List<FormRecordDO> formRecordDOS1=formRecordMapper.selectList(new QueryWrapper<FormRecordDO>().in("task_id",evaTaskIdS).between("create_time",timeStart,timeEnd));
        List<FormRecordDO> formRecordDOS2=formRecordMapper.selectList(new QueryWrapper<FormRecordDO>().in("task_id",evaTaskIdS).between("create_time",lastStart,lastEnd));
        SimpleEvaPercentCO totalEvaInfo=new SimpleEvaPercentCO();
        totalEvaInfo.setNum(formRecordDOS1.size());
        if(formRecordDOS2.size()!=0) {
            totalEvaInfo.setMorePercent(Double.parseDouble(ds.format((formRecordDOS1.size() / formRecordDOS2.size()) * 100)));
        }else {
            totalEvaInfo.setMorePercent(null);
        }
        List<MoreDateEvaNumCO> dataArr=new ArrayList<>();

        for(int i=1;i<=num;i++){
            List<FormRecordDO> formRecordDOS3;
            if(i==num){
                formRecordDOS3=formRecordMapper.selectList(new QueryWrapper<FormRecordDO>().in("task_id",evaTaskIdS).between("create_time",LocalDate.now().minusDays((long)num-i),LocalDateTime.now()));
            }else {
                formRecordDOS3 = formRecordMapper.selectList(new QueryWrapper<FormRecordDO>().in("task_id", evaTaskIdS).between("create_time", LocalDate.now().minusDays((long) num - i), LocalDate.now().minusDays((long) num - i - 1)));
            }
            MoreDateEvaNumCO dateEvaNumCO=new MoreDateEvaNumCO();
            dateEvaNumCO.setDate(LocalDate.now().minusDays((long)num-i));
            dateEvaNumCO.setMoreEvaNum(formRecordDOS3.size());
            dataArr.add(dateEvaNumCO);
        }
        //SimpleEvaPercentCO evaQualifiedInfo  SimpleEvaPercentCO qualifiedInfo
        List<Integer> getCached=localCacheManager.getCache(null,userCacheConstants.ALL_USER_ID);
        if(CollectionUtil.isEmpty(getCached)) {
            List<SysUserDO> teacher = sysUserMapper.selectList(null);
            List<Integer> teacherIdS = teacher.stream().map(SysUserDO::getId).toList();
            localCacheManager.putCache(null,userCacheConstants.ALL_USER_ID,teacherIdS);
            getCached=localCacheManager.getCache(null,userCacheConstants.ALL_USER_ID);
        }
        if(CollectionUtil.isEmpty(getCached)){
            throw new QueryException("没有找到相关老师");
        }

        Integer evaNum=0;
        Integer pastEvaNum=0;
        Integer evaEdNum=0;
        Integer pastEvaEdNum=0;
        for(int i=0;i<getCached.size();i++){
            Integer n1=getEvaNumByTeacherIdAndLocalTime(getCached.get(i),num,0);
            Integer n2=getEvaEdNumByTeacherIdAndLocalTime(getCached.get(i),num,0);
            Integer m1=getEvaNumByTeacherIdAndLocalTime(getCached.get(i),num*2,num);
            Integer m2=getEvaEdNumByTeacherIdAndLocalTime(getCached.get(i),num*2,num);
            if(n1>=target){
                evaNum++;
            }
            if(n2>=evaTarget){
                evaEdNum++;
            }
            if(m1>=target){
                pastEvaNum++;
            }
            if(m2>=evaTarget){
                pastEvaEdNum++;
            }
        }
        SimpleEvaPercentCO evaQualifiedInfo=new SimpleEvaPercentCO();
        evaQualifiedInfo.setNum(evaNum);
        if(pastEvaNum==0){
            evaQualifiedInfo.setMorePercent(null);
        }else {
            evaQualifiedInfo.setMorePercent(Double.parseDouble(ds.format((evaNum / pastEvaNum) * 100)));
        }
        SimpleEvaPercentCO qualifiedInfo=new SimpleEvaPercentCO();
        qualifiedInfo.setNum(evaEdNum);
        if(pastEvaEdNum==0) {
            qualifiedInfo.setMorePercent(null);
        }else {
            qualifiedInfo.setMorePercent(Double.parseDouble(ds.format((evaEdNum / pastEvaEdNum) * 100)));
        }
        PastTimeEvaDetailCO pastTimeEvaDetailCO=new PastTimeEvaDetailCO();
        pastTimeEvaDetailCO.setTotalEvaInfo(totalEvaInfo);
        pastTimeEvaDetailCO.setEvaQualifiedInfo(evaQualifiedInfo);
        pastTimeEvaDetailCO.setQualifiedInfo(qualifiedInfo);
        pastTimeEvaDetailCO.setDataArr(dataArr);

        return Optional.of(pastTimeEvaDetailCO);
    }

    @Override
    public Optional<UnqualifiedUserResultCO> getEvaTargetAmountUnqualifiedUser(Integer semId, Integer num, Integer target){
        List<Integer> getCached=localCacheManager.getCache(null,userCacheConstants.ALL_USER_ID);
        if(CollectionUtil.isEmpty(getCached)) {
            List<SysUserDO> teacher = sysUserMapper.selectList(null);
            List<Integer> teacherIdS = teacher.stream().map(SysUserDO::getId).toList();
            localCacheManager.putCache(null,userCacheConstants.ALL_USER_ID,teacherIdS);
            getCached=localCacheManager.getCache(null,userCacheConstants.ALL_USER_ID);
        }
        if(CollectionUtil.isEmpty(getCached)){
            throw new QueryException("找不到相关的老师");
        }

        List<UnqualifiedUserInfoCO> dataArr=new ArrayList<>();
        //根据
        for(int i=0;i<getCached.size();i++){
            Integer n=getEvaNumByTeacherId(getCached.get(i),semId);
            if(n<target){
                UnqualifiedUserInfoCO unqualifiedUserInfoCO=new UnqualifiedUserInfoCO();
                unqualifiedUserInfoCO.setDepartment(sysUserMapper.selectById(getCached.get(i)).getDepartment());
                unqualifiedUserInfoCO.setId(getCached.get(i));
                unqualifiedUserInfoCO.setName(sysUserMapper.selectById(getCached.get(i)).getName());
                unqualifiedUserInfoCO.setNum(n);
                dataArr.add(unqualifiedUserInfoCO);
            }
        }
        if(CollectionUtil.isEmpty(dataArr)){
            return Optional.empty();
        }
        //给收集的信息co排个序
        for(int i=0;i<dataArr.size()-1;i++){
            for(int j=i+1;j<dataArr.size();j++){
                if(dataArr.get(i).getNum()>dataArr.get(j).getNum()){
                    UnqualifiedUserInfoCO t=dataArr.get(j);
                    dataArr.set(j,dataArr.get(i));
                    dataArr.set(i,t);
                }
            }
        }
        List<UnqualifiedUserInfoCO> getDataArr=new ArrayList<>();
        for(int i=0;i<num;i++){
            getDataArr.add(i,dataArr.get(i));
        }
        UnqualifiedUserResultCO unqualifiedUserResultCO=new UnqualifiedUserResultCO();
        unqualifiedUserResultCO.setDataArr(getDataArr);
        unqualifiedUserResultCO.setTotal(dataArr.size());
        return Optional.of(unqualifiedUserResultCO);
    }

    @Override
    public Optional<UnqualifiedUserResultCO> getBeEvaTargetAmountUnqualifiedUser(Integer semId,Integer num,Integer target){
        //根据系查老师
        List<Integer> getCached=localCacheManager.getCache(null,userCacheConstants.ALL_USER_ID);
        if(CollectionUtil.isEmpty(getCached)) {
            List<SysUserDO> teacher = sysUserMapper.selectList(null);
            List<Integer> teacherIdS = teacher.stream().map(SysUserDO::getId).toList();
            localCacheManager.putCache(null,userCacheConstants.ALL_USER_ID,teacherIdS);
            getCached=localCacheManager.getCache(null,userCacheConstants.ALL_USER_ID);
        }

        if(CollectionUtil.isEmpty(getCached)){
            throw new QueryException("找不到相关的老师");
        }

        List<UnqualifiedUserInfoCO> dataArr=new ArrayList<>();

        //任务-》课程详情-》课程-》老师
        for(int i=0;i<getCached.size();i++){
            Integer n=getEvaEdNumByTeacherId(getCached.get(i),semId);
            if(n<target){
                UnqualifiedUserInfoCO unqualifiedUserInfoCO=new UnqualifiedUserInfoCO();
                unqualifiedUserInfoCO.setDepartment(sysUserMapper.selectById(getCached.get(i)).getDepartment());
                unqualifiedUserInfoCO.setId(getCached.get(i));
                unqualifiedUserInfoCO.setName(sysUserMapper.selectById(getCached.get(i)).getName());
                unqualifiedUserInfoCO.setNum(n);
                dataArr.add(unqualifiedUserInfoCO);
            }
        }
        if(CollectionUtil.isEmpty(dataArr)){
            return Optional.empty();
        }
        //给收集的信息co排个序
        for(int i=0;i<dataArr.size()-1;i++){
            for(int j=i+1;j<dataArr.size();j++){
                if(dataArr.get(i).getNum()>dataArr.get(j).getNum()){
                    UnqualifiedUserInfoCO t=dataArr.get(j);
                    dataArr.set(j,dataArr.get(i));
                    dataArr.set(i,t);
                }
            }
        }
        List<UnqualifiedUserInfoCO> getDataArr=new ArrayList<>();
        for(int i=0;i<num;i++){
            getDataArr.add(i,dataArr.get(i));
        }
        UnqualifiedUserResultCO unqualifiedUserResultCO=new UnqualifiedUserResultCO();
        unqualifiedUserResultCO.setDataArr(getDataArr);
        unqualifiedUserResultCO.setTotal(dataArr.size());

        return Optional.of(unqualifiedUserResultCO);
    }

    @Override
    public PaginationResultEntity<UnqualifiedUserInfoCO> pageEvaUnqualifiedUserInfo(Integer semId,PagingQuery<UnqualifiedUserConditionalQuery> query, Integer target){
        List<Integer> userIds=new ArrayList<>();
        QueryWrapper<SysUserDO> queryWrapper = new QueryWrapper<>();
        if(query.getQueryObj().getDepartment()!=null&& StringUtils.isNotBlank(query.getQueryObj().getDepartment())){
            queryWrapper.eq("department",query.getQueryObj().getDepartment());
        }
        if(query.getQueryObj().getKeyword()!=null&& StringUtils.isNotBlank(query.getQueryObj().getKeyword())){
            queryWrapper.like("name",query.getQueryObj().getKeyword());
        }

        List<SysUserDO> sysUserDOS=sysUserMapper.selectList(queryWrapper);
        userIds=sysUserDOS.stream().map(SysUserDO::getId).toList();

        if(CollectionUtil.isEmpty(userIds)){
            List list=new ArrayList();
            Page<UnqualifiedUserInfoCO> pageUnqualifiedUserInfoCO=new Page<>(query.getPage(), query.getSize(),0);
            return paginationConverter.toPaginationEntity(pageUnqualifiedUserInfoCO,list);
        }

        List<Integer> teacherIdS=new ArrayList<>();
        List<UnqualifiedUserInfoCO> records=new ArrayList<>();
        for(int i=0;i<userIds.size();i++){
            Integer k=getEvaNumByTeacherId(userIds.get(i),semId);
            if(k<target){
                teacherIdS.add(userIds.get(i));
                UnqualifiedUserInfoCO unqualifiedUserInfoCO=new UnqualifiedUserInfoCO();
                unqualifiedUserInfoCO.setId(userIds.get(i));
                unqualifiedUserInfoCO.setNum(k);
                unqualifiedUserInfoCO.setDepartment(sysUserMapper.selectById(userIds.get(i)).getDepartment());
                unqualifiedUserInfoCO.setName(sysUserMapper.selectById(userIds.get(i)).getName());

                records.add(unqualifiedUserInfoCO);
            }
        }
        List<UnqualifiedUserInfoCO> k=new ArrayList<>();
        for(int i=(query.getPage()-1)*query.getSize();i< query.getPage()* query.getSize();i++){
            if(i>(records.size()-1)){
                break;
            }
            k.add(records.get(i));
        }
        Page<UnqualifiedUserInfoCO> pageUnqualifiedUserInfoCO=new Page<>(query.getPage(), query.getSize(),records.size());

        return paginationConverter.toPaginationEntity(pageUnqualifiedUserInfoCO,k);
    }

    @Override
    public PaginationResultEntity<UnqualifiedUserInfoCO> pageBeEvaUnqualifiedUserInfo(Integer semId,PagingQuery<UnqualifiedUserConditionalQuery> query,Integer target){

        List<Integer> userIds=new ArrayList<>();
        QueryWrapper<SysUserDO> queryWrapper = new QueryWrapper<>();
        if(query.getQueryObj().getDepartment()!=null&& StringUtils.isNotBlank(query.getQueryObj().getDepartment())){
            queryWrapper.eq("department",query.getQueryObj().getDepartment());
        }
        if(query.getQueryObj().getKeyword()!=null&& StringUtils.isNotBlank(query.getQueryObj().getKeyword())){
            queryWrapper.like("name",query.getQueryObj().getKeyword());
        }

        List<SysUserDO> sysUserDOS=sysUserMapper.selectList(queryWrapper);
        userIds=sysUserDOS.stream().map(SysUserDO::getId).toList();
        if(CollectionUtil.isEmpty(userIds)){
            List list=new ArrayList();
            Page<UnqualifiedUserInfoCO> pageUnqualifiedUserInfoCO=new Page<>(query.getPage(), query.getSize(),0);
            return paginationConverter.toPaginationEntity(pageUnqualifiedUserInfoCO,list);
        }

        List<Integer> teacherIdS=new ArrayList<>();
        List<UnqualifiedUserInfoCO> records=new ArrayList<>();
        for(int i=0;i<userIds.size();i++){
            Integer k=getEvaEdNumByTeacherId(userIds.get(i),semId);
            if(k<target){
                teacherIdS.add(userIds.get(i));
                UnqualifiedUserInfoCO unqualifiedUserInfoCO=new UnqualifiedUserInfoCO();
                unqualifiedUserInfoCO.setId(userIds.get(i));
                unqualifiedUserInfoCO.setNum(k);
                unqualifiedUserInfoCO.setDepartment(sysUserMapper.selectById(userIds.get(i)).getDepartment());
                unqualifiedUserInfoCO.setName(sysUserMapper.selectById(userIds.get(i)).getName());

                records.add(unqualifiedUserInfoCO);
            }
        }
        List<UnqualifiedUserInfoCO> k=new ArrayList<>();
        for(int i=(query.getPage()-1)*query.getSize();i< query.getPage()* query.getSize();i++){
            if(i>(records.size()-1)){
                break;
            }
            k.add(records.get(i));
        }
        Page<UnqualifiedUserInfoCO> pageUnqualifiedUserInfoCO=new Page<>(query.getPage(), query.getSize(),records.size());

        return paginationConverter.toPaginationEntity(pageUnqualifiedUserInfoCO,k);
    }

    @Override
    public List<ScoreRangeCourseCO> scoreRangeCourseInfo(Integer num, Integer interval) {
        //得到全部记录数据
        List<FormRecordDO> getCached=localCacheManager.getCache(null,evaCacheConstants.LOG_LIST);
        if(CollectionUtil.isEmpty(getCached)) {
            List<FormRecordDO> formRecordDOS = formRecordMapper.selectList(null);
            localCacheManager.putCache(null,evaCacheConstants.LOG_LIST,formRecordDOS);
            getCached=localCacheManager.getCache(null,evaCacheConstants.LOG_LIST);
        }
        List<String> strings=getCached.stream().map(FormRecordDO::getFormPropsValues).toList();
        if(CollectionUtil.isEmpty(strings)){
            List list=new ArrayList();
            return list;
        }
        //整到每个记录的分
        List<Double> numbers =new ArrayList<>();
        for(int i=0;i<strings.size();i++){
            //整个方法把单个text整到平均分
            numbers.add(i, stringToSumAver(strings.get(i)));
        }
        List<ScoreRangeCourseCO> scoreRangeCourseCOS = new ArrayList<>();
        for(int i=100;i>100-(num*interval);i=i-interval){
            Integer sum=0;
            for(int j=0;j<numbers.size();j++){
                if(numbers.get(j)<=i&&numbers.get(j)>i-interval){
                    sum++;
                }
            }
            ScoreRangeCourseCO scoreRangeCourseCO=new ScoreRangeCourseCO();
            scoreRangeCourseCO.setStartScore(i-interval);
            scoreRangeCourseCO.setEndScore(i);
            scoreRangeCourseCO.setCount(sum);
            scoreRangeCourseCOS.add(scoreRangeCourseCO);
        }
        return scoreRangeCourseCOS;
    }

    @Override
    public List<Integer> getCountAbEva(Integer semId, Integer userId) {
        List k=new ArrayList();
        k.add(getEvaNumByTeacherId(userId,semId));
        k.add(getEvaEdNumByTeacherId(userId,semId));
        return k;
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

    //根据传来的String数据form_props_values中的数据解析出来得到平均分
    private Double stringToSumAver(String s) {
        Double score=0.0;
        JSONArray jsonArray;
        try {
            jsonArray = JSONUtil.parseArray(s, JSONConfig.create()
                    .setIgnoreError(true));
        }catch (Exception e){
            throw new SysException("jsonObject 数据对象转化失败");
        }
        Iterator<Object> iterator = jsonArray.iterator();
        while(iterator.hasNext()){
            JSONObject jsonObject = (JSONObject) iterator.next();
            // 处理jsonObject
            score=score+Double.parseDouble(jsonObject.get("score").toString());
        }
        return score/jsonArray.size();
    }

    //根据传来的前n天,还有evaTaskIdS返回SimplePercent对象
    private SimplePercentCO getSimplePercent(Integer n,List<Integer> evaTaskIdS,Double score){
        DecimalFormat df = new DecimalFormat("0.0");
        if(CollectionUtil.isEmpty(evaTaskIdS)){
            SimplePercentCO simplePercentCO=new SimplePercentCO();
            simplePercentCO.setValue(0.0);
            simplePercentCO.setDate(LocalDate.now().minusDays(n));
            return simplePercentCO;
        }
        List<FormRecordDO> lastFormRecordDOS=formRecordMapper.selectList(new QueryWrapper<FormRecordDO>().in("task_id",evaTaskIdS).lt("create_time",LocalDateTime.now().minusDays(n)));
        //总评教数
        Integer totalNum=lastFormRecordDOS.size();
        //根据他们的form_props_values得到对应的数值
        List<String> strings=lastFormRecordDOS.stream().map(FormRecordDO::getFormPropsValues).toList();
        if(CollectionUtil.isEmpty(strings)){
            SimplePercentCO simplePercentCO=new SimplePercentCO();
            simplePercentCO.setValue(0.0);
            simplePercentCO.setDate(LocalDate.now().minusDays(n));
            return simplePercentCO;
        }
        List<Double> numbers =new ArrayList<>();
        //低于 指定分数的数目
        Integer higherNum=0;
        for(int i=0;i<strings.size();i++){
            //整个方法把单个text整到平均分
            numbers.add(stringToSumAver(strings.get(i)));
            if(score<numbers.get(i)){
                higherNum++;
            }
        }
        Double percent;
        if(totalNum==0){
            percent=100.0;
        }else {
            percent = Double.parseDouble(df.format((higherNum / (double) totalNum) * 100.0));
        }
        SimplePercentCO simplePercentCO=new SimplePercentCO();
        simplePercentCO.setValue(percent);
        //
        simplePercentCO.setDate(LocalDate.now().minusDays(n));
        return simplePercentCO;
    }

    //获得几天前的新增评教数
    private Integer getEvaNumByDate(Integer num,Integer semId){
        LocalDateTime start;
        LocalDateTime end=LocalDateTime.now();
        LocalDateTime time=LocalDateTime.of(end.getYear(),end.getMonthValue(),end.getDayOfMonth(),0,0);
        if(num==0){
            end=LocalDateTime.now();
            start=LocalDateTime.of(end.getYear(),end.getMonthValue(),end.getDayOfMonth(),0,0);
        }else{
            end=time.minusDays(num-1);
            start=time.minusDays(num);
        }
        QueryWrapper<FormRecordDO> query=new QueryWrapper<>();
        if(semId!=null){
            List<Integer> evaTaskIds=getEvaTaskIdS(semId);
            if(CollectionUtil.isEmpty(evaTaskIds)){
                return 0;
            }
            query.in("task_id",evaTaskIds);
        }
        List<FormRecordDO> formRecordDOs=formRecordMapper.selectList(query.between("create_time",start,end));
        return formRecordDOs.size();
    }

    //
    private Integer getEvaNumByTeacherId(Integer teacherId,Integer semId){
        List<Integer> evaIds=getEvaTaskIdS(semId);
        if(CollectionUtil.isEmpty(evaIds)){
            return 0;
        }

        List<EvaTaskDO> getCached=localCacheManager.getCache(evaCacheConstants.TASK_LIST_BY_TEACH,sysUserMapper.selectById(teacherId).getName());
        if(CollectionUtil.isEmpty(getCached)) {
            List<EvaTaskDO> evaTaskDOS = evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().eq("teacher_id", teacherId).in("id", evaIds));
            localCacheManager.putCache(evaCacheConstants.TASK_LIST_BY_TEACH,sysUserMapper.selectById(teacherId).getName(),evaTaskDOS);
            getCached=localCacheManager.getCache(evaCacheConstants.TASK_LIST_BY_TEACH,sysUserMapper.selectById(teacherId).getName());
        }
        if(CollectionUtil.isEmpty(getCached)){
            return 0;
        }
        List<Integer> taskIds=getCached.stream().map(EvaTaskDO::getId).toList();
        if(CollectionUtil.isEmpty(taskIds)){
            return 0;
        }
        List<FormRecordDO> formRecordDOS=formRecordMapper.selectList(new QueryWrapper<FormRecordDO>().in("task_id",taskIds));
        if(CollectionUtil.isEmpty(formRecordDOS)){
            return 0;
        }
        List<Integer> recordIds=formRecordDOS.stream().map(FormRecordDO::getId).toList();

        return recordIds.size();
    }

    private Integer getEvaEdNumByTeacherId(Integer teacherId,Integer semId){
        List<CourseDO> courseDOS;
        if(semId!=null) {
            courseDOS = courseMapper.selectList(new QueryWrapper<CourseDO>().eq("teacher_id", teacherId).eq("semester_id",semId));
        }else {
            courseDOS = courseMapper.selectList(new QueryWrapper<CourseDO>().eq("teacher_id", teacherId));
        }
        if(CollectionUtil.isEmpty(courseDOS)){
            return 0;
        }
        List<Integer> courIdS=courseDOS.stream().map(CourseDO::getId).toList();
        if(CollectionUtil.isEmpty(courIdS)){
            return 0;
        }
        List<CourInfDO> courInfDOS=courInfMapper.selectList(new QueryWrapper<CourInfDO>().in("course_id",courIdS));
        List<Integer> courInfoIdS=courInfDOS.stream().map(CourInfDO::getId).toList();
        if(CollectionUtil.isEmpty(courIdS)){
            return 0;
        }
        List<EvaTaskDO> evaTaskDOS=evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().in("cour_inf_id",courInfoIdS));
        List<Integer> taskIds=evaTaskDOS.stream().map(EvaTaskDO::getId).toList();

        if(CollectionUtil.isEmpty(taskIds)){
            return 0;
        }

        List<FormRecordDO> formRecordDOS=formRecordMapper.selectList(new QueryWrapper<FormRecordDO>().in("task_id",taskIds));
        if(CollectionUtil.isEmpty(formRecordDOS)){
            return 0;
        }

        List<Integer> recordIds=formRecordDOS.stream().map(FormRecordDO::getId).toList();
        if(CollectionUtil.isEmpty(recordIds)){
            return 0;
        }
        return recordIds.size();
    }

    private Integer getEvaNumByTeacherIdAndLocalTime(Integer teacherId,Integer num1,Integer num2){
        List<EvaTaskDO> getCached=localCacheManager.getCache(evaCacheConstants.TASK_LIST_BY_TEACH,sysUserMapper.selectById(teacherId).getName());
        if(CollectionUtil.isEmpty(getCached)) {
            List<EvaTaskDO> evaTaskDOS = evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().eq("teacher_id", teacherId));
            localCacheManager.putCache(evaCacheConstants.TASK_LIST_BY_TEACH,sysUserMapper.selectById(teacherId).getName(),evaTaskDOS);
            getCached=localCacheManager.getCache(evaCacheConstants.TASK_LIST_BY_TEACH,sysUserMapper.selectById(teacherId).getName());
        }
        List<Integer> taskIds=getCached.stream().map(EvaTaskDO::getId).toList();
        if(CollectionUtil.isEmpty(taskIds)){
            return 0;
        }
        if(num1<num2){
            throw new SysException("你的输入数字num有问题");
        }
        LocalDateTime now=LocalDateTime.now();
        LocalDateTime time=LocalDateTime.of(now.getYear(),now.getMonthValue(),now.getDayOfMonth(),0,0);
        List<FormRecordDO> formRecordDOS;
        if(CollectionUtil.isNotEmpty(taskIds)){
            formRecordDOS=formRecordMapper.selectList(new QueryWrapper<FormRecordDO>().in("task_id",taskIds).between("create_time",time.minusDays(num1),time.minusDays(num2)));
        }else {
            formRecordDOS=null;
        }
        Integer n=0;
        if(formRecordDOS==null){
            n=0;
        }else {
            n=formRecordDOS.size();
        }
        return n;
    }

    private Integer getEvaEdNumByTeacherIdAndLocalTime(Integer teacherId,Integer num1,Integer num2){
        if(num1<num2){
            throw new SysException("你的输入数字num有问题");
        }

        List<CourseDO> courseDOS=courseMapper.selectList(new QueryWrapper<CourseDO>().eq("teacher_id",teacherId));
        List<Integer> courIdS=courseDOS.stream().map(CourseDO::getId).toList();

        if(CollectionUtil.isEmpty(courIdS)){
            return 0;
        }else {
            List<CourInfDO> courInfDOS = courInfMapper.selectList(new QueryWrapper<CourInfDO>().in("course_id", courIdS));
            List<Integer> courInfoIdS = courInfDOS.stream().map(CourInfDO::getId).toList();
            if(CollectionUtil.isEmpty(courInfoIdS)){
                return 0;
            }
            List<EvaTaskDO> evaTaskDOS = evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().in("cour_inf_id", courInfoIdS));
            List<Integer> taskIds = evaTaskDOS.stream().map(EvaTaskDO::getId).toList();
            if(CollectionUtil.isEmpty(taskIds)){
                return 0;
            }
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime time = LocalDateTime.of(now.getYear(), now.getMonthValue(), now.getDayOfMonth(), 0, 0);

            List<FormRecordDO> formRecordDOS;
            if (CollectionUtil.isNotEmpty(taskIds)) {
                formRecordDOS = formRecordMapper.selectList(new QueryWrapper<FormRecordDO>().in("task_id", taskIds).between("create_time", time.minusDays(num1), time.minusDays(num2)));
            } else {
                formRecordDOS = null;
            }
            Integer n = 0;
            if (formRecordDOS == null) {
                n = 0;
            } else {
                n = formRecordDOS.size();
            }
            return n;
        }
    }
}
