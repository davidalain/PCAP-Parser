package br.com.davidalain.pcacpparser.main;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Map.Entry;

import br.com.davidalain.pcacpparser.Context;
import br.com.davidalain.pcacpparser.Flow;
import br.com.davidalain.pcacpparser.HexPrinter;
import br.com.davidalain.pcacpparser.PacketUtil;
import br.com.davidalain.pcapparser.mqtt.FactoryMQTT;
import br.com.davidalain.pcapparser.mqtt.MQTTPacket;
import br.com.davidalain.pcapparser.mqtt.MQTTPubAck;
import br.com.davidalain.pcapparser.mqtt.MQTTPubComplete;
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

public class MainPublishMessageRTT {

	/**
	 * TODO:
	 * 
	 * @see https://github.com/emqtt/emqttd/wiki/$SYS-Topics
	 */

	public static void main(String[] args) throws IOException {

		System.out.println("Running...");

		/**
		 * Cria o caminho (n�o d� erro caso j� exista)
		 */
		new File(Parameters.OUTPUT_FLOW_PATH).mkdirs();

		final Pcap pcap = Pcap.openStream(Parameters.FILEPATH);
		final Context ctx = new Context(Parameters.BROKER1_IP, Parameters.BROKER2_IP, Parameters.CLIENT1_IP, Parameters.CLIENT2_IP);
		final FactoryMQTT factory = new FactoryMQTT();
		final PrintStream log = new PrintStream(new File(Parameters.OUTPUT_PATH+Parameters.PREFIX+"_log.txt"));
		final PrintStream resultTime = new PrintStream(new File(Parameters.OUTPUT_PATH+Parameters.PREFIX+"_resultTime.txt"));
		final PrintStream resultFlow = new PrintStream(new File(Parameters.OUTPUT_PATH+Parameters.PREFIX+"_resultFlow.txt"));
		final PrintStream printerAllFlow = new PrintStream(new File(Parameters.OUTPUT_FLOW_PATH+Parameters.PREFIX+"_allFlows.csv"));

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

						log.println("Ethernet frame size: \t"+
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

									int qos = mqttPacket.getQoS();
									/**
									 * Client1 enviando Publish Message para o Broker
									 */
									if(
											tcpPacket.getSourceIP().equals(Parameters.CLIENT1_IP) &&
											//tcpPacket.getDestinationIP().equals(Parameters.CLIENT1_IP) &&
											mqttPacket.getMessageType() == MQTTPacket.PacketType.PUBLISH.value ) 
									{
										ctx.getLastMqttReceived(mqttPacket.getQoS()).add(mqttPacket);

										log.println("CLIENTE ENVIANDO MQTT ####");
									}

									/**
									 * QoS 0 - Client1 recebendo o PUBLISH do Broker
									 */
									else if((tcpPacket.getDestinationIP().equals(Parameters.CLIENT1_IP)) &&
											(qos == 0) &&
											(mqttPacket.getMessageType() == MQTTPacket.PacketType.PUBLISH.value))
									{

										log.println("CLIENTE RECEBENDO MQTT ****");

										/**
										 * Duas mensagens PUBLISH s�o iguais quando tem o mesmo tipo, tamanho, t�pico e mensagem.
										 * N�o conta o tempo de chegada dos pacotes.
										 * 
										 * @note indexOf() usa equals().
										 * 
										 * @see equals() em MQTTPacket
										 */
										int index = ctx.getLastMqttReceived(qos).indexOf(mqttPacket);
										if(index >= 0) {

											log.println("== Cliente recebendo do broker a mensagem de Publish que foi enviada anteriormente ==");
											MQTTPublishMessage publish =  (MQTTPublishMessage) mqttPacket;
											log.println("topico:"+publish.getTopic());
											log.println("message:"+publish.getMessage());

											ctx.getMqttTXvsRXMap(qos).put(ctx.getLastMqttReceived(qos).remove(index), mqttPacket);
										} else {
											/**
											 * Erro estranho: Cliente1 recebendo Publish de uma mensagem que n�o foi enviada pelo Cliente1. 
											 */
										}

									}
									/**
									 * QoS 1 - Parte 1 - Client1 recebendo o PUBLISH do Broker
									 * 
									 * Guardar o PUBLISH enviado pelo broker para o cliente que cont�m o mesmo 'Message ID' da mensagem
									 * de PUBACK que o cliente vai enviar para o broker.
									 * O tempo ser� contabilizado atrav�s dessa mensagem PUBACK.
									 */
									else if((tcpPacket.getDestinationIP().equals(Parameters.CLIENT1_IP)) &&
											(qos == 1) &&
											(mqttPacket.getMessageType() == MQTTPacket.PacketType.PUBLISH.value))
									{

										log.println("== Cliente recebeu PUBLISH ==");

										/**
										 * @note indexOf() usa equals().
										 * 
										 * @see equals() em MQTTPacket
										 */
										int index = ctx.getLastMqttReceived(qos).indexOf(mqttPacket);
										if(index >= 0) {

											log.println("== PUBLISH recebido � o mesmo enviado (topico e mensagem s�o iguais) ==");

											//Guarda a mensagem de publish recebido que cont�m o 'Message ID' do PUBACK que vai ser enviado para depois trocar no mapa essa PUBLISH pelo PUBACK.
											ctx.getMqttTXvsRXMap(qos).put(ctx.getLastMqttReceived(qos).remove(index)	, mqttPacket);
										} else {
											/**
											 * Erro estranho: Cliente1 recebendo Publish de uma mensagem que n�o foi enviada pelo Cliente1. 
											 */
										}

									}
									/**
									 * QoS 1 - Parte 2 - Client1 enviando o PUBACK para o broker
									 */
									else if((tcpPacket.getSourceIP().equals(Parameters.CLIENT1_IP)) &&
											(mqttPacket.getQoS() == 0) && //o PUBACK (enviado como confirma��o de um PUBLISH com QoS 1) tem QoS 0
											(mqttPacket.getMessageType() == MQTTPacket.PacketType.PUBACK.value))
									{

										log.println(" == PUBACK enviado ==");

										for(Entry<MQTTPacket,MQTTPacket> pair : ctx.getMqttTXvsRXMap(1).entrySet()) {

											MQTTPublishMessage publishRx = (MQTTPublishMessage) pair.getValue();
											MQTTPubAck pubAckRx = (MQTTPubAck) mqttPacket;

											//Substitui o MQTT PUblish recebido pelo Publish ACK enviado (que cont�m o tempo final).
											//O PUBLISH que � a chave do mapa cont�m o tempo inicial.
											if(publishRx.getMessageIdentifier() == pubAckRx.getMessageIdentifier()) {
												ctx.getMqttTXvsRXMap(1).put(pair.getKey(), pubAckRx);
												break;
											}

										}

									}
									/**
									 * QoS 2 - Client1 recebendo o PUBLISH do Broker
									 * 
									 * Guardar o PUBLISH enviado pelo broker para o cliente que cont�m o mesmo 'Message ID' da mensagem
									 * de PUBCOMP que o cliente vai enviar para o broker.
									 * O tempo ser� contabilizado atrav�s dessa mensagem PUBCOMP.
									 */
									else if((tcpPacket.getDestinationIP().equals(Parameters.CLIENT1_IP)) &&
											(qos == 2) &&
											(mqttPacket.getMessageTypeEnum().equals(MQTTPacket.PacketType.PUBLISH))) 
									{

										/**
										 * @note indexOf() usa equals().
										 * 
										 * @see equals() em MQTTPacket
										 */
										int index = ctx.getLastMqttReceived(qos).indexOf(mqttPacket);
										if(index >= 0) {

											log.println("CLIENTE RECEBENDO RESPOSTA DE ENVIO MQTT @@@@");

											ctx.getMqttTXvsRXMap(qos).put(ctx.getLastMqttReceived(qos).remove(index), mqttPacket);
										} else {
											/**
											 * Erro estranho: Cliente1 recebendo Publish de uma mensagem que n�o foi enviada pelo Cliente1. 
											 */
										}

									}
									/**
									 * QoS 2 - Parte 2 - Client1 enviando o PUBCOMPLETE para o broker
									 */
									else if((tcpPacket.getSourceIP().equals(Parameters.CLIENT1_IP)) &&
											(mqttPacket.getQoS() == 0) && //o PUBCOMP (enviado como confirma��o de um PUBLISH com QoS 2) tem QoS 0
											(mqttPacket.getMessageType() == MQTTPacket.PacketType.PUBCOMP.value))
									{

										log.println(" == PUBCOMP enviado ==");

										for(Entry<MQTTPacket,MQTTPacket> pair : ctx.getMqttTXvsRXMap(2).entrySet()) {

											MQTTPublishMessage publishRx = (MQTTPublishMessage) pair.getValue();
											MQTTPubComplete pubCompleteRx = (MQTTPubComplete) mqttPacket;

											//Substitui o MQTT PUblish recebido pelo Publish Complete enviado (que cont�m o tempo final).
											//O PUBLISH que � a chave do mapa cont�m o tempo inicial.
											if(publishRx.getMessageIdentifier() == pubCompleteRx.getMessageIdentifier()) {
												ctx.getMqttTXvsRXMap(2).put(pair.getKey(), pubCompleteRx);
												break;
											}

										}

									}

								}
								/**
								 * N�o � um pacote MQTT
								 */
								else {
									
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
							log.println("Another protocol:" + packet.getProtocol());
						}

						log.println("============================================================");					
					}
				}
				return true;
			}
		});

		if(ctx.getMqttTXvsRXMap(0).isEmpty() &&
				ctx.getMqttTXvsRXMap(1).isEmpty() &&
				ctx.getMqttTXvsRXMap(2).isEmpty())
		{
			System.err.println("Nenhum pacote MQTT foi endere�ado de/para "+Parameters.CLIENT1_IP+".");
			System.err.println("� poss�vel que o endere�o IP do cliente esteja errado ou n�o h� messagens MQTT no arquivo " + Parameters.FILEPATH);
			System.err.println("Ou est� acontecendo fragmenta��o dos segmentos das mensagens MQTT.");

			System.err.println("Veja o arquivo '" + Parameters.OUTPUT_PATH+Parameters.PREFIX+"_log.txt'");
		}

		DataPrinter.printQoSTimeAnalysisRTT(ctx, log, resultTime);
		DataPrinter.printSeparatedFlows(ctx,resultFlow);
		DataPrinter.printAllFlows(ctx,printerAllFlow);

		System.out.println("Done!");

	}//fim do main



}