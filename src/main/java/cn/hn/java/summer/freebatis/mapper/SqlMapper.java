package cn.hn.java.summer.freebatis.mapper;

import cn.hn.java.summer.freebatis.mapper.rule.DefaultBeanMapperRule;
import cn.hn.java.summer.freebatis.mapper.rule.IBeanMapperRule;
import cn.hn.java.summer.freebatis.mapper.rule.IDataTypeMapperRule;
import cn.hn.java.summer.freebatis.mapper.rule.MysqlTypeMapperRule;
import cn.hn.java.summer.freebatis.utils.ClassUtils;
import cn.hn.java.summer.freebatis.utils.FileUtils;
import cn.hn.java.summer.freebatis.utils.RegExpUtils;
import cn.hn.java.summer.freebatis.utils.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ApplicationObjectSupport;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by xw2sy on 2017-04-14.
 */
public class SqlMapper extends ApplicationObjectSupport{
    static Log logger= LogFactory.getLog(SqlMapper.class);

    private static SqlMapper SQL_MAPPER;
    /**
     * 表到bean映射的缓存
     */
    private static final ConcurrentHashMap<String,BeanMap> SQL_BEANMAP_CACHE =new ConcurrentHashMap();

    //建表脚本文件名
    private static final String SQL_CREATE_SCRIPT_NAME="_create.sql";

    //匹配建表脚本中的表名
    public static final String REG_CREATE_TABLE_NAME="create\\s+table\\s+`?([\\w\\-\\_\\$]+)`?";
    //匹配建表脚本中的字段
    public static final String REG_CREATE_FIELDS="`?([\\w\\-\\_\\$]+)`?\\s+([\\w-_$]+)+(.*,)";
    //匹配建表脚本中的主键
    public static final String REG_CREATE_TABLE_KEYS="PRIMARY\\s+KEY\\s*\\((.*)\\)";
    //匹配建表脚本中的自动递增字段
    public static final String REG_CREATE_AUTO_INCREASE="([\\W]AUTO_INCREMENT[\\W])|([\\W]identity[\\W]*\\()";

    //数据库实体类扫描过滤
    private static String dbBeanScanFilter;

    //bean mapper规则
    private IBeanMapperRule beanMapperRule;

    //数据库类型映射规则
    private IDataTypeMapperRule dataTypeMapperRule;

    public SqlMapper(String dbBeanScanFilter){
        SqlMapper.dbBeanScanFilter =dbBeanScanFilter;
    }

    /**
     * Subclasses can override this for custom initialization behavior.
     * Gets called by {@code setApplicationContext} after setting the context instance.
     * <p>Note: Does </i>not</i> get called on reinitialization of the context
     * but rather just on first initialization of this object's context reference.
     * <p>The default implementation calls the overloaded {@link #initApplicationContext()}
     * method without ApplicationContext reference.
     *
     * @param context the containing ApplicationContext
     * @throws BeansException              if thrown by ApplicationContext methods
     * @see #setApplicationContext
     */
    @Override
    protected void initApplicationContext(ApplicationContext context) throws BeansException {
        super.initApplicationContext(context);
        this.beanMapperRule=autodetectBeanMapperRule();
        this.dataTypeMapperRule=autodetectDataTypeMapperRule();
        SQL_MAPPER=this;
        //生成所有bean map
        genAllBeanMap();
    }

    /**
     * 获取注入的IBeanMapperRule实例
     * @return
     */
    private IBeanMapperRule autodetectBeanMapperRule() {
        try {
            return BeanFactoryUtils.beanOfTypeIncludingAncestors(
                    getApplicationContext(), IBeanMapperRule.class, true, false);
        }catch (Exception e){
            logger.info("no IBeanMapperRule bean found will use DefaultBeanMapperRule");
        }
        return new DefaultBeanMapperRule();
    }

    /**
     * 获取注入的IDataTypeMapperRule实例
     * @return
     */
    private IDataTypeMapperRule autodetectDataTypeMapperRule() {
        try {
            return BeanFactoryUtils.beanOfTypeIncludingAncestors(
                    getApplicationContext(), IDataTypeMapperRule.class, true, false);
        }catch (Exception e){
            logger.info("no IDataTypeMapperRule bean found will use MysqlTypeMapperRule");
        }
        return new MysqlTypeMapperRule();
    }

