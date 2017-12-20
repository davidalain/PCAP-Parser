package br.com.davidalain.pcacpparser.main;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import br.com.davidalain.pcacpparser.Flow;

public class PathUtil {

	public static String getFileNameOnly(String pcapFilePath) {
		int beginIndex = pcapFilePath.lastIndexOf("/");
		if(beginIndex < 0)
			beginIndex = 0;

		int endIndex = pcapFilePath.lastIndexOf(".pcap");

		return pcapFilePath.substring(beginIndex, endIndex);
	}

	public static final String resultFlowCsvFilePath(final String pcapFilePath, final Flow flow) {
		return Parameters.OUTPUT_FLOW_DIR_PATH+getFileNameOnly(pcapFilePath)+"_resultFlow_"+flow.toStringForFileName()+".csv";
	}

	public static final String logFilePathTXT(final String pcapFilePath) {
		return Parameters.OUTPUT_FLOW_DIR_PATH+getFileNameOnly(pcapFilePath)+"_log.txt";
	}

	public static final String resultTimeFilePathTXT(final String pcapFilePath) {
		return Parameters.OUTPUT_FLOW_DIR_PATH+getFileNameOnly(pcapFilePath)+"_resultTime.txt";
	}

	public static final String resultFlowFilePathTXT(final String pcapFilePath) {
		return Parameters.OUTPUT_FLOW_DIR_PATH+getFileNameOnly(pcapFilePath)+"_resultFlow.txt";
	}

	public static final String allFlowsFilePathCSV(final String pcapFilePath) {
		return Parameters.OUTPUT_FLOW_DIR_PATH+getFileNameOnly(pcapFilePath)+"_allFlows.csv";
	}

	/**
	 * Determine if the given string is a valid IPv4 or IPv6 address.  This method
	 * uses pattern matching to see if the given string could be a valid IP address.
	 * 
	 * @see https://stackoverflow.com/questions/15875013/extract-ip-addresses-from-strings-using-regex
	 *
	 * @param ipAddress A string that is to be examined to verify whether or not
	 *  it could be a valid IP address.
	 * @return <code>true</code> if the string is a value that is a valid IP address,
	 *  <code>false</code> otherwise.
	 */
	public static boolean isIpAddress(String ipAddress) {

		final String ipv4Pattern = "(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])";
		final Pattern validIPv4pattern = Pattern.compile(ipv4Pattern, Pattern.CASE_INSENSITIVE);

		return validIPv4pattern.matcher(ipAddress).matches();
	}

}
