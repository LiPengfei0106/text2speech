package com.cleartv.text2speech.beans;

import java.util.ArrayList;

/**
 * Created by Lipengfei on 2017/4/20.
 */

public class RequestBean {

    public String getAction() {
        return action;
    }

    public String getVolume() {
        return volume;
    }

    public String getSpeed() {
        return speed;
    }

    public String getPitch() {
        return pitch;
    }

    public String getSpeaker() {
          return speaker;
    }

    public ArrayList<MsgBean> getFileList() {
        return fileList;
    }

    private String action;
    private String volume;
    private String speed;
    private String pitch;
    private String speaker;
    private ArrayList<MsgBean> fileList;

}
