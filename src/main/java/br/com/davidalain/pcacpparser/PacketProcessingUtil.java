package br.com.davidalain.pcacpparser;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map.Entry;

import br.com.davidalain.pcacpparser.main.Parameters;
import br.com.davidalain.pcapparser.mqtt.FactoryMQTT;
import br.com.davidalain.pcapparser.mqtt.MQTTFragment;
import br.com.davidalain.pcapparser.mqtt.MQTTPacket;
import br.com.davidalain.pcapparser.mqtt.MQTTPubAck;
import br.com.davidalain.pcapparser.mqtt.MQTTPubComplete;
import br.com.davidalain.pcapparser.mqtt.MQTTPublishMessage;
import io.pkts.buffer.Buffer;
import io.pkts.packet.IPPacket;
import io.pkts.packet.MACPacket;
import io.pkts.packet.Packet;
import io.pkts.packet.TCPPacket;
import io.pkts.packet.UDPPacket;
import io.pkts.packet.impl.ApplicationPacket;
import io.pkts.protocol.Protocol;

public class PacketProcessingUtil {

	public long getArrivalTime(final Packet packet) {
		return packet.getArrivalTime();
	}

	public String getSourceIP(final Packet packet) throws IOException {

		if(!packet.hasProtocol(Protocol.IPv4))
			return null;

		IPPacket ip = (IPPacket) packet.getPacket(Protocol.IPv4);
		return ip.getSourceIP();
	}

	public String getDestinationIP(final Packet packet) throws IOException {

		if(!packet.hasProtocol(Protocol.IPv4))
			return null;

		IPPacket ip = (IPPacket) packet.getPacket(Protocol.IPv4);
		return ip.getDestinationIP();
	}

	public int getSourcePort(final Packet packet) throws IOException {

		if(packet.hasProtocol(Protocol.TCP)) {
			TCPPacket ip = (TCPPacket) packet.getPacket(Protocol.TCP);
			return ip.getSourcePort();
		}

		if(packet.hasProtocol(Protocol.UDP)) {
			UDPPacket ip = (UDPPacket) packet.getPacket(Protocol.UDP);
			return ip.getSourcePort();
		}

		return -1;
	}

	public int getDestinationPort(final Packet packet) throws IOException {

		if(packet.hasProtocol(Protocol.TCP)) {
			TCPPacket ip = (TCPPacket) packet.getPacket(Protocol.TCP);
			return ip.getDestinationPort();
		}

		if(packet.hasProtocol(Protocol.UDP)) {
			UDPPacket ip = (UDPPacket) packet.getPacket(Protocol.UDP);
			return ip.getDestinationPort();
		}

		return -1;
	}

	public PacketBuffer processMACPacket(final MACPacket ethernetPacket, final Context ctx, final DataPrinter printer) throws IOException {

		final Buffer ethernetPayload = ethernetPacket.getPayload();
		final byte[] ethernetPayloadArray = ethernetPayload.getArray();
		final int ethernetPayloadLen = ethernetPayloadArray.length;
		PacketBuffer packetBuffer = null;

		printer.log.println("Ethernet frame size: \t"+
				"total=" + ethernetPacket.getTotalLength() + "\t"+
				"header=" + (ethernetPacket.getTotalLength() - ethernetPayloadLen) + "\t"+
				"payload=" + ethernetPayloadLen);


		if(ethernetPacket.getNextPacket() != null) {
			packetBuffer = new PacketBuffer(ethernetPacket.getNextPacket(), ethernetPacket.getPayload());
		}

		return packetBuffer;
	}

	public PacketBuffer processIPPacket(final PacketBuffer ipPacketBuffer, final Context ctx, final DataPrinter printer) throws IOException{

		if((ipPacketBuffer.getPacket() == null) || 
				(!ipPacketBuffer.getPacket().hasProtocol(Protocol.TCP) && !ipPacketBuffer.getPacket().hasProtocol(Protocol.UDP)))
			return null;

		final IPPacket ipPacket = (IPPacket) ipPacketBuffer.getPacket();
		final int ipPacketLen = ipPacketBuffer.getPayloadBuffer().getArray().length;
		final Buffer ipPayload = ipPacket.getPayload();
		final byte[] ipPayloadArray = ipPayload.getArray();
		final int ipPayloadLen = ipPayloadArray.length;
		PacketBuffer packetBuffer = null;

		printer.log.println("IP datagram size: \t\t"+
				"total=" + ipPacketLen + "\t"+
				"header=" + (ipPacketLen - ipPayloadLen) + "\t"+
				"payload=" + ipPayloadLen);

		if(ipPacket.getNextPacket() != null) {
			packetBuffer = new PacketBuffer(ipPacket.getNextPacket(), ipPacket.getPayload());
		}

		return packetBuffer;
	}