    /**
     * 取数据类型映射规则
     * @return 数据类型映射规则
     */
    public static IDataTypeMapperRule getDataTypeMapperRule(){
        return SQL_MAPPER.dataTypeMapperRule;
    }

    /**
     * 取实体类对应的beanMap
     * @param beanType
     * @param <T>
     * @return
     */
    protected static <T> BeanMap getBeanMap(Class<T> beanType){
        //取本类的beanMap
        BeanMap beanMap= SQL_BEANMAP_CACHE.get(beanType.getSimpleName());
        if(
            //取到的是没有建表脚本的bean映射
            beanMap!=null && beanMap.noScripts ||
            //或找不到当前类的bean映射
            beanMap==null && beanType!= Object.class
        ){
            //需要尝试找父类的映射
            //查找父类beanMap
            List<Class<?>> clz= ClassUtils.getAllSuperclasses(beanType);
            for(Class c : clz){
                beanMap= SQL_BEANMAP_CACHE.get(c.getSimpleName());
                if(beanMap!=null){
                    //使用父类的beanMap并缓存
                    SQL_BEANMAP_CACHE.put(beanType.getSimpleName(),beanMap);
                    return beanMap;
                }
            }
        }
        return beanMap;
    }

    /**
     * 取自增列属性名
     * @param beanType bean类型
     * @param <T> bean类型
     * @return 自增列信息
     */
    public static <T> ColField getAutoIncreasePropName(Class<T> beanType){
        BeanMap beanMap= getBeanMap(beanType);
        for (ColField cf : beanMap.fields){
            if(cf.autoIncreased){
                return cf;
            }
        }
        return null;
    }

    /**
     * 生成所有实体的beanMap
     */
    private static void genAllBeanMap(){
        if(dbBeanScanFilter==null){
            logger.error("dbBeanScanFilter is null, bean mapping failed!!!!");
            return;
        }
       List<Class<?>> classList= ClassUtils.getClasses("", dbBeanScanFilter);
       for(Class<?> clz : classList){
           if(clz.getName().indexOf("$")>0){
               //过滤掉内部类
               continue;
           }
           genScriptBeanMap(clz);
       }
    }

    /**
     * 生成beanMap
     * @param beanType 实体类
     * @param <T> 实体类
     */
    private static <T> void genScriptBeanMap(Class<T> beanType){
        //读取与bean目录下同名的建表脚本
        String createSql = readBeanCreateScript(beanType);
        if(createSql==null){
            //没有建表脚本，采用全局映射规则
            genBeanMapByGlobalRule(beanType);
            return;
        }

        //匹配到表名
        String[] tableName= RegExpUtils.findOne(createSql,REG_CREATE_TABLE_NAME);
        if(tableName==null || tableName.length==0){
            logger.error("no table name can be found in create script.");
            return;
        }

        //去除表名部分
        createSql=createSql.substring(createSql.indexOf("("));
        //匹配到字段列表
        List<String[]> columnsRow=RegExpUtils.findAll(createSql,REG_CREATE_FIELDS);
        if(columnsRow.size()==0){
            logger.error("no column found in create script.");
            return;
        }

        List<String[]> columns=new ArrayList();
        //去掉类型为KEY的非字段
        for(String[] column : columnsRow){
            if(column.length==3 && !column[1].toLowerCase().equals("key")){
                columns.add(column);
            }
        }

        //取到类属性列表
        List<Field> propColFields=new ArrayList();
        for(Field f : beanType.getDeclaredFields()){
            if(f.getName().startsWith("_")){
                continue;
            }
            propColFields.add(f);
        }

        //字段数量必须和属性数量一致
        if(columns.size()!=propColFields.size()){
            logger.error("column count not match bean class declared fields count.");
            return;
        }

        //匹配到主键
        List<String[]> keys=RegExpUtils.findAll(createSql,REG_CREATE_TABLE_KEYS);

        //设置列和属性映射
        List<ColField> colFields=new ArrayList();
        for(int i=0;i<columns.size();i++){
            String[] column= columns.get(i);
            Field field=propColFields.get(i);
            //检查是否为自动递增列(oracle不支持)
            boolean autoIncrease=RegExpUtils.isMatch(column[2],REG_CREATE_AUTO_INCREASE,true);
            ColField cf=new ColField(
                    column[0],
                    field.getName(),
                    column[1],
                    field.getType(),
                    false,
                    autoIncrease
            );
            //检查是否为主键
            for(String[] key : keys){
                String[] realKeys=key[0].replaceAll("[`\\s]","").split(",");
                //一个或多个主键
                for (String realKey : realKeys){
                    if(column[0].equals(realKey)){
                        cf.isPrimaryKey=true;
                        break;
                    }
                }
            }
            colFields.add(cf);
        }

        //缓存bean map
        cacheBeanMap(beanType,tableName[0],colFields,false);
    }

