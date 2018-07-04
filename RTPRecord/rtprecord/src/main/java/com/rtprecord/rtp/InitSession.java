package com.rtprecord.rtp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.DatagramSocket;
import java.net.InetSocketAddress;

import com.rtprecord.io.Consumer;
import com.rtprecord.jlibrtp.*;

public class InitSession implements RTPAppIntf, RTCPAppIntf, DebugAppIntf {
    private Logger log = LoggerFactory.getLogger(InitSession.class);
	public RTPSession rtpSession = null;
	int RTP_BUFFSIZE_MAX = 1480;
    private Consumer consumer;
    private Long localIp = 0L;

	// The number of packets we have received
	int packetCount = 0;
	int maxPacketCount = -1;
	boolean noBye = true;

	// Debug
	int dataCount = 0;
	int pktCount = 0;

	/**
	 *初始化会话，不需要返回ssrc信息的（ip信息）
	 * consumer 接收数据返回上层
	 * localRtpPort  本地rtp端口
	 * localRtcpPort 本地rtcp端口
	 * destRtpPort   目标rtp端口
	 * destRtcpPort  目标rtcp端口
	 * destNetworkAddress 目标ip
	 **/
	public InitSession(Consumer consumer,int localRtpPort, int localRtcpPort, int destRtpPort, int destRtcpPort, String destNetworkAddress) {
		super();
		this.consumer = consumer;
		log.debug("InitSession thread runing");
		DatagramSocket rtpSocket = null;
		DatagramSocket rtcpSocket = null;
       log.error("localRtpPort:"+localRtpPort);
		log.error("localRtcpPort:"+localRtcpPort);
		try {
			rtpSocket = new DatagramSocket(null);
			rtpSocket.setReuseAddress(true);
			rtpSocket.bind(new InetSocketAddress(localRtpPort));

			rtcpSocket = new DatagramSocket(null);
			rtcpSocket.setReuseAddress(true);
			rtcpSocket.bind(new InetSocketAddress(localRtcpPort));
		} catch (Exception e) {
			log.error("发送创建会话异常抛出:" + e);
		}
		log.error("destNetworkAddress:" + destNetworkAddress+" destRtpPort:"+destRtpPort+" destRtcpPort:"+destRtcpPort);
		rtpSession = new RTPSession(rtpSocket, rtcpSocket);
		rtpSession.naivePktReception(true);
		rtpSession.RTPSessionRegister(this, this, this);
		//建立会话
		//设置参与者（目标IP地址，RTP端口，RTCP端口）
		log.error("destNetworkAddress:" + destNetworkAddress+" destRtpPort:"+destRtpPort+" destRtcpPort:"+destRtcpPort);
		Participant p = new Participant(destNetworkAddress, destRtpPort, destRtcpPort);
		rtpSession.addParticipant(p);
	}

    byte [] buf;
	/**
	 *回话接收数据，并将一桢数据拼接再返回给上层
	 * frame 返回数据信息
	 * p 返回发送者信息
	 **/
	public void receiveData(DataFrame frame, Participant p){
        log.info( "接收到数据: "+ frame.getConcatenatedData()
                  + " , 参与者CNAME： " + p.getCNAME()
                  + "同步源标识符(" + p.getSSRC() + ")" );

        if (buf == null){
            buf = frame.getConcatenatedData();
        } else {
            buf = byteMerger(buf, frame.getConcatenatedData());
        }
        if (frame.marked()){
			consumer.putData(System.currentTimeMillis(), buf,buf.length);
            buf = null;
        }
	}
	/**
	*byte 类型数据拼接
	**/
    public static byte[] byteMerger(byte[] bt1, byte[] bt2){
        byte[] bt3 = new byte[bt1.length+bt2.length];
        System.arraycopy(bt1, 0, bt3, 0, bt1.length);
        System.arraycopy(bt2, 0, bt3, bt1.length, bt2.length);
        return bt3;
    }

	public void userEvent(int type, Participant[] participant) {
		// TODO Auto-generated method stub
		log.info("type:："+  type);
		switch(type)
		{
			case 5: {
				for (int i = 0 ; i <participant.length ; i++ ) {
					log.info("SSRC:："+  participant[i].getSSRC());
					log.info("CNAME:："+  participant[i].getCNAME());
					log.info("Email:："+  participant[i].getEmail());
					log.info("Location:："+  participant[i].getLocation());
					log.info("Note:："+  participant[i].getNote());
					log.info("Phone:："+  participant[i].getPhone());
					log.info("Priv:："+  participant[i].getPriv());
					log.info("Tool:："+  participant[i].getTool());
				}
			}
			break;
			default:
				break;
		}

	}

