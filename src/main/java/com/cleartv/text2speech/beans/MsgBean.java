package com.cleartv.text2speech.beans;

public class MsgBean {
    private String id;
    private String text;
    private String path;

    public MsgBean(String id, String path) {
        this.id = id;
        this.path = path;
    }

    public String getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public void setPath(String path) {
        this.path = path;
    }
}