	public PacketBuffer processTCPPacket(final PacketBuffer tcpPacketBuffer, final Context ctx, final DataPrinter printer) throws IOException {

		final TCPPacket tcpPacket = (TCPPacket) tcpPacketBuffer.getPacket();
		final int tcpPacketLen = tcpPacketBuffer.getPayloadBuffer().getArray().length;
		final Buffer tcpPayload = tcpPacket.getPayload();
		PacketBuffer packetBuffer = null;

		final byte[] applicationPacketArray = tcpPayload != null ? tcpPayload.getArray() : null;
		final int applicationPacketLen = applicationPacketArray != null ? applicationPacketArray.length : 0;

		printer.log.println("TCP segment size: \t\t"+
				"total=" + tcpPacketLen + "\t"+
				"header=" + (tcpPacketLen - applicationPacketLen) + "\t"+
				"payload=" + applicationPacketLen);

		printer.log.println("Flow: " + new Flow(tcpPacket).toString());
		printer.log.println();

		printer.log.println("Application packet length:");
		printer.log.println(applicationPacketLen);
		printer.log.println("Application packet hexdump:");
		printer.log.println(HexPrinter.toStringHexDump(applicationPacketArray));
		printer.log.println("TCP payload as String:");
		printer.log.println(tcpPayload);

		if(tcpPacket.getNextPacket() != null) {
			packetBuffer = new PacketBuffer(tcpPacket.getNextPacket(), tcpPacket.getPayload());
		}

		return packetBuffer;
	}

