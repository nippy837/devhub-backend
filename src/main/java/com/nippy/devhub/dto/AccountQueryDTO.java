package com.nippy.devhub.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountQueryDTO {
    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码必须大于0")
    private Integer page = 1;

    @NotNull(message = "每页条数不能为空")
    private Integer pageSize = 10;

    @Size(max = 500, message = "搜索关键词不能超过500个字符")
    private String keyword = "";

    @Size(max = 100, message = "环境名称不能超过100个字符")
    private String environment = "";

    @AssertTrue(message = "每页条数只能是5、10或50")
    public boolean isPageSizeAllowed() {
        return pageSize == null || pageSize == 5 || pageSize == 10 || pageSize == 50;
    }
}
