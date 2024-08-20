package edu.cuit.client.dto.data.role;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 角色分配模型
 */
@Data
@Accessors(chain = true)
public class AssignRole {
    //用户ID
    private Integer userId;
    //角色ID数组
    private List<Integer> roleIdList;
}
