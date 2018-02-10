package com.denghb.demo.model;

public class JSONModel<T> {

    private int code = 1;

    private String msg = "";

    private T data;

    public static <T> JSONModel buildSuccess(String msg) {
        JSONModel model = new JSONModel();
        model.msg = msg;
        return model;
    }
    public static <T> JSONModel buildSuccess(String msg, T data) {
        JSONModel model = new JSONModel();
        model.msg = msg;
        model.data = data;

        return model;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

}
