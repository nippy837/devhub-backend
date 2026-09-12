package com.nippy.devhub.mapper;

import com.nippy.devhub.vo.AccountVO;
import com.nippy.devhub.entity.Account;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AccountMapper {

    // 时间列通过别名配合驼峰映射，分别对应 createTime、updateTime。
    @Select("""
            SELECT id, system_name, environment, username, password, login_url, remark,
                   created_time AS create_time, updated_time AS update_time
            FROM accounts
            ORDER BY id DESC
            """)
    List<AccountVO> selectAll();

    @Insert("""
            INSERT INTO accounts (system_name, environment, username, password, login_url, remark)
            VALUES (#{systemName}, #{environment}, #{username}, #{password}, #{loginUrl}, #{remark})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Account account);
}
