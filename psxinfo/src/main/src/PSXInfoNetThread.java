package src;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

//AddonExampleNetThread.java is a class used by AddonExample.java



public class PSXInfoNetThread extends Thread {

	// CONNECTION VARIABLES
	static Socket socket = null;
	private BufferedReader in = null;
	private PrintWriter ou = null;
	private boolean remoteExit;
	private String targetHost;
	private int targetPort;
	
	// DATE/TIME VARIABLES
	SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	Date now = new Date(System.currentTimeMillis());
	
	protected boolean finishedLoading = false; // So landing score isn't calculated and printed when reloading
	public static boolean printEnabled = false;
	public static String staticFltData;
	public static String rawPosition;
	public static String[] rawRunway = {"ZZZ","0.0","0.0","0","0","0","ZZZZ","0"};
	public static int forceRouteUpdate = 0;
	public String rawRte;
	public static int prevActiveRoute = -1;
	public static int activateRoute = -1;

	public static String[] touchdown = new String[9];	// Array of the touchdown values
	public static Timestamp timestamp = new Timestamp(System.currentTimeMillis());	// Time stamp used to compare
	public static Date touchdownTimer = new Date(System.currentTimeMillis());	// Timer used to compare the touchdown
	//public static int touchdownCounter = 0;
	public static String prevRawTouchdown = "1";
	public static int cause = 0; // 0 = Good pilot, 1 = bounced, 2 = wing strike, 3 = tail strike, 4 = nose, 5 = revoked
	public static int tas = 0; //tas in 000s use for final deceleration calculation

	// LANDING SCORES
	public static int noseGear = 0;
	public static int mainGear = 0;
	public static double offset = 0;
	public static int distance = 0;
	public static double pitch = 0;
	public static double bank = 0;
	public static double crab = 0;
	public static int speed = 0;
	public static int landWindDir = 0;
	public static int landWindSpeed = 0;
	public static double maxDecel = 0;
	public static int avgOffset = 0;
	public static double noseTransitionTime = 0;
	

	public static boolean bounced = false;	// Did the pilot bounce
	public static boolean wingStrike = false;	// Did the pilot strike the wing
	public static boolean tailStrike = false;	// Did the pilot strike the tail
	//public static int tailStrikePenalty = 50;		// Deduction for using the autopilot (in %)
	public static boolean noseCollapse = false;	// Did the pilot cause the nose to collapse
	public static boolean hasAlreadyLandedMain = false;	// Has the pilot already landed the main gear
	public static boolean hasAlreadyLandedNose = false;	// Has the pilot already landed the nose gear
	
	// BOUNCED TRACKING VARIABLES
	public static int prevMainGear = 0;	// Previous main gear landing
	public static double prevNoseGear = 0;	// Previous nose gear landing
	public static int bouncedMainGear = 0;	// The value of the bounced main gear (takes the max of the first and second landing)
	public static int bouncedNoseGear = 0;	// The value of the bounced nose gear (takes the max of the first and second landing)
	public static double bouncedMainGearDeduct = 0;	// The value of the deduction for the main gear for the bounce
	public static double bouncedNoseGearDeduct = 0;	// The value of the deduction for the nose gear for the bounce
	public static int bounceMilliSeconds = 12000;
	public static Date landingTime;
	public static Date prevLandingTime = new Date(System.currentTimeMillis() - bounceMilliSeconds);

	// AUTOPILOT DETAILS
	public static boolean autopilotAssist = false;		// true if landing score is deducted for using autopilot
	public static int autopilotAssistHeight = 1890; 		// Height above rwy elevation at which the landing becomes "Assisted"
	public static int autopilotAssistPenalty = 25;		// Deduction for using the autopilot (in %)
	public static int[] autopilotStatus = new int[3];	// status of the 3 autopilots (L,C,R)
	public static int rwyElev = -1;						// Runway Elevation (for calculating height above ground
	public static int startAlt = -1;					// Altitude that PSX started at
	
	// EICAS MESSAGE TRACKING
	public static String[] FreeMsg = {"","","","",""}; // Warning, Caution, Advisory, Message, Status
	
	// PRINTING
	public static boolean readyToPrint = false;
	public static boolean cancelPrint = false;
	public static int lastPrintedMainGearNoseGear = 0; //Main gear raw x nose gear raw
	public static boolean hasLastLandingBeenPrinted = false;
	// public static String[] rteData;
	
	// TOUCHDOWN FILES
	public static File rawTouchdownLocalFile = new File(PSXInfo.touchdownLogFileName);

	PSXInfoNetThread(String h, int p) {
		targetHost = h;
		targetPort = p;
	}

