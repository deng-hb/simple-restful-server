package com.denghb.eorm.impl;

import com.denghb.eorm.Eorm;
import com.denghb.eorm.annotation.Ecolumn;
import com.denghb.eorm.domain.Paging;
import com.denghb.eorm.domain.PagingResult;
import com.denghb.eorm.utils.EormUtils;
import com.denghb.eorm.utils.JdbcUtils;
import com.denghb.utils.ReflectUtils;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EormMySQLImpl implements Eorm {

    private Connection connection;

    private String url;

    private String username;

    private String password;

    public EormMySQLImpl(Connection connection) {
        this.connection = connection;
    }

    public EormMySQLImpl(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.connection = createConnection();
    }

    public Connection createConnection() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            return DriverManager.getConnection(url, username, password);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("connection error");
        }
    }

    public int execute(String sql, Object... args) {
        int res = 0;
        PreparedStatement ps = null;
        try {
            ps = connection.prepareStatement(sql);

            int i = 1;
            for (Object object : args) {
                ps.setObject(i, object);
                i++;
            }
            res = ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            JdbcUtils.close(ps);
        }
        return res;
    }

    public <T> List<T> select(Class<T> clazz, String sql, Object... args) {
        List<T> list = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = connection.prepareStatement(sql);

            int i = 1;
            for (Object object : args) {
                ps.setObject(i, object);
                i++;
            }

            rs = ps.executeQuery();
            ResultSetMetaData data = rs.getMetaData();

            // map
            if (clazz.equals(Map.class)) {
                list = (List<T>) buildMap(rs, data);
            } else if (clazz.getSuperclass() == Number.class || clazz.getSuperclass() == CharSequence.class || clazz.isPrimitive()) {
                // 基本类型
                list = new ArrayList<T>();
                while (rs.next()) {
                    String columnName = data.getColumnName(1);
                    Object value = rs.getObject(columnName);

                    if (value instanceof Number) {
                        value = ReflectUtils.constructorInstance(clazz, String.class, String.valueOf(value));
                    }
//                    clazz.is
                    list.add((T) value);
                }

            } else {
                list = new ArrayList<T>();

                // 所有的字段
                List<Field> fields = ReflectUtils.getFields(clazz);

                int columnCount = data.getColumnCount();
                while (rs.next()) {
                    Object object = ReflectUtils.createInstance(clazz);

                    list.add((T) object);
                    for (int j = 1; j <= columnCount; j++) {

                        String columnName = data.getColumnName(j);
                        Object value = rs.getObject(columnName);

                        // 找到对应字段
                        for (Field field : fields) {
                            // 比较表字段名
                            Ecolumn ecolumn = field.getAnnotation(Ecolumn.class);
                            if (null != ecolumn) {
                                if (ecolumn.name().equalsIgnoreCase(columnName)) {
                                    ReflectUtils.setFieldValue(field, object, value);
                                }
                            }
                            // 比较字段名
                            columnName = columnName.replace("_", "");
                            if (field.getName().equalsIgnoreCase(columnName)) {
                                ReflectUtils.setFieldValue(field, object, value);
                            }
                        }

                    }
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            JdbcUtils.close(ps);
            JdbcUtils.close(rs);
        }
        return list;
    }

    public <T> int insert(T domain) {

        EormUtils.TableInfo table = EormUtils.getTableInfo(domain);

        List<Object> params = new ArrayList<Object>();

        StringBuilder csb = new StringBuilder();
        StringBuilder vsb = new StringBuilder();

        List<EormUtils.Column> columns = table.getAllColumns();
        for (int i = 0; i < columns.size(); i++) {

            EormUtils.Column column = columns.get(i);

            if (i > 0) {
                csb.append(", ");
                vsb.append(", ");
            }
            csb.append('`');
            csb.append(column.getName());
            csb.append('`');

            vsb.append("?");

            params.add(column.getValue());
        }

        StringBuilder sb = new StringBuilder("insert into ");
        sb.append(table.getTableName());
        sb.append(" (");
        sb.append(csb);
        sb.append(") values (");
        sb.append(vsb);
        sb.append(")");

        String sql = sb.toString();
        int res = this.execute(sql, params.toArray());

        if (1 == res) {
            // 获取自动生成的ID并填充
            List<Field> fields = table.getAllPrimaryKeyFields();
            if (fields.size() == 1) {// 只适合单个主键
                Field field = fields.get(0);
                Object object = ReflectUtils.getFieldValue(field, domain);
                if (null == object) {
                    Object value = selectOne(field.getType(), "select LAST_INSERT_ID() as id");
                    ReflectUtils.setFieldValue(field, domain, value);
                }
            }
        }

        return res;

    }

    public <T> int update(T domain) {

        EormUtils.TableInfo table = EormUtils.getTableInfo(domain);
        List<Object> params = new ArrayList<Object>();

        List<EormUtils.Column> commonColumns = table.getCommonColumns();
        StringBuilder ssb = new StringBuilder();
        for (int i = 0; i < commonColumns.size(); i++) {
            EormUtils.Column column = commonColumns.get(i);

            if (i > 0) {
                ssb.append(", ");
            }

            ssb.append("`");
            ssb.append(column.getName());
            ssb.append("` = ?");

            params.add(column.getValue());
        }

        List<EormUtils.Column> primaryKeyColumns = table.getPrimaryKeyColumns();
        StringBuilder wsb = new StringBuilder();
        for (int i = 0; i < primaryKeyColumns.size(); i++) {
            EormUtils.Column column = primaryKeyColumns.get(i);
            if (i > 0) {
                wsb.append(" and ");
            }
            wsb.append("`");
            wsb.append(column.getName());
            wsb.append("` = ?");

            params.add(column.getValue());
        }

        StringBuilder sb = new StringBuilder("update ");
        sb.append(table.getTableName());
        sb.append(" set ");
        sb.append(ssb);
        sb.append(" where ");
        sb.append(wsb);

        String sql = sb.toString();
        int res = this.execute(sql, params.toArray());
        return res;
    }

    public <T> int delete(T domain) {
        EormUtils.TableInfo table = EormUtils.getTableInfo(domain);
        List<Object> params = new ArrayList<Object>();

        StringBuilder sb = new StringBuilder("delete from ");
        sb.append(table.getTableName());
        sb.append(" where ");

        List<EormUtils.Column> primaryKeyColumns = table.getPrimaryKeyColumns();
        for (int i = 0; i < primaryKeyColumns.size(); i++) {
            EormUtils.Column column = primaryKeyColumns.get(i);
            if (i > 0) {
                sb.append(" and ");
            }
            sb.append("`");
            sb.append(column.getName());
            sb.append("` = ?");

            params.add(column.getValue());
        }

        String sql = sb.toString();
        int res = this.execute(sql, params.toArray());
        return res;
    }

    public <T> int delete(Class<T> clazz, Object... ids) {

        StringBuilder sb = new StringBuilder("delete from ");
        sb.append(EormUtils.getTableName(clazz));
        sb.append(" where ");

        List<String> primaryKeyNames = EormUtils.getPrimaryKeyNames(clazz);
        for (int i = 0; i < primaryKeyNames.size(); i++) {
            if (i > 0) {
                sb.append(" and ");
            }
            sb.append("`");
            sb.append(primaryKeyNames.get(i));
            sb.append("` = ?");
        }

        String sql = sb.toString();
        int res = this.execute(sql, ids);
        return res;
    }

    public <T> T selectOne(Class<T> clazz, String sql, Object... args) {

        List<T> list = select(clazz, sql, args);
        if (null != list && !list.isEmpty()) {
            return list.get(0);
        }
        return null;
    }

    public <T> int batchInsert(List<T> list) {
        if (null == list || list.isEmpty()) {
            return 0;
        }

        // 取第一个样本
        T type = list.get(0);
        EormUtils.TableInfo table = EormUtils.getTableInfo(type);

        StringBuilder csb = new StringBuilder();
        List<EormUtils.Column> allColumns = table.getAllColumns();
        int columnSize = allColumns.size();
        for (int i = 0; i < columnSize; i++) {
            EormUtils.Column column = allColumns.get(i);
            if (i > 0) {
                csb.append(", ");
            }
            csb.append('`');
            csb.append(column.getName());
            csb.append('`');

        }

        List<Object> params = new ArrayList<Object>();

        StringBuilder vsb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                vsb.append(", ");
            }

            vsb.append("(");

            table = EormUtils.getTableInfo(list.get(i));

            if (columnSize != table.getAllColumns().size()) {
                throw new RuntimeException("column size difference ...");
            }

            for (int j = 0; j < columnSize; j++) {
                if (j > 0) {
                    vsb.append(", ");
                }
                vsb.append("?");

                params.add(table.getAllColumns().get(j).getValue());
            }

            vsb.append(")");

        }

        StringBuilder sb = new StringBuilder("insert into ");
        sb.append(table.getTableName());
        sb.append(" (");
        sb.append(csb);

        sb.append(") values ");
        sb.append(vsb);

        String sql = sb.toString();
        int res = this.execute(sql, params.toArray());
        return res;
    }

    public <T> PagingResult<T> list(Class<T> clazz, StringBuffer sql, Paging paging) {
        PagingResult<T> result = new PagingResult<T>(paging);

        Object[] objects = paging.getParams().toArray();
        // 不分页 start
        long rows = paging.getRows();
        if (0 != rows) {
            // 先查总数
            String totalSql = "select count(*) ";

            String tempSql = sql.toString().toLowerCase();
            totalSql += sql.substring(tempSql.indexOf("from"), sql.length());

            // fix group by
            if (0 < totalSql.indexOf(" group ")) {
                totalSql = "select count(*) from (" + totalSql + ") temp";
            }

            long total = this.selectOne(Long.class, totalSql, objects);

            paging.setTotal(total);
            if (0 == total) {
                return result;
            }
        }
        // 不分页 end

        // 排序
        if (paging.isSort()) {
            // 判断是否有排序字段
            String[] sorts = paging.getSorts();
            if (null != sorts && 0 < sorts.length) {
                int sortIndex = paging.getSortIndex();

                // 大于排序的长度默认最后一个
                if (sortIndex >= sorts.length) {
                    sortIndex = sorts.length - 1;
                }
                sql.append(" order by ");

                sql.append('`');
                sql.append(sorts[sortIndex]);
                sql.append('`');

                // 排序方式
                if (paging.isDesc()) {
                    sql.append(" desc");
                } else {
                    sql.append(" asc");
                }
            }
        }

        if (0 != rows) {
            // 分页
            sql.append(" limit ");
            sql.append(paging.getStart());
            sql.append(",");
            sql.append(rows);
        }

        List<T> list = select(clazz, sql.toString(), objects);
        result.setList(list);

        return result;
    }

    private List<Map> buildMap(ResultSet rs, ResultSetMetaData data) throws SQLException {

        List<Map> list = new ArrayList<Map>();

        int columnCount = data.getColumnCount();

        while (rs.next()) {

            Map map = new HashMap();
            for (int j = 1; j <= columnCount; j++) {
                String columnName = data.getColumnName(j);
                map.put(columnName, rs.getObject(columnName));
            }

            list.add(map);
        }
        return list;
    }

    public void doTx(Handler handler) {
        Connection conn = createConnection();
        try {
            conn.setAutoCommit(false);
            handler.doTx(new EormMySQLImpl(conn));
            conn.commit();
        } catch (Exception e) {
            try {
                if (null != conn) {
                    conn.rollback();
                }
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            e.printStackTrace();
        } finally {
            JdbcUtils.close(conn);
        }

    }
}
