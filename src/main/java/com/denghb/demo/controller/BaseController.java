package com.denghb.demo.controller;

import com.denghb.demo.MyException;
import com.denghb.demo.model.JSONModel;
import com.denghb.forest.ForestException;
import com.denghb.forest.annotation.*;
import com.denghb.forest.server.Request;

@RESTful
public class BaseController {


    /**
     * URL访问过滤
     */
    @Filter
    void before(@RequestHeader("User-Agent") String userAgent, @RequestParameter("Token") String token,
            Request request)

    {
        System.out.println("filter start");
        System.out.println(token);
        System.out.println(request);

        // 改变请求参数
        request.getParameters().put("Token", "changeToken");
        System.out.println(userAgent);
        System.out.println("filter end");

    }

    /**
     * URL访问过滤
     */
    @Filter(value = "/user")
    JSONModel before(@RequestHeader("Token") String token) {
        System.out.println("Test Filter");
        if (null == token) {
            return JSONModel.buildFailure("请登录");

        }
        return null;// 放行
    }

    /**
     * 处理异常
     */
    @ExceptionHandler
    void error(Exception e) {
        e.printStackTrace();
    }

    // 异常返回 字符串
    @ExceptionHandler(throwable = MyException.class)
    String error2(MyException e) {
        e.printStackTrace();
        return "error2";
    }

    @ExceptionHandler(throwable = ForestException.class)
    void error3(ForestException e) {
        System.err.println(e.getCode());
        e.printStackTrace();
    }


    @GET("/error")
    void outError() throws Exception {
        throw new Exception("error");
    }

    @GET("/error2")
    void outError2() throws Exception {
        throw new MyException("my error");
    }

}
