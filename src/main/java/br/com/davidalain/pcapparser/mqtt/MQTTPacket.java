package br.com.davidalain.pcapparser.mqtt;

import java.security.InvalidParameterException;

import br.com.davidalain.pcacpparser.HexPrinter;
import io.pkts.buffer.Buffer;

public abstract class MQTTPacket{

	public class MessageType{
		public static final int CONNECT_COMMAND 	= (0x1);
		public static final int CONNECT_ACK 		= (0x2);

		public static final int PUBLISH_MESSAGE 	= (0x3);

		public static final int SUBSCRIBE_REQUEST 	= (0x8);
		public static final int SUBSCRIBE_ACK 		= (0x9);
		
		public static final int PING_REQUEST 		= (0xC);
		public static final int PING_RESPONSE 		= (0xD);
	}

	protected final byte[] data;
	protected final long arrivalTime;

	public MQTTPacket(byte[] data, long arrivalTime) {

		if(!isMQTT(data)) {
			System.err.println("Não é um pacote MQTT válido");
			System.err.println(HexPrinter.toStringHexDump(data));

			throw new InvalidParameterException("Não é um pacote MQTT válido!!");
		}

		this.data = data;
		this.arrivalTime = arrivalTime;
	}
	
	public final byte[] getData() {
		return data;
	}

	public long getArrivalTime() {
		return arrivalTime;
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

	public int getMessageLength() {
		return data[1];
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
