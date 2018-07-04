package com.rtprecord.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.rtprecord.io.Consumer;
import com.rtprecord.io.PcmPlayer;
import com.rtprecord.io.PcmRecorder;
import com.rtprecord.io.PcmWriter;
import com.rtprecord.rtp.InitSession;

public class ServerManager implements Runnable, Consumer {
    private Logger log = LoggerFactory.getLogger(ServerManager.class);
    private final Object mutex = new Object();
    public static final int CLIENT = 1;
    public static final int SEVER = 2;
    private int mode = CLIENT;
    private volatile boolean isRecording;
    private volatile boolean isRunning;
    private processedData pData;
    private List<processedData> list;
    private PcmRecorder recorder = null;
    private int localRtpPort = 8082;
    private int localRtcpPort = 8083;
    private int destRtpPort = 8084;
    private int destRtcpPort = 8085;
    private String destNetworkAddress = "127.0.0.1";
    private InitSession session = null;
    private PcmWriter pcmWriter = null;
    private PcmPlayer pcmPlayer = null;
    private String localIp = "";

    /**
     *初始化RTPRecord模块
     * localRtpPort  本地rtp端口
     * localRtcpPort 本地rtcp端口
     * destRtpPort   目标rtp端口
     * destRtcpPort  目标rtcp端口
     * destNetworkAddress 目标ip
     **/
    public ServerManager(int localRtpPort, int localRtcpPort, int destRtpPort, int destRtcpPort, String destNetworkAddress) {
        super();
        this.localRtpPort = localRtpPort;
        this.localRtcpPort = localRtcpPort;
        this.destRtpPort = destRtpPort;
        this.destRtcpPort = destRtcpPort;
        this.destNetworkAddress = destNetworkAddress;
        log.debug("ServerManager init !\n " +
                " localRtpPort:" + localRtpPort +
                " localRtcpPort:" + localRtcpPort +
                " destRtpPort:" + destRtpPort +
                " destRtcpPort:" + destRtcpPort +
                " destNetworkAddress:" + destNetworkAddress );
        list = Collections.synchronizedList(new LinkedList<processedData>());
    }

    /**
     *初始化RTPRecord模块
     * localRtpPort  本地rtp端口
     * localRtcpPort 本地rtcp端口
     * localIp 		 本机ip
     * destRtpPort   目标rtp端口
     * destRtcpPort  目标rtcp端口
     * destNetworkAddress 目标ip
     **/
    public ServerManager(int localRtpPort, int localRtcpPort,String localIp , int destRtpPort, int destRtcpPort, String destNetworkAddress) {
        super();
        this.localRtpPort = localRtpPort;
        this.localRtcpPort = localRtcpPort;
        this.localIp = localIp;
        this.destRtpPort = destRtpPort;
        this.destRtcpPort = destRtcpPort;
        this.destNetworkAddress = destNetworkAddress;
        log.debug("ServerManager init !\n " +
                " localRtpPort:" + localRtpPort +
                " localRtcpPort:" + localRtcpPort +
                " destRtpPort:" + destRtpPort +
                " destRtcpPort:" + destRtcpPort +
                " destNetworkAddress:" + destNetworkAddress );
        list = Collections.synchronizedList(new LinkedList<processedData>());
    }

    /*
    * 音频数据写入文件
    * */
    private  void StartPcmWriter(){
        pcmWriter = new PcmWriter();
        pcmWriter.setRecording(true);
        Thread th = new Thread(pcmWriter);
        th.start();
    }

    /**
    * 播放原始音频数据
    * */
    private  void StartPcmPlayer(){
        pcmPlayer = new PcmPlayer();
        pcmPlayer.setRecording(true);
        Thread th = new Thread(pcmPlayer);
        th.start();
    }

    /**
    *初始化RTP会话
    **/
    private void StartRtpSession(){
        session = new InitSession(this,
                localRtpPort,
                localRtcpPort,
                destRtpPort,
                destRtcpPort,
                destNetworkAddress);

    }

    /**
    * 设置当前的模式
    **/
    public void setMode(int mode) {
        this.mode = mode;
    }

    /**
    * 线程处理函数
    * */
    public void run() {
        log.debug("ServerManager thread runing");

        while (this.isRunning()) {
            synchronized (mutex) {
                while (!this.isRecording) {
                    try {
                        mutex.wait();
                    } catch (InterruptedException e) {
                        throw new IllegalStateException("Wait() interrupted!",
                                e);
                    }
                }
            }
            // 数据接收稳定后可以删除
            if(this.mode == SEVER) {
                StartPcmPlayer();
            }

            StartRtpSession();

            if(this.mode == CLIENT) {
                startPcmRecorder();
            }

            while (this.isRecording()) {
                if (list.size() > 0) {
                    writeTag();
                    log.debug("list size = {}", list.size());
                } else {
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            if(this.mode == CLIENT) {
                recorder.stop();
            }
            while(list.size() > 0){
                writeTag();
                log.debug("list size = {}", list.size());
            }
            stop();
        }
    }

    /**
    *开启原始音频数据采集
    * */
    private void startPcmRecorder(){
        recorder = new PcmRecorder(this);
        recorder.setRecording(true);
        Thread th = new Thread(recorder);
        th.start();
    }

    /**
    * 处理同步过来的数据
    * 客户端直接将采集到的音频数据发送给服务端
    * 服务端如果没重写putData方法则默认将接收的音频数据通过AudioPlayer播放
    * */
    private void writeTag() {
        pData = list.remove(0);
        if(this.mode == CLIENT){
            if (session != null){
                log.debug("发送的数据:"+ Arrays.toString(pData.processed) + " size:" + pData.size);
                session.sendData(pData.processed);
            }
        }
        if(this.mode == SEVER){
            log.debug("接收的数据:"+ Arrays.toString(pData.processed) + " size:" + pData.size);
            pcmPlayer.putData(pData.processed, pData.size);
        }
    }

    /**
    *将数据保持到缓存列表中
    **/
    public void putData(long ts, byte[] buf, int size) {
        processedData data = new processedData();
        data.ts = ts;
        data.size = size;
        data.init();
        System.arraycopy(buf, 0, data.processed, 0, size);
        list.add(data);
    }

    /**
    * 停止服务
    * */
    private void stop() {
        log.debug("ServerManager thread stop" +
                " isRecording:" + isRecording +
                " isRunning:" + isRunning);
        session.stop();
        if(this.mode == SEVER) {
            pcmPlayer.setRecording(false);
        }
    }

    /**
    * 判断服务是否运行
    * */
    public boolean isRunning() {
        synchronized (mutex) {
            return isRunning;
        }
    }

    /**
    * 设置服务端运行状态
    * */
    public void setRunning(boolean isRunning) {
        synchronized (mutex) {
            this.isRunning = isRunning;
            if (this.isRunning) {
                mutex.notify();
            }
        }
    }

    /**
    * 设置音频采集状态
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
    * 判断音频采集状态
    * */
    public boolean isRecording() {
        synchronized (mutex) {
            return isRecording;
        }
    }

    /**
    * 数据类
    * ts 时间
    * size 数据实际大小
    * processed 存放数据
    * */
    class processedData {
        private long ts;
        private int size;
        private byte[] processed = null;
        protected void init(){
            if (size < 0){
                processed = new byte[2048];
            }else{
                processed = new byte[size];
            }
        }
    }
}
