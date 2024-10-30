package edu.cuit.app.service.operate.course;

import edu.cuit.client.api.IMsgService;
import edu.cuit.client.bo.MessageBO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class MsgResult {
    public final IMsgService msgService;
   public void toSendMsg(Map<String, List<Integer>> map,Integer userId){
       for (Map.Entry<String, List<Integer>> entry : map.entrySet()) {
           for (Integer i : entry.getValue()) {
               msgService.sendMessage(new MessageBO()
                       .setMsg(entry.getKey())
                       .setType(1)
                       .setMode(0)
                       .setSenderId(userId)
                       .setRecipientId(i)
                       .setIsShowName(1));
           }
       }
   }
}
