package com.denghb.demo;

import com.denghb.restful.Application;
import com.denghb.restful.annotation.GET;
import com.denghb.restful.annotation.PathVariable;
import com.denghb.restful.annotation.RESTful;

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
}
