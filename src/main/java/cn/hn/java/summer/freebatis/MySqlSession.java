package cn.hn.java.summer.freebatis;

import cn.hn.java.summer.freebatis.statement.*;
import org.apache.ibatis.exceptions.ExceptionFactory;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.defaults.DefaultSqlSession;
import org.apache.ibatis.transaction.Transaction;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.managed.ManagedTransactionFactory;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.List;

/**
 * Created by xw2sy on 2017-06-09.
 */
@SuppressWarnings("ALL")
public class MySqlSession extends DefaultSqlSession {
    private Executor executor;

    public MySqlSession(Configuration configuration, Executor executor, boolean autoCommit) {
        super(configuration, executor, autoCommit);
        this.executor=executor;
    }

    public MySqlSession(Configuration configuration, Executor executor) {
        super(configuration, executor);
        this.executor=executor;
    }

    @Override
    public int insert(String statement, Object parameter) {
        //获取参数中设置的返回类型和真实参数
        //通过封装区别于其它类型的参数
        IStatementInfo statementInfo=null;
        if(parameter instanceof IStatementInfo){
            statementInfo= (IStatementInfo)parameter;
        }
        if(statementInfo!=null){
            if(statementInfo.getStatementType()==StatementType.SUMMER_BATCH){
                //批量操作参数
                BatchStatmentInfo batchStatmentInfo=(BatchStatmentInfo)parameter;
                List parList=batchStatmentInfo.getParameter();

                int result = 1;
                //批量操作的，需要重新生成executor
                final Environment environment = getConfiguration().getEnvironment();
                final TransactionFactory transactionFactory = getTransactionFactoryFromEnvironment(environment);
                Transaction tx = transactionFactory.newTransaction(environment.getDataSource(), null, false);
                final Executor executor = getConfiguration().newExecutor(tx, ExecutorType.BATCH);
                try {
                    MappedStatement ms = getConfiguration().getMappedStatement(statement);
                    int batchCount = batchStatmentInfo.getBatchSize();// 每批commit的个数
                    int batchLastIndex = batchCount;// 每批最后一个的下标
                    for (int index = 0; index < parList.size();) {
                        if (batchLastIndex >= parList.size()) {
                            batchLastIndex = parList.size();
                            result = result *executor.update(ms, wrapCollection(parList.subList(index,batchLastIndex)));
                            commit(executor,true);
                            break;// 数据插入完毕，退出循环
                        } else {
                            result = result *executor.update(ms, wrapCollection(parList.subList(index,batchLastIndex)));
                            commit(executor,true);
                            index = batchLastIndex;// 设置下一批下标
                            batchLastIndex = index + (batchCount - 1);
                        }
                    }
                } catch (Exception e) {
                    throw ExceptionFactory.wrapException("Error updating database.  Cause: " + e, e);
                } finally {
                    ErrorContext.instance().reset();
                    commit(executor,true);
                }
                return result;
            }
        }else {
            //其它
            return super.insert(statement, parameter);
        }
        return 0;
    }

    @SuppressWarnings("Since15")
    public <E> List<E> selectList(String statement, Object parameter, RowBounds rowBounds) {
        try {
            MappedStatement ms =null;
            if(statement!=null && !statement.startsWith("autoMapping-")){
                ms = getConfiguration().getMappedStatement(statement);
            }

            MappedStatement mappedStatement=ms;
            //获取参数中设置的返回类型和真实参数
            //通过封装区别于其它类型的参数
            StatementInfo statementInfo=null;
            if(parameter instanceof StatementInfo){
                statementInfo= (StatementInfo)parameter;
            }
            if(statementInfo!=null){
                //自动映射表和实体类的
                if(statementInfo.getStatementType()== StatementType.SUMMER_AUTOMAP){
                    Class resultType=statementInfo.getResultType();
                    Assert.notNull(resultType,"resultType is null!!!!!");

                    parameter=statementInfo.getParameter();
                    //使用原有的MappedStatement创建新的
                    if (ms != null) {
                        mappedStatement = MappedStatementBuilder.build(ms, resultType);
                    } else {
                        //未取得MappedStatement，创建新的MappedStatement
                        mappedStatement = MappedStatementBuilder.getSelectMappedStatement(getConfiguration(), parameter, resultType);
                    }
                }else if(statementInfo.getStatementType()==StatementType.SUMMER_BATCH){
                    //批量操作的，需要重新生成executor
                    final Environment environment = getConfiguration().getEnvironment();
                    final TransactionFactory transactionFactory = getTransactionFactoryFromEnvironment(environment);
                    Transaction tx = transactionFactory.newTransaction(environment.getDataSource(), null, false);
                    final Executor executor = getConfiguration().newExecutor(tx, ExecutorType.BATCH);
                    return executor.query(mappedStatement, wrapCollection(parameter), rowBounds, Executor.NO_RESULT_HANDLER);
                }
            }

            return executor.query(mappedStatement, wrapCollection(parameter), rowBounds, Executor.NO_RESULT_HANDLER);
        } catch (Exception e) {
            throw ExceptionFactory.wrapException("Error querying database.  Cause: " + e, e);
        } finally {
            ErrorContext.instance().reset();
        }
    }

    private Object wrapCollection(final Object object) {
        if (object instanceof Collection) {
            StrictMap<Object> map = new StrictMap<Object>();
            map.put("collection", object);
            if (object instanceof List) {
                map.put("list", object);
            }
            return map;
        } else if (object != null && object.getClass().isArray()) {
            StrictMap<Object> map = new StrictMap<Object>();
            map.put("array", object);
            return map;
        }
        return object;
    }

    private TransactionFactory getTransactionFactoryFromEnvironment(Environment environment) {
        if (environment == null || environment.getTransactionFactory() == null) {
            return new ManagedTransactionFactory();
        }
        return environment.getTransactionFactory();
    }

    public void commit(Executor executor, boolean force) {
        try {
            executor.commit(force);
        } catch (Exception e) {
            throw ExceptionFactory.wrapException("Error committing transaction.  Cause: " + e, e);
        } finally {
            ErrorContext.instance().reset();
        }
    }
}
