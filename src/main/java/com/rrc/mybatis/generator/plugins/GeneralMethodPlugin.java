package com.rrc.mybatis.generator.plugins;

import org.apache.commons.lang.StringUtils;
import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.*;
import org.mybatis.generator.api.dom.xml.*;

import java.util.List;
import java.util.Locale;

/**
 * Created by lxc on 17/03/28.
 */
public class GeneralMethodPlugin extends PluginAdapter {

	// 定义批量插入的每一行的字段数
	private static final int ROW_COLUMN = 3;

	// *****************************************************xml方法生成*****************************************************
	@Override
	public boolean sqlMapDocumentGenerated(Document document, IntrospectedTable introspectedTable) {
		if (null == document || null == document.getRootElement()) {
			throw new RuntimeException("[MyBatis][GeneralMethodPlugin][Document document || document.getRootElement()] can not null.");
		}
		if (null == introspectedTable || null == introspectedTable.getTableConfiguration()) {
			throw new RuntimeException("[MyBatis][GeneralMethodPlugin][IntrospectedTable introspectedTable || introspectedTable.getTableConfiguration()] can not null.");
		}
		List<Element> elements = document.getRootElement().getElements();
		List<IntrospectedColumn> primaryColumns = introspectedTable.getPrimaryKeyColumns();
		List<IntrospectedColumn> baseColumns = introspectedTable.getBaseColumns();
		if (null == primaryColumns || null == baseColumns || primaryColumns.size() <= 0 || baseColumns.size() <= 0) {
			throw new RuntimeException("[MyBatis][GeneralMethodPlugin][List<IntrospectedColumn> primaryColumns || List<IntrospectedColumn> baseColumns] can not null.");
		}
		// 判断是否替换名字
		String tableName = introspectedTable.getTableConfiguration().getTableName();

		// 生成通用查询where语句
		XmlElement xeWhereSQL = getGeneralWhereSQL(primaryColumns, baseColumns);
		// 生成通用查询语句
		XmlElement xeSelect = getGeneralSelect(tableName);
		// 生成通用查询count语句
		XmlElement xeSelectCount = getGeneralSelectCount(tableName);
		//生成通用批量插入
		XmlElement xeBatchInsert = getGeneralBatchInsert(tableName, primaryColumns, baseColumns);
		// 生成通用批量插入(selective)
//		XmlElement xeBatchInsertSelective = getGeneralBatchInsertSelective(tableName, primaryColumns, baseColumns);

		elements.add(xeWhereSQL);
		elements.add(xeSelect);
		elements.add(xeSelectCount);
		elements.add(xeBatchInsert);
//		elements.add(xeBatchInsertSelective);

		System.out.println("[GeneralMethodPlugin]......................[" + tableName + "]......................" + "Done.....");
		return super.sqlMapDocumentGenerated(document, introspectedTable);
	}

	/**
	 * 生成通用的where条件
	 *
	 * @param primaryColumns
	 * @param baseColumns
	 * @return
	 */
	private XmlElement getGeneralWhereSQL(List<IntrospectedColumn> primaryColumns, List<IntrospectedColumn> baseColumns) {
		XmlElement xe = new XmlElement("sql");
		xe.addAttribute(new Attribute("id", "where_sql"));
		XmlElement xeWhere = new XmlElement("where");
		for (IntrospectedColumn column : baseColumns) {
			XmlElement xeWhereIf = new XmlElement("if");
			xeWhereIf.addAttribute(new Attribute("test", "po." + column.getJavaProperty() + " != null"));
			xeWhereIf.addElement(new TextElement("AND " + column.getActualColumnName() + " = #{po." + column.getJavaProperty() + ",jdbcType=" + column.getJdbcTypeName() + "}"));
			xeWhere.addElement(xeWhereIf);
		}
		xe.addElement(xeWhere);
		return xe;
	}

