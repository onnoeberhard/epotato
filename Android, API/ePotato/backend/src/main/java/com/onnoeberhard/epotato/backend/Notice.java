package com.onnoeberhard.epotato.backend;

@SuppressWarnings("unused")
class Notice {

    static int ERROR = 2;
    static int OTHER = 3;
    static int NULL = 4;
    private static int OK = 1;
    private String message = "";
    private int code = 0;

    Notice() {
    }

    Notice(boolean ok) {
        setOk(ok);
    }

    Notice(String message) {
        setMessage(message);
    }

    Notice(int code) {
        setCode(code);
    }

    public boolean isOk() {
        return code == OK;
    }

    public void setOk(boolean ok) {
        this.code = ok ? OK : ERROR;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    private void setCode(int code) {
        this.code = code;
    }

}