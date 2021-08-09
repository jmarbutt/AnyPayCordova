package com.bbpos;
/**
 * @author Derek
 * 
 */

public class DUKPTServer {
	
	public static byte[] GetIPEK(byte[] ksn, byte[] bdk)
	{
		byte[] ksnTemp = new byte[8];
		byte[] keyTemp = new byte[16];
		byte[] result, resultTemp;
		int i;
		
		// mask KSN to get serial number part of KSN
		for (i = 0; i<8; i++)
			ksnTemp[i] = ksn[i];
		ksnTemp[7] &= 0xE0;
		
		result = new byte[16];
		for (i=0; i<16; i++)
			keyTemp[i] = bdk[i];
		resultTemp = TripleDES.encrypt(ksnTemp, keyTemp);
		for (i=0; i<8; i++)
			result[i] = resultTemp[i];
		
		keyTemp[0] ^= 0xC0;
		keyTemp[1] ^= 0xC0;
		keyTemp[2] ^= 0xC0;
		keyTemp[3] ^= 0xC0;
		keyTemp[8] ^= 0xC0;
		keyTemp[9] ^= 0xC0;
		keyTemp[10] ^= 0xC0;
		keyTemp[11] ^= 0xC0;
		
		resultTemp = TripleDES.encrypt(ksnTemp, keyTemp);
		for (i=0; i<8; i++)
			result[i+8] = resultTemp[i];

		return result;
	}
	
	public static byte[] GetDukptKey(byte[] ksn, byte[] bdk)
	{
		int i, num;
		byte[] key, KSNTemp, counter, counterTemp;

		KSNTemp = new byte[8];
		counter = new byte[3];

		for (i=0; i<8; i++)
			KSNTemp[i] = ksn[i+2];
		KSNTemp[5] &= 0xE0;
		KSNTemp[6] = 0;
		KSNTemp[7] = 0;

		key = GetIPEK(ksn, bdk);
		
		counter[0] = (byte)(ksn[7] & 0x1F);
		counter[1] = ksn[8];
		counter[2] = ksn[9];
		num = CountOne(counter[0]);
		num += CountOne(counter[1]);
		num += CountOne(counter[2]);
		
		counterTemp = SearchOne(counter);
		procCounter(KSNTemp, counter, counterTemp);
		
		while (num > 0)
		{
			key = NonRevKeyGen(KSNTemp, key);
			counterTemp = SearchOne(counter);
			procCounter(KSNTemp, counter, counterTemp);
			num--;
		}
		
		return key;
	}
	
	public static byte[] GetPinKeyVar(byte[] ksn, byte[] bdk)
	{
		byte[] key;
		
		key = GetDukptKey(ksn, bdk);
		key[7] ^= 0xFF;
		key[15] ^= 0xFF;
		
		return key;
	}
	
	public static byte[] GetDataKeyVar(byte[] ksn, byte[] bdk)
	{
		byte[] key;
		
		key = GetDukptKey(ksn, bdk);
		key[5] ^= 0xFF;
		key[13] ^= 0xFF;
		
		return key;
	}
	
	public static byte[] GetDataKey(byte[] ksn, byte[] bdk)
	{
		int i;
		byte[] key, keyTemp1, keyTemp2;
		
		keyTemp1 = GetDataKeyVar(ksn, bdk);
		keyTemp2 = new byte[16];
		for (i=0; i<16; i++)
			keyTemp2[i] = keyTemp1[i];
		
		key = TripleDES.encrypt(keyTemp1, keyTemp2);
		return key;
	}
	
	public static byte[] GetFixedKey(byte[] ksn, byte[] bdk)
	{
		byte[] ksnTemp = new byte[8];
		byte[] keyTemp = new byte[16];
		byte[] result, resultTemp;
		int i;
		
		// mask KSN to get serial number part of KSN
		for (i = 0; i<8; i++)
			ksnTemp[i] = ksn[i];
		
		result = new byte[16];
		for (i=0; i<16; i++)
			keyTemp[i] = bdk[i];
		resultTemp = TripleDES.encrypt(ksnTemp, keyTemp);
		for (i=0; i<8; i++)
			result[i] = resultTemp[i];
		
		keyTemp[0] ^= 0xC0;
		keyTemp[1] ^= 0xC0;
		keyTemp[2] ^= 0xC0;
		keyTemp[3] ^= 0xC0;
		keyTemp[8] ^= 0xC0;
		keyTemp[9] ^= 0xC0;
		keyTemp[10] ^= 0xC0;
		keyTemp[11] ^= 0xC0;
		
		resultTemp = TripleDES.encrypt(ksnTemp, keyTemp);
		for (i=0; i<8; i++)
			result[i+8] = resultTemp[i];

		return result;
	}
	
	public static String GetIPEK(String ksn, String bdk)
	{
		byte[] bKSN = DES.String2Hex(ksn);
		byte[] bBDK = DES.String2Hex(bdk);
		byte[] bKey = GetIPEK(bKSN, bBDK);
		String result = DES.Hex2String(bKey);
		
		return result;
	}
	