	/**
	 * 生成通用查询sql语句
	 *
	 * @param tableName
	 * @return
	 */
	public XmlElement getGeneralSelect(String tableName) {
		XmlElement xe = new XmlElement("select");
		xe.addAttribute(new Attribute("id", "selectByParam"));
		xe.addAttribute(new Attribute("resultMap", "BaseResultMap"));
		xe.addAttribute(new Attribute("parameterType", "java.util.Map"));
		xe.addElement(new TextElement("select"));

		XmlElement xeInclude = new XmlElement("include");
		xeInclude.addAttribute(new Attribute("refid", "Base_Column_List"));
		xe.addElement(xeInclude);

		xe.addElement(new TextElement("from " + tableName));

		XmlElement xeIf = new XmlElement("if");
		xeIf.addAttribute(new Attribute("test", "po!= null"));

		XmlElement xeIfInclude = new XmlElement("include");
		xeIfInclude.addAttribute(new Attribute("refid", "where_sql"));

		xeIf.addElement(xeIfInclude);

		XmlElement xeIfLimit = new XmlElement("if");
		xeIfLimit.addAttribute(new Attribute("test", "start != null and end != null"));
		xeIfLimit.addElement(new TextElement("LIMIT #{start},#{end}"));

		xe.addElement(xeIf);
		xe.addElement(xeIfLimit);
		return xe;
	}

	/**
	 * 生成通用查询count语句
	 *
	 * @param tableName
	 * @return
	 */
	public XmlElement getGeneralSelectCount(String tableName) {
		XmlElement xe = new XmlElement("select");
		xe.addAttribute(new Attribute("id", "selectCountByParam"));
		xe.addAttribute(new Attribute("parameterType", "java.util.Map"));
		xe.addAttribute(new Attribute("resultType", "java.lang.Integer"));

		xe.addElement(new TextElement("select count(1)"));
		xe.addElement(new TextElement("from " + tableName));

		XmlElement xeIf = new XmlElement("if");
		xeIf.addAttribute(new Attribute("test", "po!= null"));

		XmlElement xeIfInclude = new XmlElement("include");
		xeIfInclude.addAttribute(new Attribute("refid", "where_sql"));
		xeIf.addElement(xeIfInclude);

		xe.addElement(xeIf);
		return xe;
	}

	/**
	 * 批量插入的batchInsert
	 *
	 * @param tableName
	 * @param primaryColumns
	 * @param baseColumns
	 * @return
	 */
	private XmlElement getGeneralBatchInsert(String tableName, List<IntrospectedColumn> primaryColumns, List<IntrospectedColumn> baseColumns) {
		XmlElement xe = new XmlElement("insert");
		xe.addAttribute(new Attribute("id", "batchInsert"));
		xe.addAttribute(new Attribute("parameterType", "java.util.Map"));

		xe.addElement(new TextElement("insert into " + tableName + " ("));
		int i = 0;
		String tempJdbcColumn = "";
		for (IntrospectedColumn column : baseColumns) {
			++i;
			if (i != baseColumns.size()) {
				tempJdbcColumn = tempJdbcColumn + column.getActualColumnName() + ",";
			}
			if (i == baseColumns.size()) {
				tempJdbcColumn = tempJdbcColumn + column.getActualColumnName();
			}
			if (i % ROW_COLUMN == 0) {
				xe.addElement(new TextElement(tempJdbcColumn));
				tempJdbcColumn = "";
			}
		}
		if (baseColumns.size() % ROW_COLUMN != 0) {
			xe.addElement(new TextElement(tempJdbcColumn));
		}

		xe.addElement(new TextElement(")" + " values"));

		XmlElement foreach = new XmlElement("foreach");
		foreach.addAttribute(new Attribute("collection", "list"));
		foreach.addAttribute(new Attribute("item", "item"));
		foreach.addAttribute(new Attribute("index", "index"));
		foreach.addAttribute(new Attribute("separator", ","));
		foreach.addElement(new TextElement("("));

		int j = 0;
		String tempForeach = "";
		for (IntrospectedColumn column : baseColumns) {
			++j;
			if (j != baseColumns.size()) {
				tempForeach = tempForeach + "#{item." + column.getJavaProperty() + ",jdbcType=" + column.getJdbcTypeName() + "}" + ",";
			}
			if (j == baseColumns.size()) {
				tempForeach = tempForeach + "#{item." + column.getJavaProperty() + ",jdbcType=" + column.getJdbcTypeName() + "}";
			}
			if (j % ROW_COLUMN == 0) {
				foreach.addElement(new TextElement(tempForeach));
				tempForeach = "";
			}
		}
		if (baseColumns.size() % ROW_COLUMN != 0) {
			foreach.addElement(new TextElement(tempForeach));
		}
		foreach.addElement(new TextElement(")"));
		xe.addElement(foreach);

		return xe;
	}

