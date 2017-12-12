package br.com.davidalain.pcacpparser;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import br.com.davidalain.pcapparser.mqtt.FactoryMQTT;
import br.com.davidalain.pcapparser.mqtt.MQTTPacket;
import br.com.davidalain.pcapparser.mqtt.MQTTPublishMessage;
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
	
	//TODO: https://github.com/emqtt/emqttd/wiki/$SYS-Topics

	public static final String CLIENT1_IP = "192.168.25.63";
	public static final String BROKER1_IP = "172.17.0.3";
	public static final String BROKER2_IP = "172.17.0.2";

	public static void main(String[] args) throws IOException {

		final Pcap pcap = Pcap.openStream("trace_747a0d82.pcap");
		final Context ctx = new Context(CLIENT1_IP, BROKER1_IP, BROKER2_IP);
		final FactoryMQTT factory = new FactoryMQTT(); 

		pcap.loop(new PacketHandler() {
			@Override
			public boolean nextPacket(Packet packet) throws IOException {

				ctx.incrementPackerNumber();

				if (packet.hasProtocol(Protocol.ETHERNET_II)) {
					System.out.println("============================================================");
					System.out.println("packetNumber: " + ctx.getPackerNumber());

					MACPacket ethernetPacket = (MACPacket) packet.getPacket(Protocol.ETHERNET_II);
					Buffer ethernetPayload = ethernetPacket.getPayload();
					byte[] ethernetPayloadArray = ethernetPayload.getArray();
					int ethernetPayloadLen = ethernetPayloadArray.length;

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
						Buffer ipPayload = ipPacket.getPayload();
						byte[] ipPayloadArray = ipPayload.getArray();
						int ipPayloadLen = ipPayloadArray.length;

						System.out.println("IP datagram size: \t\t"+
								"total=" + ethernetPayloadLen + "\t"+
								"header=" + (ethernetPayloadLen - ipPayloadLen) + "\t"+
								"payload=" + ipPayloadLen);


						if (ipPacket.hasProtocol(Protocol.TCP)) {

							TCPPacket tcpPacket = (TCPPacket) ipPacket.getPacket(Protocol.TCP);
							Buffer tcpPayload = tcpPacket.getPayload();

							if (tcpPayload != null) {

								byte[] tcpPayloadArray = tcpPayload.getArray();
								int tcpPayloadLen = tcpPayloadArray.length;

								System.out.println("TCP segment size: \t\t"+
										"total=" + ipPayloadLen + "\t"+
										"header=" + (ipPayloadLen - tcpPayloadLen) + "\t"+
										"payload=" + tcpPayloadLen);

								System.out.println(tcpPayloadArray.length);
								System.out.println(HexPrinter.toStringHexDump(tcpPayloadArray));
								System.out.println(tcpPayload);

								if(MQTTPacket.isMQTT(tcpPayload)) {
									System.out.println("==== MQTT: ===");
									System.out.println("pktNum="+ ctx.getPackerNumber() + ", É um pacote MQTT");

									MQTTPacket mqttPacket = factory.getMQTTPacket(tcpPayloadArray, tcpPacket.getArrivalTime());

									ctx.setLastMqttReceived(mqttPacket);
								} else {
									//TCP payload não é MQTT. Outro protocolo de camada 7.

									MQTTPacket lastMqtt = ctx.getLastMqttReceived();
									if(lastMqtt != null) {

										System.out.println("==== Não é MQTT: ===");
										System.out.println(HexPrinter.toStringHexDump(tcpPayloadArray));
										
										int len = ByteBuffer.wrap(tcpPayloadArray, 0, 4).getInt();
										System.out.println("len="+len);
										System.out.println(tcpPayloadLen);

										if(lastMqtt.getMessageType() == MQTTPacket.MessageType.PUBLISH_MESSAGE) {

											MQTTPublishMessage mqttPublish = (MQTTPublishMessage) lastMqtt;

											byte[] topic = mqttPublish.getTopicArray();
											byte[] message = mqttPublish.getMessageArray();

											//FIXME:
											//Note: this is a not safe check, because topic name and message can be equals (same content) or shorter than necessary to guarantee correct working
											//The correct way to check is analyze if topic and message are into TCP payload at the their specific correct positions
											if(
													ArraysUtil.contains(tcpPayloadArray, topic) &&
													ArraysUtil.contains(tcpPayloadArray, message)) 
											{

												//This is the TCP Segment that broker uses to synchronize last MQTT Publish Message to another broker in the cluster

												ctx.getMqttToTcpBrokerSyncMap().put(lastMqtt, tcpPacket);
												ctx.setLastMqttReceived(null);
											}

										}

									}
								}
							}

						} else if (packet.hasProtocol(Protocol.UDP)) {

							UDPPacket udpPacket = (UDPPacket) packet.getPacket(Protocol.UDP);
							Buffer udpPayload = udpPacket.getPayload();
							if (udpPayload != null) {

								byte[] udpPayloadArray = udpPayload.getArray();
								int udpPayloadLen = udpPayloadArray.length;

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

		System.out.println("########################################################################");
		for(Entry<MQTTPacket, TCPPacket> pair : ctx.getMqttToTcpBrokerSyncMap().entrySet()) {
			System.out.println("MQTT");
			System.out.println(HexPrinter.toStringHexDump(pair.getKey().getData()));
			System.out.println("TCP");
			System.out.println(HexPrinter.toStringHexDump(pair.getValue().getPayload().getArray()));
			long diffTime = (pair.getValue().getArrivalTime() - pair.getKey().getArrivalTime());
			System.out.println("difftime(us) = " + diffTime);
			
			ctx.getTimes().add(diffTime);
		}
		System.out.println("########################################################################");
		
		System.out.println("========================================================================");
		double avg = 0;
		double max = Collections.max(ctx.getTimes());
		double min = Collections.min(ctx.getTimes());
		for(long l : ctx.getTimes()) {
			avg += (double)l;
		}
		avg /= (double)ctx.getTimes().size();
		System.out.println(ctx.getTimes());
		System.out.println("max(us)="+max+", max(ms)="+(max/1000.0)+", max(s)="+(max/(1000.0*1000.0)));
		System.out.println("min(us)="+min+", min(ms)="+(min/1000.0)+", min(s)="+(min/(1000.0*1000.0)));
		System.out.println("avg(us)="+avg+", avg(ms)="+(avg/1000.0)+", avg(s)="+(avg/(1000.0*1000.0)));
		System.out.println("========================================================================");
	}
}