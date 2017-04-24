package com.cleartv.text2speech;

import android.content.Context;
import android.graphics.Color;
import android.os.Environment;
import android.text.TextUtils;

import com.baidu.tts.auth.AuthInfo;
import com.baidu.tts.client.SpeechError;
import com.baidu.tts.client.SpeechSynthesizer;
import com.baidu.tts.client.SpeechSynthesizerListener;
import com.baidu.tts.client.TtsMode;
import com.cleartv.text2speech.beans.MsgBean;
import com.cleartv.text2speech.beans.RequestBean;
import com.cleartv.text2speech.beans.ResponseBean;
import com.cleartv.text2speech.server.NanoHTTPD;
import com.cleartv.text2speech.utils.Utils;

import java.util.ArrayList;

/**
 * Created by Lipengfei on 2017/4/20.
 */

public class SpeechManager implements SpeechSynthesizerListener {

    private static SpeechManager manager;
    // 语音合成客户端
    private SpeechSynthesizer mSpeechSynthesizer;
    public String mainDirPath;
    private String mediaPath;
    private static final String SAMPLE_DIR_NAME = "ClearTV_Voice";
    private static final String MEDIA_DIR_NAME = "media";
    private static final String SPEECH_FEMALE_MODEL_NAME = "bd_etts_ch_speech_female.dat";
    private static final String TEXT_MODEL_NAME = "bd_etts_ch_text.dat";

    public String responseStr;
    public NanoHTTPD.Response.Status responseStatus;
    public String responseBeanCode;
    public String responseBeanMsg;
    private ArrayList<MsgBean> responseBeanMsgs;
    public boolean isFinish = true;
    private int finishCount = 0;
    private int totalCount = 0;
    public boolean isAuthSuccess = false;

    private LogListener listener;

    private Context context;

    public static SpeechManager getInstance() {
        if(manager == null){
            manager = new SpeechManager();
        }
        return manager;
    }

    public void initialEnv(LogListener listener,Context context) {
        this.context = context;
        this.listener = listener;
        mainDirPath = Environment.getExternalStorageDirectory().toString() + "/" + SAMPLE_DIR_NAME;
        mediaPath = mainDirPath +"/"+ MEDIA_DIR_NAME;
        Utils.makeDir(mainDirPath);
        Utils.makeDir(mediaPath);
        Utils.copyFromAssetsToSdcard(false, SPEECH_FEMALE_MODEL_NAME, mainDirPath + "/" + SPEECH_FEMALE_MODEL_NAME,context);
        Utils.copyFromAssetsToSdcard(false, TEXT_MODEL_NAME, mainDirPath + "/" + TEXT_MODEL_NAME,context);
    }

    // 初始化语音合成客户端并启动
    public void startTTS() {
        listener.logInfo("初始化语音合成");
        // 获取语音合成对象实例
        mSpeechSynthesizer = SpeechSynthesizer.getInstance();
        // 设置context
        mSpeechSynthesizer.setContext(context);
        // 设置语音合成状态监听器
        mSpeechSynthesizer.setSpeechSynthesizerListener(this);
        // 设置在线语音合成授权，需要填入从百度语音官网申请的api_key和secret_key
        mSpeechSynthesizer.setApiKey("Gl8VYpbSR8RTKsqWGe8TfIBp", "5a2138f09d13d6e4ac8660471573b898");
        // 设置离线语音合成授权，需要填入从百度语音官网申请的app_id
        mSpeechSynthesizer.setAppId("9528663");

        // 设置语音合成声音模型文件
        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_TTS_TEXT_MODEL_FILE, mainDirPath + "/" + TEXT_MODEL_NAME);
        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_TTS_SPEECH_MODEL_FILE, mainDirPath + "/" + SPEECH_FEMALE_MODEL_NAME);

        //合成引擎速度优化等级，取值范围[0, 2]，值越大速度越快（离线引擎）
        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_VOCODER_OPTIM_LEVEL,"2");

        // 设置语音合成声音授权文件