	/**
	 * 批量插入的batchInsertSelective
	 *
	 * @param tableName
	 * @param primaryColumns
	 * @param baseColumns
	 * @return
	 */
	private XmlElement getGeneralBatchInsertSelective(String tableName, List<IntrospectedColumn> primaryColumns, List<IntrospectedColumn> baseColumns) {
		XmlElement xe = new XmlElement("insert");
		xe.addAttribute(new Attribute("id", "batchInsertSelective"));
		xe.addAttribute(new Attribute("parameterType", "java.util.Map"));


		XmlElement foreach = new XmlElement("foreach");
		foreach.addAttribute(new Attribute("collection", "list"));
		foreach.addAttribute(new Attribute("item", "item"));
		foreach.addAttribute(new Attribute("index", "index"));
		foreach.addAttribute(new Attribute("separator", ";"));
		foreach.addElement(new TextElement("insert into " + tableName));

		XmlElement trimColumn = new XmlElement("trim");
		trimColumn.addAttribute(new Attribute("prefix", "("));
		trimColumn.addAttribute(new Attribute("suffix", ")"));
		trimColumn.addAttribute(new Attribute("suffixOverrides", ","));
		for (IntrospectedColumn column : baseColumns) {
			XmlElement ifXe = new XmlElement("if");
			ifXe.addAttribute(new Attribute("test", "item." + column.getJavaProperty() + " != null"));
			ifXe.addElement(new TextElement(column.getActualColumnName() + ","));
			trimColumn.addElement(ifXe);
		}
		foreach.addElement(trimColumn);

		XmlElement trimValue = new XmlElement("trim");
		trimValue.addAttribute(new Attribute("prefix", "values ("));
		trimValue.addAttribute(new Attribute("suffix", ")"));
		trimValue.addAttribute(new Attribute("suffixOverrides", ","));
		for (IntrospectedColumn column : baseColumns) {
			XmlElement ifXe = new XmlElement("if");
			ifXe.addAttribute(new Attribute("test", "item." + column.getJavaProperty() + " != null"));
			ifXe.addElement(new TextElement("#{item." + column.getJavaProperty() + ",jdbcType=" + column.getJdbcTypeName() + "}" + ","));
			trimValue.addElement(ifXe);
		}
		foreach.addElement(trimValue);

		xe.addElement(foreach);
		return xe;
	}

	// *****************************************************mapper方法生成*****************************************************

	/**
	 * 生成mapper通用方法
	 *
	 * @param interfaze
	 * @param topLevelClass
	 * @param introspectedTable
	 * @return
	 */
	@Override
	public boolean clientGenerated(Interface interfaze, TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
		if (null == interfaze) {
			throw new RuntimeException("[MyBatis][GeneralMethodPlugin][Interface interfaze] can not null.");
		}
		if (null == introspectedTable) {
			throw new RuntimeException("[MyBatis][GeneralMethodPlugin][IntrospectedTable introspectedTable] can not null.");
		}
		// 添加方法
		List<Method> methods = interfaze.getMethods();
		// 添加导入包
		interfaze.addImportedType(new FullyQualifiedJavaType("org.apache.ibatis.annotations.Param"));
		interfaze.addImportedType(new FullyQualifiedJavaType("java.util.List"));

		// 设置method方法
		Method methodSelect = getSelectMethod(introspectedTable);
		methods.add(methodSelect);

		// 设置method的count方法
		Method methodSelectCount = getSelectMethodCount(introspectedTable);
		methods.add(methodSelectCount);

		// 设置批量插入的batchInsert
		Method methodBatchInsert = getBatchInsertMethod(introspectedTable);
		methods.add(methodBatchInsert);

		// 设置批量插入的batchInsertSelective
//		Method methodBatchInsertSelective = getBatchInsertSelectiveMethod(introspectedTable);
//		methods.add(methodBatchInsertSelective);

		System.out.println("[GeneralMethodPlugin]......................[" + introspectedTable.getAliasedFullyQualifiedTableNameAtRuntime() + "]......................" + "Done.....");
		return super.clientGenerated(interfaze, topLevelClass, introspectedTable);
	}

