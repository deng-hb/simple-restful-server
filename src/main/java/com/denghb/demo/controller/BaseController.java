package com.denghb.demo.controller;

import com.denghb.demo.MyException;
import com.denghb.demo.model.JSONModel;
import com.denghb.restful.RESTfulException;
import com.denghb.restful.annotation.Error;
import com.denghb.restful.annotation.*;
import com.denghb.restful.utils.LogUtils;

@RESTful
public class BaseController {


    /**
     * URL访问过滤
     */
    @Filter
    void filter(@RequestParameter("Token") String token) {

        System.out.println(token);
    }

    /**
     * URL访问过滤
     */
    @Filter(value = "/user")
    JSONModel filter2(@RequestHeader("Token") String token) {

        return null;// 放行
    }

    /**
     * 处理异常
     */
    @Error
    void error(Exception e) {
        LogUtils.error(getClass(), e.getMessage(), e);
    }

    // 异常返回 字符串
    @Error(throwable = MyException.class)
    String error2(MyException e) {
        LogUtils.error(getClass(), e.getMessage(), e);
        return "error2";
    }

    @Error(throwable = RESTfulException.class)
    void error3(RESTfulException e) {
        System.err.println(e.getCode());
        LogUtils.error(getClass(), e.getMessage(), e);
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
