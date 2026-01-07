package edu.cuit.infra.aop;

import edu.cuit.client.dto.cmd.course.AlignTeacherCmd;
import edu.cuit.infra.dal.database.dataobject.course.CourInfDO;
import edu.cuit.infra.dal.database.dataobject.course.CourseDO;
import edu.cuit.infra.dal.database.mapper.course.CourInfMapper;
import edu.cuit.infra.dal.database.mapper.course.CourseMapper;
import edu.cuit.zhuyimeng.framework.common.exception.UpdateException;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Component
@Aspect
@RequiredArgsConstructor
public class JudgeSemester {
    private final CourInfMapper courInfMapper;
    private final CourseMapper courseMapper;

    @Pointcut("@annotation(edu.cuit.infra.aop.annotation.JudgeClassForSemester)")
    public void pt(){}
    @Around("pt()")
    public Object add(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        Integer semId= (Integer) args[0];
        AlignTeacherCmd alignTeacherCmd= (AlignTeacherCmd) args[1];
        CourInfDO courInfDO = courInfMapper.selectById(alignTeacherCmd.getId());
        CourseDO courseDO = courseMapper.selectById(courInfDO.getCourseId());
        if(courseDO.getSemesterId()!=semId){
            throw new UpdateException("该课程不属于该学期，请核对学期");
        }


        return joinPoint.proceed();
    }

}
