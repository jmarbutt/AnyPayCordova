package com.bbpos;

import java.io.ByteArrayOutputStream;

public class EmvSwipeDecrypt {
	
	private static byte[] hexStringToBytes(String input) {
		byte[] b = new byte[input.length() / 2];
		for(int i = 0; i < b.length; ++i) {
			b[i] = (byte)(Character.digit(input.charAt(i * 2), 16) << 4 | Character.digit(input.charAt(i * 2 + 1), 16));
		}
		return b;
	}
	
	private static String decodeTrack1(String track1, String nameField) {
		byte[] temp;
		int index;
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		temp = hexStringToBytes(track1);
		
		for(int i = 0; i < temp.length - 2; i += 3) {
			int threeBytes = ((temp[i] & 0xFF) << 16) | ((temp[i + 1] & 0xFF) << 8) | (temp[i + 2] & 0xFF);
			baos.write(((threeBytes >> 18) & 0x3F) + 0x20);
			baos.write(((threeBytes >> 12) & 0x3F) + 0x20);
			baos.write(((threeBytes >> 6) & 0x3F) + 0x20);
			baos.write((threeBytes & 0x3F) + 0x20);
		}
		track1 = new String(baos.toByteArray());
		
		index = track1.indexOf("^");
		if(index < 0) {
			return "";
		}
		
		track1 = track1.substring(0, index + 1) + nameField + track1.substring(index + 1);
		
		index = track1.indexOf("?");
		if(index < 0) {
			return "";
		}
		track1 = track1.substring(0, index + 1);
		
		if(!track1.startsWith("%B")) {
			return "";
		}
		
		return track1;
	}
	
	private static String decodeTrack2or3(String track2or3) {
		byte[] temp;
		int index;
		
		boolean isASCII = false;
		if(track2or3.toLowerCase().startsWith("3b")) {
			isASCII = true;
		}
		
		if(isASCII) {
			temp = hexStringToBytes(track2or3);
		} else {
			temp = new byte[track2or3.length()];
			for(int i = 0; i < track2or3.length(); ++i) {
				temp[i] = (byte)(Integer.parseInt("" + track2or3.charAt(i), 16) + 0x30);
			}
		}
		track2or3 = new String(temp);
		
		index = track2or3.indexOf("?");
		if(index < 0) {
			return "";
		}
		track2or3 = track2or3.substring(0, index + 1);
		
		if(!track2or3.startsWith(";")) {
			return "";
		}
		
		return track2or3;
	}
	
