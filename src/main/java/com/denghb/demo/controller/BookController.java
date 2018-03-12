package com.denghb.demo.controller;

import com.denghb.demo.domain.Book;
import com.denghb.eorm.Eorm;
import com.denghb.forest.annotation.*;

import java.util.Date;
import java.util.List;

@RESTful("/book")
public class BookController {


    // 测试用
    @GET("/init")
    String init(Eorm eorm) {

        String ddl = "CREATE TABLE book (\n" +
                "    id           INTEGER         PRIMARY KEY AUTOINCREMENT,\n" +
                "    name         TEXT            NOT NULL,\n" +
                "    price        DECIMAL (10, 2),\n" +
                "    created_time DATETIME,\n" +
                "    updated_time DATETIME\n" +
                ")";
        eorm.execute(ddl);
        return "ok";
    }

    @GET
    List<Book> list(Eorm eorm) {

        List<Book> list = eorm.select(Book.class, "select * from book");

        return list;
    }

    @PUT
    String update(@RequestBody Book book, Eorm eorm) {

        book.setUpdatedTime(new Date());
        eorm.update(book);

        return "ok";
    }

    @POST
    String create(@RequestBody Book book, Eorm eorm) {
        book.setCreatedTime(new Date());
        eorm.insert(book);
        return "ok";
    }

    @DELETE("/{id}")
    String delete(@PathVariable("id") Long id, Eorm eorm) {

        eorm.delete(Book.class, id);
        return "ok";
    }

    @GET("/{id}")
    Book query(@PathVariable("id") Long id, Eorm eorm) {
        Book book = eorm.selectOne(Book.class, "select * from book where id = ?", id);
        return book;
    }
}
