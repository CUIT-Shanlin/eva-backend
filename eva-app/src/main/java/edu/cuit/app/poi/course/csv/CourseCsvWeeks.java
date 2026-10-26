package edu.cuit.app.poi.course.csv;

import cn.hutool.core.util.StrUtil;
import com.alibaba.cola.exception.BizException;

import java.util.ArrayList;
import java.util.List;

final class CourseCsvWeeks {

    private CourseCsvWeeks() {
    }

    static List<Integer> resolve(String weeksText, int lineNumber) {
        if (StrUtil.isBlank(weeksText)) {
            throw new BizException("第" + lineNumber + "行的weeks不能为空");
        }

        try {
            String normalized = weeksText.trim();
            if (normalized.endsWith("周")) {
                normalized = normalized.substring(0, normalized.length() - 1);
            }

            List<Integer> results = new ArrayList<>();
            for (String block : normalized.split("[,，]")) {
                String item = block.trim();
                if (item.isEmpty()) {
                    continue;
                }

                boolean oddOnly = item.endsWith("单");
                boolean evenOnly = item.endsWith("双");
                if (oddOnly || evenOnly) {
                    item = item.substring(0, item.length() - 1).trim();
                }
                if (item.isEmpty()) {
                    throw new IllegalArgumentException("weeks format is invalid");
                }

                if (!item.contains("-")) {
                    results.add(Integer.parseInt(item));
                    continue;
                }

                String[] range = item.split("-", 2);
                if (range.length != 2 || range[0].trim().isEmpty() || range[1].trim().isEmpty()) {
                    throw new IllegalArgumentException("weeks format is invalid");
                }

                int start = Integer.parseInt(range[0].trim());
                int end = Integer.parseInt(range[1].trim());
                if (end < start) {
                    throw new IllegalArgumentException("weeks format is invalid");
                }
                for (int week = start; week <= end; week++) {
                    if (oddOnly && week % 2 == 0) {
                        continue;
                    }
                    if (evenOnly && week % 2 != 0) {
                        continue;
                    }
                    results.add(week);
                }
            }
            if (results.isEmpty()) {
                throw new IllegalArgumentException("weeks format is invalid");
            }
            return results;
        } catch (RuntimeException e) {
            throw new BizException("第" + lineNumber + "行的weeks格式不正确");
        }
    }
}
