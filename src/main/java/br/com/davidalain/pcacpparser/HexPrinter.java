package br.com.davidalain.pcacpparser;

public class HexPrinter {

	public static final int HEX_PRINT_MAX_LEN = 65;

	public static String toHexString(byte b) {

		final char[] values = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};

		int nibbleLeft = (b & 0xF0) >> 4;
		int nibbleRigth = (b & 0x0F);

		StringBuilder sb = new StringBuilder(2);
		sb.append(values[nibbleLeft]);
		sb.append(values[nibbleRigth]);

		return sb.toString();
	}

	public static String toHexString(byte[] array) {
		return toHexString(array, 0, array.length);
	}

	public static String toHexString(byte[] array, int start, int end) {

		StringBuilder sb = new StringBuilder((end - start)*4);

		sb.append("[");

		for(int i = start ; i < array.length && i < end ; i++) {
			sb.append(toHexString(array[i]));
			if(i < array.length - 1 && i < end - 1)
				sb.append(", ");
		}

		sb.append("]");

		return sb.toString();
	}

	public static String bytesToString(byte[] array, int start, int end) {

		StringBuilder sb = new StringBuilder(end-start+1);

		for(int i = start ; i < array.length && i < end; i++) {

			if((array[i] >= 0x20 && array[i] <= 0x7E) || (array[i] >= 0xA0 && array[i] <= 0xFF))
				sb.append((char)array[i]);
			else
				sb.append('.');

		}

		return sb.toString();
	}
	
	public static String toStringHexDump(byte[] array, int start, int end) {
		
		if(array == null)
			return "null";
		
		final StringBuilder sb = new StringBuilder();
		final byte[] line = new byte[16];

		int stop = Math.min(array.length, end);
		
		int i;
		for(i = start ; i < stop; i += 16) {

			int remainingLen = stop - i;
			if(remainingLen >= 16) {
				System.arraycopy(array, i, line, 0, 16);
				sb.append(toHexString(line, 0, 8));
				sb.append(' ');
				sb.append(toHexString(line, 8, 16));

			}else { //remainingLen < 16
				System.arraycopy(array, i, line, 0, remainingLen);

				if(remainingLen > 8) {
					sb.append(toHexString(line, 0, 8));
					sb.append(' ');
					sb.append(toHexString(line, 8, remainingLen));

				} else { //remainingLen <= 8
					sb.append(toHexString(line, 0, remainingLen));
					sb.append(' ');

				}
			}

			int currentByteCount = remainingLen > 16 ? 16 : remainingLen;
			int lineLen = (currentByteCount * 4) + 1;
			for(int c = 0 ; c < HEX_PRINT_MAX_LEN - lineLen ; c++) {
				sb.append(' ');
			}

			String txt = bytesToString(line, 0, Math.min(remainingLen, 16));

			sb.append(" texto=[" + txt + "]");
			sb.append("\r\n");
		}

		return sb.toString();
	}

	public static String toStringHexDump(byte[] array) {
		if(array == null)
			return "null";
		
		return toStringHexDump(array, 0, array.length);
	}

}
