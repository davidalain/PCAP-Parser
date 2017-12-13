package br.com.davidalain.pcapparser.mqtt;

public class MQTTPingResponse extends MQTTPacket{

	public MQTTPingResponse(byte[] data, long arrivalTime) {
		super(data, arrivalTime);
	}
	
}
