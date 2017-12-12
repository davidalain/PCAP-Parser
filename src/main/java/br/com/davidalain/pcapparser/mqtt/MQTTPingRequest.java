package br.com.davidalain.pcapparser.mqtt;

public class MQTTPingRequest extends MQTTPacket{

	public MQTTPingRequest(byte[] data, long arrivalTime) {
		super(data, arrivalTime);
	}
	
}
