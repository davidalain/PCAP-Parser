package br.com.davidalain.pcapparser.mqtt;

import br.com.davidalain.pcacpparser.ArraysUtil;

public class MQTTSubscribeRequest extends MQTTPacket {

	public MQTTSubscribeRequest(byte[] data, long arrivalTime) {
		super(data, arrivalTime);
	}

	public int getMessageIdentifier() {
		return ArraysUtil.toInt(data, 2, 2);
	}
	
	public int getTopicLength() {
		return ArraysUtil.toInt(data, 4, 2);
	}
	
	public final byte[] getTopicArray() {
		final byte[] array = new byte[getTopicLength()];
		int index = 6;
		System.arraycopy(data, index, array, 0, array.length);
		return array;
	}
	
	public final String getTopic() {
		return new String(getTopicArray());
	}
	
	public int getGrantedQoS() {
		int index = 6 + getTopicLength();
		return data[index];
	}
	
}
