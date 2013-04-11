package com.kac.customcomponents;

import java.math.BigDecimal;

public enum NumericType {
	BYTE, SHORT, INTEGER, LONG, BIG_DECIMAL, FLOAT, DOUBLE;

	
	public static <T extends Number> NumericType fromNumber(T value) throws IllegalArgumentException {
		NumericType numberType = null;
		
		if (value instanceof Byte) {
			numberType = NumericType.BYTE;
		} else if (value instanceof Short) {
			numberType = NumericType.SHORT;
		} else if (value instanceof Integer) {
			numberType = NumericType.INTEGER;
		} else if (value instanceof Long) {
			numberType = NumericType.LONG;
		} else if (value instanceof BigDecimal) {
			numberType = NumericType.BIG_DECIMAL;
		} else if (value instanceof Float) {
			numberType = NumericType.FLOAT;
		} else if (value instanceof Double) {
			numberType = NumericType.DOUBLE;
		}
		
		
		if(numberType != null) {
			return numberType;
		}
		
		throw new IllegalArgumentException("This '"
								+ value.getClass().getName() + "' is not supported");
	}

	public Number toNumber(double value) {
		switch (this) {
				
		case BYTE:
			return Byte.valueOf((byte) value);
		case SHORT:
			return Short.valueOf((short) value);
		case INTEGER:
			return Integer.valueOf((int) value);
		case LONG:
			return Long.valueOf((long) value);
		case BIG_DECIMAL:
			return BigDecimal.valueOf(value);
		case FLOAT:
			return Float.valueOf((float) value);
		case DOUBLE:
			return value;
		}
		
		throw new InstantiationError("Conversion of " + this + " to a Number object failed");
	}
}