	public static DecryptedData decrypt(String bdk, String ksn, String nameField, String encTracks, int format) {
		try {
			if(format == 54) {
				String key = DUKPTServer.GetDataKeyVar(ksn, bdk);
				
				String cardholderName = nameField.indexOf("^") < 0? nameField : nameField.substring(0, nameField.indexOf("^"));
				
				String tracks = TripleDES.decrypt(encTracks, key);
				String track1 = "";
				String track2 = "";
				String track3 = "";
				
				if(tracks.startsWith("16")) {
					track1 = tracks.substring(0, 128);
					encTracks = encTracks.substring(128);
					tracks = TripleDES.decrypt(encTracks, key);
				}
				
				if(tracks.length() == 48 || tracks.length() == 160) {
					track2 = tracks.substring(0, 48);
					encTracks = encTracks.substring(48);
					tracks = TripleDES.decrypt(encTracks, key);
				}
				
				if(tracks.length() == 112) {
					track3 = tracks;
				}
				
				track1 = decodeTrack1(track1, "");
				track2 = decodeTrack2or3(track2);
				track3 = decodeTrack2or3(track3);
				
				if(track1.startsWith("%B")) {
					int endIndex = 0;
					endIndex = track1.indexOf('?');
					if(endIndex < 0) {
						track1 = "";
					} else {
						try {
							track1 = track1.substring(0, endIndex + 1);
						} catch(Exception e) {
							track1 = "";
						}
					}
				} else {
					track1 = "";
				}
				
				if(track2.startsWith(";")) {
					int endIndex = 0;
					endIndex = track2.indexOf('?');
					if(endIndex < 0) {
						track2 = "";
					} else {
						try {
							track2 = track2.substring(0, endIndex + 1);
						} catch(Exception e) {
							track2 = "";
						}
					}
				} else {
					track2 = "";
				}
				
				if(track3.startsWith(";")) {
					int endIndex = 0;
					endIndex = track3.indexOf('?');
					if(endIndex < 0) {
						track3 = "";
					} else {
						try {
							track3 = track3.substring(0, endIndex + 1);
							if(track3.length() < 13) {
								track3 = "";
							}
						} catch(Exception e) {
							track3 = "";
						}
					}
				} else {
					track3 = "";
				}
				
				return new DecryptedData(cardholderName, track1, track2, track3);
			} else if(format == 60) {
				String key = DUKPTServer.GetDataKey(ksn, bdk);
				
				String cardholderName = nameField.indexOf("^") < 0? nameField : nameField.substring(0, nameField.indexOf("^"));
				
				String tracks = TripleDES.decrypt_CBC(encTracks, key);
				String track1 = "";
				String track2 = "";
				String track3 = "";
				if(tracks.startsWith("25")) {
					track1 = tracks.substring(0, 160);
					encTracks = encTracks.substring(160);
					tracks = TripleDES.decrypt_CBC(encTracks, key);
				}
				
				if(tracks.length() == 80 || tracks.length() == 304) {
					track2 = tracks.substring(0, 80);
					encTracks = encTracks.substring(80);
					tracks = TripleDES.decrypt_CBC(encTracks, key);
				}
				
				if(tracks.length() == 224) {
					track3 = tracks;
				}
				
				if(!track1.equals("")) {
					track1 = new String(hexStringToBytes(track1));
				}
				track2 = decodeTrack2or3(track2);
				track3 = decodeTrack2or3(track3);
				
				if(track1.startsWith("%B")) {
					int endIndex = 0;
					endIndex = track1.indexOf('?');
					if(endIndex < 0) {
						track1 = "";
					} else {
						try {
							track1 = track1.substring(0, endIndex + 1);
						} catch(Exception e) {
							track1 = "";
						}
					}
				} else {
					track1 = "";
				}
				
				if(track2.startsWith(";")) {
					int endIndex = 0;
					endIndex = track2.indexOf('?');
					if(endIndex < 0) {
						track2 = "";
					} else {
						try {
							track2 = track2.substring(0, endIndex + 1);
						} catch(Exception e) {
							track2 = "";
						}
					}
				} else {
					track2 = "";
				}
				
				if(track3.startsWith(";")) {
					int endIndex = 0;
					endIndex = track3.indexOf('?');
					if(endIndex < 0) {
						track3 = "";
					} else {
						try {
							track3 = track3.substring(0, endIndex + 1);
							if(track3.length() < 13) {
								track3 = "";
							}
						} catch(Exception e) {
							track3 = "";
						}
					}
				} else {
					track3 = "";
				}
				
				return new DecryptedData(cardholderName, track1, track2, track3);
			}
		} catch(Exception e) {
		}
		return new DecryptedData("", "", "", "");
	}
	
	public static String decryptEPB(String bdk, String ksn, String epb, DecryptedData decryptedData) {
		String pan = "";
		if(!decryptedData.track1.equals("")) {
			int startIndex = decryptedData.track1.indexOf("%B");
			if(startIndex >= 0) {
				startIndex += 2;
				int endIndex = decryptedData.track1.indexOf("^", startIndex);
				if(endIndex >= 0) {
					pan = decryptedData.track1.substring(startIndex, endIndex);
				}
			}
		}
		
		if(pan.equals("")) {
			if(!decryptedData.track2.equals("")) {
				int startIndex = decryptedData.track2.indexOf(";");
				if(startIndex >= 0) {
					startIndex += 1;
					int endIndex = decryptedData.track2.indexOf("=", startIndex);
					if(endIndex >= 0) {
						pan = decryptedData.track2.substring(startIndex, endIndex);
					}
				}
			}
		}
		
		String key = DUKPTServer.GetPinKeyVar(ksn, bdk);
		String pinBlock = TripleDES.decrypt(epb, key);
		
		pan = "0000" + pan.substring(pan.length() - 13, pan.length() - 1);
		
		byte[] b1 = DES.String2Hex(pan);
		byte[] b2 = DES.String2Hex(pinBlock);
		
		byte[] b = new byte[b1.length]; 
		for(int i = 0; i < b.length; ++i) {
			b[i] = (byte)(b1[i] ^ b2[i]);
		}
		
		return DES.Hex2String(b);
	}
	
	public static String decryptEPB(String bdk, String ksn, String epb, String pan) {
		String key = DUKPTServer.GetPinKeyVar(ksn, bdk);
		String pinBlock = TripleDES.decrypt(epb, key);
		
		pan = "0000" + pan.substring(pan.length() - 13, pan.length() - 1);
		
		byte[] b1 = DES.String2Hex(pan);
		byte[] b2 = DES.String2Hex(pinBlock);
		
		byte[] b = new byte[b1.length]; 
		for(int i = 0; i < b.length; ++i) {
			b[i] = (byte)(b1[i] ^ b2[i]);
		}
		
		return DES.Hex2String(b);
	}
}