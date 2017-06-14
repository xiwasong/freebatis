package cn.hn.java.summer.freebatis.statement;

import java.util.List;

/**
 * Created by xw2sy on 2017-06-14.
 * 批量操作
 */
public class BatchStatmentInfo extends BaseStatementInfo<List>{

    //批量大小
    private int batchSize=200;

    public BatchStatmentInfo(List parameter) {
        super(StatementType.SUMMER_BATCH, parameter);
    }

    public BatchStatmentInfo(int batchSize, List parameter) {
        super(StatementType.SUMMER_BATCH, parameter);
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }
}
