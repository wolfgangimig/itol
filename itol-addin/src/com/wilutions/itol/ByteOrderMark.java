package com.wilutions.itol;

import java.util.Arrays;

public class ByteOrderMark {

	private byte[] bytes;
	
	public final static ByteOrderMark UTF_8 = new ByteOrderMark(0xEF, 0xBB, 0xBF);
	public final static ByteOrderMark UTF_16_BE = new ByteOrderMark(0xFE, 0xFF);
	public final static ByteOrderMark UTF_16_LE = new ByteOrderMark(0xFF, 0xFE);
	public final static ByteOrderMark UTF_32_BE = new ByteOrderMark(0x00, 0x00, 0xFF, 0xFE);
	public final static ByteOrderMark UTF_32_LE = new ByteOrderMark(0xFF, 0xFE, 0x00, 0x00);
	
	public final static ByteOrderMark[] BOMs = new ByteOrderMark[] { UTF_8, UTF_16_BE, UTF_16_LE, UTF_32_BE, UTF_32_LE };
	
	public ByteOrderMark(int ... ints) {
		this.bytes = new byte[ints.length];
		for (int i = 0; i < ints.length; i++) {
			this.bytes[i] = (byte)(ints[i] & 0xFF);
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(bytes);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (obj instanceof ByteOrderMark) {
			ByteOrderMark other = (ByteOrderMark) obj;
			if (!Arrays.equals(bytes, other.bytes)) return false;
		}
		if (obj instanceof byte[]) {
			byte[] other = (byte[]) obj;
			if (other.length < bytes.length) return false;
			for (int i = 0; i < bytes.length; i++) {
				if (bytes[i] != other[i]) return false;
			}
		}
		return true;
	}

	public static ByteOrderMark fromValue(byte[] bytes) {
		for (int i = 0; i < BOMs.length; i++) {
			if (BOMs[i].equals(bytes)) return BOMs[i];
		}
		return null;
	}
	
}
