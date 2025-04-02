package edu.cuit.adapter.controller.eva.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.cuit.client.dto.cmd.eva.NewEvaLogCmd;

import java.time.LocalDateTime;

public class StringBecomeCmd {
    public static NewEvaLogCmd stringBecomeCmd(String props) throws JsonProcessingException {
        // 手动将 JSON 字符串转换为 NewEvalogCmd 对象
        ObjectMapper objectMapper = new ObjectMapper();
        NewEvaLogCmd newEvalogCmd = objectMapper.readValue(props, NewEvaLogCmd.class);
        // 处理业务逻辑
        System.out.println("解析后的对象: " + newEvalogCmd);
        return newEvalogCmd;
    }
}
