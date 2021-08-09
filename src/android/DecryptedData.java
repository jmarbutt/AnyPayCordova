package com.bbpos;

public class DecryptedData {
	public String cardholderName = "";
	public String track1 = "";
	public String track2 = "";
	public String track3 = "";
	
	public DecryptedData(String cardholderName, String track1, String track2, String track3) {
		this.cardholderName = cardholderName;
		this.track1 = track1;
		this.track2 = track2;
		this.track3 = track3;
	}
}
