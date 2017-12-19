package com.rrc.mybatis.generator.plugins;

import org.mybatis.generator.api.GeneratedXmlFile;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;

import java.util.List;

/**
 * Created by lxc on 17/3/28.
 */
public class OverWriteXmlPlugin extends PluginAdapter {

	// 设置默认xml文件覆盖
	@Override
	public boolean sqlMapGenerated(GeneratedXmlFile sqlMap, IntrospectedTable introspectedTable) {
		if (null == sqlMap || null == sqlMap.getClass()) {
			throw new RuntimeException("[MyBatis][OverWriteXmlPlugin][GeneratedXmlFile sqlMap || sqlMap.getClass()] can not null.");
		}
		try {
			java.lang.reflect.Field field = sqlMap.getClass().getDeclaredField("isMergeable");
			field.setAccessible(true);
			field.setBoolean(sqlMap, false);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("[MyBatis][OverWriteXmlPlugin][Set xml isMergeable] has error.");
		}
		return super.sqlMapGenerated(sqlMap, introspectedTable);
	}

	@Override
	public boolean validate(List<String> warnings) {
		return true;
	}
}