	/**
	 * @param applicationPacketBuffer
	 * @param ctx
	 * @param printer
	 * @return	A MQTTPacket or null
	 * 			If return is a MQTT packet is because either conditions:
	 * 				The applicationPacketBuffer contains a full MQTTPacket as TCP payload, or
	 * 				The applicationPacketBuffer contanis the last fragment of a MQTTPacket that is assembled and returned.
	 * 				
	 * 			If return is null is because either conditions:
	 * 				The applicationPacketBuffer does not contain a MQTT packet, or
	 * 				The applicationPacketBuffer contains only a fragment of a MQTT packet.
	 * 
	 * @throws IOException
	 */
	public final MQTTPacket processApplicationPacket(final PacketBuffer applicationPacketBuffer, final Context ctx, final DataPrinter printer) throws IOException {

		if(applicationPacketBuffer == null)
			return null;

		final ApplicationPacket applicationPacket = (ApplicationPacket) applicationPacketBuffer.getPacket();
		final byte[] applicationPacketArray = applicationPacketBuffer.getPayloadBuffer().getArray();
		final int applicationPacketLen = applicationPacketArray.length;
		final Flow flow = new Flow(applicationPacket);

		MQTTPacket mqttPacket = null;

		/**
		 * TCP payload não é MQTT ou é um fragmento de um MQTT.
		 */
		if(!MQTTPacket.isMQTT(applicationPacketArray)) {

			printer.log.println("==== TCP payload (application packet) não é um MQTT completo: ===");

			/**
			 * Pacotes com menos de Parameters.MQTT_FRAGMENT_BYTES_THRESHOLD bytes são os possíveis fragmentos
			 */
			if(applicationPacketLen < Parameters.MQTT_FRAGMENT_BYTES_THRESHOLD) {

				printer.log.println("==== MQTT fragment: ===");
				MQTTFragment mqttFragment = ctx.getMapMqttFragments().get(flow);

				/** Ainda não tinha recebido pedaços de uma mensagem MQTT neste fluxo **/
				if(mqttFragment == null) {

					/** Verifica se é um possível pedaço de MQTT **/
					if(MQTTPacket.hasPacketType(applicationPacketArray)) {

						printer.log.println("==== primeiro fragmento ("+flow+") ===");

						mqttFragment = new MQTTFragment(applicationPacket.getArrivalTime()); //Primeiro fragmento

						System.arraycopy(applicationPacketArray, 0,
								mqttFragment.getPartialMessageBuffer(), mqttFragment.getPartialMessageLen(), applicationPacketLen);

						mqttFragment.addPartialMessageLen(applicationPacketLen);

						printer.log.println(HexPrinter.toStringHexDump(applicationPacketArray));

						ctx.getMapMqttFragments().put(flow, mqttFragment);

					}
					/** Não é um pedaço de MQTT. Processa como outro pacote qualquer de camada de aplicação **/
					else {
						//Como não é MQTT e é pequeno demais pra uma mensagem sync então nada faz!
						printer.log.println("==== não tem formato de MQTT ou é pequeno demais ===");
					}
				}
				/** Está recebendo pedaços de uma mensagem MQTT neste fluxo, então armazena os bytes recebidos **/
				else {

					printer.log.println("==== outro fragmento ("+flow+")===");

					System.arraycopy(applicationPacketArray, 0,
							mqttFragment.getPartialMessageBuffer(), mqttFragment.getPartialMessageLen(), applicationPacketLen);

					mqttFragment.addPartialMessageLen(applicationPacketLen);

					printer.log.println("fragmento recebido:");
					printer.log.println(HexPrinter.toStringHexDump(applicationPacketArray));

					printer.log.println("fragmento parcial:");
					printer.log.println(HexPrinter.toStringHexDump(mqttFragment.getPartialMessageBuffer(), 0, mqttFragment.getPartialMessageLen()));

					switch (mqttFragment.check()) {
					case -1: //mensagem inválida
						ctx.getMapMqttFragments().remove(flow);
						printer.log.println("==== fragmentos formaram mensagem MQTT inválida ===");
						break;
					case 0: //mensagem incompleta (ainda tem bytes pra receber)
						printer.log.println("==== mensagem incompleta, ainda falta receber fragmentos ===");
						break;
					case 1: //mensagem completa (processar o pacote recebido)
						mqttPacket = mqttFragment.buildMQTTPacket();
						ctx.getMapMqttFragments().remove(flow);

						printer.log.println("==== mensagem MQTT remontada ===");
						break;
					}

				}

			}
			/** (applicationPacketLen >= Parameters.MQTT_FRAGMENT_BYTES_THRESHOLD) **/
			else {

				printer.log.println("==== Não é um fragmento MQTT (applicationPacketLen >= Parameters.MQTT_FRAGMENT_BYTES_THRESHOLD=("+Parameters.MQTT_FRAGMENT_BYTES_THRESHOLD+")) ===");
			}

		}
		/**
		 * É um pacote MQTT
		 */
		else {

			mqttPacket = new FactoryMQTT().getMQTTPacket(applicationPacketArray, applicationPacket.getArrivalTime());
		}

		return mqttPacket;
	}