	/**
	 * 生成方法
	 *
	 * @param introspectedTable
	 * @return
	 */
	private Method getSelectMethod(IntrospectedTable introspectedTable) {
		String type = introspectedTable.getBaseRecordType();
		String parameterName = StringUtils.substringAfterLast(type, ".");
		String firstFieldVar = parameterName.substring(0, 1);
		parameterName = firstFieldVar.toLowerCase(Locale.ENGLISH) + parameterName.substring(1, parameterName.length());
		Method method = new Method();
		method.setName("selectByParam");
		method.setVisibility(JavaVisibility.PUBLIC);
		Parameter param1 = new Parameter(new FullyQualifiedJavaType(type), parameterName);
		param1.addAnnotation("@Param(\"po\")");
		method.addParameter(0, param1);
		Parameter param2 = new Parameter(new FullyQualifiedJavaType("java.lang.Integer"), "start");
		param2.addAnnotation("@Param(\"start\")");
		method.addParameter(1, param2);
		Parameter param3 = new Parameter(new FullyQualifiedJavaType("java.lang.Integer"), "end");
		param3.addAnnotation("@Param(\"end\")");
		method.addParameter(2, param3);
		method.setReturnType(new FullyQualifiedJavaType("List<" + introspectedTable.getBaseRecordType() + ">"));
		return method;
	}

	/**
	 * 生成selectCount方法
	 *
	 * @param introspectedTable
	 * @return
	 */
	private Method getSelectMethodCount(IntrospectedTable introspectedTable) {
		String type = introspectedTable.getBaseRecordType();
		String parameterName = StringUtils.substringAfterLast(type, ".");
		String firstFieldVar = parameterName.substring(0, 1);
		parameterName = firstFieldVar.toLowerCase(Locale.ENGLISH) + parameterName.substring(1, parameterName.length());
		Method method = new Method();
		method.setName("selectCountByParam");
		method.setVisibility(JavaVisibility.PUBLIC);
		Parameter param1 = new Parameter(new FullyQualifiedJavaType(type), parameterName);
		param1.addAnnotation("@Param(\"po\")");
		method.addParameter(0, param1);
		method.setReturnType(new FullyQualifiedJavaType("java.lang.int"));
		return method;
	}

	/**
	 * 生成mapper的BatchInsert
	 *
	 * @param introspectedTable
	 * @return
	 */
	private Method getBatchInsertMethod(IntrospectedTable introspectedTable) {
		Method method = new Method();
		method.setName("batchInsert");
		method.setVisibility(JavaVisibility.PUBLIC);
		Parameter param = new Parameter(new FullyQualifiedJavaType("List<" + introspectedTable.getBaseRecordType() + ">"), "list");
		param.addAnnotation("@Param(\"list\")");
		method.addParameter(0, param);
		method.setReturnType(new FullyQualifiedJavaType("int"));
		return method;
	}

	/**
	 * 生成mapper的BatchInsertSelective
	 *
	 * @param introspectedTable
	 * @return
	 */
	private Method getBatchInsertSelectiveMethod(IntrospectedTable introspectedTable) {
		Method method = new Method();
		method.setName("batchInsertSelective");
		method.setVisibility(JavaVisibility.PUBLIC);
		Parameter param = new Parameter(new FullyQualifiedJavaType("List<" + introspectedTable.getBaseRecordType() + ">"), "list");
		param.addAnnotation("@Param(\"list\")");
		method.addParameter(0, param);
		method.setReturnType(new FullyQualifiedJavaType("int"));
		return method;
	}

	@Override
	public boolean validate(List<String> warnings) {
		return true;
	}
}
