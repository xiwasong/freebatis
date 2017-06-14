package cn.hn.java.summer.freebatis.mapper;

import cn.hn.java.summer.freebatis.utils.ReflectUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 条件项
 * Created by xw2sy on 2017-04-06.
 */
public class ConditionEntity {

    private Map conditionMap=new HashMap();

    /**
     * 添加条件项
     * @param key
     * @param value
     * @return
     */
    public ConditionEntity set(String key, Object value){
        conditionMap.put(key,value);
        return this;
    }

    public Map getConditionMap(){
        return conditionMap;
    }

    /**
     * 从实体对象转换为ConditionEntity
     * @param bean bean
     * @return ConditionEntity
     */
    public static ConditionEntity fromObj(Object bean){
        Map<String,Object> data= ReflectUtils.beanToMap(bean);
        ConditionEntity ce=new ConditionEntity();
        for(String key:data.keySet()){
            ce.set(key,data.get(key));
        }
        return ce;
    }
}
