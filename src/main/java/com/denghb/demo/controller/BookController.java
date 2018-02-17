package com.denghb.demo.controller;

import com.denghb.demo.domain.Book;
import com.denghb.restful.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RESTful("/book")
public class BookController {

    static Map<Long, Book> data = new ConcurrentHashMap();

    static {
        Book book = new Book();
        book.setId(1L);
        book.setName("语文");
        book.setPrice(100);
        data.put(1L, book);
    }


    @GET
    List<Book> list() {

        List<Book> list = new ArrayList<Book>();
        for (Long key : data.keySet()) {
            list.add(data.get(key));
        }

        return list;
    }

    @PUT
    String update(@RequestBody Book book) {
        data.put(book.getId(), book);

        return "ok";
    }

    @POST
    String create(@RequestBody Book book) {
        data.put(book.getId(), book);

        return "ok";
    }

    @DELETE("/{id}")
    String delete(@PathVariable("id") Long id) {
        data.remove(id);

        return "ok";
    }

    @GET("/{id}")
    Book query(@PathVariable("id") Long id) {
        return data.remove(id);
    }
}
