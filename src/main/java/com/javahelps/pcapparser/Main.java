package com.javahelps.pcapparser;

import java.io.IOException;

import com.javahelps.pcapparser.mqtt.MQTT;

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

	//	public static void main2(String[] args) throws IOException {
	//
	//		byte[] a = {(byte) 0x80, 0x10, 0x04, (byte) 0xB2, 0x58, 0x4E, 0x00, 0x00};
	//		byte[] b = {0x01, 0x01, 0x08, 0x0A, (byte) 0xA7, (byte) 0xB8, 0x20, 0x7F};
	//
	//		for(int i = 0 ; i < a.length ; i++) {
	//			System.out.println("c="+HexPrinter.toHexString(a[i]) + "__"+(new String(a,i,1))+"__");	
	//		}
	//
	//		for(int i = 0 ; i < b.length ; i++) {
	//			System.out.println("c="+HexPrinter.toHexString(b[i]) + "__"+(new String(b,i,1))+"__");	
	//		}
	//
	//	}

	private static int packetNumber = 0;

	public static void main(String[] args) throws IOException {

		final Pcap pcap = Pcap.openStream("trace (1).pcap");

		pcap.loop(new PacketHandler() {
			@Override
			public boolean nextPacket(Packet packet) throws IOException {

				packetNumber++;

				if (packet.hasProtocol(Protocol.ETHERNET_II)) {
					System.out.println("============================================================");
					System.out.println("packetNumber: " + packetNumber);

					MACPacket ethernetPacket = (MACPacket) packet.getPacket(Protocol.ETHERNET_II);
					Buffer ethernetBuffer = ethernetPacket.getPayload();
					byte[] ethernetBufferArray = ethernetBuffer.getArray();
					int ethernetPayloadLen = ethernetBufferArray.length;

					System.out.println("time(us): " + PacketUtil.getArrivalTime(packet));
					System.out.println("direction: "
							+ PacketUtil.getSourceIP(packet) 		+ ":" + PacketUtil.getSourcePort(packet) + " -> "
							+ PacketUtil.getDestinationIP(packet) 	+ ":" + PacketUtil.getDestinationPort(packet));

					System.out.println("Ethernet frame sizes: \t"+
							"total=" + ethernetPacket.getTotalLength() + "\t"+
							"header=" + (ethernetPacket.getTotalLength() - ethernetPayloadLen) + "\t"+
							"payload=" + ethernetPayloadLen);

					if (ethernetPacket.hasProtocol(Protocol.IPv4)) {

						IPPacket ipPacket = (IPPacket) ethernetPacket.getPacket(Protocol.IPv4);
						Buffer ipBuffer = ipPacket.getPayload();
						byte[] ipBufferArray = ipBuffer.getArray();
						int ipPayloadLen = ipBufferArray.length;

						System.out.println("IP datagram size: \t\t"+
								"total=" + ethernetPayloadLen + "\t"+
								"header=" + (ethernetPayloadLen - ipPayloadLen) + "\t"+
								"payload=" + ipPayloadLen);


						if (ipPacket.hasProtocol(Protocol.TCP)) {

							TCPPacket tcpPacket = (TCPPacket) ipPacket.getPacket(Protocol.TCP);
							Buffer tcpBuffer = tcpPacket.getPayload();

							if (tcpBuffer != null) {

								byte[] tcpBufferArray = tcpBuffer.getArray();
								int tcpPayloadLen = tcpBufferArray.length;

								System.out.println("TCP segment size: \t\t"+
										"total=" + ipPayloadLen + "\t"+
										"header=" + (ipPayloadLen - tcpPayloadLen) + "\t"+
										"payload=" + tcpPayloadLen);
								
								System.out.println(tcpBufferArray.length);
								System.out.println(HexPrinter.toStringHexFormatted(tcpBufferArray));
								System.out.println(tcpBuffer);

								if(MQTT.isMQTT(tcpBuffer)) {
									System.out.println("==== MQTT: ===");
									System.out.println("pktNum="+ packetNumber + ", É um pacote MQTT");
								}
							}

						} else if (packet.hasProtocol(Protocol.UDP)) {

							UDPPacket udpPacket = (UDPPacket) packet.getPacket(Protocol.UDP);
							Buffer udpBuffer = udpPacket.getPayload();
							if (udpBuffer != null) {
								
								byte[] udpBufferArray = udpBuffer.getArray();
								int udpPayloadLen = udpBufferArray.length;
								
								System.out.println("TCP segment size: \t\t"+
										"total=" + ipPayloadLen + "\t"+
										"header=" + (ipPayloadLen - udpPayloadLen) + "\t"+
										"payload=" + udpPayloadLen);
								
							}
						} else {
							System.out.println(packet.getProtocol());
						}

						System.out.println("============================================================");					
					}
				}
				return true;
			}
		});
	}
}