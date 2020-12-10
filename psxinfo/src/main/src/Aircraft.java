package src;


public class Aircraft {
	// AIRCRAFT STATUS
	static float latitude = 0;
	static float longitude = 0;
	static int altitude = -1;
	static int tas = -1;
	
	// PREVIOUS POSITION VARIABLES
	static float previousLatitude = 0;
	static float previousLongitude = 0;
	
	
	void setLatitude(float newLatitude) {
		latitude = newLatitude;
		PSXInfo.setLabel_Lat(String.valueOf(newLatitude));
	}
	
	float getLatitude() {
		return latitude;
	}
	
	void setAltitude(int newAltitude) {
		altitude = newAltitude;
		PSXInfo.setLabel_Alt(String.valueOf(altitude));
	}
	
	int getAltitude() {
		return altitude;
	}
}
