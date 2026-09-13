package com.nippy.devhub.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// 普通 Java 类：Lombok 生成 getter/setter、无参构造和全参构造。
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountPageVO {
    // 当前页的账号列表。
    private List<AccountVO> records;

    // 符合搜索和环境筛选条件的总条数，不是当前页条数。
    private long total;

    // 实际返回的页码，从 1 开始。
    private int page;

    // 每页最多返回的条数：5、10 或 50。
    private int pageSize;

    // 全部账号的统计信息，不随搜索、筛选或翻页变化。
    private AccountStatsVO stats;

    // 全部账号使用的环境，供前端下拉框展示。
    private List<String> environments;
}
