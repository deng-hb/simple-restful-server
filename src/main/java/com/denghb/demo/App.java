package com.denghb.demo;

import com.denghb.restful.Application;
import com.denghb.restful.annotation.*;
import com.denghb.restful.utils.LogUtils;

/**
 * Hello world!
 */
@RESTful
public class App {


    public static void main(String[] args) {
        Application.run(App.class, args);
    }

    @GET("/xx")
    String xx() {
        return "XX";
    }

    @GET("/xx/{aa}/b{bb}")
    String xx2(@PathVariable("aa") String aa, @PathVariable("bb") String bb) {
        return "XX:" + aa + ":" + bb;
    }

    @GET("/test/header")
    String header(@RequestHeader("Host") String host, @RequestHeader("User-Agent") String userAgent) {

        return String.format("Host:%s\nUser-Agent:%s", host, userAgent);
    }

    // 处理异常
    @ExceptionHandler(throwable = Exception.class)
    void error(Exception e) {
        LogUtils.error(getClass(), e.getMessage(), e);
    }
}
