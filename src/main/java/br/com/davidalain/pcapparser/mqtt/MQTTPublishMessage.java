package br.com.davidalain.pcapparser.mqtt;

import br.com.davidalain.pcacpparser.HexPrinter;

public class MQTTPublishMessage extends MQTTPacket{

	public MQTTPublishMessage(byte[] data, long arrivalTime) {
		super(data, arrivalTime);
	}
	
	public int getTopicLength() {
		return ((data[2] << 8) | data[3]);
	}
	
	public final byte[] getTopicArray() {
		final byte[] array = new byte[getTopicLength()];
		System.arraycopy(data, 4, array, 0, array.length);
		return array;
	}
	
	public final String getTopic() {
		return new String(getTopicArray());
	}
	
	public final byte[] getMessageArray() {
		int index = 4 + getTopicLength();
		final byte[] array = new byte[data.length-index];
		System.arraycopy(data, index, array, 0, array.length);
		return array;
	}
	
	public final String getMessage() {
		return new String(getMessageArray());
	}

}
