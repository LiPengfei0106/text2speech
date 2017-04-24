package com.cleartv.text2speech.beans;

import java.util.ArrayList;

/**
 * Created by Lipengfei on 2017/4/20.
 */

public class ResponseBean {

    private String rescode;
    private String errInfo;
    private String fileformate;
    private ArrayList<MsgBean> fileList;

    public ResponseBean(String rescode, String errInfo, String fileformate, ArrayList<MsgBean> fileList) {
        this.rescode = rescode;
        this.errInfo = errInfo;
        this.fileformate = fileformate;
        this.fileList = fileList;
    }
}