	public static String GetDukptKey(String ksn, String bdk)
	{
		byte[] bKSN = DES.String2Hex(ksn);
		byte[] bBDK = DES.String2Hex(bdk);
		byte[] bKey = GetDukptKey(bKSN, bBDK);
		String result = DES.Hex2String(bKey);
		
		return result;
	}
	
	public static String GetPinKeyVar(String ksn, String bdk)
	{
		byte[] bKSN = DES.String2Hex(ksn);
		byte[] bBDK = DES.String2Hex(bdk);
		byte[] bKey = GetPinKeyVar(bKSN, bBDK);
		String result = DES.Hex2String(bKey);
		
		return result;
	}
	
	public static String GetDataKeyVar(String ksn, String bdk)
	{
		byte[] bKSN = DES.String2Hex(ksn);
		byte[] bBDK = DES.String2Hex(bdk);
		byte[] bKey = GetDataKeyVar(bKSN, bBDK);
		String result = DES.Hex2String(bKey);
		
		return result;
	}
	
	public static String GetDataKey(String ksn, String bdk)
	{
		byte[] bKSN = DES.String2Hex(ksn);
		byte[] bBDK = DES.String2Hex(bdk);
		byte[] bKey = GetDataKey(bKSN, bBDK);
		String result = DES.Hex2String(bKey);
		
		return result;
	}
	
	public static String GetFixedKey(String ksn, String bdk)
	{
		byte[] bKSN = DES.String2Hex(ksn);
		byte[] bBDK = DES.String2Hex(bdk);
		byte[] bKey = GetFixedKey(bKSN, bBDK);
		String result = DES.Hex2String(bKey);
		
		return result;
	}

	static int CountOne(byte input)
	{
		int temp = 0;
		if ((input & 0x80) != 0)
			temp++;
		if ((input & 0x40) != 0)
			temp++;
		if ((input & 0x20) != 0)
			temp++;
		if ((input & 0x10) != 0)
			temp++;
		if ((input & 0x08) != 0)
			temp++;
		if ((input & 0x04) != 0)
			temp++;
		if ((input & 0x02) != 0)
			temp++;
		if ((input & 0x01) != 0)
			temp++;
		return temp;
	}
	
	static byte[] SearchOne(byte[] counter)
	{
		byte[] result = new byte[3];
		
		if (counter[0] == 0)
		{
			if (counter[1] == 0)
				result[2] = SearchOneCore(counter[2]);
			else
				result[1] = SearchOneCore(counter[1]);
		}
		else
			result[0] = SearchOneCore(counter[0]);
		return result;
	}
	
	static byte SearchOneCore(byte input)
	{
		if ((input & 0x80) != 0)
			return (byte)(0x80);
		if ((input & 0x40) != 0)
			return (byte)(0x40);
		if ((input & 0x20) != 0)
			return (byte)(0x20);
		if ((input & 0x10) != 0)
			return (byte)(0x10);
		if ((input & 0x08) != 0)
			return (byte)(0x08);
		if ((input & 0x04) != 0)
			return (byte)(0x04);
		if ((input & 0x02) != 0)
			return (byte)(0x02);
		if ((input & 0x01) != 0)
			return (byte)(0x01);
		return 0;
	}
	
	static void procCounter(byte[] ksn, byte[] counter, byte[] counterTemp)
	{
		ksn[5] |= counterTemp[0];
		ksn[6] |= counterTemp[1];
		ksn[7] |= counterTemp[2];
		counter[0] ^= counterTemp[0];
		counter[1] ^= counterTemp[1];
		counter[2] ^= counterTemp[2];
	}
			
	static byte[] NonRevKeyGen(byte[] cReg1, byte[] key)
	{
		int i;
		byte[] keyTemp1, keyTemp2, data;
		byte[] result, resultTemp;
		
		keyTemp1 = new byte[8];
		keyTemp2 = new byte[8];
		for (i=0; i<8; i++)
		{
			keyTemp1[i] = key[i];
			keyTemp2[i] = key[i+8];
		}

		data = new byte[8];
		result = new byte[16];

		for (i=0; i<8; i++)
			data[i] = (byte)(cReg1[i] ^ keyTemp2[i]);
		resultTemp = DES.encrypt(data, keyTemp1);
		for (i=0; i<8; i++)
			result[i+8] = (byte)(resultTemp[i] ^ keyTemp2[i]);
		
		keyTemp1[0] ^= 0xC0; 
		keyTemp1[1] ^= 0xC0; 
		keyTemp1[2] ^= 0xC0; 
		keyTemp1[3] ^= 0xC0; 
		keyTemp2[0] ^= 0xC0; 
		keyTemp2[1] ^= 0xC0; 
		keyTemp2[2] ^= 0xC0; 
		keyTemp2[3] ^= 0xC0; 
		
		for (i=0; i<8; i++)
			data[i] = (byte)(cReg1[i] ^ keyTemp2[i]);
		resultTemp = DES.encrypt(data, keyTemp1);
		for (i=0; i<8; i++)
			result[i] = (byte)(resultTemp[i] ^ keyTemp2[i]);
		
		return result;
	}
}
