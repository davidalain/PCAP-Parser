package br.com.davidalain.pcacpparser;

import java.io.IOException;

import io.pkts.packet.Packet;

public class Flow {
	
	private String srcIp;
	private int srcPort;
	private String dstIP;
	private int dstPort;
	
	public Flow(String srcIp, int srcPort, String dstIP, int destPort) {
		super();
		this.srcIp = srcIp;
		this.srcPort = srcPort;
		this.dstIP = dstIP;
		this.dstPort = destPort;
	}
	
	private Flow(final PacketProcessing util, final Packet packet) throws IOException {
		this(util.getSourceIP(packet), util.getSourcePort(packet), util.getDestinationIP(packet), util.getDestinationPort(packet));
	}
	
	public Flow(Packet packet) throws IOException {
		this(new PacketProcessing(), packet);
	}
	
//	public Flow(Packet packet) throws IOException {
//		this(new PacketProcessingUtil().getSourceIP(packet), new PacketProcessingUtil().getSourcePort(packet), new PacketProcessingUtil().getDestinationIP(packet), new PacketProcessingUtil().getDestinationPort(packet));
//	}
	
	public String getSrcIp() {
		return srcIp;
	}
	public void setSrcIp(String srcIp) {
		this.srcIp = srcIp;
	}
	public String getDstIP() {
		return dstIP;
	}
	public void setDstIP(String dstIP) {
		this.dstIP = dstIP;
	}
	public int getSrcPort() {
		return srcPort;
	}
	public void setSrcPort(int srcPort) {
		this.srcPort = srcPort;
	}
	public int getDestPort() {
		return dstPort;
	}
	public void setDestPort(int destPort) {
		this.dstPort = destPort;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((dstIP == null) ? 0 : dstIP.hashCode());
		result = prime * result + dstPort;
		result = prime * result + ((srcIp == null) ? 0 : srcIp.hashCode());
		result = prime * result + srcPort;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Flow other = (Flow) obj;
		if (dstIP == null) {
			if (other.dstIP != null)
				return false;
		} else if (!dstIP.equals(other.dstIP))
			return false;
		if (dstPort != other.dstPort)
			return false;
		if (srcIp == null) {
			if (other.srcIp != null)
				return false;
		} else if (!srcIp.equals(other.srcIp))
			return false;
		if (srcPort != other.srcPort)
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return new StringBuilder().append(srcIp).append(":").append(srcPort).append(" -> ").append(dstIP).append(":").append(dstPort).toString();
	}
	
	public String toStringForFileName() {
		return new StringBuilder().append(srcIp).append(".").append(srcPort).append("_to_").append(dstIP).append(".").append(dstPort).toString();
	}
	

}
