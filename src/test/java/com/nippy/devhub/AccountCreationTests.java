package com.nippy.devhub;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional // 测试结束自动回滚，不保留测试账号。
class AccountCreationTests {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbc;

    private Map<String, String> validRequest() {
        return new HashMap<>(Map.of(
                "systemName", " 新增接口测试 ",
                "environment", "test",
                "username", " sample_'_user ",
                "password", " fictional password ",
                "loginUrl", " https://example.com/login ",
                "remark", "事务回滚测试"));
    }

    @Test
    void savesAccountAndReturnsItInList() throws Exception {
        String response = mvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(response).get("data").asLong();
        Map<String, Object> row = jdbc.queryForMap("SELECT * FROM accounts WHERE id = ?", id);
        assertThat(row.get("system_name")).isEqualTo("新增接口测试");
        assertThat(row.get("username")).isEqualTo("sample_'_user");
        assertThat(row.get("password")).isEqualTo(" fictional password ");
        assertThat(row.get("login_url")).isEqualTo("https://example.com/login");
        assertThat(row.get("created_time")).isNotNull();
        assertThat(row.get("updated_time")).isNotNull();

        String list = mvc.perform(get("/api/accounts")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode first = objectMapper.readTree(list).get("data").get(0);
        assertThat(first.get("id").asLong()).isEqualTo(id);
        assertThat(first.get("createTime").isNull()).isFalse();
    }

    @Test
    void allowsOptionalFieldsToBeOmitted() throws Exception {
        mvc.perform(post("/api/accounts").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"systemName":"最小账号测试","environment":"dev","username":"sample"}
                                """))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void rejectsInvalidFieldsWithoutInsertingRows() throws Exception {
        Long before = jdbc.queryForObject("SELECT COUNT(*) FROM accounts", Long.class);
        Map<String, String> invalidValues = Map.of(
                "systemName", "   ", "username", "x".repeat(101),
                "environment", "unknown", "loginUrl", "javascript:alert(1)",
                "remark", "x".repeat(501), "password", "x".repeat(4097));
        for (Map.Entry<String, String> invalid : invalidValues.entrySet()) {
            Map<String, String> body = validRequest();
            body.put(invalid.getKey(), invalid.getValue());
            mvc.perform(post("/api/accounts").contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM accounts", Long.class)).isEqualTo(before);
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        mvc.perform(post("/api/accounts").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }
}
