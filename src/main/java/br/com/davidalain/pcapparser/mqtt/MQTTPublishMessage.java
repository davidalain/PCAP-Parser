package br.com.davidalain.pcapparser.mqtt;

import br.com.davidalain.pcacpparser.Util;

public class MQTTPublishMessage extends MQTTPacket{

	public MQTTPublishMessage(byte[] data, long arrivalTime) {
		super(data, arrivalTime);
	}

	public int getTopicLength() {
		return Util.toInt(data, 2, 2);
	}

	public final byte[] getTopicArray() {
		final byte[] array = new byte[getTopicLength()];
		System.arraycopy(data, 4, array, 0, array.length);
		return array;
	}

	public final String getTopic() {
		return new String(getTopicArray());
	}

	public final int getMessageIdentifier() {
		
		if(getQoS() == 0)
			return -1;
		
		int index = 4 + getTopicLength();
		return Util.toInt(data, index, 2);
	}

	public final byte[] getMessageArray() {
		int index = 4 + getTopicLength() + 2;
		final byte[] array = new byte[data.length-index];
		System.arraycopy(data, index, array, 0, array.length);
		return array;
	}

	public final String getMessage() {
		return new String(getMessageArray());
	}

}
