package edu.cuit.adapter.controller.course.util;

import java.time.LocalDateTime;

public class CalculateClassTime {
    public static LocalDateTime calculateClassTime(LocalDateTime baseTime, int classNumber) {
        // 每节课的时间（45分钟）
        long classDuration = 45;
        // 课间休息时间（10分钟）
        long breakDuration = 10;

        // 判断上午、下午或晚上的起始时间
        LocalDateTime startTime;
        long totalOffset;
        if (classNumber <= 4) {
            // 上午第一节课从8点开始
            startTime = baseTime.withHour(8).withMinute(20).withSecond(0);
            if(classNumber>=3){
                totalOffset = (classDuration + breakDuration) * (classNumber - 1)+breakDuration;
            }else{
                totalOffset = (classDuration + breakDuration) * (classNumber - 1);
            }
        } else if (classNumber <= 9) {
            // 下午第一节课从2点开始
            startTime = baseTime.withHour(14).withMinute(0).withSecond(0);
            // 调整课节号，使其从1开始计算
            classNumber -= 4;
           /* if(classNumber>=3){
                totalOffset = (classDuration + breakDuration) * (classNumber - 1)+breakDuration;
            }else{
                totalOffset = (classDuration + breakDuration) * (classNumber - 1);
            }*/
            totalOffset = (classDuration + breakDuration) * (classNumber - 1);
        } else {
            // 晚上第一节课从7:30开始
            startTime = baseTime.withHour(19).withMinute(30).withSecond(0);
            // 调整课节号，使其从1开始计算
            classNumber -= 9;
            // 计算总时间偏移量
            totalOffset = (classDuration + breakDuration) * (classNumber - 1);
        }

        // 计算具体上课时间
        return startTime.plusMinutes(totalOffset);
    }
}