    /**
     * 使用全局规则，而不是通过建表脚本 生成bean map
     * @param beanType bean类
     * @param <T> bean类型
     */
    private static <T> void genBeanMapByGlobalRule(Class<T> beanType){
        IBeanMapperRule beanMapperRule= SQL_MAPPER.beanMapperRule;
        IDataTypeMapperRule dataTypeMapperRule=SQL_MAPPER.dataTypeMapperRule;
        Field[] fields=beanType.getDeclaredFields();
        //取表名
        String tableName=beanMapperRule.getTableName(beanType.getSimpleName());
        //取字段
        List<ColField> colFields=new ArrayList();
        for(Field f : fields){
            ColField cf=new ColField(
                    beanMapperRule.getColName(f.getName()),
                    f.getName(),
                    dataTypeMapperRule.getColumnType(f.getType()),
                    f.getType(),
                    false,//无建表脚本无法确定主键
                    false
            );
            colFields.add(cf);
        }
        //缓存
        cacheBeanMap(beanType,tableName,colFields,true);
    }

    /**
     * 缓存bean map
     * @param beanType 实体类
     * @param tableName 表名
     * @param colFields 列
     * @param noScripts 是否没有建表脚本
     * @param <T> 实体类
     */
    private static <T> void cacheBeanMap(Class<T> beanType, String tableName, List<ColField> colFields, boolean noScripts){
        BeanMap beanMap=new BeanMap();
        //以列为键存储映射关系
        Map<String,ColField> colKeyMap=new HashMap();
        for(ColField cf : colFields){
            colKeyMap.put(cf.colName,cf);
        }

        //以属性为键存储映射关系
        Map<String,ColField> propKeyMap=new HashMap();
        for(ColField cf : colFields){
            propKeyMap.put(cf.propName,cf);
        }

        beanMap.tableName=tableName;
        beanMap.fields=colFields;
        beanMap.colKeyMap=colKeyMap;
        beanMap.propKeyMap=propKeyMap;
        beanMap.noScripts=noScripts;
        //缓存bean map
        SQL_BEANMAP_CACHE.put(beanType.getSimpleName(),beanMap);
    }

    /**
     * 读取bean的建表脚本
     * @param beanType
     * @param <T>
     * @return
     */
    private static <T> String readBeanCreateScript(Class<T> beanType){
        //读取与bean同名的建表脚本
        String scriptFileName=beanType.getSimpleName() + SQL_CREATE_SCRIPT_NAME;
        InputStream createSqlFile = beanType.getClassLoader()
            .getResourceAsStream(
                beanType.getPackage().getName().replace(".","/")+
                "/"+scriptFileName
        );
        try {
            if(createSqlFile==null){
                logger.info(StringUtils.format("no create table script file {0} can be found in {1}",scriptFileName,beanType.getPackage().toString()));
                return null;
            }
            return FileUtils.readString(createSqlFile);
        } catch (IOException e) {
            logger.error(StringUtils.format("read create table script file {0} failed in {1}",scriptFileName,beanType.getPackage().toString()),e);
            return null;
        }
    }

    /**
     * 字段
     */
    public static class ColField{
        //列名
        public String colName;
        //属性名
        public String propName;
        //列类型
        public String colType;
        //属性类型
        public Type propType;
        //是否为主键
        public boolean isPrimaryKey;
        //是否为自动递增
        public boolean autoIncreased=false;

        public ColField(String colName, String propName, String colType, Type propType, boolean isPrimaryKey, boolean autoIncreased) {
            this.colName = colName;
            this.propName = propName;
            this.colType = colType;
            this.propType = propType;
            this.isPrimaryKey = isPrimaryKey;
            this.autoIncreased=autoIncreased;
        }
    }

    /**
     * bean映射
     */
    static class BeanMap{
        //表名
        public String tableName;
        //字段列表
        public List<ColField> fields;
        //列-字段hash
        public Map<String,ColField> colKeyMap;
        //属性-字段hash
        public Map<String,ColField> propKeyMap;
        //是否没有建表脚本
        public boolean noScripts;
    }
}
