package cn.hn.java.summer.freebatis.statement;

/**
 * Created by xw2sy on 2017-06-12.
 */
public class StatementInfo extends BaseStatementInfo<Object> {

    //结果类型
    private Class resultType;

    public StatementInfo(Object parameter, Class resultType) {
        super(StatementType.SUMMER_AUTOMAP, parameter);
        this.resultType = resultType;
    }

    public Class getResultType() {
        return resultType;
    }

    public void setResultType(Class resultType) {
        this.resultType = resultType;
    }
}
