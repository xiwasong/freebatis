package cn.hn.java.summer.freebatis.statement;

/**
 * Created by xw2sy on 2017-06-14.
 */
public interface IStatementInfo<T> {

    StatementType getStatementType();

    T getParameter();
}
