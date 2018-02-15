package com.denghb.restful;

/**
 * 异常
 */
public class RESTfulException extends RuntimeException {

    private int code;

    public RESTfulException(String message, int code) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }
}