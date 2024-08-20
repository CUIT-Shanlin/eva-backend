package edu.cuit.client.dto.data.role;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * menuIdList
 */
@Data
@Accessors(chain = true)
public class AssignRights {
    //角色id
    private Integer roleId;
    //菜单id的数组
    private List<Integer> menuIdList;
}
