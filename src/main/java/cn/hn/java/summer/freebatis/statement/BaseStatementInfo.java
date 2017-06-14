package cn.hn.java.summer.freebatis.statement;

/**
 * Created by xw2sy on 2017-06-12.
 */
public class BaseStatementInfo<T> implements IStatementInfo<T> {

    //操作语句类型
    protected StatementType statementType=StatementType.NONE;

    //参数
    protected T parameter;

    public BaseStatementInfo(StatementType statementType, T parameter) {
        this.statementType = statementType;
        this.parameter = parameter;
    }

    public StatementType getStatementType() {
        return statementType;
    }

    public void setStatementType(StatementType statementType) {
        this.statementType = statementType;
    }


    public T getParameter() {
        return parameter;
    }

    public void setParameter(T parameter) {
        this.parameter = parameter;
    }
}
