package br.com.davidalain.pcapparser.mqtt;

import java.security.InvalidParameterException;

import br.com.davidalain.pcacpparser.ArraysUtil;

public class MQTTPubComplete extends MQTTPacket{

	public MQTTPubComplete(byte[] data, long arrivalTime) {
		super(data, arrivalTime);
		
		if(data.length != 4)
			throw new InvalidParameterException("Invalid Pub Complete length. len="+data.length); 
	}

	public final int getMessageIdentifier() {
		return ArraysUtil.toInt(data, 2, 2);
	}

//	@Override
//	public boolean equals(Object obj) {
//
//		//Condição que checa a menor mensagem MQTT (apenas os dois primeiros bytes)
//		if(!super.equals(obj))
//			return false;
//
//		if(!(obj instanceof MQTTPubComplete))
//			return false;
//		
//		MQTTPubComplete mqtt = (MQTTPubComplete) obj;
//
//		if(this.getMessageIdentifier() != mqtt.getMessageIdentifier())
//			return false;
//		
//		return true;
//	}

}
