package com.rtprecord.io;

/**
 * 消费者模式数据类
 * */
public interface Consumer {
	
	public void putData(long ts, byte[] buf, int size);
	
	public void setRecording(boolean isRecording);
	
	public boolean isRecording();	
}
