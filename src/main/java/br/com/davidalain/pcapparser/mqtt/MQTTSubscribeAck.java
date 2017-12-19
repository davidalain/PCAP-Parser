package br.com.davidalain.pcapparser.mqtt;

import br.com.davidalain.pcacpparser.ArraysUtil;

public class MQTTSubscribeAck extends MQTTPacket {

	public MQTTSubscribeAck(byte[] data, long arrivalTime) {
		super(data, arrivalTime);
	}
	
	public int getMessageIdentifier() {
		return ArraysUtil.toInt(data, 2, 2);
	}
	
	public int getGrantedQoS() {
		return data[4];
	}

}
