package edu.cuit.app.service.impl.eva;
import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.cola.exception.SysException;
import edu.cuit.app.aop.CheckSemId;
import edu.cuit.app.convertor.eva.EvaRecordBizConvertor;
import edu.cuit.client.api.eva.IUserEvaService;
import edu.cuit.client.dto.clientobject.eva.EvaRecordCO;
import edu.cuit.domain.entity.course.CourseEntity;
import edu.cuit.domain.entity.course.SingleCourseEntity;
import edu.cuit.domain.entity.eva.EvaRecordEntity;
import edu.cuit.domain.gateway.course.CourseQueryGateway;
import edu.cuit.domain.gateway.eva.EvaDeleteGateway;
import edu.cuit.domain.gateway.eva.EvaQueryGateway;
import edu.cuit.domain.gateway.eva.EvaUpdateGateway;
import edu.cuit.domain.gateway.user.UserQueryGateway;
import edu.cuit.zhuyimeng.framework.common.exception.QueryException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserEvaServiceImpl implements IUserEvaService {private final EvaDeleteGateway evaDeleteGateway;
    private final EvaUpdateGateway evaUpdateGateway;
    private final EvaQueryGateway evaQueryGateway;
    private final UserQueryGateway userQueryGateway;
    private final CourseQueryGateway courseQueryGateway;
    private final EvaRecordBizConvertor evaRecordBizConvertor;

    //怎么获取自己的 TODO
    //去评教
    @Override
    @CheckSemId
    public List<EvaRecordCO> getEvaLogInfo(Integer semId, String keyword) {
        Integer userId= (Integer) StpUtil.getLoginId();
        List<EvaRecordCO> evaRecordCOS=new ArrayList<>();
        if(userId==null){
            throw new SysException("还没有登录，怎么查到这里的");
        }
        List<EvaRecordEntity> evaRecordEntities=evaQueryGateway.getEvaLogInfo(userId,semId,keyword);
        if(evaRecordEntities.size()==0){
            throw new QueryException("并没有找到相关的评教记录");
        }
        for(int i=0;i<evaRecordEntities.size();i++){
            CourseEntity courseEntity =courseQueryGateway.getCourseByInfo
                            (evaRecordEntities.get(i).getTask().getCourInf().getId()).get();
            SingleCourseEntity singleCourseEntity=evaRecordEntities.get(i).getTask().getCourInf();
            EvaRecordCO evaRecordCO=evaRecordBizConvertor.evaRecordEntityToCo(evaRecordEntities.get(i),singleCourseEntity,courseEntity);

            evaRecordCO.setAverScore(evaQueryGateway.getScoreFromRecord(evaRecordEntities.get(i).getFormPropsValues()).get());
            evaRecordCOS.add(evaRecordCO);
        }
        return evaRecordCOS;
    }
    //被评教
    @Override
    @CheckSemId
    public List<EvaRecordCO> getEvaLoggingInfo(Integer courseId, Integer semId) {
        Integer userId= (Integer) StpUtil.getLoginId();
        if(userId==null){
            throw new SysException("还没有登录，怎么查到这里的");
        }
        List<EvaRecordEntity> evaRecordEntities=evaQueryGateway.getEvaEdLogInfo(userId,semId,courseId);
        List<EvaRecordCO> evaRecordCOS=new ArrayList<>();
        if(evaRecordEntities.size()==0){
            throw new QueryException("并没有找到相关的评教记录");
        }
        for(int i=0;i<evaRecordEntities.size();i++){
            CourseEntity courseEntity =courseQueryGateway.getCourseByInfo
                    (evaRecordEntities.get(i).getTask().getCourInf().getId()).get();
            SingleCourseEntity singleCourseEntity=evaRecordEntities.get(i).getTask().getCourInf();
            EvaRecordCO evaRecordCO=evaRecordBizConvertor.evaRecordEntityToCo(evaRecordEntities.get(i),singleCourseEntity,courseEntity);

            evaRecordCO.setAverScore(evaQueryGateway.getScoreFromRecord(evaRecordEntities.get(i).getFormPropsValues()).get());
            evaRecordCOS.add(evaRecordCO);
        }
        return evaRecordCOS;
    }
}
