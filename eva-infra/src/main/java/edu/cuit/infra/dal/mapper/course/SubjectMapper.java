package edu.cuit.infra.dal.mapper.course;

import com.github.yulichang.base.MPJBaseMapper;
import edu.cuit.infra.dal.po.course.SubjectDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* @author XiaoMo
* @description 针对表【subject(科目表)】的数据库操作Mapper
* @createDate 2024-08-07 16:48:58
* @Entity edu.cuit.infra.dal.po.course.SubjectDO
*/
@Mapper
public interface SubjectMapper extends MPJBaseMapper<SubjectDO> {

}




