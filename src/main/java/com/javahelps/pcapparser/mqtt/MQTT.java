package com.javahelps.pcapparser.mqtt;

import java.security.InvalidParameterException;

import com.javahelps.pcapparser.HexPrinter;

import io.pkts.buffer.Buffer;

public class MQTT {

	public class MessageType{
		public static final int CONNECT_COMMAND 	= (0x1);
		public static final int CONNECT_ACK 		= (0x2);
		
		public static final int PUBLISH_MESSAGE 	= (0x3);
		
		public static final int SUBSCRIBE_REQUEST 	= (0x8);
		public static final int SUBSCRIBE_ACK 		= (0x9);
	}

	private byte[] data;

	public MQTT(byte[] data) {
		
		if(!isMQTT(data)) {
			System.err.println("Não é um pacote MQTT válido");
			System.err.println(HexPrinter.toStringHexFormatted(data));
			
			throw new InvalidParameterException("Não é um pacote MQTT válido!!");
		}
		
		this.data = data;
	}

	public int getMessageType() {
		return ((data[0] & 0xF0) >> 4);
	}

	public int getDupFlag() {
		return ((data[0] & 0x08) == 0) ? 1 : 0;
	}

	public int getQoS() {
		return ((data[0] & 0x06) >> 1);
	}

	public int getRetainFlag() {
		return (data[0] & 0x01);
	}

	public int getMQTTMessageLength() {
		return data[1];
	}

	public int getTopicLength() {
		return (data[2] << 8) | (data[3]);
	}

	public String getTopic() {
		return new String(data, 4, getTopicLength());
	}

	public String getMessage() {
		int index = 4 + getTopicLength();
		return new String(data, index, data.length-index);
	}
	
	public static boolean isMQTT(Buffer buffer) {

		if(buffer == null)
			return false;

		return isMQTT(buffer.getArray());
	}
	
	public static boolean isMQTT(byte[] data) {

		if(data == null)
			return false;
		
		if(data.length < 2)
			return false;

		switch((data[0] & 0xF0) >> 4) {
		case MessageType.PUBLISH_MESSAGE:
		case MessageType.CONNECT_COMMAND:
		case MessageType.CONNECT_ACK:
		case MessageType.SUBSCRIBE_REQUEST:
		case MessageType.SUBSCRIBE_ACK:
			break;
		default:
			return false;
		}
		
		if(data[1] != (data.length - 2))
			return false;

		return true;
	}

}
