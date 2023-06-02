package com.dromara.flow.ui.domain.bo;

import lombok.Data;

import java.util.List;

/**
 * 自定义group查询条件
 * Author: 土豆仙
 */
@Data
public class GroupQueryBo {


    //约定  解析为   区域code/部门code/角色code
  /*  protected String id;
    protected List<String> ids;*/

    private boolean isReturnList = true;

    // private DdrCodeDTO ddrCodeDTO;

    private List<GroupCodeBo> groupCodeBos;

    //约定  解析为   区域name/部门name/角色name
   /* protected String name;
    protected String nameLike;
    protected String nameLikeIgnoreCase;*/

    private GroupNameBo groupNameBo;

    /*
    protected String type;
    protected String userId;
    protected List<String> userIds;*/

    protected List<String> userNames;
}
