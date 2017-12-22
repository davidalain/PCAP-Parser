package br.com.davidalain.pcacpparser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Util {

	/**
	 * Checks if 'sub' is inside of 'array'
	 * 
	 * @param array
	 * @param sub
	 * @return
	 */
	public static boolean contains(byte[] array, byte[] sub) {
		
		for(int i = 0 ; i < array.length - sub.length; i++) {
			
			int count = 0;
			for(int j = 0 ; j < sub.length ; j++) {
				
				if(array[i + j] == sub[j]) {
					count++;
				} else {
					break;
				}
			}
			
			if(count == sub.length)
				return true;
		}
		
		return false;
	}
	
	/**
	 * Read nBytes from byteArray and convert them to a int value
	 * 
	 * @param byteArray		Array that contains bytes to be converted to int 
	 * @param offset		Initial index
	 * @param nBytes		How many bytes represents number that will be converted 
	 * @return
	 */
	public static int toInt(byte[] byteArray, int offset, int nBytes) {
		int ret = 0;
		for (int i=offset; i<offset+nBytes && i<byteArray.length; i++) {
			ret <<= 8;
			ret |= (int)byteArray[i] & 0xFF;
		}
		return ret;
	}
	
	public static double max(List<Long> list) {
		return (double)(list.size() == 0 ? Double.NaN : Collections.max(list));
	}
	
	public static double min(List<Long> list) {
		return (double)(list.size() == 0 ? Double.NaN : Collections.min(list));
	}
	
	public static double avg(List<Long> list) {
		double avg = 0;
		for(long l : list) {
			avg += (double)l;
		}
		avg /= (double)list.size();
		return avg;
	}
	
	public static double median(List<Long> list) {
		List<Long> list2 = new ArrayList<>(list);
		Collections.sort(list2);
		return (double)(list2.size() == 0 ? Double.NaN : list2.get(list2.size()/2));
	}
	
}
