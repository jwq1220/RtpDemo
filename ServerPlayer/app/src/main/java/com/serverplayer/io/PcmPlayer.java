package com.serverplayer.io;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class PcmPlayer implements Runnable {
    private Logger log = LoggerFactory.getLogger(PcmPlayer.class);
    private final Object mutex = new Object();
    private volatile boolean isRecording;
    private List<PcmPlayer.rawData> list;
    private PcmPlayer.rawData rawData;
    private AudioTrack trackPlayer = null;
    private static final int sampleRate = 16000;
    private static final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;

    /**
     * 初始化
     * */
    public PcmPlayer(){
        super();
        log.debug("PcmPlayer init!");
        list = Collections.synchronizedList(new LinkedList<PcmPlayer.rawData>());
    }

    /**
     * 初始化音频
     * */
    public void  AudioInit(){
        log.debug("PcmPlayer AudioTrack  init!");
        int bufSize = AudioTrack.getMinBufferSize(sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                audioEncoding);

        trackPlayer = new AudioTrack(AudioManager.STREAM_MUSIC,
                                                sampleRate,
                                                AudioFormat.CHANNEL_OUT_MONO,
                                                audioEncoding,
                                                bufSize,
                                                AudioTrack.MODE_STREAM);

        trackPlayer.play() ;
    }

    @Override
    public void run() {
        log.debug("PcmPlayer thread runing");
        AudioInit();
        while (this.isRecording()) {
            if (list.size() > 0) {
                rawData = list.remove(0);
                trackPlayer.write(rawData.pcm,0,rawData.size);
            } else {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        stop();
    }

    /**
     * 暂停播放
     * */
    public void stop() {
        log.debug("PcmPlayer thread stop!" +
                  " isRecording:" + isRecording);
        trackPlayer.stop();
        trackPlayer.release();
    }

    /**
     * 将音频数据添加到播放列表中
     * */
    public void putData(byte[] buf, int size) {
        PcmPlayer.rawData data = new PcmPlayer.rawData();
        data.size =size ;
        data.init();
        System.arraycopy(buf, 0, data.pcm, 0, size);
        list.add(data);
    }

    /**
     * 设置播放状态
     * */
    public void setRecording(boolean isRecording) {
        synchronized (mutex) {
            this.isRecording = isRecording;
            if (this.isRecording) {
                mutex.notify();
            }
        }
    }

    /**
     * 判断播放状态
     * */
    public boolean isRecording() {
        synchronized (mutex) {
            return isRecording;
        }
    }

    /**
     * 音频数据存储类
     * size 实际数据大小
     * pcm 存储实际音频原始数据
     * */
    class rawData {
        int size;
        byte[] pcm = null;
        protected void init(){
            if (size < 0 ) {
                pcm = new byte[2048];
            }else {
                pcm = new byte[size];
            }
        }
    }
}
