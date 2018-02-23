package com.denghb.demo;

import com.denghb.eorm.Eorm;
import com.denghb.eorm.domain.Paging;
import com.denghb.eorm.domain.PagingResult;
import com.denghb.eorm.impl.EormAbstractImpl;
import com.denghb.eorm.utils.EormUtils;
import com.denghb.utils.ReflectUtils;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public class EormSQLiteImpl extends EormAbstractImpl implements Eorm {

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public EormSQLiteImpl(Connection connection) {
        super(connection);
    }

    public EormSQLiteImpl(String url, String username, String password) {
        super(url, username, password);
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
                    Object value = selectOne(field.getType(), "select last_insert_rowid() as id");
                    ReflectUtils.setFieldValue(field, domain, value);
                }
            }
        }

        return res;

    }

    public <T> int batchInsert(List<T> list) {
        throw new RuntimeException("未实现");
    }

    public <T> PagingResult<T> list(Class<T> clazz, StringBuffer sql, Paging paging) {
        throw new RuntimeException("未实现");
    }

    public void doTx(Handler handler) {
        throw new RuntimeException("未实现");
    }
}
