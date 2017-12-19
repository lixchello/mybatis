package com.rrc.mybatis.generator.plugins;

import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.internal.types.JavaTypeResolverDefaultImpl;

/**
 * Created with check
 * Created By lxc
 * Date: 2017/11/10
 */
public class ByteJavaTypeResolver extends JavaTypeResolverDefaultImpl {
    public ByteJavaTypeResolver() {
        super();
        this.typeMap.put(Integer.valueOf(-6), new JdbcTypeInformation("TINYINT", new FullyQualifiedJavaType(Integer.class.getName())));
    }
}
