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
   public void toSendMsg(Map<String, Map<Integer,Integer>> map,Integer userId){
       for (Map.Entry<String, Map<Integer, Integer>> entry : map.entrySet()) {
           for (Map.Entry<Integer, Integer> integerIntegerEntry : entry.getValue().entrySet()) {
               msgService.sendMessage(new MessageBO()
                       .setTaskId(integerIntegerEntry.getKey())
                       .setMsg(entry.getKey())
                       .setType(1)
                       .setMode(1)
                       .setSenderId(userId)
                       .setRecipientId(integerIntegerEntry.getValue())
                       .setIsShowName(1));
           }
          /* for (Integer i : entry.getValue()) {
               msgService.sendMessage(new MessageBO()
                       .setMsg(entry.getKey())
                       .setType(1)
                       .setMode(0)
                       .setSenderId(userId)
                       .setRecipientId(i)
                       .setIsShowName(1));
           }*/
       }
   }
    public void SendMsgToAll(Map<String, Map<Integer,Integer>> map,Integer userId){
        for (Map.Entry<String, Map<Integer, Integer>> entry : map.entrySet()) {
                msgService.sendMessage(new MessageBO()
                        .setMsg(entry.getKey())
                        .setType(1)
                        .setMode(0)
                        .setSenderId(userId)
                        .setRecipientId(null)
                        .setIsShowName(1));
        }
    }
}
