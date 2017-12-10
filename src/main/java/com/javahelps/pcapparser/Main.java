package com.javahelps.pcapparser;

import java.io.IOException;

import io.pkts.PacketHandler;
import io.pkts.Pcap;
import io.pkts.buffer.Buffer;
import io.pkts.packet.IPPacket;
import io.pkts.packet.MACPacket;
import io.pkts.packet.Packet;
import io.pkts.packet.TCPPacket;
import io.pkts.packet.UDPPacket;
import io.pkts.protocol.Protocol;

public class Main {

	public static final int ETHERNET_HEADER_SIZE = 14;
	public static final int IP_HEADER_SIZE = 20;

	public static void main2(String[] args) throws IOException {

		byte[] a = {(byte) 0x80, 0x10, 0x04, (byte) 0xB2, 0x58, 0x4E, 0x00, 0x00};
		byte[] b = {0x01, 0x01, 0x08, 0x0A, (byte) 0xA7, (byte) 0xB8, 0x20, 0x7F};

		for(int i = 0 ; i < a.length ; i++) {
			System.out.println("c="+HexPrinter.toHexString(a[i]) + "__"+(new String(a,i,1))+"__");	
		}

		for(int i = 0 ; i < b.length ; i++) {
			System.out.println("c="+HexPrinter.toHexString(b[i]) + "__"+(new String(b,i,1))+"__");	
		}

	}

	public static void main(String[] args) throws IOException {

		final Pcap pcap = Pcap.openStream("trace.pcap");

		pcap.loop(new PacketHandler() {
			@Override
			public boolean nextPacket(Packet packet) throws IOException {

				if (packet.hasProtocol(Protocol.ETHERNET_II)) {
					System.out.println("============================================================");

					MACPacket ethernetPacket = (MACPacket) packet.getPacket(Protocol.ETHERNET_II);
					Buffer ethernetBuffer = ethernetPacket.getPayload();
					byte[] ethernetBufferArray = ethernetBuffer.getArray();
					int ethernetPayloadLen = ethernetBufferArray.length;
					
					System.out.println("==== MAC: ===");
					System.out.println("Ethernet frame size: " + ethernetPacket.getTotalLength());
					System.out.println("Ethernet header size: " + (ethernetPacket.getTotalLength() - ethernetPayloadLen));
					System.out.println("Ethernet payload size: " + ethernetPayloadLen);
					
					System.out.println(ethernetBufferArray.length);
					System.out.println(HexPrinter.toStringHexFormatted(ethernetBufferArray));
					
					if (ethernetPacket.hasProtocol(Protocol.IPv4)) {

						IPPacket ipPacket = (IPPacket) ethernetPacket.getPacket(Protocol.IPv4);
						Buffer ipBuffer = ipPacket.getPayload();
						byte[] ipBufferArray = ipBuffer.getArray();
						int ipPayloadLen = ipBufferArray.length;

						System.out.println("==== IP: ===");
						System.out.println("IP datagram size: " + ethernetPayloadLen);
						System.out.println("IP header size: " + (ethernetPayloadLen - ipPayloadLen));
						System.out.println("IP payload size: " + ipPayloadLen);

						System.out.println(ipBufferArray.length);
						System.out.println(HexPrinter.toStringHexFormatted(ipBufferArray));

						if (ipPacket.hasProtocol(Protocol.TCP)) {

							TCPPacket tcpPacket = (TCPPacket) ipPacket.getPacket(Protocol.TCP);
							Buffer tcpBuffer = tcpPacket.getPayload();

							if (tcpBuffer != null) {
								
								byte[] tcpBufferArray = tcpBuffer.getArray();
								int tcpPayloadLen = tcpBufferArray.length;

								System.out.println("==== TCP: ===");
								System.out.println("TCP datagram size: " + ipPayloadLen);
								System.out.println("TCP header size: " + (ipPayloadLen - tcpPayloadLen));
								System.out.println("TCP payload size: " + tcpPayloadLen);

								System.out.println("time(us): " + tcpPacket.getArrivalTime());
								System.out.println("IP: "
										+ tcpPacket.getSourceIP() 		+ ":" + tcpPacket.getSourcePort() + " -> "
										+ tcpPacket.getDestinationIP() 	+ ":" + tcpPacket.getDestinationPort());

								System.out.println(tcpBufferArray.length);
								System.out.println(HexPrinter.toStringHexFormatted(tcpBufferArray));
							}

						} else if (packet.hasProtocol(Protocol.UDP)) {

							UDPPacket udpPacket = (UDPPacket) packet.getPacket(Protocol.UDP);
							Buffer buffer = udpPacket.getPayload();
							if (buffer != null) {
								System.out.println("UDP: " + buffer);
							}
						}

						System.out.println("============================================================");					
					}
				}
				return true;
			}
		});
	}
}