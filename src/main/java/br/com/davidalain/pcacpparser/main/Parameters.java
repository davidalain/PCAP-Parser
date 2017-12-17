package br.com.davidalain.pcacpparser.main;

import br.com.davidalain.pcacpparser.Flow;

public class Parameters {

	public static final String BROKER1_IP = "172.17.0.3";
	public static final String BROKER2_IP = "172.17.0.2";
	public static final String CLIENT1_IP = "172.17.0.1"; //"192.168.43.1"; //"192.168.25.63"; //"172.17.0.1";
	public static final String CLIENT2_IP = "172.17.0.1";

	public static final String PCAP_FILEPATH = "trace_pc_rtt_cliente1_172.17.0.1.pcap";
	
	public static final String FILENAME = PCAP_FILEPATH.substring(0, PCAP_FILEPATH.indexOf(".pcap"));
	public static final String OUTPUT_DIR_PATH = "output/";
	public static final String OUTPUT_FLOW_DIR_PATH = "output/flow/";
	
	public static final String LOG_FILEPATH = Parameters.OUTPUT_DIR_PATH + Parameters.FILENAME+"_log.txt";
	public static final String RESULT_TIME_FILEPATH = Parameters.OUTPUT_DIR_PATH + Parameters.FILENAME+"_resultTime.txt";
	public static final String RESULT_FLOW_FILEPATH = Parameters.OUTPUT_DIR_PATH + Parameters.FILENAME+"_resultFlow.txt";
	public static final String ALL_FLOW_CSV_FILEPATH = Parameters.OUTPUT_DIR_PATH + Parameters.FILENAME+"_allFlows.csv";
	
	public static final String resultFlowCsvFilePath(Flow flow) {
		return Parameters.OUTPUT_FLOW_DIR_PATH+Parameters.FILENAME+"_resultFlow_"+flow.toStringForFileName()+".csv";
	}
	
}