	public void processRTTPackets(final ApplicationPacket applicationPacket, final MQTTPacket mqttPacket, final Context ctx, final DataPrinter printer) throws IOException {

		/**
		 * Recebeu um MQTT ou remontou um MQTT
		 */
		if(MQTTPacket.isMQTT(mqttPacket.getData())) {

			printer.log.println("==== MQTT: ===");
			printer.log.println("pktNum="+ ctx.getPackerNumber() + ", É um pacote MQTT");

			printer.log.println("msgType:"+mqttPacket.getMessageTypeEnum());
			printer.log.println("DUPFlag:"+mqttPacket.getDupFlag());
			printer.log.println("QoS:"+mqttPacket.getQoS());
			printer.log.println("RetainFlag:"+mqttPacket.getRetainFlag());
			printer.log.println("msgLen:"+mqttPacket.getMessageLength());

			int qos = mqttPacket.getQoS();

			/**
			 * Client1 enviando PUBLISH Message para o Broker
			 */
			if(
					applicationPacket.getSourceIP().equals(Parameters.CLIENT1_IP) &&
					//tcpPacket.getDestinationIP().equals(Parameters.CLIENT1_IP) &&
					mqttPacket.getMessageType() == MQTTPacket.PacketType.PUBLISH.value ) 
			{
				ctx.getLastPublishSentToBroker(qos).add((MQTTPublishMessage)mqttPacket);

				printer.log.println("==== Cliente1 enviando PUBLISH ===");

				printer.log.println("==== PUBLISH QoS "+qos+" - (out)"+
						", pktNum: "+ctx.getPackerNumber()+
						", time(us): "+mqttPacket.getArrivalTime()+
						", msgId: "+ ((MQTTPublishMessage)mqttPacket).getMessageIdentifier()+
						", listLen: "+ctx.getLastPublishSentToBroker(qos).size()+
						", mapLen: "+ctx.getMqttTXvsRXMap(qos).size()+
						"===");
			}
			/**
			 * QoS 0 - Client1 recebendo o PUBLISH do Broker
			 */
			else if((applicationPacket.getDestinationIP().equals(Parameters.CLIENT1_IP)) &&
					(mqttPacket.getMessageType() == MQTTPacket.PacketType.PUBLISH.value))
			{

				printer.log.println("==== Cliente1 recebendo PUBLISH ===");

				/**
				 * Duas mensagens PUBLISH são iguais quando tem o mesmo tipo, tamanho, tópico e mensagem.
				 * Não conta o tempo de chegada dos pacotes.
				 * 
				 * @note indexOf() usa equals().
				 * 
				 * @see equals() em MQTTPacket
				 */
				int index = ctx.getLastPublishSentToBroker(qos).lastIndexOf(mqttPacket);
				if(index >= 0) {

					printer.log.println("== Cliente recebendo do broker a mensagem de Publish que foi enviada anteriormente ==");

					MQTTPublishMessage publishRx =  (MQTTPublishMessage) mqttPacket;
					printer.log.println("topic:"+publishRx.getTopic());
					printer.log.println("message:"+publishRx.getMessage());

					ctx.getMqttTXvsRXMap(qos).put(ctx.getLastPublishSentToBroker(qos).remove(index), publishRx);

					printer.log.println("==== PUBLISH QoS "+qos+" - (in)"+
							", pktNum: "+ctx.getPackerNumber()+
							", time(us): "+mqttPacket.getArrivalTime()+
							", msgId: "+ publishRx.getMessageIdentifier()+
							", listLen: "+ctx.getLastPublishSentToBroker(qos).size()+
							", mapLen: "+ctx.getMqttTXvsRXMap(qos).size()+
							"===");

					printer.log.println("== QoS "+qos+" - PUBLISH recebido é o mesmo enviado (topico e mensagem são iguais) ==");

				} else {
					/**
					 * Erro:
					 * 		Cliente1 recebendo Publish de uma mensagem que não foi enviada pelo Cliente1.
					 * 		Isto não faz parte do experimento.
					 * 		O cliente não deveria estar escrito em outro tópico, e sim apenas naquele em que ele mesmo publica.
					 */
				}

			}
			/**
			 * QoS 1 - Parte 2 - Client1 enviando o PUBACK para o broker.
			 * 
			 * Guarda o pacote de PUBACK e substitui no lugar do pacote de PUBLISH recebida anteriormente do broker.
			 * 
			 * PUBACK é um pacote do fluxo do QoS 1, mas sempre tem QoS 0 no campo de cabeçalho
			 */
			else if((applicationPacket.getSourceIP().equals(Parameters.CLIENT1_IP)) &&
					(mqttPacket.getMessageType() == MQTTPacket.PacketType.PUBACK.value))
			{

				printer.log.println(" == QoS 1 - PUBACK enviado ==");

				for(Entry<MQTTPacket,MQTTPacket> pair : ctx.getMqttTXvsRXMap(1 /*qos*/).entrySet()) {

					/**
					 * Checa apenas os pares cujo valor ainda é um PUBLISH, ou seja, ainda não foi substituido pelo PUBACK
					 */
					if(pair.getValue() instanceof MQTTPublishMessage) {

						MQTTPublishMessage publishRx = (MQTTPublishMessage) pair.getValue();

						MQTTPubAck pubAckRx = (MQTTPubAck) mqttPacket;

						//Substitui o MQTT Publish recebido pelo Publish ACK enviado (que contém o tempo final).
						//O PUBLISH que é a chave do mapa contém o tempo inicial.
						if(publishRx.getMessageIdentifier() == pubAckRx.getMessageIdentifier()) {
							ctx.getMqttTXvsRXMap(1 /*qos*/).replace(pair.getKey(), pubAckRx);

							printer.log.println("==== PUBACK QoS 1 - (out)"+
									", pktNum: "+ctx.getPackerNumber()+
									", time(us): "+mqttPacket.getArrivalTime()+
									", msgId: "+ ((MQTTPubAck)mqttPacket).getMessageIdentifier()+
									", listLen: "+ctx.getLastPublishSentToBroker(1).size()+
									", mapLen: "+ctx.getMqttTXvsRXMap(1).size()+
									"===");

							break;
						}
					}

				}

			}
			/**
			 * QoS 2 - Parte 2 - Client1 enviando o PUBCOMPLETE para o broker
			 * 
			 * Guarda o pacote de PUBCOMPLETE e substitui no lugar do pacote de PUBLISH recebido anteriormente do broker.
			 * 
			 * PUBCOMPLETE é um pacote do fluxo do QoS 2, mas sempre tem QoS 0 no campo de cabeçalho.
			 */
			else if((applicationPacket.getSourceIP().equals(Parameters.CLIENT1_IP)) &&
					(mqttPacket.getMessageType() == MQTTPacket.PacketType.PUBCOMP.value))
			{

				printer.log.println(" == PUBCOMP enviado ==");

				for(Entry<MQTTPacket,MQTTPacket> pair : ctx.getMqttTXvsRXMap(2 /*qos*/).entrySet()) {

					/**
					 * Checa apenas os pares cujo valor ainda é um PUBLISH, ou seja, ainda não foi substituido pelo PUBCOMP
					 */
					if(pair.getValue() instanceof MQTTPublishMessage) {

						MQTTPublishMessage publishRx = (MQTTPublishMessage) pair.getValue();

						MQTTPubComplete pubCompleteRx = (MQTTPubComplete) mqttPacket;

						//Substitui o MQTT PUblish recebido pelo Publish Complete enviado (que contém o tempo final).
						//O PUBLISH que é a chave do mapa contém o tempo inicial.
						if(publishRx.getMessageIdentifier() == pubCompleteRx.getMessageIdentifier()) {
							ctx.getMqttTXvsRXMap(2 /*qos*/).replace(pair.getKey(), pubCompleteRx);

							printer.log.println("==== PUBCOMP QoS 2 - (out)"+
									", pktNum: "+ctx.getPackerNumber()+
									", time(us): "+mqttPacket.getArrivalTime()+
									", msgId: "+ ((MQTTPubComplete)mqttPacket).getMessageIdentifier()+
									", listLen: "+ctx.getLastPublishSentToBroker(2).size()+
									", mapLen: "+ctx.getMqttTXvsRXMap(2).size()+
									"===");

							break;
						}

					}


				}

			}

		}//é MQTT

	}//fim do método