	public void printLanding(final Timestamp timestamp, final double landingScores, final boolean autopilot, final boolean bounced,
			final boolean wingStrike, final boolean tailStrike, final boolean noseCollapsed, final int mainGear, final double mainGearDeduct,
			final int noseGear, final double noseGearDeduct, final double offset, final double offsetDeduct, final int distance, final double distanceDeduct,
			final double pitch, final double pitchDeduct, final double bank, final double bankDeduct, final double crab, final double crabDeduct, final int speed,
			final double speedDeduct, final int windDir, final int windSpeed) {
		SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
			@Override
			public Void doInBackground() {
				
				//maxDecel = LandingCalc.getDecelerationRate(tas);
				//PSXInfo.lblDecelData.setText(String.format("%.1f m/s\u00B3", maxDecel));
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				

				boolean serviceable = true;

				
				if (bounced || wingStrike || tailStrike || noseCollapsed) {
					serviceable = false;
				}
				

				
				
				//System.out.println("Printer enabled:" + printEnabled + "\t" + cancelPrint + "\t" + mainGear * noseGear + "\t" + lastPrintedMainGearNoseGear + "\t" +  hasLastLandingBeenPrinted);
				
				if (printEnabled && !cancelPrint && mainGear * noseGear != lastPrintedMainGearNoseGear && !hasLastLandingBeenPrinted) {

					//System.out.println("LAST LANDING SCORE");
					hasLastLandingBeenPrinted = true;
					//LandingCalc.calculateScore(false);
					String print = "|------------- LANDING DATA -------------|^" 
							+ "         " + timestamp + "^"
							+ "^"
							+ "Landing Score: " + (cause == 6 ? "NONE" : (serviceable ? String.format("%1$10s", String.format("%05.2f", (PSXInfo.landingScore >= 0 ? PSXInfo.landingScore : 0))) : "00.00") + "%")
							+ (cause == 6 ? "^PILOT LICENCE REVOKED" : "")
							+ "^" 
							+ "^" 
							+ "COMPONENT             DATA       DEDUCTION^"
							+ "Nose Gear: " + 			String.format("%1$15s", String.valueOf(noseGear)) + " fpm      "
							+ String.format("%1$6s", 	String.format("%.1f", noseGearDeduct)) + "^"
							+ "Main Gear: " + 			String.format("%1$15s", String.valueOf(mainGear)) + " fpm      "
							+ String.format("%1$6s", 	String.format("%.1f", mainGearDeduct)) + "^"
							+ "Centreline Offset: " + String.format("%1$7s", String.format("%.1f", offset)) + " m        "
							+ (LandingCalc.scoreOffsetAndThreshold ? String.format("%1$6s", 	String.format("%.1f", offsetDeduct)) : String.format("%1$6s", "N/A")) + "^"
							+ "Threshold Distance: " + 	String.format("%1$6s", String.valueOf(distance)) + " ft       "
							+ (LandingCalc.scoreOffsetAndThreshold ? String.format("%1$6s", 	String.format("%.1f", distanceDeduct)) : String.format("%1$6s", "N/A")) + "^"
							+ "Pitch: " + 				String.format("%1$19s", String.format("%.1f", pitch)) + " deg      "
							+ String.format("%1$6s", String.format("%.1f", pitchDeduct)) + "^"
							+ "Bank: " + String.format("%1$20s", String.format("%.1f",bank))
							+ " deg      " + String.format("%1$6s", String.format("%.1f", bankDeduct)) + "^"
							+ "Crab: " + String.format("%1$20s", String.format("%.1f", crab))
							+ " deg      " + String.format("%1$6s", String.format("%.1f", crabDeduct)) + "^"
							+ "Speed Deviation: " + String.format("%1$9s", String.valueOf(speed)) + " kts      "
							+ String.format("%1$6s", String.format("%.1f", speedDeduct)) + "^"
							+ "Change in Decel: " + String.format("%1$9s", String.format("%.1f", maxDecel))
							+ " m/s/s/s " + String.format("%1$7s", String.format("%.1f", PSXInfo.landingMaxDecel)) + "^"
							+ "Average Offset: " + String.format("%1$10s", String.valueOf(LandingCalc.averageOffset))
							+ " m      " + String.format("%1$8s", String.format("%.1f", PSXInfo.landingAvgOffset)) + "^"
							+ "Nose Transition: " + String.format("%1$9s", String.format("%.1f", PSXInfoNetThread.noseTransitionTime))
							+ " sec      " + String.format("%1$6s", String.format("%.1f", PSXInfo.landingNoseTransitionTime)) + "^"
							+ "^----- OTHER NOTES -----"
							+ "^Runway: " + PSXInfo.destination + " / " + PSXInfo.arrRwy
							+ "^Wind: " + (landWindSpeed < 5 ? "< 05 kts" : String.format("%03d", landWindDir)	+ "/" + String.format("%02d", landWindSpeed))
							+ "^Expert Pilot Mode: " + (LandingCalc.scoreOffsetAndThreshold ? "ACTIVATED" : "DEACTIVATED")
							+ (autopilot ? "^AUTOPILOT ASSISTED (-"+ autopilotAssistPenalty + "%)" : "") 
							+ (bounced ? "^BOUNCED" : "")
							+ (noseCollapsed ? "^NOSE COLLAPSED" : "") 
							+ (wingStrike ? "^WING STRIKE" : "")
							+ (tailStrike ? "^TAIL STRIKE" : "")
							+ "^-----------------------"
							+ "^"
							+ (cause == 6 ? "^  ----- IMMEDIATE ACTION REQUIRED -----^Please fill out this performance slip and^return it to the AOC listed Chief Pilot.^^You have violated the following:^Australian Civil Aviation Act 1998^Part III Division 2.E.28BF (2)^^I ......................... (name) hereby^accept responsibility for the above.^^Signed:   ...........................^^Date:     ...........................^^Witness:  ...........................^^" : "")
							+ "^|--------- END OF LANDING DATA ----------|";
					send("Qs119=" + print);

					Timestamp time = new Timestamp(System.currentTimeMillis());
					System.out.println(time + "\tPRINTED - Landing Score: " + PSXInfo.landingScore + " = " + LandingCalc.landingScoreTouchdown + " - " + LandingCalc.landingScoreContinual);
					

					
					lastPrintedMainGearNoseGear = mainGear * noseGear;
					bouncedMainGear = 0;
					bouncedNoseGear = 0;
					bouncedMainGearDeduct = 0;
					bouncedNoseGearDeduct = 0;
				}

				cancelPrint = false;

				return null;
			}

		};

