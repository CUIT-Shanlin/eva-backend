package edu.cuit.infra.aop;

import edu.cuit.client.dto.cmd.course.AlignTeacherCmd;
import edu.cuit.infra.dal.database.dataobject.course.CourInfDO;
import edu.cuit.infra.dal.database.dataobject.course.CourseDO;
import edu.cuit.infra.dal.database.dataobject.course.SemesterDO;
import edu.cuit.infra.dal.database.mapper.course.CourInfMapper;
import edu.cuit.infra.dal.database.mapper.course.CourseMapper;
import edu.cuit.infra.dal.database.mapper.course.SemesterMapper;
import edu.cuit.zhuyimeng.framework.common.exception.UpdateException;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

@Component
@Aspect
@RequiredArgsConstructor
public class JudgeSemester {
    private final CourInfMapper courInfMapper;
    private final CourseMapper courseMapper;
    private final SemesterMapper semesterMapper;

    @Pointcut("@annotation(edu.cuit.infra.aop.annotation.JudgeClassForSemester)")
    public void pt(){}
    @Around("pt()")
    public Object add(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        Integer semId= (Integer) args[0];
        AlignTeacherCmd alignTeacherCmd= (AlignTeacherCmd) args[1];
        CourInfDO courInfDO = courInfMapper.selectById(alignTeacherCmd.getId());
        CourseDO courseDO = courseMapper.selectById(courInfDO.getCourseId());
        if(!Objects.equals(courseDO.getSemesterId(), semId)){
            throw new UpdateException("该课程不属于该学期，请核对学期");
        }
       /* SemesterDO semesterDO = semesterMapper.selectById(semId);
        LocalDate now = LocalDate.now();
        int week=calculateWeekNumber(semesterDO.getStartDate(),now);
        int day=calculateDayOfWeek(semesterDO.getStartDate(),now);
        if(week>courInfDO.getWeek()){
            throw new UpdateException("该课程已经结束");
        }else if(week==courInfDO.getWeek()&&day>courInfDO.getDay()){
            throw new UpdateException("该课程已经结束");
        }
        LocalDateTime localDateTime = LocalDateTime.now();
        judgeTime(localDateTime.getHour(),localDateTime.getMinute(),courInfDO.getStartTime());
*/

        return joinPoint.proceed();
    }

    /**
     * 函数1：计算目标日期相对于基准周一的「第几周」
     * @param baseMonday 基准周一
     * @param targetDate 目标日期（如当前日期）
     * @return 周数（基准当天=1，之后每7天+1；之前为负数，如基准前3天=-1周）
     */
    private int calculateWeekNumber(LocalDate baseMonday, LocalDate targetDate) {
        // 计算基准到目标日期的间隔天数
        long daysBetween = ChronoUnit.DAYS.between(baseMonday, targetDate);

        // 计算周数：基准当天（0天）=1周，7天=2周，-1天=-1周，-7天=-1周，-8天=-2周
        if (daysBetween < 0) {
            long absDays = Math.abs(daysBetween);
            return -(int) (absDays / 7 + (absDays % 7 == 0 ? 0 : 1));
        } else {
            return (int) (daysBetween / 7) + 1;
        }
    }


    /**
     * 函数2：计算目标日期相对于基准周一（2025-09-08）的「周几」
     * @param baseMonday 固定基准：2025-09-08（周一）
     * @param targetDate 目标日期
     * @return 周几的中文简称（一/二/.../日）
     */
    private int calculateDayOfWeek(LocalDate baseMonday, LocalDate targetDate) {
        // 计算间隔天数
        long daysBetween = ChronoUnit.DAYS.between(baseMonday, targetDate);
        int dayOfWeekNum;

        // 计算周几数字（1=周一，7=周日）
        if (daysBetween < 0) {
            dayOfWeekNum = 7 - (int) (Math.abs(daysBetween) % 7);
        } else {
            dayOfWeekNum = (int) (daysBetween % 7) + 1;
        }

        // 转换为中文简称
        return dayOfWeekNum;
    }

    private void judgeTime(int h,int m,int startTime){
        int sH,sM;
        if(startTime==1){
            sH=8;
            sM=0;
        } else if (startTime==3) {
            sH=10;
            sM=20;
        }else if(startTime==5){
            sH=14;
            sM=0;
        } else if (startTime==7) {
            sH=15;
            sM=50;
        }else{
            sH=19;
            sM=30;
        }
        if(h>sH){
            throw new UpdateException("该课程已经不可选了");
        } else if (h==sH&&m>sM) {

            throw new UpdateException("该课程已经开始了");
        }
    }


}
