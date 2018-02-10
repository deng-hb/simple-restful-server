package com.denghb.demo.controller;

import com.denghb.demo.domain.User;
import com.denghb.demo.model.JSONModel;
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
    String home() {
        return "Hello World";
    }

    @GET("/user")
    JSONModel<List<User>> list() {

        List<User> list = new ArrayList<User>();
        for (Long id : data.keySet()) {
            list.add(data.get(id));
        }

        return JSONModel.buildSuccess("ok", list);
    }

    @POST("/user")
    JSONModel create(User user) {
        id = id + 1;
        user.setId(id);
        data.put(id, user);

        return JSONModel.buildSuccess("OK");
    }

    @GET("/user/{id}")
    JSONModel<User> query(@PathVariable("id") Long id) {
        User user = data.get(id);
        return JSONModel.buildSuccess("ok", user);
    }

    @GET("/user2")
    JSONModel<User> query2(@ParameterName("id") Long id) {
        User user = data.get(id);
        return JSONModel.buildSuccess("ok", user);
    }

    @POST("/user2")
    JSONModel create(@ParameterName("name") String name, @ParameterName("mobile") String mobile) {
        id = id + 1;
        User user = new User();
        user.setName(name);
        user.setMobile(mobile);
        user.setId(id);
        data.put(id, user);

        return JSONModel.buildSuccess("OK");
    }
}