		worker.execute();
	
	}

	public void routeUpdate() {
		String[] rteData = rawRte.split(";");

		if (PSXInfo.activeRoute == 0) {
			PSXInfo.origin = "----";
			PSXInfo.destination = "----";
			PSXInfo.depRwy = "---";
			PSXInfo.arrRwy = "---";
		} else {
			if (rteData[0].equals("bbbb")) {
				PSXInfo.origin = "----";
			} else if ((PSXInfo.ias == 0 && !rteData[0].equals(rteData[1])) || forceRouteUpdate == 1) {
				PSXInfo.origin = rteData[0];
				PSXInfo.setLabel_Service(true);
				bounced = false;
				hasAlreadyLandedMain = false;
				wingStrike = false;
				tailStrike = false;
				PSXInfo.sendToServer("Qs144=0;6;-99");
				PSXInfo.sendToServer("Qs217=0;6;-99");
				PSXInfo.sendToServer("Qs218=0;6;-99");
				PSXInfo.sendToServer("Qs219=0;6;-99");
				PSXInfo.sendToServer("Qs220=0;6;-99");
				PSXInfo.sendToServer("Qi130=1");
				noseCollapse = false;
				cause = 0;
				// PSXInfo.lbl_Landing.setForeground(Color.BLACK);
				// PSXInfo.lbl_Landing.setText("Landing Score: ---");

			}

			if (rteData[1].equals("bbbb")) {
				PSXInfo.destination = "----";
			} else if (PSXInfo.ias == 0 || forceRouteUpdate == 1) {
				PSXInfo.destination = rteData[1];
			}

			if (rteData[2].length() >= 2) {
				PSXInfo.depRwy = rteData[2];
			} else if (PSXInfo.origin.equals("----") || forceRouteUpdate == 1) {
				PSXInfo.depRwy = "---";
			}

			// Arrival Runway
			Pattern pattern = Pattern.compile("___RW(.*?)'");
			Matcher matcher = pattern.matcher(rawRte);
			if (matcher.find()) {
				PSXInfo.arrRwy = matcher.group(1);
			} else {
				PSXInfo.arrRwy = "---";
			}

			PSXInfo.setLabel_Dep("Origin: " + PSXInfo.origin + " / " + PSXInfo.depRwy);
			PSXInfo.setLabel_Arr("Destination: " + PSXInfo.destination + " / " + PSXInfo.arrRwy);

		}

		PSXInfo.setLabel_Dep("Origin: " + PSXInfo.origin + " / " + PSXInfo.depRwy);
		PSXInfo.setLabel_Arr("Destination: " + PSXInfo.destination + " / " + PSXInfo.arrRwy);
	}

	@Override
	public void run() {

		try {

			PSXInfo.connectingUI(2); // Set UI labels/colours etc for Connecting
			socket = new Socket(targetHost, targetPort);
			ou = new PrintWriter(socket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			PSXInfo.connectingUI(3); // Set UI to be isConnected
			PSXInfo.sendToServer("demand=Qs481");
			PSXInfo.sendToServer("demand=Qs482");
		} catch (UnknownHostException e) {
			//System.out.println(e);

			PSXInfo.connectingUI(1); // Set UI to be disconnected
			if (!PSXInfo.autoConnecting) {
				JOptionPane.showMessageDialog(null,
						e,
						//	"Unable to connect. Please check the Server IP / Port and try again", 
						"ERROR: Unknown Address",
						JOptionPane.ERROR_MESSAGE);
			}

			return;
		} catch (IOException e) {
			//System.out.println("Unable to connect to " + PSXInfo.psxhost);
			PSXInfo.connectingUI(1); // Set UI to be disconnected
			if (!PSXInfo.autoConnecting) {
				JOptionPane.showMessageDialog(null,
						e,
						//	"Unable to connect. Please check the Server IP / Port and try again", 
						"ERROR: PSX Not Running",
						JOptionPane.ERROR_MESSAGE);
			}
			return;
		}

		// *********************************************************************
		// Reader:

		try {
			String message;
			char qCategory;
			int qIndex;
			// int val;
			int parseMark;
			while (true) {
				if ((message = in.readLine()) != null) {
					//System.out.println(message);
					try {

						now = new Date(System.currentTimeMillis());
						
						
						if (message.charAt(0) == 'Q') {
							parseMark = message.indexOf('=');
							try {
								qIndex = Integer.parseInt(message.substring(2, parseMark));
								qCategory = message.charAt(1);
								parseMark++;

								if (qCategory == 'h') {
									if (qIndex == 73 | qIndex == 74 | qIndex == 75) { // If autopilot is being used
										int raw = Integer.parseInt(message.substring(parseMark).trim());
										int autopilotIndex = qIndex - 73;
										autopilotStatus[autopilotIndex] = raw;
									}
									
									

									if (qIndex == 143) {
										PSXInfo.magtruehdg = Integer.parseInt(message.substring(parseMark).trim()); // 0
																													// =
																													// mag,
																													// 1
																													// =
																													// true
										// System.out.println("Mag/True: " + PSXInfo.magtruehdg);

										if (PSXInfo.magtruehdg == 0) {
											PSXInfo.flightHdg = PSXInfo.magHdg;
										} else if (PSXInfo.magtruehdg == 1) {
											PSXInfo.flightHdg = PSXInfo.trueHdg;
										}

										PSXInfo.setLabel_Hdg(
												"Heading: " + String.format("%03.0f", PSXInfo.flightHdg) + " (°)");
									}
									
									
								} else if (qCategory == 's') {

									// Free Messages on EICAS tracking
									if(qIndex >= 418 && qIndex <= 422) {
										FreeMsg[qIndex-418] = message.substring(parseMark).trim();
										//System.out.println((qIndex-418) + " - " + FreeMsg[qIndex-418]);
									}
									
									
									if (qIndex == 481) {	//Engine data (if engines switches off and n1 almost 0, service the aircraft
										String raw = message.substring(parseMark).trim();
										if(raw.length() <= 12 && raw.substring(0,4).equals("xxxx") && cause > 0) {
											PSXInfo.aircraftService(false);
											cause = 0;
											PSXInfo.setLabel_Ldg("Landing Score: ---");
										}
									}
									
									if (qIndex == 482) {	//Engine parameters
										String raw = message.substring(parseMark).trim();
										String[] strArray = raw.split(";");
										
										int[] intArray = new int[strArray.length];
										for(int i = 0; i < strArray.length; i++) {
										    intArray[i] = Integer.parseInt(strArray[i]);
										}

										PSXInfo.avgN1 = (float) (intArray[1] + intArray[2] + intArray[3] + intArray[4])/10/4; //Original in x10 %
										PSXInfo.avgEgt = (intArray[9] + intArray[10] + intArray[11] + intArray[12])/4;
										
										//System.out.println("Average N1: " + PSXInfo.avgN1 + " from " + intArray[1] + " " + intArray[2] + " " + intArray[3] + " " + intArray[4] + " and Average EGT: " + PSXInfo.avgEgt + " from " + intArray[9] + " " + intArray[10] + " " + intArray[11] + " " + intArray[12]);
									}
									
									if (qIndex == 483) {	//Engine parameters
										String raw = message.substring(parseMark).trim();
										String[] strArray = raw.split(";");
										
										int[] intArray = new int[strArray.length];
										for(int i = 0; i < strArray.length; i++) {
										    intArray[i] = Integer.parseInt(strArray[i]);
										}
										PSXInfo.oat = (float) intArray[1]/10; //Original in x10 %
										//System.out.println("OAT: " + PSXInfo.oat);
									}
									
									
									if (qIndex == 444) { //Active Runway
										String raw = message.substring(parseMark).trim();
										rawRunway = raw.split(";");
										
										//Only update the runway if it's possitble that it could be the new runway (i.e PSX is flying through the air etc)
										if(PSXInfo.onGround == 0) {
										PSXInfo.rwyLatitude = Float.parseFloat(rawRunway[1]);
										PSXInfo.rwyLongitude = Float.parseFloat(rawRunway[2]);
										rwyElev = Integer.parseInt(rawRunway[3]);
										PSXInfo.rwyHeadingDegrees = Integer.parseInt(rawRunway[5]);
										
										hasLastLandingBeenPrinted = false;
										
										}
										// System.out.println("Cause: " + cause);
										// Autopilot Assist
										if ((startAlt == -1 || PSXInfo.altitude < Math.abs(startAlt - 10))
												&& (PSXInfo.altitude - rwyElev) < autopilotAssistHeight
												&& (autopilotStatus[0] == 8 || autopilotStatus[1] == 8
														|| autopilotStatus[2] == 8) & rwyElev >= 0) {
											autopilotAssist = true;
											if (cause == 0 || (PSXInfo.altitude - rwyElev) < 10) {
												int lblLandingIndex = PSXInfo.lbl_Landing.getText().indexOf(":");
												PSXInfo.lbl_Landing.setText("Autopilot Assisted"
														+ PSXInfo.lbl_Landing.getText().substring(lblLandingIndex));
												PSXInfo.lbl_Landing.setForeground(Color.magenta);
											}
										} else if ((PSXInfo.altitude - rwyElev) >= autopilotAssistHeight) {
											autopilotAssist = false;
										}

									}
									if (qIndex == PSXInfo.StartPiBaHeAlVsTasYw) {
										String raw = message.substring(parseMark).trim();
										String[] pos = rawPosition.split(";");
										PSXInfo.update = Integer.parseInt(raw.substring(0, 1));

										// send("Qs119= --- LANDING DATA ---");
										// send("Qs119= --- LANDING DATA ---");
										// send("Qs119= --- LANDING DATA ---");
										// send("Qs119= --- LANDING DATA ---");

										if (PSXInfo.update == 1) {
											routeUpdate();
										}

										if (PSXInfo.update != PSXInfo.updateStart) {
											PSXInfo.updateOccur = 1;
											forceRouteUpdate = 1;
											if (PSXInfo.update == 1) {
												routeUpdate();
											}
											PSXInfo.updateStart = PSXInfo.update;

										}

										int alt = Integer.parseInt(pos[3]);
										startAlt = alt / 1000;
										// Autopilot Assist
										if ((startAlt == -1 || alt < Math.abs(startAlt - 10))
												&& (alt - rwyElev) < autopilotAssistHeight
												&& (autopilotStatus[0] == 8 || autopilotStatus[1] == 8
														|| autopilotStatus[2] == 8) & rwyElev >= 0) {
											autopilotAssist = true;
											if (cause == 0 || (alt - rwyElev) < 10) {
												int lblLandingIndex = PSXInfo.lbl_Landing.getText().indexOf(":");
												PSXInfo.lbl_Landing.setText("Autopilot Assisted" +
												PSXInfo.lbl_Landing.getText().substring(lblLandingIndex));
												PSXInfo.lbl_Landing.setForeground(Color.magenta);
											}
										} else if ((alt - rwyElev) >= autopilotAssistHeight) {
											autopilotAssist = false;
										}

									}

									if (qIndex == PSXInfo.qsPiBaHeAlTas) {
										// Pitch,Bank, Heading, Position, Altitude,True Airspeed
										
										
										rawPosition = message.substring(parseMark).trim();
										String[] pos = rawPosition.split(";");
										
										// Latitude
										float lat = (float) (Math.toDegrees(Double.parseDouble(pos[5])));
										PSXInfo.latitude = lat;
										if (lat > 0) {
											PSXInfo.setLabel_Lat("Latitude: " + String.format("%.3f", lat) + " (°N)");
										} else if (lat <= 0) {
											PSXInfo.setLabel_Lat("Latitude: " + String.format("%.3f", lat) + " (°S)");
										}

										// Longitude
										float lon = (float) (Math.toDegrees(Double.parseDouble(pos[6])));
										PSXInfo.longitude = lon;
										if (lon >= 0) {
											PSXInfo.setLabel_Lon("Longitude: " + String.format("%.3f", lon) + " (°E)");
										} else if (lon < 0) {
											PSXInfo.setLabel_Lon("Longitude: " + String.format("%.3f", lon) + " (°W)");
										}
										
										
										// True Altitude
										int alt = Integer.parseInt(pos[3]);
										alt = alt / 1000;
										PSXInfo.setLabel_Alt(
												"Altitude: " + NumberFormat.getIntegerInstance().format(alt) + " (ft)");
										PSXInfo.altitude = alt;
										

										// AUTOPILOT ASSIST
										/* IF
											1. The aircraft altitude is less than the start height (gives a chance to disengage autopilot while frozen)
										AND 2. Aircraft altitude AGL is less than specified cut off height
										AND 3. Autopilot is turned on on either the autopilots
										AND 4. Runway is above sea level												
										*/
										if ((startAlt == -1 || alt < Math.abs(startAlt - 30)) &		
											(alt - rwyElev) < autopilotAssistHeight &		
											(autopilotStatus[0] == 8 || autopilotStatus[1] == 8 || autopilotStatus[2] == 8) &				//
											rwyElev >= 0 &
											finishedLoading
											)
										{
											autopilotAssist = true;
											if (
													// If on the ground with a good landing (no bounce, wing strike etc)
													cause == 0 || (alt - rwyElev) < 10
												) 
												
											{	
												int lblLandingIndex = PSXInfo.lbl_Landing.getText().indexOf(":");
												if(
														// If pilot licence not revoked while speed is high
														PSXInfo.lbl_Landing.getText() != "PILOT LICENCE REVOKED" && PSXInfo.tas > 100
													) 
												{
														PSXInfo.lbl_Landing.setText("Autopilot Assisted"
														+ PSXInfo.lbl_Landing.getText().substring(lblLandingIndex));
														if(PSXInfo.onGround == 0)
														send("Qs422=(A/P ASSISTED)");
												}
												
												// Set text to be magenta at the end
												PSXInfo.lbl_Landing.setForeground(Color.magenta);
											}

										} else if 
										(
												//IF the aircraft then flies back above the height
												(alt - rwyElev) >= autopilotAssistHeight
												&& autopilotAssist == true
										) 
										{
											autopilotAssist = false;
											send("Qs422=");
											if(PSXInfo.lbl_Landing.getForeground().equals(Color.magenta))
											PSXInfo.lbl_Landing.setForeground(Color.black);
											
											if(PSXInfo.lbl_Landing.getText().startsWith("Autopilot Assisted"))
												PSXInfo.lbl_Landing.setText("Landing Score" + PSXInfo.lbl_Landing.getText().substring(PSXInfo.lbl_Landing.getText().indexOf(":")));
										}

										// True Heading
										float hdg = (float) (Float.parseFloat(pos[2]) * 180 / Math.PI);
										PSXInfo.trueHdg = hdg;
										if (PSXInfo.magtruehdg == 0) {
											PSXInfo.flightHdg = PSXInfo.magHdg;
										} else if (PSXInfo.magtruehdg == 1) {
											PSXInfo.flightHdg = PSXInfo.trueHdg;
										}

										PSXInfo.setLabel_Hdg(
												"Heading: " + String.format("%03.0f", PSXInfo.flightHdg) + " (°)");

										// True Airspeed
										tas = Integer.parseInt(pos[4]); // TAS is in tas * 1000 kts
										if(PSXInfo.onGround == 1 && noseGear > 0 && LandingCalc.isDecelerationRateActive) {
											if(tas > 3000) {
												maxDecel = LandingCalc.getDecelerationRate(tas);
											} else {
												LandingCalc.isDecelerationRateActive = false;
											}
										}
										
										PSXInfo.lblDecelData.setText(String.format("%.1f m/s\u00B3", maxDecel));
										//if(LandingCalc.isDecelerationRateActive)
										//PSXInfo.lblDecelDeduct.setText(String.format("%.1f", PSXInfo.landingMaxDecel));
										tas = tas / 1000; // Reduced to actual tas for display

										PSXInfo.tas = tas;

										if(PSXInfo.tas > LandingCalc.maxExitSpeed || PSXInfo.onGround == 0) {
											PSXInfo.rwyLatitude = Float.parseFloat(rawRunway[1]);
											PSXInfo.rwyLongitude = Float.parseFloat(rawRunway[2]);
											rwyElev = Integer.parseInt(rawRunway[3]);
											PSXInfo.rwyHeadingDegrees = Integer.parseInt(rawRunway[5]);
										}
										

										
										//if(LandingCalc.isValidOffset())
											PSXInfo.lblAvgOffsetData.setText(String.format("%.0f m", LandingCalc.getCurrentOffset(PSXInfo.rwyLatitude, PSXInfo.rwyLongitude, PSXInfo.rwyHeadingDegrees, PSXInfo.latitude, PSXInfo.longitude)));
											if(LandingCalc.isValidOffset) {
												//System.out.println("Recalculating Score");
												if(!hasLastLandingBeenPrinted)
												LandingCalc.calculateScore(false);
											}
											//System.out.println(tas);
										if (tas < PSXInfo.maxTaxiSpeed && readyToPrint && PSXInfo.printEnabled && !hasLastLandingBeenPrinted) {
											printLanding(timestamp, PSXInfo.landingScore, autopilotAssist, bounced,
													wingStrike, tailStrike, noseCollapse,
													(bounced ? bouncedMainGear: mainGear), Double.parseDouble(PSXInfo.lblMainGearDeduct.getText()),
													(bounced ? bouncedNoseGear: noseGear),
													(bounced ? bouncedNoseGearDeduct
															: Double.parseDouble(PSXInfo.lblNoseGearDeduct.getText())),
													offset, PSXInfo.landingOffset, distance, PSXInfo.landingDistance,
													pitch, PSXInfo.landingPitch, bank, PSXInfo.landingBank, crab,
													PSXInfo.landingCrab, speed, PSXInfo.landingSpeed, landWindDir, landWindSpeed);
											readyToPrint = false;
										}
										
										
										// Record wind for landing when at least # feet above runway
										if(alt - rwyElev > 50 && alt - rwyElev < autopilotAssistHeight) {
											landWindDir = PSXInfo.windDir;
											landWindSpeed = PSXInfo.windSpd;
										}
										
									}
									if (qIndex == PSXInfo.qsFuelQty) {
										String rawFuel = message.substring(7); // Remove first 7 characters
										String[] rawTanks = rawFuel.split(";");

										double FuelTanks = ((Integer.parseInt(rawTanks[0])
												+ Integer.parseInt(rawTanks[1]) + Integer.parseInt(rawTanks[2])
												+ Integer.parseInt(rawTanks[3]) + Integer.parseInt(rawTanks[4])
												+ Integer.parseInt(rawTanks[5]) + Integer.parseInt(rawTanks[6])
												+ Integer.parseInt(rawTanks[7]) + Integer.parseInt(rawTanks[8]))
												* .453592);
										// 0=TankM1;1=TankM2;2=TankM3;3=TankM4;4=TankR2;5=TankR3;6=TankCe;7=TankSt;8=TankAu;9=PreselectNewFuel;10=FuelTemp

										PSXInfo.fuelQty = FuelTanks / 10000;
									}
									if (qIndex == PSXInfo.qsFreqsAntenna) {
										String rawFreq = message.substring(parseMark).trim(); // Remove first few
																								// characters
										String[] freqs = rawFreq.split(";");

										if (Double.parseDouble(freqs[0]) >= 100) {
											PSXInfo.comm1 = Double.parseDouble(freqs[0]) / 1000;
											PSXInfo.comm2 = Double.parseDouble(freqs[2]) / 1000;
										}
									}
									if (qIndex == PSXInfo.qsActiveRoute) {
										String rawRte = message.substring(parseMark).trim(); // Remove first few
																								// characters
										// String[] rteData = rawRte.split(";");
										// System.out.println(rawRte);
										
										
										prevActiveRoute = PSXInfo.activeRoute;
										PSXInfo.activeRoute = (int) Integer.parseInt(rawRte.substring(2, 3));
										if(PSXInfo.activeRoute != prevActiveRoute || (activateRoute <= 10 && Integer.parseInt(rawRte.substring(0,2)) > 10 )) {
											forceRouteUpdate = 1;
										}
										activateRoute = (int) Integer.parseInt(rawRte.substring(0,2));
										
										if (PSXInfo.activeRoute == 1)
											PSXInfo.qsRteData = 376;
										else
											PSXInfo.qsRteData = 377;
									}
									if (qIndex == PSXInfo.qsRteData) {
										rawRte = message.substring(parseMark).trim(); // Remove first few

										 //System.out.println("Active route: "+PSXInfo.activeRoute);
										// System.out.println(rawRte);
										// System.out.println("Update Occur? " + forceRouteUpdate);

										routeUpdate();

										forceRouteUpdate = 0;
									}

									if (qIndex == PSXInfo.MiscFltData) {
										String rawFltData = message.substring(parseMark).trim(); // Remove first few

										staticFltData = rawFltData;
										if (rawFltData.charAt(0) >= 0) {
											String[] fltData = rawFltData.split(";");

											PSXInfo.ias = Float.parseFloat(fltData[0]) / 10;
											PSXInfo.windDir = Integer.parseInt(fltData[3]);
											PSXInfo.windSpd = Integer.parseInt(fltData[4]);
											PSXInfo.magVar = Float.parseFloat(fltData[5]) / 10;

											PSXInfo.setLabel_Ias("IAS: "
													+ NumberFormat.getIntegerInstance().format(PSXInfo.ias) + " (kts)");

											// Magnetic Heading
											if (PSXInfo.trueHdg - PSXInfo.magVar < 0) {
												PSXInfo.magHdg = 360 + (PSXInfo.trueHdg - PSXInfo.magVar);
											} else {
												PSXInfo.magHdg = PSXInfo.trueHdg - PSXInfo.magVar;
											}

											if (PSXInfo.magtruehdg == 0) {
												PSXInfo.flightHdg = PSXInfo.magHdg;
											} else if (PSXInfo.magtruehdg == 1) {
												PSXInfo.flightHdg = PSXInfo.trueHdg;
											}

											PSXInfo.setLabel_Hdg(
													"Heading: " + String.format("%03.0f", PSXInfo.flightHdg) + " (°)");

											// Wind Label
											PSXInfo.setLabel_Wnd("Wind: " + String.format("%03d", PSXInfo.windDir)
													+ " ° / " + PSXInfo.windSpd + " kts");

										}
									}

									if (qIndex == 488 && finishedLoading) {
										String rawTouchdown = message.substring(parseMark).trim(); // Remove first few
										System.out.println("------------------LANDING--------------------");
										System.out.println("Raw touchdown: " + rawTouchdown);
										
										String[] touchdown = rawTouchdown.split(";");
										
										if(mainGear == 0 & Integer.parseInt(touchdown[2]) > 0 & PSXInfo.onGround == 1) {
											System.out.println("RESETTING VARIABLES");
											LandingCalc.averageOffset = 0;
											LandingCalc.maxChangeInDeceleration = 0;
											maxDecel = 0;
										}

										noseGear = Integer.parseInt(touchdown[1]);
										mainGear = Integer.parseInt(touchdown[2]);
										offset = Integer.parseInt(touchdown[3]) / 3.281; //ft to m
										distance = Integer.parseInt(touchdown[4]);
										pitch = Double.parseDouble(touchdown[5]) / 10.0;
										bank = Double.parseDouble(touchdown[6]) / 10.0;
										crab = Double.parseDouble(touchdown[7]) / 10.0;
										speed = Integer.parseInt(touchdown[8]);
										bouncedNoseGear = (int) Math.max(bouncedNoseGear, noseGear);
										bouncedMainGear = Math.max(bouncedMainGear, mainGear);
										bouncedNoseGearDeduct = LandingCalc.getNoseGearScore(bouncedNoseGear);
										bouncedMainGearDeduct = LandingCalc.getMainGearScore(bouncedMainGear);

										
										//String format = "%-7s%-5s%-5s%-5s%-6s%-6s%-6s%-6s%-6s%-40s%s%n";
										//System.out.printf(format, touchdown[0], noseGear, mainGear, offset, distance, pitch, bank, crab, speed, "Raw: " + rawTouchdown, sdf.format(now));
										
										Timestamp newTimestamp = new Timestamp(System.currentTimeMillis());
										timestamp = newTimestamp;

										
										if (!rawTouchdown.substring(1).equals(prevRawTouchdown.substring(1))) {
											try {
												FileWriter fw = new FileWriter(rawTouchdownLocalFile, true);
												fw.write(
														timestamp + "\t\t" + PSXInfo.origin + " - "
																+ PSXInfo.destination + ":\tMain Gear:" + mainGear
																+ " \tNose Gear:" + noseGear
																+ (noseGear >= 10 || noseGear < 0 ? "\t" : "\t\t")
																+ PSXInfo.lbl_Serviceable.getText()
																+ (PSXInfo.lbl_Serviceable.getText()
																		.equals("SERVICEABLE") ? "\t\t\t" : "\t\t")
																+ rawTouchdown + "\n");

												fw.close();
											} catch (IOException ioe) {
												System.err.println("IOException: " + ioe.getMessage());
											}
										}

										/*
										 * if (noseGear < 0) { prevNoseGear = noseGear; }
										 */

										// System.out.println("Touchdown Timer: " + String.format("%d", now.getTime() -
										// touchdownTimer.getTime()));

										// if (now.getTime() > touchdownTimer.getTime() + 6000) {
										//System.out.println(PSXInfo.landingNoseGear + "\t" + noseGear + "\t" + LandingCalc.noseGearLandingTime);
										if(mainGear > 0 && LandingCalc.mainGearLandingTime == 0) {
											LandingCalc.mainGearLandingTime = System.currentTimeMillis();
											LandingCalc.timer = new NoseTransitionTime();
											LandingCalc.t = new Thread(LandingCalc.timer, "Timer");
											LandingCalc.t.start();
										}

										
										
										if(noseGear > 0 && LandingCalc.noseGearLandingTime == 0) {
											LandingCalc.noseGearLandingTime = System.currentTimeMillis();
											LandingCalc.timer.exit();
										}
										
										PSXInfo.landingNoseGear = LandingCalc.getNoseGearScore(noseGear);
										PSXInfo.landingMainGear = LandingCalc.getMainGearScore(mainGear);
										PSXInfo.landingOffset = LandingCalc.getLandingOffsetScore(offset);
										PSXInfo.landingDistance = LandingCalc.getThresholdScore(distance);
										PSXInfo.landingPitch = LandingCalc.getPitchScore(pitch);
										PSXInfo.landingBank = LandingCalc.getBankScore(bank);
										PSXInfo.landingCrab = LandingCalc.getCrabScore(crab);
										PSXInfo.landingSpeed = LandingCalc.getSpeedScore(speed);
										
										
										
										
										
										if (mainGear > 0) {
											if (noseGear == 0 && hasAlreadyLandedMain) {
												bounced = true;

												if (cause == 0)
													cause = 1;
												try {
													FileWriter fw = new FileWriter(rawTouchdownLocalFile, true);
													fw.write(timestamp
															+ "\t\t--- BOUNCED --- BOUNCED --- BOUNCED --- BOUNCED ---\n");
													fw.close();

													if(cause <= 1) {
													String strScore = "BOUNCED: 0.0%";
													Color color = Color.RED;
													PSXInfo.lbl_Landing.setForeground(color);
													PSXInfo.setLabel_Ldg(strScore);
													}
												} catch (IOException ioe) {
													System.err.println("IOException: " + ioe.getMessage());
												}
											}

											hasAlreadyLandedMain = true;
											if (mainGear == prevMainGear && (noseGear == prevNoseGear || noseGear > 0)
													&& System.currentTimeMillis() - touchdownTimer.getTime() >= 12000) {
												hasAlreadyLandedMain = false;
												System.out.println("BOUNCED SET TO FALSE");
											}
											touchdownTimer.setTime(now.getTime());
										} else if (mainGear == 0 && mainGear == prevMainGear && noseGear == prevNoseGear
												&& System.currentTimeMillis() - touchdownTimer.getTime() >= 12000) {
											hasAlreadyLandedMain = false;
											bounced = false;
										}
										
										

										// Is the aircraft unserviceable?
										String wing = String.valueOf(touchdown[0].charAt(2));
										String tail = String.valueOf(touchdown[0].charAt(3));

										if (!wing.equals("w") || tail.equals("T") || noseGear < 0) {
											PSXInfo.setLabel_Service(false);

											if (!wing.equals("w")) {
												wingStrike = true;
												if (cause == 0)
													cause = 2;
												send("Qs422=(WING STRIKE)");
												String str = String.format("WING STRIKE: 0.0%n%%",
														PSXInfo.landingScore);
												PSXInfo.setLabel_Ldg(str);
												PSXInfo.lbl_Landing.setForeground(Color.RED);
												try {
													FileWriter fw = new FileWriter(rawTouchdownLocalFile, true);
													fw.write(timestamp
															+ "\t\t--- WING STRIKE --- WING STRIKE --- WING STRIKE ---\n");
													fw.close();
												} catch (IOException ioe) {
													System.err.println("IOException: " + ioe.getMessage());
												}
											}

											if (tail.equals("T")) {
												tailStrike = true;
												if (cause == 0)
													cause = 3;
												send("Qs422=(TAIL STRIKE)");
												String str = String.format("TAIL STRIKE: %.2f%n%%",0.0);//PSXInfo.landingScore);
												PSXInfo.setLabel_Ldg(str);
												
												/*
												int lblLandingIndex = PSXInfo.lbl_Landing.getText().indexOf(":");
												PSXInfo.lbl_Landing.setText("Tail Strike"
												+ PSXInfo.lbl_Landing.getText().substring(lblLandingIndex));
												*/
												
												PSXInfo.lbl_Landing.setForeground(Color.RED);
												try {
													FileWriter fw = new FileWriter(rawTouchdownLocalFile, true);
													fw.write(timestamp
															+ "\t\t--- TAIL STRIKE --- TAIL STRIKE --- TAIL STRIKE ---\n");
													fw.close();
												} catch (IOException ioe) {
													System.err.println("IOException: " + ioe.getMessage());
												}
											}

											if (noseGear < 0) {
												noseCollapse = true;
												if (cause == 0)
													cause = 4;
												send("Qs422=(NOSE COLLAPSE)");
												String str = String.format("NOSE COLLAPSE: 0.0%n%%",
														PSXInfo.landingScore);
												PSXInfo.setLabel_Ldg(str);
												PSXInfo.lbl_Landing.setForeground(Color.RED);
												try {
													FileWriter fw = new FileWriter(rawTouchdownLocalFile, true);
													fw.write(timestamp
															+ "\t\t--- NOSE COLLAPSE --- NOSE COLLAPSE --- NOSE COLLAPSE ---\n");
													fw.close();
												} catch (IOException ioe) {
													System.err.println("IOException: " + ioe.getMessage());
												}
											}
										}

										
										

										// Touchdown File
										if ((mainGear != PSXInfo.landingPrevMainGear) && (noseGear != PSXInfo.landingPrevNoseGear)) {
											
											LandingCalc.calculateScore(true);
											
											/*
											PSXInfo.PathTouchdownLocalFile = PSXInfo.ftpFile + "-touchdown.txt";
											File TouchdownLocalFile = new File(PSXInfo.PathTouchdownLocalFile);

											// Current Time
											SimpleDateFormat sdf = new SimpleDateFormat();
											sdf.setTimeZone(TimeZone.getTimeZone("Australia/Sydney"));
											String time = sdf.format(now);

											try {
												FileWriter fw = new FileWriter(TouchdownLocalFile, true);
												fw.write(PSXInfo.origin + " - " + PSXInfo.destination + ":\tMain Gear: "
														+ mainGear + (mainGear >= 800 ? "\t" : "\t\t") + "Nose Gear: "
														+ noseGear + " \t" + PSXInfo.lbl_Serviceable.getText()
														+ " \tTime: " + time + "\n");
												fw.close();
											} catch (IOException ioe) {
												System.err.println("IOException: " + ioe.getMessage());
											}
											*/

											PSXInfo.landingPrevMainGear = mainGear;
											PSXInfo.landingPrevNoseGear = noseGear;
										}

										prevMainGear = mainGear;
										prevNoseGear = noseGear;
										prevRawTouchdown = rawTouchdown;
										
									}

								} else if (qCategory == 'i') {
									if (qIndex == 219) {
										int data = Integer.parseInt(message.substring(parseMark).trim());
										PSXInfo.acftHeight = data;
									}
									
									if (qIndex == 257) {
										int data = Integer.parseInt(message.substring(parseMark).trim());
										PSXInfo.onGround = data;
										
										if(PSXInfo.tas > LandingCalc.maxExitSpeed || PSXInfo.onGround == 0) {
											PSXInfo.rwyLatitude = Float.parseFloat(rawRunway[1]);
											PSXInfo.rwyLongitude = Float.parseFloat(rawRunway[2]);
											rwyElev = Integer.parseInt(rawRunway[3]);
											PSXInfo.rwyHeadingDegrees = Integer.parseInt(rawRunway[5]);
										}
										
										//System.out.println("Previous Landing Time = " + prevLandingTime);
										//System.out.println((PSXInfo.onGround == 1 ? "On ground" : "In air") + " at Time: " + now);
										//System.out.println("Free MSG:" + FreeMsg[3]);
										landingTime = now;
										
										if(PSXInfo.onGround == 1 && landingTime.getTime() - prevLandingTime.getTime() <= bounceMilliSeconds) {
											//System.out.println("BOUNCED");
											bounced = true;
											send("Qs422=(BOUNCED)");
											
											if (cause == 0)
												cause = 1;
											
										} else if(PSXInfo.onGround == 0 && landingTime.getTime() - prevLandingTime.getTime() <= 2000) {
											//System.out.println("BOUNCED BACK INTO AIR");
											bounced = true;
											send("Qs422=(BOUNCED)");
											if (cause == 0)
												cause = 1;
										} else {
											//System.out.println("DIDNT BOUNCE");
											prevLandingTime = landingTime;
											hasAlreadyLandedMain = false;
											bounced = false;
										}
										
										
										if(bounced) {
											try {
												File rawTouchdownLocalFile = new File(PSXInfo.touchdownLogFileName);
												FileWriter fw = new FileWriter(rawTouchdownLocalFile, true);
												fw.write(timestamp
														+ "\t\t--- BOUNCED --- BOUNCED --- BOUNCED --- BOUNCED ---\n");
												fw.close();

												if(cause <= 1) {
												String strScore = "BOUNCED: 0.0%";
												Color color = Color.RED;
												PSXInfo.lbl_Landing.setForeground(color);
												PSXInfo.setLabel_Ldg(strScore);
												}
											} catch (IOException ioe) {
												System.err.println("IOException: " + ioe.getMessage());
											}
										}
										
									
										
										// Clear out lower EICAS when airborne again
										if(PSXInfo.onGround == 0) {
											// System.out.println("Clearing LWR EICAS");
											send("Qs421=");
											FreeMsg[3] = "";
											send("Qs422=");
											FreeMsg[4] = "";
										}
										
										// Reset Nose Transition Timer
										if(PSXInfo.onGround == 0) {
										LandingCalc.mainGearLandingTime = 0;
										LandingCalc.noseGearLandingTime = 0;
										}

									}

									if (qIndex == PSXInfo.qiActDestEta) {

										PSXInfo.destEta = Integer.parseInt(message.substring(parseMark).trim());
										// -1 if not valid data
										if (PSXInfo.destEta >= 0) {
											PSXInfo.eta = String.format("%04d", PSXInfo.destEta);
										} else {
											PSXInfo.eta = "----";
										}

										PSXInfo.setLabel_Eta("ETA: " + PSXInfo.eta + " z");
										// System.out.println("ETA = " + PSXInfo.eta);

									}

								}

							} catch (NumberFormatException nfe) {
								// nfe.printStackTrace();
							}
						} else if (message.charAt(0) == 'L') {

							// Lexicon at net connect - Ignore

						} else if (message.substring(0, 3).equals("id=")) {

							try {
								// System.out.println("Connection OK. Our client id: " + message);
							} catch (NumberFormatException nfe) {
								nfe.printStackTrace();
							}

						} else if (message.length() > 8 && message.substring(0, 8).equals("version=")) {
							// Check version agreement if required
						} else if (message.equals("load1")) {
							// Situation loading phase 1 (paused and reading variables)
							finishedLoading = false;
							printEnabled = false;
							cause = 0;
							prevMainGear = 0;
							prevNoseGear = 0;
							//prevLandingTime = now;
							bounced = false;
							wingStrike = false;
							tailStrike = false;
							noseCollapse = false;
							autopilotAssist = false;
							hasAlreadyLandedMain = false;
						} else if (message.equals("load2")) {
							// Situation loading phase 2 (reading model options)
						} else if (message.equals("load3")) {
							finishedLoading = true;
							cause = 0;
							prevMainGear = 0;
							prevNoseGear = 0;
							prevLandingTime = new Date(now.getTime() - bounceMilliSeconds);
							bounced = false;
							wingStrike = false;
							tailStrike = false;
							noseCollapse = false;
							autopilotAssist = false;
							hasAlreadyLandedMain = false;
							printEnabled = true;
								PSXInfo.lbl_Landing.setText("Landing Score: ---");
								mainGear = -1;
								bouncedMainGear = -1;
							PSXInfo.lbl_Landing.setForeground(Color.black);

						} else if (message.equals("exit")) {
							remoteExit = true;
							break;
						} else if (message.startsWith("metar=")) {
							// METAR feeder status message
						}

						
					} catch (StringIndexOutOfBoundsException sioobe) {
						sioobe.printStackTrace();
					} catch(Exception e) {
						e.printStackTrace();
					}
				}
			}

			try {
				notifyAll();
			} catch (Exception e) {

			}
		} catch (

		IOException e) {
		}

		if (PSXInfo.isConnected & PSXInfo.autoReconnect) {
			finalJobs();
			PSXInfo.autoConnecting = true;
			PSXInfo.txt_HostIP.setEnabled(false);
			PSXInfo.txt_HostPort.setEnabled(false);
			PSXInfo.lab_Conn.setText("CONNECTING");
			PSXInfo.setBtnConnectText("Reconnecting");
			PSXInfo.lab_Conn.setForeground(Color.decode("#FF9900"));

			
			try {
				TimeUnit.MILLISECONDS.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			while (!PSXInfo.isConnected & PSXInfo.autoConnecting) {
				// System.out.println("Is isConnected?: " + PSXInfo.isConnected);
				// System.out.println("Attempting reconnect...");
				PSXInfo.swingWorker = null;
				run();
				
				try {
					TimeUnit.MILLISECONDS.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				

			}
		} else {
			if(!socket.isClosed())
				finalJobs();
		}

	}

	public void finalJobs() {
		try {
			//System.out.println("Disconnecting...");
			PSXInfo.connectingUI(1);
			//Config.saveProperties();
			if (!PSXInfo.netThread.remoteExit && PSXInfo.netThread.ou != null) {
			//if(PSXInfo.netThread.ou != null) {
				PSXInfo.netThread.ou.println("exit");
				try {
					sleep(30);
				} catch (InterruptedException e) {
				}
				PSXInfo.netThread.ou.close();
			}
			if (PSXInfo.netThread.in != null)
				PSXInfo.netThread.in.close();
			if (socket != null) {
				socket.close();
			}
		} catch (Exception ioe) {
			ioe.printStackTrace();
		}
	}

	public void send(String s) {
		if (PSXInfo.netThread.ou != null) {
			PSXInfo.netThread.ou.println(s);
			if (PSXInfo.netThread.ou.checkError())
				finalJobs();
		}
	}
}
