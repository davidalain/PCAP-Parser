package br.com.davidalain.pcapparser.mqtt;

import java.security.InvalidParameterException;

import br.com.davidalain.pcacpparser.ArraysUtil;

public class MQTTPubAck extends MQTTPacket{

	public MQTTPubAck(byte[] data, long arrivalTime) {
		super(data, arrivalTime);
		
		if(data.length != 4) {
			throw new InvalidParameterException("Invalid Pub Ack length. len="+data.length);
		}
	}

	public final int getMessageIdentifier() {
		return ArraysUtil.toInt(data, 2, 2);
	}

}
