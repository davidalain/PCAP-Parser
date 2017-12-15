package br.com.davidalain.pcapparser.mqtt;

import java.security.InvalidParameterException;

public class MQTTPubComplete extends MQTTPacket{

	public MQTTPubComplete(byte[] data, long arrivalTime) {
		super(data, arrivalTime);
		
		if(data.length != 4)
			throw new InvalidParameterException("Invalid Pub Complete length. len="+data.length); 
	}

	public final int getMessageIdentifier() {
		return ((data[2] << 8) | data[3]);
	}

//	@Override
//	public boolean equals(Object obj) {
//
//		//Condi��o que checa a menor mensagem MQTT (apenas os dois primeiros bytes)
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
