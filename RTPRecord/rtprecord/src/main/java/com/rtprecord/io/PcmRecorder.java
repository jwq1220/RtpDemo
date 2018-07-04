package com.rtprecord.io;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PcmRecorder implements Runnable {

	private Logger log = LoggerFactory.getLogger(PcmRecorder.class);
	private volatile boolean isRecording;
	private final Object mutex = new Object();
	private static final int frequency = 16000;
	private static final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
	private Consumer consumer;
	AudioRecord recordInstance = null;

	/**
	 * 初始化录音
	 * */
	public PcmRecorder(Consumer consumer) {
		super();
		this.consumer = consumer;
		log.debug("Pcmecoder init !");
	}

	/**
	 * 音频录音线程
	 * */
	public void run() {
		log.debug("Pcmecoder thread runing !");
		// speex编码
//		Encoder encoder = new Encoder(this.consumer);
//		Thread encodeThread = new Thread(encoder);
//		encoder.setRecording(true);
//		encodeThread.start();
//
//		android.os.Process
//				.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

		int bufferRead = 0;
		int bufferSize = AudioRecord.getMinBufferSize(frequency,
				AudioFormat.CHANNEL_IN_MONO, audioEncoding);

		final int BUFFER_SIZE = 2048;//缓存区的大小
		byte[] tempBuffer = new byte[bufferSize];
		log.debug("bufferSize:" + bufferSize + " BUFFER_SIZE:" + BUFFER_SIZE);
		if (recordInstance == null) {
			try {
//				recordInstance = new AudioRecord(MediaRecorder.AudioSource.MIC,
//						frequency, AudioFormat.CHANNEL_IN_MONO,
//						audioEncoding, Math.min(BUFFER_SIZE,bufferSize));
                recordInstance = new AudioRecord(MediaRecorder.AudioSource.MIC,
                        frequency, AudioFormat.CHANNEL_IN_MONO,
                        audioEncoding, bufferSize);
			} catch (Exception e) {
				e.printStackTrace();
				log.error("recordInstance:" + recordInstance + " eror:" + e);
			}
		}

		recordInstance.startRecording();

		while (this.isRecording) {

			//bufferRead = recordInstance.read(tempBuffer, 0, Math.min(BUFFER_SIZE,bufferSize));
            bufferRead = recordInstance.read(tempBuffer, 0, bufferSize);
			if (bufferRead == AudioRecord.ERROR_INVALID_OPERATION) {
				throw new IllegalStateException(
						"read() returned AudioRecord.ERROR_INVALID_OPERATION");
			} else if (bufferRead == AudioRecord.ERROR_BAD_VALUE) {
				throw new IllegalStateException(
						"read() returned AudioRecord.ERROR_BAD_VALUE");
			} else if (bufferRead == AudioRecord.ERROR_INVALID_OPERATION) {
				throw new IllegalStateException(
						"read() returned AudioRecord.ERROR_INVALID_OPERATION");
			}

			if (bufferRead >  0){
                consumer.putData(System.currentTimeMillis(), tempBuffer, bufferRead);
            }
//			if (encoder.isIdle()) {
//				encoder.putData(System.currentTimeMillis(), tempBuffer,
//						bufferRead);
//			} else {
//				// ��Ϊ���봦��������ֱ�Ӷ�����ζ���������
//				log.error("drop data!");
//			}

		}
		recordInstance.stop();
		recordInstance.release();
//		encoder.setRecording(false);
	}


	/**
	 * 停止录音
	 * */
	public void stop(){
		log.debug("Pcmecoder thread stop !" +
				  " isRecording:" + isRecording);
		setRecording(false);
	}

	/**
	 * 设置录音状态
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
	*查询录音状态
	* */
	public boolean isRecording() {
		synchronized (mutex) {
			return isRecording;
		}
	}
}
