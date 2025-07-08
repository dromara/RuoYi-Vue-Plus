package org.dromara.system.domain.bo;


import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.system.domain.SysUser;
import org.dromara.system.domain.SysUsrPerm;

@Data
@AutoMapper(target = SysUsrPerm.class, reverseConvertGenerate = false)
public class SysUsrPermBo {
    private String userName;
    private String userPerm;
}
