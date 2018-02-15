package com.denghb.demo.controller;

import com.denghb.demo.domain.User;
import com.denghb.demo.model.JSONModel;
import com.denghb.eorm.Eorm;
import com.denghb.restful.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RESTful
public class UserController {

    static Map<Long, User> data = new ConcurrentHashMap<Long, User>();
    static long id = 1;

    static {
        User user = new User();
        user.setId(id);
        user.setName("Jack Ma");
        user.setMobile("13838383838");
        data.put(1L, user);
    }

    @GET("/")
    String home(@RequestParameter("Token") String token, Eorm eorm) {
        System.out.println(token);
        final User[] user = {null};
        eorm.doTx(new Eorm.Handler() {
            public void doTx(Eorm eorm) {

                user[0] = new User();
                user[0].setName("张三");
                user[0].setMobile("1233453453");
                eorm.insert(user[0]);

                throw new RuntimeException("sss");
            }
        });
        return "Hello World" + user[0].getId();
    }

    @GET("/user")
    JSONModel<List<User>> list() {

        List<User> list = new ArrayList<User>();
        for (Long id : data.keySet()) {
            list.add(data.get(id));
        }

        return JSONModel.buildSuccess("ok", list);
    }

    @GET("/userList")
    List<User> list2() {

        List<User> list = new ArrayList<User>();
        for (Long id : data.keySet()) {
            list.add(data.get(id));
        }

        return list;
    }

    @POST("/user")
    JSONModel create(@RequestBody User user) {
        id = id + 1;
        user.setId(id);
        data.put(id, user);

        return JSONModel.buildSuccess("OK");
    }

    @DELETE("/user/{id}")
    JSONModel create(@PathVariable("id") Long id) {
        if (data.containsKey(id)) {
            data.remove(id);
            return JSONModel.buildSuccess("OK");
        }

        return JSONModel.buildFailure("用户不存在");
    }

    @GET("/user/{id}")
    JSONModel<User> query(@PathVariable("id") Long id) {
        User user = data.get(id);
        return JSONModel.buildSuccess("ok", user);
    }

    @GET("/user2")
    JSONModel<User> query2(@RequestParameter("id") Long id) {
        User user = data.get(id);
        return JSONModel.buildSuccess("ok", user);
    }

    @POST("/user2")
    JSONModel create(@RequestParameter("name") String name, @RequestParameter("mobile") String mobile) {
        id = id + 1;
        User user = new User();
        user.setName(name);
        user.setMobile(mobile);
        user.setId(id);
        data.put(id, user);

        return JSONModel.buildSuccess("OK");
    }
}
