package com.cleartv.text2speech.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import com.cleartv.text2speech.BaseApplication;
import com.google.gson.Gson;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;

/**
 * Created by Lipengfei on 2017/4/18.
 */

public class Utils {

    public static String TAG = "Utils";
    private static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    public static <T> T getBeanFromJson(String json,Class<T> cls){
        try {
            return new Gson().fromJson(json,cls);
        }catch (Exception e){
            return null;
        }
    }

    public static String getJsonFromBean(Object obj){
        try {
            return new Gson().toJson(obj);
        }catch (Exception e){
            return null;
        }
    }

    public static String getTime() {
        long time = System.currentTimeMillis();
        return format.format(new Date(time));
    }

    public static String getLocalIPAddres() {

        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && (inetAddress instanceof Inet4Address)) {
                        return inetAddress.getHostAddress().toString();
                    }
                }
            }
        } catch (SocketException e) {

        }

        return "0.0.0.0";
    }

    public static void playAudioTrack(String audioPath, String utteranceId) throws IOException {
        int frequence = 16000;
        int channelConfig = AudioFormat.CHANNEL_OUT_MONO;
        int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
        int streamType = AudioManager.STREAM_MUSIC;
        int bufferSize = AudioTrack.getMinBufferSize(frequence, channelConfig, audioEncoding);
        byte[] buffer = new byte[bufferSize / 4];
        File file = new File(audioPath, utteranceId + ".pcm");
        FileInputStream is = new FileInputStream(file);
        DataInputStream dis = new DataInputStream(new BufferedInputStream(is));
        AudioTrack track = new AudioTrack(streamType, frequence, channelConfig, audioEncoding, bufferSize,
                AudioTrack.MODE_STREAM);
        track.setPlaybackRate(frequence);
        /* start play */
        track.setStereoVolume(1.0f, 1.0f);
        track.play();
        while (dis.available() > 0) {
            int i = 0;
            while (dis.available() > 0 && i < buffer.length) {
                buffer[i] = dis.readByte();
                i++;
            }
            /*write data to AudioTrack*/
            track.write(buffer, 0, buffer.length);
        }
        /*stop play*/
        track.stop();
        dis.close();
    }

    public static void convertAudioFiles(String src, String target) throws Exception {
        FileInputStream fis = new FileInputStream(src);
        FileOutputStream fos = new FileOutputStream(target);

        //计算长度
        byte[] buf = new byte[1024 * 4];
        int size = fis.read(buf);
        int PCMSize = 0;
        while (size != -1) {
            PCMSize += size;
            size = fis.read(buf);
        }
        fis.close();

        //填入参数，比特率等等。这里用的是16位单声道 16000 hz
        WaveHeader header = new WaveHeader();
        //长度字段 = 内容的大小（PCMSize) + 头部字段的大小(不包括前面4字节的标识符RIFF以及fileLength本身的4字节)
        header.fileLength = PCMSize + (44 - 8);
        header.FmtHdrLeth = 16;
        header.BitsPerSample = 16;
        header.Channels = 1;
        header.FormatTag = 0x0001;
        header.SamplesPerSec = 16000;
        header.BlockAlign = (short) (header.Channels * header.BitsPerSample / 8);
        header.AvgBytesPerSec = header.BlockAlign * header.SamplesPerSec;
        header.DataHdrLeth = PCMSize;

        byte[] h = header.getHeader();

        assert h.length == 44; //WAV标准，头部应该是44字节
        //write header
        fos.write(h, 0, h.length);
        //write data stream
        fis = new FileInputStream(src);
        size = fis.read(buf);
        while (size != -1) {
            fos.write(buf, 0, size);
            size = fis.read(buf);
        }
        fis.close();
        fos.close();
        System.out.println("Convert OK!");
    }

    public static void deleteFile(String s) {
        File file = new File(s);
        if (file.exists()) {
            file.delete();
        }
    }

    public static void reStartApp() {
        Intent intent = BaseApplication.application.getPackageManager()
                .getLaunchIntentForPackage(BaseApplication.application.getPackageName());
        PendingIntent restartIntent = PendingIntent
                .getActivity(BaseApplication.application, 0, intent,
                        PendingIntent.FLAG_ONE_SHOT);
        AlarmManager mgr = (AlarmManager) BaseApplication.application.getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 50,
                restartIntent);
        System.exit(0);
    }

    public static boolean doReboot() {
        String uri = "http://127.0.0.1:19003/index.html?op=reboot";
        Log.i(TAG, "do reboot ");
        try {
            URL url = new URL(uri);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Content-Type", "text/xml");
            conn.setRequestProperty("charset", "utf-8");
            conn.setConnectTimeout(10000);
            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                return true;
            } else {
                Log.e(TAG, "response code=" + conn.getResponseCode());
            }
        } catch (Exception e) {
            Log.e(TAG, "[" + Log.getStackTraceString(e) + "]");
        }
        return false;
    }

    public static void startAppByPackageName(Context ctx, String packageNmae) {
        try {
            Intent intent = ctx.getPackageManager().getLaunchIntentForPackage(packageNmae);
            ctx.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "no such package:" + packageNmae);
        }
    }

    public static void makeDir(String dirPath) {
        File file = new File(dirPath);
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    public static void copyFromAssetsToSdcard(boolean isCover, String source, String dest , Context context) {
        File file = new File(dest);
        if (isCover || (!isCover && !file.exists())) {
            InputStream is = null;
            FileOutputStream fos = null;
            try {
                is = context.getResources().getAssets().open(source);
                String path = dest;
                fos = new FileOutputStream(path);
                byte[] buffer = new byte[1024];
                int size = 0;
                while ((size = is.read(buffer, 0, 1024)) >= 0) {
                    fos.write(buffer, 0, size);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    if (is != null) {
                        is.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static BufferedOutputStream Buff;

    public static void prepareLocalfile(String fileName, String path) {
        File folder = new File(path);
        if (!folder.exists()) {/* 判断文件夹是否存在（不存在则创建这个文件夹） */
            folder.mkdirs();/* 创建文件夹 */
        }

        try {
            File file = new File(path, fileName);
            FileOutputStream outSTr = new FileOutputStream(file);
            Buff = new BufferedOutputStream(outSTr);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void appendLocalfileSection(byte[] buffer) {
        try {
            Buff.write(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void endLocalFileData(String fileName, String path) {
        try {
            Buff.flush();
            Buff.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            Utils.convertAudioFiles(path + "/" + fileName, path + "/" + fileName + ".wav");
            Utils.deleteFile(path + "/" + fileName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    WavHeader辅助类。用于生成头部信息。

    public static class WaveHeader {
        public final char fileID[] = {'R', 'I', 'F', 'F'};
        public int fileLength;
        public char wavTag[] = {'W', 'A', 'V', 'E'};
        ;
        public char FmtHdrID[] = {'f', 'm', 't', ' '};
        public int FmtHdrLeth;
        public short FormatTag;
        public short Channels;
        public int SamplesPerSec;
        public int AvgBytesPerSec;
        public short BlockAlign;
        public short BitsPerSample;
        public char DataHdrID[] = {'d', 'a', 't', 'a'};
        public int DataHdrLeth;

        public byte[] getHeader() throws IOException {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            WriteChar(bos, fileID);
            WriteInt(bos, fileLength);
            WriteChar(bos, wavTag);
            WriteChar(bos, FmtHdrID);
            WriteInt(bos, FmtHdrLeth);
            WriteShort(bos, FormatTag);
            WriteShort(bos, Channels);
            WriteInt(bos, SamplesPerSec);
            WriteInt(bos, AvgBytesPerSec);
            WriteShort(bos, BlockAlign);
            WriteShort(bos, BitsPerSample);
            WriteChar(bos, DataHdrID);
            WriteInt(bos, DataHdrLeth);
            bos.flush();
            byte[] r = bos.toByteArray();
            bos.close();
            return r;
        }

        private void WriteShort(ByteArrayOutputStream bos, int s) throws IOException {
            byte[] mybyte = new byte[2];
            mybyte[1] = (byte) ((s << 16) >> 24);
            mybyte[0] = (byte) ((s << 24) >> 24);
            bos.write(mybyte);
        }


        private void WriteInt(ByteArrayOutputStream bos, int n) throws IOException {
            byte[] buf = new byte[4];
            buf[3] = (byte) (n >> 24);
            buf[2] = (byte) ((n << 8) >> 24);
            buf[1] = (byte) ((n << 16) >> 24);
            buf[0] = (byte) ((n << 24) >> 24);
            bos.write(buf);
        }

        private void WriteChar(ByteArrayOutputStream bos, char[] id) {
            for (int i = 0; i < id.length; i++) {
                char c = id[i];
                bos.write(c);
            }
        }
    }
}