	/**
	 * Verifica se o pacote recebido é um TCP sync e processa o tempo caso seja.
	 * 
	 * @param applicationPacket
	 * @param mqttPacket
	 * @param ctx
	 * @param printer
	 * @throws IOException
	 */
	public void processMQTTandSyncPackets(final PacketBuffer applicationPacketBuffer, final MQTTPacket mqttPacket, final Context ctx, final DataPrinter printer) throws IOException {

		if(applicationPacketBuffer == null)
			return;

		final ApplicationPacket applicationPacket = (ApplicationPacket) applicationPacketBuffer.getPacket();
		final byte[] applicationPacketArray = applicationPacketBuffer.getPayloadBuffer().getArray();
		final int applicationPacketLen = applicationPacketArray.length;

		/**
		 * É um pacote MQTT
		 */
		if(mqttPacket != null) {

			printer.log.println("==== É um MQTT: ===");
			printer.log.println("msgType:"+mqttPacket.getMessageTypeEnum());
			printer.log.println("DUPFlag:"+mqttPacket.getDupFlag());
			printer.log.println("QoS:"+mqttPacket.getQoS());
			printer.log.println("RetainFlag:"+mqttPacket.getRetainFlag());
			printer.log.println("msgLen:"+mqttPacket.getMessageLength());

			/**
			 * Publish Message sendo enviado para o broker1
			 */
			if(
					//					applicationPacket.getSourceIP().equals(Parameters.CLIENT1_IP) && 
					applicationPacket.getDestinationIP().equals(Parameters.BROKER1_IP) &&
					mqttPacket.getMessageType() == MQTTPacket.PacketType.PUBLISH.value)
			{
				ctx.getLastPublishSentToBroker(mqttPacket.getQoS()).add((MQTTPublishMessage)mqttPacket);

			} else {

				//				printer.log.println(/* "{IP de origem = ("+applicationPacket.getSourceIP()+") != ("+Parameters.CLIENT1_IP+")} ou"+ */
				//						" {IP de destino = ("+applicationPacket.getDestinationIP()+") != ("+Parameters.BROKER1_IP+")}");
			}

		}
		/**
		 * Não é um pacote MQTT.
		 * 
		 * Verifica se é um pacote se SYNC.
		 */
		else {

			if(applicationPacketLen > 4) {
				int tcpSyncMessageLen = ByteBuffer.wrap(applicationPacketArray, 0, 4).getInt();

				/**
				 * É um pacote de SYNC
				 */
				if(tcpSyncMessageLen == applicationPacketLen - 4) {

					printer.log.println("==== É um pacote de SYNC ===");
					printer.log.println("packetLen="+applicationPacketLen);
					printer.log.println("syncLen="+tcpSyncMessageLen);

					printer.log.println(HexPrinter.toStringHexDump(applicationPacketArray));

				} else {

					//Não é um MQTT e também não é um pacote de SYNC.
					//Não há o que procurar neste pacote.
					return;
				}
			}

			for(int qosId = 0 ; qosId < 3 ; qosId++) {

				MQTTPublishMessage publishToRemove = null;

				for(MQTTPublishMessage lastPublish : ctx.getLastPublishSentToBroker(qosId)) {

					printer.log.println("qos="+qosId+", lastMqtt="+lastPublish);

					if(lastPublish != null) {

						if(lastPublish.getMessageType() == MQTTPacket.PacketType.PUBLISH.value) {

							MQTTPublishMessage mqttPublish = (MQTTPublishMessage) lastPublish;

							byte[] topic = mqttPublish.getTopicArray();
							byte[] message = mqttPublish.getMessageArray();

							/**
							 * Broker1 enviando mensagem de sync para Broker2
							 */
							if(applicationPacket.getSourceIP().equals(Parameters.BROKER1_IP) && applicationPacket.getDestinationIP().equals(Parameters.BROKER2_IP)) {

								//FIXME:
								//Note: this is a not safe check, because topic name and message can be equals each other (same content) or shorter than necessary to guarantee correct working
								//The correct way to check is analyze if topic and message are into TCP payload at the their specific correct positions
								if(
										ArraysUtil.contains(applicationPacketArray, topic) &&
										ArraysUtil.contains(applicationPacketArray, message))
								{

									//This is the TCP Segment that broker uses to synchronize last MQTT Publish Message to another broker in the cluster
									int qos = lastPublish.getQoS();
									printer.log.println("qos="+qos);

									ctx.getMqttToTcpBrokerSyncMap(qos).put(lastPublish, applicationPacket);
									publishToRemove = lastPublish;

								} else {

									printer.log.println("ArraysUtil.contains(tcpPayloadArray, topic)="+ArraysUtil.contains(applicationPacketArray, topic));
									printer.log.println(HexPrinter.toStringHexDump(topic));

									printer.log.println("ArraysUtil.contains(tcpPayloadArray, message)="+ArraysUtil.contains(applicationPacketArray, message));
									printer.log.println(HexPrinter.toStringHexDump(message));
								}

							}

						}

					} //fim do if(lastPublish != null) 

				} //fim do for each


				//Remove o publish já associado com o SYNC
				if(publishToRemove != null)
					ctx.getLastPublishSentToBroker(qosId).remove(publishToRemove);

			}

		}



	}//fim do método

}