//        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_TTS_LICENCE_FILE, "your_licence_path");

        // 获取语音合成授权信息
        AuthInfo authInfo = mSpeechSynthesizer.auth(TtsMode.MIX);
        // 判断授权信息是否正确，如果正确则初始化语音合成器并开始语音合成，如果失败则做错误处理
        isAuthSuccess = authInfo.isSuccess();
        if (isAuthSuccess) {
            mSpeechSynthesizer.initTts(TtsMode.MIX);
            listener.logInfo("volume:音量(0-9)");
            listener.logInfo("speed:语速(0-9)");
            listener.logInfo("pitch:声调(0-9)");
            listener.logInfo("speaker:音色(0.普通女声；1.普通男声；2.特别男声；3.情感男声；4.情感儿童声)");
            listener.logInfo("--------------------------语音合成启动认证成功-----------------------------------\n");
        } else {
            // 授权失败
            listener.logInfo("--------------------------语音合成认证授权失败--------------------------\n");
//            try {
//                Thread.sleep(3000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            startTTS();
        }
    }

    public void speakMsgByJson(String jsonMsg) throws Exception{
        listener.logInfo("-------------------------------StartSpeech-------------------------------------");
        isFinish = false;
        responseBeanMsgs = new ArrayList<>();
        responseBeanCode = "200";
        responseBeanMsg = "success";
        finishCount = 0;

        RequestBean requestBean = Utils.getBeanFromJson(jsonMsg,RequestBean.class);
        if(requestBean == null || requestBean.getFileList() == null || requestBean.getFileList().size()<1){
            isFinish = true;
            responseStatus = NanoHTTPD.Response.Status.BAD_REQUEST;
            responseStr = "Probably JsonError:\n"+jsonMsg;
            listener.logInfo("Probably JsonError:\n"+jsonMsg,Color.RED);
            listener.logInfo("-------------------------------FinishSpeech-------------------------------------",Color.RED);
            return;
        }

        totalCount = requestBean.getFileList().size();
        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_SPEED,requestBean.getSpeed());
        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_VOLUME,requestBean.getVolume());
        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_PITCH,requestBean.getPitch());
        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_SPEAKER, requestBean.getSpeaker());

        for(MsgBean msg : requestBean.getFileList()){
            if(msg!=null && !TextUtils.isEmpty(msg.getText()) && !TextUtils.isEmpty(msg.getId()))
                mSpeechSynthesizer.speak(msg.getText(), msg.getId());
            else
                finishSpeech();
//            msg.setPath(hostAddress+"/"+MEDIA_DIR_NAME+"/"+msg.getId()+".wav");
        }
//        responseStr = new Gson().toJson(new ResponseBean("200","success","wav",requestBean.getFileList()));
    }

    @Override
    public void onError(String arg0, SpeechError arg1) {
        // 监听到出错，在此添加相关操作
        listener.logInfo("Error:"+arg0, Color.RED);
        listener.logInfo("Error:"+arg1.description, Color.RED);
        responseBeanCode = "300";
        responseBeanMsg = "warm";
        finishSpeech();
    }

    @Override
    public void onSpeechFinish(String arg0) {
        // 监听到播放结束，在此添加相关操作
//        listener.logInfo("onSpeechFinish:"+arg0);
    }

    @Override
    public void onSpeechProgressChanged(String arg0, int arg1) {
        // 监听到播放进度有变化，在此添加相关操作
    }

    @Override
    public void onSpeechStart(String arg0) {
        // 监听到合成并播放开始，在此添加相关操作
//        listener.logInfo("onSpeechStart:"+arg0);
    }

    @Override
    public void onSynthesizeStart(String utteranceId) {
        // 监听到合成开始，在此添加相关操作
        Utils.prepareLocalfile(utteranceId,mediaPath);
        listener.logInfo("Start：" + utteranceId + ".wav",Color.GREEN);
    }

    @Override
    public void onSynthesizeDataArrived(final String utteranceId, final byte[] data, int progress) {
        if (null != data) {
            Utils.appendLocalfileSection(data);
        }

    }

    @Override
    public void onSynthesizeFinish(String utteranceId) {
        // 监听到合成结束，在此添加相关操作
        Utils.endLocalFileData(utteranceId,mediaPath);
        responseBeanMsgs.add(new MsgBean(utteranceId,MainActivity.ipAddress+"/"+MEDIA_DIR_NAME+"/"+utteranceId+".wav"));
        listener.logInfo("Finish：" + utteranceId + ".wav",Color.GREEN);
        finishSpeech();
    }

    private void finishSpeech(){
        finishCount++;
        if(finishCount>=totalCount){
            finishAllSpeech();
        }
    }

    private void finishAllSpeech(){
        isFinish = true;
        listener.logInfo("-------------------------------FinishSpeech-------------------------------------");
        responseStatus = NanoHTTPD.Response.Status.OK;
        responseStr = Utils.getJsonFromBean(new ResponseBean(responseBeanCode, responseBeanMsg,"wav", responseBeanMsgs));
    }

}
