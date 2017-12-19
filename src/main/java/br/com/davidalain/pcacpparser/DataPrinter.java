package br.com.davidalain.pcacpparser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

import br.com.davidalain.pcacpparser.main.Parameters;
import br.com.davidalain.pcapparser.mqtt.MQTTPacket;
import io.pkts.packet.impl.ApplicationPacket;

public class DataPrinter {

	public final PrintStream log;
	public final PrintStream resultTime;
	public final PrintStream resultFlow;
	public final PrintStream printerAllFlow;

	public DataPrinter() throws FileNotFoundException {
		
		/**
		 * Cria o caminho, caso não exista
		 */
		if(!new File(Parameters.OUTPUT_DIR_PATH).exists())
		new File(Parameters.OUTPUT_FLOW_DIR_PATH).mkdirs();
		
		log = new PrintStream(new File(Parameters.LOG_FILEPATH));
		resultTime = new PrintStream(new File(Parameters.RESULT_TIME_FILEPATH));
		resultFlow = new PrintStream(new File(Parameters.RESULT_FLOW_FILEPATH));
		printerAllFlow = new PrintStream(new File(Parameters.ALL_FLOW_CSV_FILEPATH));
	}

	/**
	 * Faz a análise da diferença de tempo de:
	 * 	um Cliente1 enviar um Publish Message para o Broker1,
	 * 	Broker1 sincronizar a mensagem com o Broker2,
	 * 	Broker2 retransmitir o Publish Message para o Cliente2. 
	 * 
	 * 	Nota: Cliente1 e Cliente2 são dois processos distintos no mesmo hospedeiro. 
	 * 
	 * @param ctx
	 * @param log
	 * @param resultTime
	 * @throws FileNotFoundException
	 */
	public void printQoSTimeAnalysisRTT(Context ctx) throws FileNotFoundException {

		log.println("########################################################################");
		for(int qos = 0 ; qos < Context.QOS_QUANTITY ; qos++) {
			log.println("******************************* QoS = "+qos+" ****************************");
			for(Entry<MQTTPacket, MQTTPacket> pair : ctx.getMqttTXvsRXMap(qos).entrySet()) {
				log.println("MQTT arrivalTime:" + pair.getKey().getArrivalTime());
				log.println(HexPrinter.toStringHexDump(pair.getKey().getData()));
				log.println("MQTT arrivalTime:" + pair.getKey().getArrivalTime());
				log.println(HexPrinter.toStringHexDump(pair.getValue().getData()));

				long diffTime = (pair.getValue().getArrivalTime() - pair.getKey().getArrivalTime());
				log.println("difftime(us) = " + diffTime);

				ctx.getTimes(qos).add(diffTime);
			}
			log.println("**************************************************************************");

			resultTime.println("============================= QoS = "+qos+" ===============================");
			double[] timeMs = new double[ctx.getTimes(qos).size()];
			double[] timeS = new double[ctx.getTimes(qos).size()];
			double avg = 0;
			double median = 0;
			double max = ctx.getTimes(qos).size() == 0 ? Double.NaN : Collections.max(ctx.getTimes(qos));
			double min = ctx.getTimes(qos).size() == 0 ? Double.NaN : Collections.min(ctx.getTimes(qos));
			int i = 0;
			for(long l : ctx.getTimes(qos)) {
				avg += (double)l;
				
				timeMs[i] = ((double)l)/1000.0;
				timeS[i] = ((double)l)/(1000.0 * 1000.0);
				i++;
			}
			avg /= (double)ctx.getTimes(qos).size();
			resultTime.println("us: "+ctx.getTimes(qos));
			resultTime.println("ms: "+Arrays.toString(timeMs));
			resultTime.println("s: "+Arrays.toString(timeS));
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

	public void printSeparatedFlows(final Context ctx) throws FileNotFoundException {

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
			final PrintStream printerCurrentFlow = new PrintStream(new File(Parameters.resultFlowCsvFilePath(flow)));

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

	public void printAllFlows(final Context ctx) throws FileNotFoundException {

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
		final String[] headerFlowsStr = new String[columnCount];
		final long[][] matrixAllFlow = new long[lineCount][columnCount];
		int column = 0;

		/**
		 * Lê o mapa de todos os fluxos
		 */
		for(Entry<Flow, Map<Long/*second*/, Long/*bytes*/>> pairFlowThroughtput : ctx.getMapFlowThroughput().entrySet()) {

			/**
			 * Para cada fluxo, contabiliza a quantidade de bytes transferida em cada segundo 
			 */
			final Flow flow = pairFlowThroughtput.getKey();
			final Map<Long, Long> mapThroughput = pairFlowThroughtput.getValue();

			headerFlowsStr[column] = flow.toString(); //Preenche o cabeçalho

			for(long second = firstSecond, line = 0 ; second <= lastSecond; second++, line++) {

				Long bytes = mapThroughput.get(second);
				if(bytes == null) bytes = 0L;

				matrixAllFlow[(int)line][column] = bytes.longValue();
			}

			column++;
		}

		/**
		 * Imprime os dados no formato CSV, com o seguinte padrão de cabeçalho:
		 * 	"second, fluxo1, fluxo2, ..." 
		 */
		printerAllFlow.print("second");
		for(String header : headerFlowsStr) {
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

	public void printQoSTimeAnalysisClusterSync(Context ctx) throws FileNotFoundException {

		log.println("########################################################################");
		for(int qos = 0 ; qos < Context.QOS_QUANTITY ; qos++) {
			log.println("******************************* QoS = "+qos+" ****************************");
			for(Entry<MQTTPacket, ApplicationPacket> pair : ctx.getMqttToTcpBrokerSyncMap(qos).entrySet()) {
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


}
