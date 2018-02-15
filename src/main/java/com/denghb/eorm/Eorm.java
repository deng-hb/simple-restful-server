package com.denghb.eorm;

import com.denghb.eorm.domain.Paging;
import com.denghb.eorm.domain.PagingResult;

import java.util.List;

public interface Eorm {

    public int execute(String sql, Object... args);

    public <T> List<T> select(Class<T> clazz, String sql, Object... args);

    public <T> int insert(T domain);

    public <T> int update(T domain);

    public <T> int delete(T domain);

    public <T> int delete(Class<T> clazz, Object... ids);

    public <T> T selectOne(Class<T> clazz, String sql, Object... args);

    public <T> int batchInsert(List<T> list);

    public <T> PagingResult<T> list(Class<T> clazz, StringBuffer sql, Paging paging);

    public interface Handler {

        public void doTx(Eorm eorm);
    }

    public void doTx(Handler handler);
}
