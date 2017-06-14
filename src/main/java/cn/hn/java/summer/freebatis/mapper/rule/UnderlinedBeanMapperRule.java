package cn.hn.java.summer.freebatis.mapper.rule;

/**
 * 下划线风格的映射规则
 * UserId=>user_id
 * Created by xw2sy on 2017-04-15.
 */
public class UnderlinedBeanMapperRule implements IBeanMapperRule {

    //表名前缀
    private String tablePrefix="";
    //类名前缀
    private String classPrefix="";

    public UnderlinedBeanMapperRule() {
    }

    /**
     * @param tablePrefix 表名前缀
     */
    public UnderlinedBeanMapperRule(String tablePrefix){
        this.tablePrefix=tablePrefix;
    }

    /**
     * @param tablePrefix 表名前缀
     * @param classPrefix 类名前缀
     */
    public UnderlinedBeanMapperRule(String tablePrefix, String classPrefix){
        this.tablePrefix=tablePrefix;
        this.classPrefix=classPrefix;
    }

    /**
     * 取表名
     *
     * @param className 类名
     * @return 表名
     */
    public String getTableName(String className) {
        return tablePrefix+camelToUnderline(className);
    }

    /**
     * 取列名
     *
     * @param fieldName 属性名
     * @return 列名
     */
    public String getColName(String fieldName) {
        return camelToUnderline(fieldName);
    }

    /**
     * 驼峰转为下划线风格
     * @param name 待转换的名称
     * @return 下划线风格
     */
    private String camelToUnderline(String name){
        return name.replaceAll("([a-z]+)([A-Z])","$1_$2").toLowerCase();
    }

    /**
     * 通过表名取类名
     *
     * @param tableName 表名
     * @return 类名
     */
    public String getClassName(String tableName) {
        return underlinedToCamel(classPrefix+"_"+tableName.replace(tablePrefix,""),false);
    }

    /**
     * 通过列名取属性名
     *
     * @param colName
     * @return 属性名
     */
    public String getFieldName(String colName) {
        return underlinedToCamel(colName,true);
    }

    /**
     * 下划线转为驼峰
     * @param name 待转换的名称
     * @param skipFirst 忽略第一个
     * @return 驼峰风格
     */
    private String underlinedToCamel(String name, boolean skipFirst){
        String[] parts=name.split("_");
        StringBuilder sbClassName=new StringBuilder();
        for(String p : parts){
            if (p == null || p.length() == 0) {
                continue;
            }
            if(skipFirst && sbClassName.length()==0){
                sbClassName.append(p);
                continue;
            }
            sbClassName.append(p.substring(0,1).toUpperCase()+p.substring(1));
        }
        return sbClassName.toString();
    }
}
