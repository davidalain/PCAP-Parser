package br.com.davidalain.pcacpparser.main;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;

import br.com.davidalain.pcacpparser.ArraysUtil;
import br.com.davidalain.pcacpparser.Context;
import br.com.davidalain.pcacpparser.Flow;
import br.com.davidalain.pcacpparser.HexPrinter;
import br.com.davidalain.pcacpparser.PacketUtil;
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

public class MainClusterSync {

	/**
	 * TODO:
	 * 
	 * @see https://github.com/emqtt/emqttd/wiki/$SYS-Topics
	 */

	public static void main(String[] args) throws IOException {
		
		System.out.println("Running...");

		/**
		 * Cria o caminho (não dá erro caso já exista)
		 */
		new File(Constants.OUTPUT_FLOW_PATH).mkdirs();

		final Pcap pcap = Pcap.openStream(Constants.FILEPATH);
		final Context ctx = new Context(Constants.BROKER1_IP, Constants.BROKER2_IP, Constants.CLIENT1_IP, Constants.CLIENT2_IP);
		final FactoryMQTT factory = new FactoryMQTT();
		final PrintStream log = new PrintStream(new File(Constants.OUTPUT_PATH+Constants.PREFIX+"_log.txt"));
		final PrintStream resultTime = new PrintStream(new File(Constants.OUTPUT_PATH+Constants.PREFIX+"_resultTime.txt"));
		final PrintStream resultFlow = new PrintStream(new File(Constants.OUTPUT_PATH+Constants.PREFIX+"_resultFlow.txt"));
		final PrintStream printerAllFlow = new PrintStream(new File(Constants.OUTPUT_FLOW_PATH+Constants.PREFIX+"_allFlows.csv"));

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

						Flow flow = new Flow(ethernetPacket);

						log.println("time(us): " + PacketUtil.getArrivalTime(packet));
						log.println("flow: " + flow);
						ctx.addBytes(flow, PacketUtil.getArrivalTime(packet), ethernetPacket.getTotalLength());

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
									log.println("pktNum="+ ctx.getPackerNumber() + ", É um pacote MQTT");

									MQTTPacket mqttPacket = factory.getMQTTPacket(tcpPayloadArray, tcpPacket.getArrivalTime());

									log.println("msgType:"+mqttPacket.getMessageType());
									log.println("DUPFlag:"+mqttPacket.getDupFlag());
									log.println("QoS:"+mqttPacket.getQoS());
									log.println("RetainFlag:"+mqttPacket.getRetainFlag());
									log.println("msgLen:"+mqttPacket.getMessageLength());

									/**
									 * Client1 enviando Publish Message para o broker1
									 */
									if(tcpPacket.getSourceIP().equals(Constants.CLIENT1_IP) && tcpPacket.getDestinationIP().equals(Constants.BROKER1_IP)) {
										ctx.setLastMqttReceived(mqttPacket);	
									}

								} else {
									//TCP payload não é MQTT. Outro protocolo de camada 7.

									MQTTPacket lastMqtt = ctx.getLastMqttReceived();
									if(lastMqtt != null) {

										log.println("==== Não é MQTT: ===");
										log.println(HexPrinter.toStringHexDump(tcpPayloadArray));

										int len = ByteBuffer.wrap(tcpPayloadArray, 0, 4).getInt();
										log.println("len="+len);
										log.println(tcpPayloadLen);

										if(lastMqtt.getMessageType() == MQTTPacket.MessageType.PUBLISH_MESSAGE) {

											MQTTPublishMessage mqttPublish = (MQTTPublishMessage) lastMqtt;

											byte[] topic = mqttPublish.getTopicArray();
											byte[] message = mqttPublish.getMessageArray();

											/**
											 * Broker1 enviando mensagem de sync para Broker2
											 */
											if(tcpPacket.getSourceIP().equals(Constants.BROKER1_IP) && tcpPacket.getDestinationIP().equals(Constants.BROKER2_IP)) {

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


		DataPrinter.printQoSTimeAnalysisClusterSync(ctx, log, resultTime);
		DataPrinter.printSeparatedFlows(ctx,resultFlow);
		DataPrinter.printAllFlows(ctx,printerAllFlow);

		System.out.println("Done!");

	}//fim do main



}
