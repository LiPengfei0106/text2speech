package com.cleartv.text2speech.server;

import android.graphics.Color;
import android.os.Handler;
import android.util.Log;

import com.cleartv.text2speech.LogListener;
import com.cleartv.text2speech.SpeechManager;
import com.cleartv.text2speech.utils.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class VodServer extends NanoHTTPD {
    public final static String TAG = "VodServer";
    public static int port = 19000;

    private LogListener listener;

    private Handler mHandler = new Handler();
    private Runnable startRunnable = new Runnable() {

        @Override
        public void run() {
            Log.i(TAG, "port:" + port);
            mLocalServer = new VodServer(port);
            mLocalServer.startWork();
        }
    };

    private static VodServer mLocalServer = null;

    public static VodServer instance() {
        if (mLocalServer == null) {
            mLocalServer = new VodServer(port);
        }
        return mLocalServer;
    }

    public void setListener(LogListener listener){
        this.listener = listener;
    }

    public void startWork() {
        try {
            start();
        } catch (Exception e) {
            listener.logInfo("本地服务器启动失败:" + e.getMessage(), Color.YELLOW);
            listener.logInfo("3秒后重启...", Color.YELLOW);
            Log.e(TAG, "[" + e.getMessage() + "]");
            mHandler.postDelayed(startRunnable, 3000);
        }
    }

    public void stopWork() {
        mLocalServer.stop();
    }

    public VodServer(int port) {
        super(port);
    }

    @Override
    public Response serve(IHTTPSession session) {
        Map<String, String> files = new HashMap<String, String>();
        String body = null;
        String mimeType = "text/html";
        Response.Status status = Response.Status.OK;
        String responseStr = "";
        /*获取header信息，NanoHttp的header不仅仅是HTTP的header，还包括其他信息。*/
        Map<String, String> headers = session.getHeaders();
		 /*我在这里做了一个限制，只接受POST请求。这个是项目需求。*/
        listener.logInfo("-------------------------"+session.getMethod().name()+"----------------------------");
        listener.logInfo("HEAD:\n"+session.getHeaders().toString());
        if (Method.POST.equals(session.getMethod())) {
            synchronized(this) {
                try {
                /*这句尤为重要就是将将body的数据写入files中，大家可以看看parseBody具体实现，倒现在我也不明白为啥这样写。*/
                    session.parseBody(files);
                /*看就是这里，POST请教的body数据可以完整读出*/
                    body = session.getQueryParameterString();
                    listener.logInfo("BODY:\n"+body);
                    while (!SpeechManager.getInstance().isFinish) {
                        Thread.sleep(200);
                    }
                    SpeechManager.getInstance().speakMsgByJson(body);
                    while (!SpeechManager.getInstance().isFinish) {
                        Thread.sleep(200);
                    }
                    mimeType = "text/html";
                    status = SpeechManager.getInstance().responseStatus;
                    responseStr = SpeechManager.getInstance().responseStr;
                } catch (Exception e) {
                    e.printStackTrace();
                    mimeType = "text/html";
                    status = Response.Status.INTERNAL_ERROR;
                    responseStr = "500\n" + e + "\nbody:\n" + body;
                }
            }
        } else {
            listener.logInfo("URI:"+session.getUri());
            if (session.getUri().toLowerCase().contains("reboot")) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Utils.doReboot();
                    }
                }).start();
                mimeType = "text/html";
                status = Response.Status.OK;
                responseStr = "Reboot......";
            }else if (session.getUri().toLowerCase().contains("restart")) {
                Utils.reStartApp();
                mimeType = "text/html";
                status = Response.Status.OK;
                responseStr = "Restart......";
            }else{
                String filePath = SpeechManager.getInstance().mainDirPath + session.getUri();
                if(!new File(filePath).exists()){
                    mimeType = "text/html";
                    status = Response.Status.NOT_FOUND;
                    responseStr = "File not found:"+filePath;
                }else{
                    mimeType = "application/octet-stream";
                    if (filePath.endsWith(".mp4")) {
                        mimeType = "video/mp4";
                    }
                    String range = null;
                    Log.d(TAG, "Request headers:");
                    for (String key : headers.keySet()) {
                        Log.d(TAG, "  " + key + ":" + headers.get(key));
                        if ("range".equals(key)) {
                            range = headers.get(key);
                        }
                    }
                    try {
                        if (range == null) {
                            return getFullResponse(mimeType, filePath);
                        } else {
                            return getPartialResponse(mimeType, filePath, range);
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Exception serving file: " + filePath, e);
                        mimeType = "text/html";
                        status = Response.Status.INTERNAL_ERROR;
                        responseStr = "Exception serving file:"+filePath + "\n ERROR: " +e.getLocalizedMessage();
                    }
                }
            }
        }
        listener.logInfo("Response status:\n" + status.name(),status.compareTo(Response.Status.OK) == 0 ? Color.BLUE : Color.RED);
        listener.logInfo("Response body:\n" + responseStr,status.compareTo(Response.Status.OK) == 0 ? Color.BLUE : Color.RED);
        listener.logInfo("------------------------Response "+status+"-------------------------------\n");
        return new Response(status, mimeType, responseStr);
    }

    private Response getFullResponse(String mimeType, String filePath) throws FileNotFoundException {
        FileInputStream fileInputStream = new FileInputStream(filePath);
        listener.logInfo("Response status:\n" + Response.Status.OK.name());
        listener.logInfo("Response body:\n FileInputStream :" + filePath);
        listener.logInfo("------------------------Response "+Response.Status.OK+"-------------------------------\n");
        return new Response(Response.Status.OK, mimeType, fileInputStream);
    }

    private Response getPartialResponse(String mimeType, String filePath, String rangeHeader) throws IOException {
        File file = new File(filePath);
        String rangeValue = rangeHeader.trim().substring("bytes=".length());
        long fileLength = file.length();
        long start, end;
        if (rangeValue.startsWith("-")) {
            end = fileLength - 1;
            start = fileLength - 1
                    - Long.parseLong(rangeValue.substring("-".length()));
        } else {
            String[] range = rangeValue.split("-");
            start = Long.parseLong(range[0]);
            end = range.length > 1 ? Long.parseLong(range[1])
                    : fileLength - 1;
        }
        if (end > fileLength - 1) {
            end = fileLength - 1;
        }
        if (start <= end) {
            long contentLength = end - start + 1;
            FileInputStream fileInputStream = new FileInputStream(file);
            //noinspection ResultOfMethodCallIgnored
            fileInputStream.skip(start);
            Response response = new Response(Response.Status.PARTIAL_CONTENT, mimeType, fileInputStream);
            response.addHeader("Content-Length", contentLength + "");
            response.addHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileLength);
            response.addHeader("Content-Type", mimeType);
            listener.logInfo("------------------------Response "+Response.Status.PARTIAL_CONTENT+"-------------------------------\n");
            return response;
        } else {
            listener.logInfo("------------------------Response "+Response.Status.RANGE_NOT_SATISFIABLE+"-------------------------------\n");
            return new Response(Response.Status.RANGE_NOT_SATISFIABLE, "text/html", rangeHeader);
        }
    }

}