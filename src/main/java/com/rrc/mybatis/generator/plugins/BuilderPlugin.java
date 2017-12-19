package com.rrc.mybatis.generator.plugins;

import org.apache.commons.lang.StringUtils;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.Field;
import org.mybatis.generator.api.dom.java.TopLevelClass;

import java.io.*;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Created by lxc on 17/3/27.
 */
public class BuilderPlugin extends PluginAdapter {

    private static final String PO = ".po";

    private static final String BUILDER = ".builder";

    /**
     * 定义格式化符号
     */
    private static final String N = "\n";
    private static final String R_N = "\r\n";
    private static final String TAB = "    ";
    private static final String TAB2 = TAB + TAB;
    private static final String TAB3 = TAB2 + TAB;

    @Override
    public boolean modelBaseRecordClassGenerated(TopLevelClass topLevelClass,
                                                 IntrospectedTable introspectedTable) {
        addBuilder(topLevelClass, introspectedTable);
        return super.modelBaseRecordClassGenerated(topLevelClass, introspectedTable);

    }

    private void addBuilder(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        if (null == topLevelClass) {
            throw new RuntimeException("[MyBatis][BuilderPlugin][TopLevelClass topLevelClass] can not null.");
        }
        if (null == topLevelClass.getType()) {
            throw new RuntimeException("[MyBatis][BuilderPlugin][FullyQualifiedJavaType fullyQualifiedJavaType] can not null.");
        }
        if (null == this.context) {
            throw new RuntimeException("[MyBatis][BuilderPlugin][Context context] can not null.");
        }
        if (null == this.context.getJavaModelGeneratorConfiguration()) {
            throw new RuntimeException("[MyBatis][BuilderPlugin][JavaModelGeneratorConfiguration javaModelGeneratorConfiguration] can not null.");
        }
        List<Field> fields = topLevelClass.getFields();
        if (null == fields || fields.size() <= 0) {
            throw new RuntimeException("[MyBatis][BuilderPlugin][List<org.mybatis.generator.api.dom.java.Field> fields] can not null.");
        }
        String shortName = topLevelClass.getType().getShortName();
        String qualifiedName = topLevelClass.getType().getFullyQualifiedName();
        String targetPackage = this.context.getJavaModelGeneratorConfiguration().getTargetPackage();
        // 这里是固定写法
        targetPackage = StringUtils.replace(targetPackage, PO, BUILDER);
        String targetProject = this.context.getJavaModelGeneratorConfiguration().getTargetProject();
        // 定义写入内容的builder
        StringBuilder builder = new StringBuilder();
        // 写入包
        builder = getPackage(builder, targetPackage);
        // 写入import
        builder = getImport(builder, fields, qualifiedName);
        // 写入内容主体
        builder = getBody(builder, fields, shortName);
        // 写文件
        try {
            writeFile(builder.toString(), null, shortName, targetPackage, targetProject);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("[MyBatis][BuilderPlugin][BuilderPlugin writeFile] has error.");
        }
        System.out.println("[BuilderPlugin]......................[" + shortName + "]......................" + "Done.....");
    }

    public StringBuilder getPackage(StringBuilder builder, String packagePath) {
        builder.append("package " + packagePath + ";" + R_N);
        return builder.append(N);
    }

    private StringBuilder getBody(StringBuilder builder, List<Field> fields, String shortName) {
        builder.append("public class " + shortName + "Builder" + " {" + R_N);
        builder.append(TAB + "public static class Builder {" + R_N);
        String firstVar = shortName.substring(0, 1);
        firstVar = firstVar.toLowerCase(Locale.ENGLISH);
        String lowerShortName = firstVar + shortName.substring(1, shortName.length());
        builder.append(TAB2 + "private final " + shortName + " " + lowerShortName + " = new " + shortName + "();" + R_N);
        builder.append(N);
        for (Field field : fields) {
            if (field.isStatic()) {
                continue;
            }
            String fieldName = field.getName();
            builder.append(TAB2 + "public Builder " + field.getName() + "(" + field.getType().getShortName() + " " + fieldName + ")" + " {" + R_N);
            String firstFieldVar = fieldName.substring(0, 1);
            String lowerFieldName = firstFieldVar.toUpperCase(Locale.ENGLISH) + fieldName.substring(1, fieldName.length());
            builder.append(TAB3 + lowerShortName + ".set" + lowerFieldName + "(" + fieldName + ")" + ";" + R_N);
            builder.append(TAB3 + "return this;" + R_N);
            builder.append(TAB2 + "}" + R_N);
            builder.append(N);
        }
        builder.append(TAB2 + "public " + shortName + " build() {" + R_N);
        builder.append(TAB3 + "return " + lowerShortName + ";" + R_N);
        builder.append(TAB2 + "}" + R_N);
        builder.append(TAB + "}" + R_N);
        builder.append("}" + R_N);
        return builder.append(N);
    }

    private StringBuilder getImport(StringBuilder builder, List<Field> fields, String qualifiedName) {
        builder.append("import " + qualifiedName + ";" + R_N);
        Set<String> imports = new HashSet<>();
        for (Field field : fields) {
            if (field.isStatic()) {
                continue;
            }
            imports.add("import " + field.getType().getFullyQualifiedName() + ";" + R_N);
        }
        builder.append(N);
        for (String str : imports) {
            builder.append(str);
        }
        return builder.append(N);
    }

    private void writeFile(String content, String fileEncoding, String shortName, String targetPackage, String targetProject) throws IOException {
        File fileEmpty = new File("");
        String projectPath = fileEmpty.getAbsolutePath();
        targetPackage = StringUtils.replace(targetPackage, ".", "/");
        // 判断文件夹是否存在,如果不存在直接创建
        String filePath = projectPath + "/" + targetProject + "/" + targetPackage + "/";
        File pathFile = new File(filePath);
        if (!pathFile.exists()) {
            pathFile.mkdir();
        }
        // 判断当前要生成的builder文件是否存在,如果存在,删除直接生成
        String fileName = filePath + shortName + "Builder.java";
        File file = new File(fileName);
        if (file.exists()) {
            file.createNewFile();
        }
        FileOutputStream fos = new FileOutputStream(file, false);
        OutputStreamWriter osw;
        if (fileEncoding == null) {
            osw = new OutputStreamWriter(fos);
        } else {
            osw = new OutputStreamWriter(fos, fileEncoding);
        }
        BufferedWriter bw = new BufferedWriter(osw);
        bw.write(content);
        bw.close();
    }


    @Override
    public boolean validate(List<String> warnings) {
        return true;
    }

}
