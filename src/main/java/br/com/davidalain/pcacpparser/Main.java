package br.com.davidalain.pcacpparser;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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

	//TODO:@see https://github.com/emqtt/emqttd/wiki/$SYS-Topics

	public static final String CLIENT1_IP = "192.168.25.63";
	public static final String BROKER1_IP = "172.17.0.3";
	public static final String BROKER2_IP = "172.17.0.2";

	public static final String FILEPATH = "trace_747a0d82.pcap";

	public static void main(String[] args) throws IOException {

		final Pcap pcap = Pcap.openStream(FILEPATH);
		final Context ctx = new Context(CLIENT1_IP, BROKER1_IP, BROKER2_IP);
		final FactoryMQTT factory = new FactoryMQTT();
		final PrintStream log = new PrintStream(new File("log.txt"));
		final PrintStream result = new PrintStream(new File("result.txt"));

		pcap.loop(new PacketHandler() {
			@Override
			public boolean nextPacket(Packet packet) throws IOException {

				ctx.incrementPackerNumber();

				if (packet.hasProtocol(Protocol.ETHERNET_II)) {
					log.println("============================================================");
					log.println("packetNumber: " + ctx.getPackerNumber());

					MACPacket ethernetPacket = (MACPacket) packet.getPacket(Protocol.ETHERNET_II);
					Buffer ethernetPayload = ethernetPacket.getPayload();
					byte[] ethernetPayloadArray = ethernetPayload.getArray();
					int ethernetPayloadLen = ethernetPayloadArray.length;

					if (ethernetPacket.hasProtocol(Protocol.IPv4)) {

						Flow flow = new Flow(packet);

						log.println("time(us): " + PacketUtil.getArrivalTime(packet));
						log.println("flow: " + flow);
						ctx.addBytes(flow, PacketUtil.getArrivalTime(packet), ethernetPacket.getTotalLength());

//						COLOCAR PRA IMPRIMIR A QUANTIDADE DE BYTES EM CADA FLOW POR CADA SEGUNDO
						
						log.println("Ethernet frame sizes: \t"+
								"total=" + ethernetPacket.getTotalLength() + "\t"+
								"header=" + (ethernetPacket.getTotalLength() - ethernetPayloadLen) + "\t"+
								"payload=" + ethernetPayloadLen);

						IPPacket ipPacket = (IPPacket) ethernetPacket.getPacket(Protocol.IPv4);
						Buffer ipPayload = ipPacket.getPayload();
						byte[] ipPayloadArray = ipPayload.getArray();
						int ipPayloadLen = ipPayloadArray.length;

						log.println("IP datagram size: \t\t"+
								"total=" + ethernetPayloadLen + "\t"+
								"header=" + (ethernetPayloadLen - ipPayloadLen) + "\t"+
								"payload=" + ipPayloadLen);


						if (ipPacket.hasProtocol(Protocol.TCP)) {

							TCPPacket tcpPacket = (TCPPacket) ipPacket.getPacket(Protocol.TCP);
							Buffer tcpPayload = tcpPacket.getPayload();

							if (tcpPayload != null) {

								byte[] tcpPayloadArray = tcpPayload.getArray();
								int tcpPayloadLen = tcpPayloadArray.length;

								log.println("TCP segment size: \t\t"+
										"total=" + ipPayloadLen + "\t"+
										"header=" + (ipPayloadLen - tcpPayloadLen) + "\t"+
										"payload=" + tcpPayloadLen);

								log.println(tcpPayloadArray.length);
								log.println(HexPrinter.toStringHexDump(tcpPayloadArray));
								log.println(tcpPayload);

								if(MQTTPacket.isMQTT(tcpPayload)) {
									log.println("==== MQTT: ===");
									log.println("pktNum="+ ctx.getPackerNumber() + ", � um pacote MQTT");

									MQTTPacket mqttPacket = factory.getMQTTPacket(tcpPayloadArray, tcpPacket.getArrivalTime());

									log.println("msgType:"+mqttPacket.getMessageType());
									log.println("DUPFlag:"+mqttPacket.getDupFlag());
									log.println("QoS:"+mqttPacket.getQoS());
									log.println("RetainFlag:"+mqttPacket.getRetainFlag());
									log.println("msgLen:"+mqttPacket.getMessageLength());

									ctx.setLastMqttReceived(mqttPacket);
								} else {
									//TCP payload n�o � MQTT. Outro protocolo de camada 7.

									MQTTPacket lastMqtt = ctx.getLastMqttReceived();
									if(lastMqtt != null) {

										log.println("==== N�o � MQTT: ===");
										log.println(HexPrinter.toStringHexDump(tcpPayloadArray));

										int len = ByteBuffer.wrap(tcpPayloadArray, 0, 4).getInt();
										log.println("len="+len);
										log.println(tcpPayloadLen);

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

												int qos = lastMqtt.getQoS();
												log.println("qos="+qos);

												ctx.getMqttToTcpBrokerSyncMap(qos).put(lastMqtt, tcpPacket);
												ctx.setLastMqttReceived(null);

											} else {

												//												log.println("ArraysUtil.contains(tcpPayloadArray, topic)="+ArraysUtil.contains(tcpPayloadArray, topic));
												//												log.println(HexPrinter.toStringHexDump(topic));
												//												
												//												log.println("ArraysUtil.contains(tcpPayloadArray, message)="+ArraysUtil.contains(tcpPayloadArray, message));
												//												log.println(HexPrinter.toStringHexDump(message));

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

								log.println("TCP segment size: \t\t"+
										"total=" + ipPayloadLen + "\t"+
										"header=" + (ipPayloadLen - udpPayloadLen) + "\t"+
										"payload=" + udpPayloadLen);

							}
						} else {
							log.println(packet.getProtocol());
						}

						log.println("============================================================");					
					}
				}
				return true;
			}
		});

		log.println("########################################################################");
		for(int qos = 0 ; qos < Context.QOS_QUANTITY ; qos++) {
			log.println("******************************* QoS = "+qos+" ****************************");
			for(Entry<MQTTPacket, TCPPacket> pair : ctx.getMqttToTcpBrokerSyncMap(qos).entrySet()) {
				log.println("MQTT");
				log.println(HexPrinter.toStringHexDump(pair.getKey().getData()));
				log.println("TCP");
				log.println(HexPrinter.toStringHexDump(pair.getValue().getPayload().getArray()));
				long diffTime = (pair.getValue().getArrivalTime() - pair.getKey().getArrivalTime());
				log.println("difftime(us) = " + diffTime);

				ctx.getTimes(qos).add(diffTime);
			}
			log.println("**************************************************************************");

			result.println("============================= QoS = "+qos+" ===============================");
			double avg = 0;
			double median = 0;
			double max = ctx.getTimes(qos).size() == 0 ? Double.NaN : Collections.max(ctx.getTimes(qos));
			double min = ctx.getTimes(qos).size() == 0 ? Double.NaN : Collections.min(ctx.getTimes(qos));
			for(long l : ctx.getTimes(qos)) {
				avg += (double)l;
			}
			avg /= (double)ctx.getTimes(qos).size();
			result.println(ctx.getTimes(qos));
			result.println("max(us)="+max+", max(ms)="+(max/1000.0)+", max(s)="+(max/(1000.0*1000.0)));
			result.println("min(us)="+min+", min(ms)="+(min/1000.0)+", min(s)="+(min/(1000.0*1000.0)));
			result.println("avg(us)="+avg+", avg(ms)="+(avg/1000.0)+", avg(s)="+(avg/(1000.0*1000.0)));

			Collections.sort(ctx.getTimes(qos));
			median = ctx.getTimes(qos).size() == 0 ? Double.NaN : ctx.getTimes(qos).get(ctx.getTimes(qos).size()/2);
			result.println("median(us)="+median+", median(ms)="+(median/1000.0)+", median(s)="+(median/(1000.0*1000.0)));
			result.println("========================================================================");
		}
		log.println("########################################################################");

		System.out.println("Done!");
	}
}