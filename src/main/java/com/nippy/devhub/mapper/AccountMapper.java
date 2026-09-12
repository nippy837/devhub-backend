package com.nippy.devhub.mapper;

import com.nippy.devhub.vo.AccountVO;
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
}
