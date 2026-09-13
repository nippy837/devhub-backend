package com.nippy.devhub.mapper;

import com.nippy.devhub.vo.AccountVO;
import com.nippy.devhub.vo.AccountStatsVO;
import com.nippy.devhub.entity.Account;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AccountMapper {

    // COUNT 和分页查询复用筛选条件；绑定参数防止 SQL 注入。
    // LOCATE 按普通文字搜索，用户输入的 % 和 _ 不会被当作通配符。
    String FILTER = """
            WHERE (#{environment} = '' OR environment = #{environment})
              AND (#{keyword} = ''
                OR LOCATE(LOWER(#{keyword}), LOWER(system_name)) > 0
                OR LOCATE(LOWER(#{keyword}), LOWER(username)) > 0
                OR LOCATE(LOWER(#{keyword}), LOWER(COALESCE(remark, ''))) > 0)
            """;

    @Select("""
            SELECT id, system_name, environment, username, password, login_url, remark,
                   created_time AS create_time, updated_time AS update_time
            FROM accounts
            """ + FILTER + " ORDER BY id DESC LIMIT #{pageSize} OFFSET #{offset}")
    List<AccountVO> selectPage(@Param("keyword") String keyword, @Param("environment") String environment,
                               @Param("pageSize") int pageSize, @Param("offset") long offset);

    @Select("SELECT COUNT(*) FROM accounts " + FILTER)
    long countFiltered(@Param("keyword") String keyword, @Param("environment") String environment);

    @Select("""
            SELECT COUNT(*) AS total_accounts, COUNT(DISTINCT system_name) AS system_count,
                   COALESCE(SUM(CASE WHEN environment = 'test' THEN 1 ELSE 0 END), 0) AS test_count
            FROM accounts
            """)
    AccountStatsVO selectStats();

    @Select("SELECT DISTINCT environment FROM accounts WHERE environment IS NOT NULL AND environment <> '' ORDER BY environment")
    List<String> selectEnvironments();

    @Select("SELECT id FROM accounts WHERE id = #{id} FOR UPDATE")
    Long lockById(@Param("id") Long id);

    @Update("""
            UPDATE accounts SET system_name = #{systemName}, environment = #{environment},
                   username = #{username}, password = #{password}, login_url = #{loginUrl},
                   remark = #{remark}, updated_time = CURRENT_TIMESTAMP
            WHERE id = #{id}
            """)
    int update(Account account);

    @Insert("""
            INSERT INTO accounts (system_name, environment, username, password, login_url, remark)
            VALUES (#{systemName}, #{environment}, #{username}, #{password}, #{loginUrl}, #{remark})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Account account);

    @Delete("""
            DELETE FROM accounts
            WHERE id = #{id}
            """)
    int deleteById(@Param("id") Long id);
}
