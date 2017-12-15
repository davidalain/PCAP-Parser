package br.com.davidalain.pcapparser.mqtt;

import java.security.InvalidParameterException;

public class MQTTPubAck extends MQTTPacket{

	public MQTTPubAck(byte[] data, long arrivalTime) {
		super(data, arrivalTime);
		
		if(data.length != 4)
			throw new InvalidParameterException("Invalid Pub Ack length. len="+data.length); 
	}

	public final int getMessageIdentifier() {
		return ((data[2] << 8) | data[3]);
	}

//	@Override
//	public boolean equals(Object obj) {
//
//		//Condição que checa a menor mensagem MQTT (apenas os dois primeiros bytes)
//		if(!super.equals(obj))
//			return false;
//
//		if(!(obj instanceof MQTTPubAck))
//			return false;
//		
//		MQTTPubAck mqtt = (MQTTPubAck) obj;
//
//		if(this.getMessageIdentifier() != mqtt.getMessageIdentifier())
//			return false;
//		
//		return true;
//	}

}
