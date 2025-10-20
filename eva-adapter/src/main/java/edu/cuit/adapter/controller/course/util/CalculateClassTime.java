package edu.cuit.adapter.controller.course.util;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


public class CalculateClassTime {
    // 定义常量
    private static final int EVENING_START_HOUR = 19;
    private static final int EVENING_START_MINUTE = 30;
    private static final int CLASS_DURATION = 45;
    private static final int CLASS_INTERVAL = 10;
    private static final int EVENING_CLASS_DURATION = 155; // 晚上课程时长（分钟）
    private static final int NORMAL_CLASS_DURATION = 100; // 普通课程时长（分钟）
    public static LocalDateTime calculateClassTime(LocalDateTime baseTime, int classNumber) {
        // 判断上午、下午或晚上的起始时间
        LocalDateTime startTime;
        long totalOffset;
        if (classNumber <= 4) {
            // 上午第一节课从8点开始
            startTime = baseTime.withHour(8).withMinute(20).withSecond(0);
            if(classNumber>=3){
                totalOffset = (CLASS_DURATION + CLASS_INTERVAL) * (classNumber - 1)+CLASS_INTERVAL;
            }else{
                totalOffset = (CLASS_DURATION + CLASS_INTERVAL) * (classNumber - 1);
            }
        } else if (classNumber <= 9) {
            // 下午第一节课从2点开始
            startTime = baseTime.withHour(14).withMinute(0).withSecond(0);
            // 调整课节号，使其从1开始计算
            classNumber -= 4;
            totalOffset = (CLASS_DURATION + CLASS_INTERVAL) * (classNumber - 1);
        } else {
            // 晚上第一节课从7:30开始
            startTime = baseTime.withHour(19).withMinute(30).withSecond(0);
            // 调整课节号，使其从1开始计算
            classNumber -= 9;
            // 计算总时间偏移量
            totalOffset = (CLASS_DURATION + CLASS_INTERVAL) * (classNumber - 1);
        }

        // 计算具体上课时间
        return startTime.plusMinutes(totalOffset);
    }

    public static List<LocalDateTime> calculateStartAndEndTime(LocalDateTime localDateTime, Integer startTime) {
        LocalDateTime classStartTime = calculateClassTime(localDateTime, startTime);
        LocalDateTime classEndTime;
        //如果这个classStartTime是在19.30开始的
        if(classStartTime.getHour()==EVENING_START_HOUR&&classStartTime.getMinute()==EVENING_START_MINUTE){
           //classStartTime就等于在classStartTime的基础上加上155分钟
            classEndTime = classStartTime.plusMinutes(EVENING_CLASS_DURATION);
        }else{
            classEndTime = classStartTime.plusMinutes(NORMAL_CLASS_DURATION);
        }
        List<LocalDateTime> list=new ArrayList<>();
        list.add(classStartTime);
        list.add(classEndTime);
        return list;
    }
}
