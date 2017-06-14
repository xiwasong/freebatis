package cn.hn.java.summer.freebatis;

import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

/**
 * Created by xw2sy on 2017-06-09.
 */
public class MySessionFactoryBuilder extends SqlSessionFactoryBuilder {

    public SqlSessionFactory build(Configuration config) {
        return new MySqlSessionFactory(config);
    }
}
