package com.aven.avenclipboard.model;


public class WebResponse {
    private int code;
    private Object msg;

    public WebResponse(int code, Object msg) {
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public Object getMsg() {
        return msg;
    }

    public void setMsg(Object msg) {
        this.msg = msg;
    }
}
