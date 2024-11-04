package edu.cuit.infra.enums.cache;

import org.springframework.stereotype.Component;
/**
 * 评教缓存键常量
 */
@Component("EvaCacheConstants")
public class EvaCacheConstants {
    //任务相关
    //单个任务
    public final String ONE_TASK = "task.one.";
    //通过学期id获得任务数据
    public final String TASK_LIST_BY_SEM = "course.list.sem.";



    //记录相关
    //单个记录
    public final String ONE_LOG = "log.one.";
    //多个记录
    public final String LOG_LIST = "log.list";
    //统计相关



    //模板相关
    //单个模板
    public final String ONE_TEMPLATE = "template.one.";
    //所有模板
    public final String TEMPLATE_LIST = "template.list";


    //快照模板
    public final String COUR_TEMPLATE = "courOneEvaTemplate";
}
