package com.denghb.restful.annotation;


import java.lang.annotation.*;

/**
 * 过滤器
 */
@Target(value = {ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Filter {
    String value() default "/**";

    Class[] method() default {GET.class, POST.class, DELETE.class};
}