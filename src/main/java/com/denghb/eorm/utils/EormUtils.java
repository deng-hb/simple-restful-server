package com.denghb.eorm.utils;


import com.denghb.eorm.annotation.Ecolumn;
import com.denghb.eorm.annotation.Etable;
import com.denghb.utils.ReflectUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;


/**
 * 支持使用Annotation标注表结构
 */
public class EormUtils {
    private static final ThreadLocal<EormUtils> local = new ThreadLocal<EormUtils>();
    /**
     * 默认主键名
     */
    private final static String DEFAULT_PRIMATRY_KEY_NAME = "id";

    private final static String serialVersionUID = "serialVersionUID";

    public static <T> String getTableName(final Class<T> clazz) {
        // 获取表名
        Etable table = clazz.getAnnotation(Etable.class);
        if (null == table) {
            return JdbcUtils.humpToUnderline(clazz.getSimpleName());
        }
        StringBuffer tableName = new StringBuffer("`");
        // 获取数据库名称
        String database = table.database();

        if (null != database && database.trim().length() != 0) {
            tableName.append(database);
            tableName.append("`.`");
        }
        // 获取注解的表名
        tableName.append(table.name());
        tableName.append("`");
        return tableName.toString();
    }


    /**
     * 获取主键名
     *
     * @param clazz
     * @return
     */
    public static List<String> getPrimaryKeyNames(Class clazz) {

        List<String> primaryKeys = new ArrayList<String>();
        Field[] classFields = clazz.getDeclaredFields();
        for (Field field : classFields) {

            Ecolumn ecolumn = field.getAnnotation(Ecolumn.class);
            if (null != ecolumn && ecolumn.primaryKey()) {
                primaryKeys.add(ecolumn.name());
            } else {
                if (DEFAULT_PRIMATRY_KEY_NAME.equals(field.getName())) {
                    primaryKeys.add(field.getName());
                }
            }

        }

        return primaryKeys;
    }

    public static <T> TableInfo getTableInfo(T domain) {
        TableInfo table = new TableInfo();

        Class clazz = domain.getClass();

        table.setTableName(getTableName(clazz));

        //
        List<Field> fields = ReflectUtils.getFields(clazz);
        for (Field field : fields) {

            if (serialVersionUID.equals(field.getName())) {
                continue;
            }
            Object value = ReflectUtils.getFieldValue(field, domain);

            Ecolumn ecolumn = field.getAnnotation(Ecolumn.class);
            if (null != ecolumn) {

                boolean isPrimaryKey = ecolumn.primaryKey();
                if (isPrimaryKey) {
                    table.getAllPrimaryKeyFields().add(field);
                }

                List<Column> columnList = isPrimaryKey ? table.getPrimaryKeyColumns() : table.getCommonColumns();
                if (null != value) {
                    columnList.add(new Column(ecolumn.name(), value));
                    table.getAllColumns().add(new Column(ecolumn.name(), value));
                }

            } else {

                String columnName = field.getName();
                columnName = JdbcUtils.humpToUnderline(columnName);
                boolean isPrimaryKey = DEFAULT_PRIMATRY_KEY_NAME.equals(columnName);
                if (isPrimaryKey) {
                    table.getAllPrimaryKeyFields().add(field);
                }

                List<Column> columnList = isPrimaryKey ? table.getPrimaryKeyColumns() : table.getCommonColumns();
                if (null != value) {
                    columnList.add(new Column(columnName, value));
                    table.getAllColumns().add(new Column(columnName, value));
                }

            }

        }


        return table;
    }


    /**
     *
     */
    public static class TableInfo {

        private String tableName;

        /**
         * 主键字段
         */
        private List<Field> allPrimaryKeyFields = new ArrayList<Field>();

        /**
         * 有值的列
         */
        private List<Column> allColumns = new ArrayList<Column>();

        /**
         * 有值的主键列
         */
        private List<Column> primaryKeyColumns = new ArrayList<Column>();

        /**
         * 有值的普通列
         */
        private List<Column> commonColumns = new ArrayList<Column>();

        public String getTableName() {
            return tableName;
        }

        public void setTableName(String tableName) {
            this.tableName = tableName;
        }

        public List<Field> getAllPrimaryKeyFields() {
            return allPrimaryKeyFields;
        }

        public void setAllPrimaryKeyFields(List<Field> allPrimaryKeyFields) {
            this.allPrimaryKeyFields = allPrimaryKeyFields;
        }

        public List<Column> getAllColumns() {
            return allColumns;
        }

        public void setAllColumns(List<Column> allColumns) {
            this.allColumns = allColumns;
        }

        public List<Column> getPrimaryKeyColumns() {
            return primaryKeyColumns;
        }

        public void setPrimaryKeyColumns(List<Column> primaryKeyColumns) {
            this.primaryKeyColumns = primaryKeyColumns;
        }

        public List<Column> getCommonColumns() {
            return commonColumns;
        }

        public void setCommonColumns(List<Column> commonColumns) {
            this.commonColumns = commonColumns;
        }
    }

    public static class Column {

        private String name;

        private Object value;

        public Column(String name, Object value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }

    }

}
