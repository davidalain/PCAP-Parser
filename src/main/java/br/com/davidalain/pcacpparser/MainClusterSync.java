package br.com.davidalain.pcacpparser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;
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

public class MainClusterSync {

	/**
	 * TODO:
	 * 
	 * @see https://github.com/emqtt/emqttd/wiki/$SYS-Topics
	 */
	
	public static final String BROKER1_IP = "172.16.0.3";
	public static final String BROKER2_IP = "172.16.0.2";
	public static final String CLIENT1_IP = "192.168.43.1";
	public static final String CLIENT2_IP = "192.168.43.2"; //FIXME

	public static final String FILEPATH = "trace_747a0d82_2_Broker1ClusterDoisBrokers.pcap";
	
	public static final String PREFIX = FILEPATH.substring(0, FILEPATH.length() - 5);
	public static final String OUTPUT_PATH = "output/";
	public static final String OUTPUT_FLOW_PATH = "output/flow/";

	public static void main(String[] args) throws IOException {

		final Pcap pcap = Pcap.openStream(FILEPATH);
		final Context ctx = new Context(BROKER1_IP, BROKER2_IP, CLIENT1_IP, CLIENT2_IP);
		final FactoryMQTT factory = new FactoryMQTT();
		final PrintStream log = new PrintStream(new File(OUTPUT_PATH+PREFIX+"_log.txt"));
		final PrintStream resultTime = new PrintStream(new File(OUTPUT_PATH+PREFIX+"_resultTime.txt"));
		final PrintStream resultFlow = new PrintStream(new File(OUTPUT_PATH+PREFIX+"_resultFlow.txt"));
		final PrintStream printerAllFlow = new PrintStream(new File(OUTPUT_FLOW_PATH+PREFIX+"_allFlows.csv"));

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
									if(tcpPacket.getSourceIP().equals(CLIENT1_IP) && tcpPacket.getDestinationIP().equals(BROKER1_IP)) {
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
											if(tcpPacket.getSourceIP().equals(BROKER1_IP) && tcpPacket.getDestinationIP().equals(BROKER2_IP)) {

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

		
		printQoSTimeAnalysis(ctx, log, resultTime);
		printSeparatedFlows(ctx,resultFlow);
		printAllFlows(ctx,printerAllFlow);
		
		System.out.println("Done!");
		
	}//fim do main
	
	
	public static void printQoSTimeAnalysis(Context ctx, final PrintStream log, final PrintStream resultTime) throws FileNotFoundException {
		
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

			resultTime.println("============================= QoS = "+qos+" ===============================");
			double avg = 0;
			double median = 0;
			double max = ctx.getTimes(qos).size() == 0 ? Double.NaN : Collections.max(ctx.getTimes(qos));
			double min = ctx.getTimes(qos).size() == 0 ? Double.NaN : Collections.min(ctx.getTimes(qos));
			for(long l : ctx.getTimes(qos)) {
				avg += (double)l;
			}
			avg /= (double)ctx.getTimes(qos).size();
			resultTime.println(ctx.getTimes(qos));
			resultTime.println("max(us)="+max+", max(ms)="+(max/1000.0)+", max(s)="+(max/(1000.0*1000.0)));
			resultTime.println("min(us)="+min+", min(ms)="+(min/1000.0)+", min(s)="+(min/(1000.0*1000.0)));
			resultTime.println("avg(us)="+avg+", avg(ms)="+(avg/1000.0)+", avg(s)="+(avg/(1000.0*1000.0)));

			Collections.sort(ctx.getTimes(qos));
			median = ctx.getTimes(qos).size() == 0 ? Double.NaN : ctx.getTimes(qos).get(ctx.getTimes(qos).size()/2);
			resultTime.println("median(us)="+median+", median(ms)="+(median/1000.0)+", median(s)="+(median/(1000.0*1000.0)));
			resultTime.println("========================================================================");
		}
		log.println("########################################################################");
		
	}
	
	public static void printSeparatedFlows(final Context ctx, final PrintStream resultFlow) throws FileNotFoundException {
		
		/**
		 * Tempo inicial e final para ser mostrado em segundos (diferença do tempo real de chegada/saida dos pacotes em relação ao primeiro pacote capturado)
		 */
		final long firstSecond = 0;
		final long lastSecond = (ctx.getEndTimeUs() - ctx.getStartTimeUs()) / (1000L * 1000L);
		
		/**
		 * Impressão de cada fluxo em um arquivo separado
		 */
		for(Entry<Flow, Map<Long, Long>> pairFlowThroughtput : ctx.getMapFlowThroughput().entrySet()) {
			resultFlow.println("========================================================================");
			
			final Flow flow = pairFlowThroughtput.getKey();
			final PrintStream printerCurrentFlow = new PrintStream(new File(OUTPUT_FLOW_PATH+PREFIX+"_resultFlow_"+flow.toStringForFileName()+".csv"));
			
			resultFlow.println("Flow: " + flow);
			resultFlow.println();
			resultFlow.println("second, bytes"); //CSV like
			
			printerCurrentFlow.println("second, bytes"); //CSV like
			
			Map<Long, Long> mapThroughput = pairFlowThroughtput.getValue();
			for(long second = firstSecond ; second <= lastSecond; second++) {
				
				Long bytes = mapThroughput.get(second);
				if(bytes == null) bytes = 0L;
				
				resultFlow.println(second + ", " + bytes); //imprime no arquivo com todos os fluxos
				printerCurrentFlow.println(second + ", " + bytes); //imprime no arquivo separado por fluxo
			}
			
			resultFlow.println("========================================================================");
		}
		
	}
	
	public static void printAllFlows(final Context ctx, final PrintStream printerAllFlow) throws FileNotFoundException {
		
		/**
		 * Tempo inicial e final para ser mostrado em segundos (diferença do tempo real de chegada/saida dos pacotes em relação ao primeiro pacote capturado)
		 */
		final long firstSecond = 0;
		final long lastSecond = (ctx.getEndTimeUs() - ctx.getStartTimeUs()) / (1000L * 1000L);
		
		/**
		 * Impressão de todos os fluxos em um único arquivo
		 */
		
		final int columnCount = ctx.getMapFlowThroughput().entrySet().size();
		final int lineCount = (int) lastSecond + 1; //+1 pq começa a contar do zero
		final String[] headers = new String[columnCount];
		final long[][] matrixAllFlow = new long[lineCount][columnCount];
		int column = 0;
		
		for(Entry<Flow, Map<Long, Long>> pairFlowThroughtput : ctx.getMapFlowThroughput().entrySet()) {
			
			final Flow flow = pairFlowThroughtput.getKey();
			final Map<Long, Long> mapThroughput = pairFlowThroughtput.getValue();
			
			headers[column] = flow.toString(); //Preenche o cabeçalho
			
			for(long second = firstSecond, line = 0 ; second <= lastSecond; second++, line++) {
				
				Long bytes = mapThroughput.get(second);
				if(bytes == null) bytes = 0L;
				
				matrixAllFlow[(int)line][column] = bytes.longValue();
			}

			column++;
		}
		
		printerAllFlow.print("second");
		for(String header : headers) {
			printerAllFlow.print(", " + header);
		}
		printerAllFlow.println();

		int secondValue = 0;
		for(long[] lineValues : matrixAllFlow) {
			printerAllFlow.print(secondValue++);
			for(long value : lineValues) {
				printerAllFlow.print(", " + value);	
			}
			printerAllFlow.println();
		}
	}
}