	public int frameSize(int payloadType) {
		return 1;
	}

	/**
	 *回话数据发送
	 **/
	public void sendData(byte[] bytes) {
		int dataLength = (bytes.length - 1) / RTP_BUFFSIZE_MAX + 1;
		final byte[][] data = new byte[dataLength][];
		final boolean[] marks = new boolean[dataLength];
		marks[marks.length - 1] = true;
		int x = 0;
		int y = 0;
		int length = bytes.length;
		for (int i = 0; i < length; i++){
			if (y == 0){
				data[x] = new byte[length - i > RTP_BUFFSIZE_MAX ? RTP_BUFFSIZE_MAX : length - i];
			}
			data[x][y] = bytes[i];
			y++;
			if (y == data[x].length){
				y = 0;
				x++;
			}
		}

		rtpSession.sendData(data, null, marks, -1, null);
	}

	/**
	 *停止回话
	 **/
	public void stop() {
		rtpSession.endSession();
	}


	@Override
	public void packetReceived(int type, InetSocketAddress inetSocketAddress, String description) {
		log.info("type:" + type + "description: " + description);
	}

	@Override
	public void packetSent(int type, InetSocketAddress inetSocketAddress, String description) {
		log.info("type:" + type + "description: " + description);
	}

	@Override
	public void importantEvent(int i, String s) {

	}

	/**
	 * RTCP
	 * */
	@Override
	public void SRPktReceived(long ssrc, long ntpHighOrder, long ntpLowOrder,
							  long rtpTimestamp, long packetCount, long octetCount,
							  // Get the receiver reports, if any
							  long[] reporteeSsrc, int[] lossFraction, int[] cumulPacketsLost, long[] extHighSeq,
							  long[] interArrivalJitter, long[] lastSRTimeStamp, long[] delayLastSR) {
		log.info("RTPTimestamp:" + Long.toString(rtpTimestamp));
		log.info("NTPHigh:" + Long.toString(ntpHighOrder));
		log.info("NTPLow:" +Long.toString(ntpLowOrder) );
		log.info("SSRC:" + Long.toString(ssrc));
		log.info("PacketCount:" + Long.toString(packetCount));
		log.info("OctetCount:" + Long.toString(octetCount));
		this.packetCount++;
	}

	@Override
	public void RRPktReceived(long l, long[] longs, int[] ints, int[] ints1, long[] longs1, long[] longs2, long[] longs3, long[] longs4) {
		this.packetCount++;
	}

	@Override
	public void SDESPktReceived(Participant[] relevantParticipants) {
		if(relevantParticipants != null) {
			for(int i=0;i<relevantParticipants.length;i++) {
				Participant part = relevantParticipants[i];

				log.info("SSRC:" + Long.toString(part.getSSRC()));
				log.info("CNAME:" + part.getCNAME());
				log.info("CNAME:" + part.getNAME());
				log.info("Email:" + part.getEmail());
				log.info("Phone:" + part.getPhone());
				log.info("Location:" + part.getLocation());
				log.info("Note:" + part.getNote());
				log.info("Priv:" + part.getPriv());
				log.info("Tool:" + part.getTool());
			}
		} else {
			System.out.println("SDES with no participants?");
		}
	}

	@Override
	public void BYEPktReceived(Participant[] relevantParticipants, String reason) {
		if(relevantParticipants != null) {
			for(int i=0;i<relevantParticipants.length;i++) {
				Participant part = relevantParticipants[i];
				log.info("SSRC:" + Long.toString(part.getSSRC()));
				if(part.getCNAME() != null) {
					log.info("SSRC:" + part.getCNAME());
				}
			}
		}
		if(reason != null) {
			log.info("reason:" + reason);
		}

		this.packetCount++;

		// Terminate the session
		this.maxPacketCount = this.packetCount;
	}

	@Override
	public void APPPktReceived(Participant part, int subtype, byte[] name, byte[] data) {
		log.info("SSRC:" + Long.toString(part.getSSRC()));
		log.info("subtype:" + Integer.toString(subtype));

		byte[] tmp;
		byte[] output = new byte[name.length*2];
		for(int i=0; i<name.length; i++) {
			tmp = StaticProcs.hexOfByte(name[i]).getBytes();
			output[i*2] = tmp[0];
			output[i*2+1] =  tmp[1];
		}
		log.info("name:" + new String(output));

		output = new byte[data.length*2];
		for(int i=0; i<name.length; i++) {
			tmp = StaticProcs.hexOfByte(data[i]).getBytes();
			output[i*2] = tmp[0];
			output[i*2+1] =  tmp[1];
		}
		log.info("data:" + new String(output));
	}
}
