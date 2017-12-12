package br.com.davidalain.pcapparser.mqtt;

public class MQTTSubscribeAck extends MQTTPacket {

	public MQTTSubscribeAck(byte[] data, long arrivalTime) {
		super(data, arrivalTime);
	}
	
	public int getMessageIdentifier() {
		return ((data[2] << 8)| data[3]);
	}
	
	public int getGrantedQoS() {
		return data[4];
	}

}
