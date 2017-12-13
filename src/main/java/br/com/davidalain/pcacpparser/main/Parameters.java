package br.com.davidalain.pcacpparser.main;

public class Parameters {

	public static final String BROKER1_IP = "172.17.0.3";
	public static final String BROKER2_IP = "172.17.0.2";
	public static final String CLIENT1_IP = "192.168.43.1"; //"192.168.25.63"; //"172.17.0.1";
	public static final String CLIENT2_IP = "172.17.0.1";

	public static final String FILEPATH = "trace_747a0d82_2_Broker1ClusterDoisBrokers.pcap";
	
	public static final String PREFIX = FILEPATH.substring(0, FILEPATH.length() - 5);
	public static final String OUTPUT_PATH = "output/";
	public static final String OUTPUT_FLOW_PATH = "output/flow/";
	
}