package cn.hn.java.summer.freebatis;

import cn.hn.java.summer.freebatis.mapper.SqlGenerator;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.scripting.defaults.RawSqlSource;
import org.apache.ibatis.session.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by xw2sy on 2017-06-12.
 */
public class MappedStatementBuilder {

    private static final ConcurrentHashMap<String,MappedStatement> mappedStatementMap=new ConcurrentHashMap();

    /**
     * 使用已有的MappedStatement，即有在mapper.xml中配置的
     * 来创建新的MappedStatement
     * @param fromMs 原有的MappedStatement
     * @param resultType 结果类型
     * @return 新的MappedStatement
     */
    public static MappedStatement build(MappedStatement fromMs, Class resultType){

        //使用返回类型，创建内联ResultMap
        ResultMap inlineResultMap = new ResultMap.Builder(
                fromMs.getConfiguration(),
                fromMs.getId() + "-Inline",
                resultType,
                new ArrayList(),
                null).build();
        List resultMaps=new ArrayList();
        resultMaps.add(inlineResultMap);

        //使用原有的MappedStatement，构造新的MappedStatement
        MappedStatement.Builder statementBuilder = new MappedStatement.Builder(fromMs.getConfiguration(), fromMs.getId(), fromMs.getSqlSource(), fromMs.getSqlCommandType())
                .resource(fromMs.getResource())
                .fetchSize(fromMs.getFetchSize())
                .timeout(fromMs.getTimeout())
                .statementType(fromMs.getStatementType())
                .keyGenerator(fromMs.getKeyGenerator())
                //.keyProperty(keyProperty)
                //.keyColumn(keyColumn)
                .databaseId(fromMs.getDatabaseId())
                .lang(fromMs.getLang())
                .resultOrdered(false)
                //.resultSets(resultSets)
                //hack resultMaps
                .resultMaps(resultMaps)
                .resultSetType(fromMs.getResultSetType())
                .flushCacheRequired(fromMs.isFlushCacheRequired())
                .useCache(fromMs.isUseCache())
                .cache(fromMs.getCache());

        return statementBuilder.build();
    }

    /**
     * 获取已配置的MappedStatement
     * 如果不存在则生成并返回
     * @param fromMs 原有的MappedStatement
     * @param resultType 结果类型
     * @return MappedStatement
     */
    public static MappedStatement getMappedStatement(MappedStatement fromMs, Class resultType){
        if(!mappedStatementMap.containsKey(fromMs.getId())){
            mappedStatementMap.put(fromMs.getId(),build(fromMs,resultType));
        }
        return mappedStatementMap.get(fromMs.getId());
    }

    /**
     * 获取未配置的select查询MappedStatement
     * @param configuration 全局配置
     * @param resultType 返回类型
     * @return MappedStatement
     */
    public static MappedStatement getSelectMappedStatement(Configuration configuration,Object param, Class resultType){
        //使用操作类型和返回结果类型作为id
        String sqlId="select-"+resultType.getCanonicalName()+ "-Inline";
        MappedStatement mappedStatement=null;
        try {
            mappedStatement = configuration.getMappedStatement(sqlId, false);
        }catch (IllegalArgumentException e){
            //ignore
        }
        if(mappedStatement!=null){
            return mappedStatement;
        }

        //使用返回类型，创建内联ResultMap
        ResultMap inlineResultMap = new ResultMap.Builder(
                configuration,
                sqlId,
                resultType,
                new ArrayList(),
                null).build();
        List resultMaps=new ArrayList();
        resultMaps.add(inlineResultMap);

        SqlSource sqlSource=new RawSqlSource(configuration, SqlGenerator.genSelectStatement(resultType,null),param.getClass());
        //根据返回类型类构建sql语句，创建新的MappedStatement
        MappedStatement.Builder builder=new MappedStatement.Builder(configuration,sqlId,sqlSource,SqlCommandType.SELECT)
                .resultMaps(resultMaps);
        mappedStatement=builder.build();
        configuration.addMappedStatement(mappedStatement);
        return mappedStatement;
    }
}
