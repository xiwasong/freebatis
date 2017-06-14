package cn.hn.java.summer.freebatis.statement;

/**
 * Created by xw2sy on 2017-06-14.
 */
public enum StatementType {
    NONE,
    //mybatis的操作
    MYBATIS,
    //summer自动映射的操作
    SUMMER_AUTOMAP,
    //summer的批量操作
    SUMMER_BATCH;
}
