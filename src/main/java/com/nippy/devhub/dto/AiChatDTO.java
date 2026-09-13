package com.nippy.devhub.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;

@Data
public class AiChatDTO {
    @NotEmpty @Size(max = 21, message = "对话过长，请新建对话")
    private List<@NotNull @Valid Message> messages;

    @Data
    public static class Message {
        @NotNull @Pattern(regexp = "user|assistant", message = "消息角色无效")
        private String role;
        @NotBlank @Size(max = 12000, message = "单条消息不能超过 12000 字")
        private String content;
    }
}
