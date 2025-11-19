package edu.cuit.adapter.controller.eva.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.cuit.client.dto.cmd.eva.NewEvaLogCmd;
import edu.cuit.zhuyimeng.framework.common.exception.UpdateException;

import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringBecomeCmd {
    public static NewEvaLogCmd stringBecomeCmd(String props) throws JsonProcessingException {
        // 手动将 JSON 字符串转换为 NewEvalogCmd 对象
        //ObjectMapper objectMapper = new ObjectMapper();

        //NewEvaLogCmd newEvalogCmd = objectMapper.readValue(props, NewEvaLogCmd.class);
        // 处理业务逻辑
        //return newEvalogCmd;

        if (containsEmptyScore(props)) {
            throw new UpdateException("每个指标score都不能为空");
        }

        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(props, NewEvaLogCmd.class);
    }
    /**
     * 检查所有 score 字段后面是否有数字
     */
    private static boolean containsEmptyScore(String json) {
        // 模式1: "score":}  (直接跟闭括号)
        if (json.matches(".*\"score\"\\s*:\\s*\\}.*")) {
            return true;
        }

        // 模式2: "score":,  (直接跟逗号)
        if (json.matches(".*\"score\"\\s*:\\s*,.*")) {
            return true;
        }

        // 模式3: "score":]  (直接跟数组结束)
        if (json.matches(".*\"score\"\\s*:\\s*\\].*")) {
            return true;
        }

        // 模式4: "score":后面只有空格然后遇到结构字符
        Pattern pattern = Pattern.compile("\"score\"\\s*:\\s*([^\\d\\-][^},]*?)(,|}|])");
        Matcher matcher = pattern.matcher(json);

        while (matcher.find()) {
            String value = matcher.group(1).trim();
            if (value.isEmpty() || value.equals("null")) {
                return true;
            }
        }

        return false;
    }
}
