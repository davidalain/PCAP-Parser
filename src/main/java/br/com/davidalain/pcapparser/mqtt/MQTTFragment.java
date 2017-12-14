package br.com.davidalain.pcapparser.mqtt;

public class MQTTFragment {
	
	public static final int BUFFER_MAX_LEN = 200;

	private final byte[] partialMessageBuffer;
	private int partialMessageLen;
	private final long arrivalTime;
	
	public MQTTFragment(long arrivalTime) {
		this.partialMessageBuffer = new byte[BUFFER_MAX_LEN];
		this.partialMessageLen = 0;
		
		this.arrivalTime = arrivalTime;
	}
	
	public int getPartialMessageLen() {
		return partialMessageLen;
	}
	public void setPartialMessageLen(int partialMessageLen) {
		this.partialMessageLen = partialMessageLen;
	}
	public void addPartialMessageLen(int lenToAdd) {
		this.partialMessageLen += lenToAdd;
	}
	public byte[] getPartialMessageBuffer() {
		return partialMessageBuffer;
	}
	public long getArrivalTime() {
		return arrivalTime;
	}
	
	public MQTTPacket buildMQTTPacket() {
		
		byte[] data = new byte[partialMessageLen];
		System.arraycopy(partialMessageBuffer, 0, data, 0, data.length);
		
		return new FactoryMQTT().getMQTTPacket(data, arrivalTime);
	}
	
	public void clear() {
		for(int i = 0 ; i < partialMessageBuffer.length ; i++) {
			partialMessageBuffer[i] = 0;
		}
		partialMessageLen = 0;
	}
	
	/**
	 * -1, mensagem inválida
	 * 0, mensagem incompleta (ainda não recebeu todos os bytes da mensagem)
	 * 1, mensagem completa e 'válida'
	 * 
	 * @param fragment
	 * @return
	 */
	public int check() {
		
		if(!MQTTPacket.hasPacketType(partialMessageBuffer)) {
			return -1;
		}
		
		int realMsgLen = 2 /*header*/ + partialMessageBuffer[1];
		
		if(realMsgLen < partialMessageLen) //recebeu mais do que o tamanho da mensagem: erro!
			return -1;
		if(realMsgLen > partialMessageLen) //recebeu menos do que o tamanho da mensagem: ainda tem mais bytes pra receber.
			return 0;
		
		return 1; //recebeu a quantidade correta.
	}
	
	
}
