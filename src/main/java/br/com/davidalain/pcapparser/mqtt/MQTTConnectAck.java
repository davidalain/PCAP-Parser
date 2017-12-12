package br.com.davidalain.pcapparser.mqtt;

public class MQTTConnectAck extends MQTTPacket{

	public MQTTConnectAck(byte[] data, long arrivalTime) {
		super(data, arrivalTime);
	}
	
	public byte getAcknowledgeFlags() {
		return data[2];
	}
	
	public byte getReturnCode() {
		return data[3];
	}

}
