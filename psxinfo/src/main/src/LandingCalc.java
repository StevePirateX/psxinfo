package src;

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class LandingCalc extends PSXInfo {
	final static int RADIUS_OF_EARTH = 6371 * 1000; // kilometres * 1000 metres (approximate)
	final static int LICENCE_REVOKED_TOUCHDOWN_RATE = 1000; // Main gear feet / min
	
	protected static boolean scoreOffsetAndThreshold = true;
	protected static boolean isDecelerationRateActive = false;
	static double maxChangeInDeceleration = 0; //
	private static int decelerationCalculationCount = 0;
	private static double initialDeceleration = -1;
	private static double previousSpeed = -1;
	private static double previousDeceleration = 0;
	// private static double previousChangeInDeceleration = 0;
	private static long previousTimeMillis = System.currentTimeMillis(); // Used to calculate the change in deceleration

	// private static int previousDistanceOffsetRecorded = 0; // Measured in m
	protected static int averageOffset = 0; // Measured in m
	static boolean isValidOffset = false;
	private static int metresToCalculateAverage = 200; // How long each sector is to calculate the average
	private static int numberOfAveragesForSmoothing = 6; // Number of distances that get taken into account for the average
	private static double[] smoothedDistancesFromCentreline = new double[numberOfAveragesForSmoothing];

	private static double distanceToThreshold = 0;
	protected static boolean isAverageOffsetActive = false;
	protected static int maxExitSpeed = 60;
	private static int currentSector = -1;
	private static double[] averageOffsetMeasurements = new double[Math.round(5000 / metresToCalculateAverage)];
	private static boolean hasAverageMeasurementBeenTaken[] = new boolean[averageOffsetMeasurements.length];

	protected static long mainGearLandingTime = 0;
	protected static long noseGearLandingTime = 0;
	protected static float noseTransitionTime = 0; // In seconds
	protected static NoseTransitionTime timer;
	protected static Thread t;

	public static Font labelFont = new Font("Tahoma", Font.PLAIN, 11);
	// public static Font labelFont = PSXInfo.lblMainGearData.getFont();

	private LandingCalc() {
		
	}
	
	// Landing score top label
	static String formatLandingLabel(String label, String value, String units, String deduction) {
		return label + ": \t \t" + value + " (" + units + ") [" + deduction + "%]";
	}
	
	static double calculateScoreAbs(double rate, double coefficient, double exponent, double target, double subtract) {
		// When values which are negative are the same as positive (e.g.
		// bank/pitch/threshold distance)
		// The target is the ideal distance from the rate
		return Math.min(100, Math.max(0, coefficient * Math.pow(Math.abs(rate - target), exponent) - subtract));
	}

	static double getNoseGearScore(int i) {
		double rate = (double) i;
		//rate = rate / 10;
		if(rate > 0) {
			isDecelerationRateActive = true;						// This is for the change in deceleration. The nose gear activates it
		}
		//return 10.2;
		return calculateScoreAbs(rate, 0.075, 1, 100, 1.5);
	}

	static double getMainGearScore(int i) {
		double rate = (double) i;
		//rate = rate / 10;
		return calculateScoreAbs(rate, 0.0031625, 1.5, 100, 0);
		
	}

	static double getLandingOffsetScore(double d) {
		double rate = (double) Math.abs(d) * 3.281 / 10;
		double score;

		if(scoreOffsetAndThreshold) {
			score = calculateScoreAbs(rate, 0.318, 1.1, 0, 0.3);
		} else {
			score = 0;
		}
		return score;
	}

	static double getThresholdScore(int rate) {
		double score;
		if(scoreOffsetAndThreshold) {
			score = calculateScoreAbs(rate, 0.000013, 2, 1500, 1);
		} else {
			score = 0;
		}
		return score;
	}

	static double getPitchScore(double d) {
		double rate = d;
		return calculateScoreAbs(rate, 2, 1.8, 4, 4);
	}

	static double getBankScore(double d) {
		double rate = Math.abs(d);
		return calculateScoreAbs(rate, 0.5, 1.8, 0, 1);
	}

	static double getCrabScore(double d) {
		double rate = Math.abs(d); // Rate is in 10x degrees
		double score;
		score = Math.max(calculateScoreAbs(rate, 0.3, 1.8, 0, 5) - PSXInfoNetThread.landWindSpeed / 4, 0);
		return Math.min(100, score);
	}

	static double getSpeedScore(int i) {
		double rate = i;
		return calculateScoreAbs(rate, 0.2, 1.6, 2.5, 1);
	}

	static double getDecelerationRate(int tas) {
		if ((PSXInfo.onGround == 0 && PSXInfoNetThread.noseGear > 0)) {
			isDecelerationRateActive = false;
			maxChangeInDeceleration = 0;
			initialDeceleration = -1;
			decelerationCalculationCount = 0;
			return maxChangeInDeceleration;
		}

		if (!isDecelerationRateActive) {
			initialDeceleration = -1;
			decelerationCalculationCount = 0;
			return maxChangeInDeceleration;
		}
		
		decelerationCalculationCount++;

		long t = System.currentTimeMillis(); // Current Time in millisecionds

		//System.out.println("Current Time " + t + " Previous " + previousTimeMillis);
		double changeInTime = (t - previousTimeMillis);
		changeInTime = changeInTime / 1000;
		// System.out.println(String.format("Change in time = %.9f", changeInTime));
		if (changeInTime < 0.9)
			return maxChangeInDeceleration;

		double knotsToMetresPerSecond = (double) 463 / 900;
		double speed = tas / 1000 * knotsToMetresPerSecond; // TAS is in kts * 1000
		if (previousSpeed == -1 || !netThread.finishedLoading) {
			previousSpeed = speed;
		}
		//System.out.println("True Airspeed: " + String.format("%.1f", Double.valueOf((double) tas / 1000)) + " | m/s: " + String.format("%.1f", speed));

		double deceleration = Math.abs(speed - previousSpeed) / changeInTime; // m/s^2
		
		if(initialDeceleration == -1) {
			previousDeceleration = deceleration;
		}
		double changeInDeceleration = 0;
		if(decelerationCalculationCount > 5) {
			changeInDeceleration = Math.abs(deceleration - previousDeceleration) / changeInTime; // m/s^3
			if (onGround == 1 && changeInDeceleration > maxChangeInDeceleration && initialDeceleration != -1/* && tas < (maxExitSpeed * 1000) */) {
				maxChangeInDeceleration = changeInDeceleration;
			} 
		}
		
		if(initialDeceleration == -1) {
			initialDeceleration = deceleration;
		}

		/*
		System.out.println("ID:" + decelerationCalculationCount
				+ "\tTime: " + String.format("%.3f", changeInTime) 
				+ "\tSpeed = " + String.format("%.1f",Double.valueOf(speed)) + "m/s"
				+ "\t\tPrev Decel = " + String.format("%.3f", previousDeceleration)
				+ "\t\tDeceleration = " + String.format("%.3f", deceleration) 
				+ "\t\tChange = " + String.format("%.3f", changeInDeceleration) 
				+ "\t\tMax Change = " + String.format("%.3f", maxChangeInDeceleration));
		*/

		// Reset values for recalculation
		previousSpeed = speed;
		previousDeceleration = deceleration;
		// previousChangeInDeceleration = changeInDeceleration;
		previousTimeMillis = t;

		landingMaxDecel = getMaxDecelScore(maxChangeInDeceleration);

		if (onGround == 1) {
			lblDecelDeduct.setText(String.format("%.1f", landingMaxDecel));
		} else {
			lblDecelDeduct.setText("---");
		}
		return maxChangeInDeceleration;

	}

	static double getMaxDecelScore(double d) {						// Yet to be implemened in the main score
		//double rate = d;
		return 0;
		//return calculateScoreAbs(rate, 2, 10, 0, 0.1);
	}

	static double getCurrentOffset(float rwyLatitude, float rwyLongitude, double rwyHeadingDegrees,
			float aircraftLatitudeDegrees, float aircraftLongitudeDegrees) {
		// Font f = lblAvgOffsetData.getFont();
		// System.out.println("Offset");
		labels = PSXInfo.lblMainGearData.getFont();
		if (PSXInfo.tas < LandingCalc.maxExitSpeed || onGround == 0) {
			isAverageOffsetActive = false;
			// lblAvgOffsetData.setFont(labels);
			return averageOffset;
		}

		double aircraftLatitude = Math.toRadians(latitude);
		double aircraftLongitude = Math.toRadians(longitude);

		double latDistance = (PSXInfo.rwyLatitude - aircraftLatitude);
		double lonDistance = (PSXInfo.rwyLongitude - aircraftLongitude);

		// Haversine formula to work out distance from aircraft to runway using the great circle distance
		double a = Math.pow(Math.sin(latDistance / 2),2) + Math.cos(PSXInfo.rwyLatitude) * Math.cos(aircraftLatitude) * Math.pow(Math.sin(lonDistance / 2),2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		distanceToThreshold = RADIUS_OF_EARTH * c;

		if ((PSXInfo.rwyLatitude == 0 & PSXInfo.rwyLongitude == 0) || distanceToThreshold > getNauticalMilesToMetres(10)) {
			return 0;
		}

		rwyHeadingDegrees = rwyHeadingDegrees / 100;
		double bearing = angleFromCoordinate(PSXInfo.rwyLatitude, PSXInfo.rwyLongitude, aircraftLatitude,
				aircraftLongitude);

		double differenceInAngle = bearing - rwyHeadingDegrees; // Angle between the centreline and aircraft
		// System.out.println("Bearing: " + bearing + "\tRunway Heading: " +
		// rwyHeadingDegrees + "\tDiff: " + differenceInAngle);

		double distanceFromCentreline = Math.abs(Math.sin(Math.toRadians(differenceInAngle)) * distanceToThreshold);
		float distanceDownRunway = Math.round(Math.cos(Math.toRadians(differenceInAngle)) * distanceToThreshold);

		// Smooth out the noise
		double totalDistanceForAverage = distanceFromCentreline;
		for (int i = numberOfAveragesForSmoothing - 1; i > 0; i--) {
			smoothedDistancesFromCentreline[i] = smoothedDistancesFromCentreline[i - 1];
			if (i == 1)
				smoothedDistancesFromCentreline[0] = distanceFromCentreline;

			totalDistanceForAverage = totalDistanceForAverage + smoothedDistancesFromCentreline[i];
		}
		double smoothedOffset = totalDistanceForAverage / numberOfAveragesForSmoothing;
		// End of smoothing out the noise
		// Every ____ m

		currentSector = Math.round(distanceDownRunway / metresToCalculateAverage);

		if (currentSector >= 0 && currentSector < averageOffsetMeasurements.length) {
			averageOffsetMeasurements[currentSector] = smoothedOffset;
			// System.out.println(currentSector + " = " + smoothedOffset);
		}

		int count = 0;
		double totalOfMeasurements = 0;

		for (int i = 0; i < averageOffsetMeasurements.length; i++) {
			// System.out.println(i);
			totalOfMeasurements += averageOffsetMeasurements[i];
			count++;
			hasAverageMeasurementBeenTaken[i] = true;
		}

		averageOffset = (int) Math.round(totalOfMeasurements / (count > 0 ? count : 1));

		// Set the label to bold if "recording"
		/*
		 * if(currentSector >= 0 ) { Font f = lblAvgOffsetData.getFont();
		 * lblAvgOffsetData.setFont(f.deriveFont(f.getStyle() | Font.BOLD));
		 * isAverageOffsetActive = true; } else { lblAvgOffsetData.setFont(labels); }
		 */

		landingAvgOffset = getAverageOffsetScore(averageOffset);
		if (onGround == 1) {
			lblAvgOffsetDeduct.setText(String.format("%.1f", landingAvgOffset));
		} else {
			if (prevLandingScore == -1)
				lblAvgOffsetDeduct.setText("---");
		}
		// System.out.println("Total Measurements: " + String.format("%.3f",
		// totalOfMeasurements) + "\tCount: " + count + "\tAverage: " +
		// String.format("%.3f", averageOffset) + "\tCurrent Offset: " +
		// String.format("%.3f", smoothedOffset));
		// System.out.println("Centreline Offset: " + distanceFromCentreline + " Total
		// Average: " + totalDistanceForAverage + " Smoothed Offset: " +
		// smoothedOffset);
		isValidOffset = true;
		// calculateScore();
		return averageOffset;

	}

	private static double angleFromCoordinate(double lat1, double long1, double lat2, double long2) {

		// double dLat = lat2 - lat1; // Not used
		double dLon = long2 - long1;

		double y = Math.sin(dLon) * Math.cos(lat2);
		double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);

		double brng = Math.atan2(y, x);

		brng = (brng * 180 / Math.PI + 360) % 360;

		return brng;
	}

	static double getAverageOffsetScore(int i) {
		// Offset based on feet
		return 0;
		//return calculateScoreAbs(rate, 0.318, 1.1, 9, 0);
	}

	static double getNoseTransitionScore(double i) {
		// i is in seconds x 10
		double rate = i;
		return calculateScoreAbs(rate, 3, 1, 6, 3);
	}

	static void calculateScore(boolean updateTouchdownData) {		// Calculate final score
		if (updateTouchdownData) {
			landingScoreTouchdown = PSXInfo.landingNoseGear + PSXInfo.landingMainGear + PSXInfo.landingOffset
					+ PSXInfo.landingDistance + PSXInfo.landingPitch + PSXInfo.landingBank + PSXInfo.landingCrab
					+ PSXInfo.landingSpeed + PSXInfo.landingNoseTransitionTime;

			// System.out.println("Resetting has last landing been printed");
			PSXInfoNetThread.hasLastLandingBeenPrinted = false;
		}

		landingScoreContinual = landingAvgOffset + landingMaxDecel;
		landingScore = 100 - (landingScoreTouchdown + landingScoreContinual) - (PSXInfoNetThread.autopilotAssist ? PSXInfoNetThread.autopilotAssistPenalty : 0);

		// Alter main Landing Score label
		if (PSXInfoNetThread.noseGear != 0) {

			String strScore = "Landing Score: ---%";
			String eicasScore = "(SCORE: 0.0)";
			Color color = Color.black;
			if (PSXInfo.landingScore < -100) {
				PSXInfo.landingScore = 0;
			} else if (PSXInfo.landingScore > 100 || PSXInfo.landingScore < 0) {
				PSXInfo.landingScore = 0.01;
			}

			if (updateTouchdownData) {
				try {
					FileWriter fw = new FileWriter(PSXInfoNetThread.rawTouchdownLocalFile, true);
					fw.write(PSXInfoNetThread.timestamp + "\t\tLanding Score: "
							+ String.format("%.2f", PSXInfo.landingScore)
							+ "%\t\tNose Time: " + PSXInfoNetThread.noseTransitionTime + " (" + String.format("%.2f", PSXInfo.landingNoseTransitionTime) + ")"
							+ "Cause: " + PSXInfoNetThread.cause
							+ (PSXInfoNetThread.autopilotAssist ? "\tAUTOPILOT ASSISTED" : "")
							+ (PSXInfoNetThread.bounced ? "\tBOUNCED" : "")
							+ (PSXInfoNetThread.noseCollapse ? "\tNOSE COLLAPSED" : "")
							+ (PSXInfoNetThread.wingStrike ? "\tWING STRIKE" : "")
							+ (PSXInfoNetThread.tailStrike ? "\tTAIL STRIKE" : "") + "\n");
					fw.close();
				} catch (IOException ioe) {
					System.err.println("IOException: " + ioe.getMessage());
				}
			}

			switch (PSXInfoNetThread.cause) {
			case 0:
				if (PSXInfoNetThread.autopilotAssist) {
					strScore = String.format("Autopilot Assisted: %.2f%n%%", PSXInfo.landingScore);
					color = Color.MAGENTA;
					netThread.send(String.format("Qs421=(SCORE: %.2f)", PSXInfo.landingScore));
					PSXInfoNetThread.FreeMsg[3] = "SCORE:";
				} else {
					strScore = String.format("Landing Score: %.2f%n%%", PSXInfo.landingScore);
					color = Color.black;
					// System.out.println("Sending score 2");
					netThread.send(String.format("Qs421=(SCORE: %.2f)", PSXInfo.landingScore));
					PSXInfoNetThread.FreeMsg[3] = "SCORE:";
				}
				PSXInfo.prevLandingScore = PSXInfo.landingScore;
				break;

			case 1:
				strScore = "BOUNCED: 0.0%";
				color = Color.RED;
				netThread.send("Qs421=" + eicasScore);
				PSXInfo.prevLandingScore = 0;
				break;
			case 2:
				strScore = "WING STRIKE: 0.0%";
				color = Color.RED;
				netThread.send("Qs421=" + eicasScore);
				PSXInfo.prevLandingScore = 0;
				break;
			case 3:
				strScore = "TAIL STRIKE: 0.0%";
				color = Color.RED;
				netThread.send("Qs421=" + eicasScore);
				PSXInfo.prevLandingScore = 0;
				break;
			case 4:
				strScore = "NOSE COLLAPSE: 0.0%";
				color = Color.RED;
				netThread.send("Qs421=" + eicasScore);
				PSXInfo.prevLandingScore = 0;
				break;
			case 5:
				strScore = "PILOT LICENCE REVOKED";
				PSXInfoNetThread.cause = 6;
				color = Color.RED;
				netThread.send("Qs421=" + eicasScore);
				netThread.send("Qs418=LICENCE REVOKED");
				PSXInfo.prevLandingScore = 0;
				break;
			case 6:
				strScore = "PILOT LICENCE REVOKED";
				color = Color.RED;
				netThread.send("Qs421=" + eicasScore);
				netThread.send("Qs418=LICENCE REVOKED");
				PSXInfo.prevLandingScore = 0;
				break;
			}

			PSXInfo.lbl_Landing.setForeground(color);
			PSXInfo.setLabel_Ldg(strScore);

			PSXInfoNetThread.readyToPrint = true;

			if (updateTouchdownData) {
				if ((PSXInfoNetThread.mainGear > LICENCE_REVOKED_TOUCHDOWN_RATE || PSXInfoNetThread.bouncedMainGear > LICENCE_REVOKED_TOUCHDOWN_RATE)
						&& PSXInfoNetThread.cause != 6) {
					// String str = "PILOT LICENCE REVOKED";
					// PSXInfo.setLabel_Ldg(str);
					// PSXInfo.lbl_Landing.setForeground(Color.RED);
					PSXInfoNetThread.cause = 5;

					// Record pilot licence revoked to touch down file
					try {
						FileWriter fw = new FileWriter(PSXInfoNetThread.rawTouchdownLocalFile, true);
						fw.write(PSXInfoNetThread.timestamp + "\t\t" + PSXInfo.origin + " - " + PSXInfo.destination
								+ "\t<<<!!!-------- PILOT LICENCE REVOKED --------!!!>>>" + "\n");
						fw.close();
					} catch (IOException ioe) {
						System.err.println("IOException: " + ioe.getMessage());
					}
				}
			}

			// -------- Set labels --------------//
			//if (updateTouchdownData) {
				if (PSXInfoNetThread.mainGear > 0) {
					if (!PSXInfoNetThread.bounced) {
						PSXInfo.lblMainGearData.setText(PSXInfoNetThread.mainGear + " fpm");
						PSXInfo.lblMainGearDeduct.setText(String.format("%.1f", PSXInfo.landingMainGear));
					} else {
						PSXInfo.lblMainGearData.setText(PSXInfoNetThread.bouncedMainGear + " fpm");
						PSXInfo.lblMainGearDeduct.setText(
								String.format("%.1f", LandingCalc.getMainGearScore(PSXInfoNetThread.bouncedMainGear)));
					}
				}

				//if (PSXInfoNetThread.noseGear > 0) {
					PSXInfo.lblNoseGearData.setText(String.valueOf(PSXInfoNetThread.noseGear) + " fpm");
					PSXInfo.lblNoseGearDeduct.setText(String.format("%.1f", PSXInfo.landingNoseGear));
				//}

				PSXInfo.lblOffsetData.setText(String.format("%.1f", PSXInfoNetThread.offset) + " m");
				if(scoreOffsetAndThreshold) {
					PSXInfo.lblOffsetDeduct.setText(String.format("%.1f", PSXInfo.landingOffset));
				}
				PSXInfo.lblDistanceData.setText(PSXInfoNetThread.distance + " ft");
				if(scoreOffsetAndThreshold) {
					PSXInfo.lblDistanceDeduct.setText(String.format("%.1f", PSXInfo.landingDistance));
				}
				PSXInfo.lblPitchData.setText(String.format("%.1f", PSXInfoNetThread.pitch) + " \u00B0");
				PSXInfo.lblPitchDeduct.setText(String.format("%.1f", PSXInfo.landingPitch));
				PSXInfo.lblBankData.setText(String.format("%.1f", PSXInfoNetThread.bank) + " \u00B0");
				PSXInfo.lblBankDeduct.setText(String.format("%.1f", PSXInfo.landingBank));
				PSXInfo.lblCrabData.setText(String.format("%.1f", PSXInfoNetThread.crab) + " \u00B0");
				PSXInfo.lblCrabDeduct.setText(String.format("%.1f", PSXInfo.landingCrab));
				PSXInfo.lblSpeedData.setText(PSXInfoNetThread.speed + " kts");
				PSXInfo.lblSpeedDeduct.setText(String.format("%.1f", PSXInfo.landingSpeed));
			//}
			LandingCalc.updateLandingColours();
			// ----------- END OF SET LABELS ----------//

			// ---------- SEND TO LOG FILE --------//
			if (updateTouchdownData) {
				try {
					FileWriter fw = new FileWriter(PSXInfoNetThread.rawTouchdownLocalFile, true);
					fw.write("Deductions\n");
					String deductions = "Nose: " + String.format("%.2f", PSXInfo.landingNoseGear) + ";" + 
										"Main = " + String.format("%.2f", PSXInfo.landingMainGear) + ";" +
										"Offset = " + (scoreOffsetAndThreshold ? String.format("%.2f", PSXInfo.landingOffset) : "N/A") + ";" +
										"Distance = " + (scoreOffsetAndThreshold ? String.format("%.2f", PSXInfo.landingDistance) : "N/A") + ";" +
										"Pitch = " + String.format("%.2f", PSXInfo.landingPitch) + ";" +
										"Bank = " + String.format("%.2f", PSXInfo.landingPitch) + ";" +
										"Crab = " + String.format("%.2f", PSXInfo.landingCrab) + ";" +
										"Speed = " + String.format("%.2f", PSXInfo.landingSpeed) + ";" +
										"Nose Transition = " + String.format("%.2f", PSXInfo.landingNoseTransitionTime) + ";";
					fw.write(deductions);
					String expertMode = "\nExpert Mode = " + (scoreOffsetAndThreshold ? "ACTIVATED" : "DEACTIVATED");
					fw.write(expertMode);
					 fw.write("\n\n-------------------------\n");
					fw.close();
				} catch (IOException ioe) {
					System.err.println("IOException: " + ioe.getMessage());
				}
			}
			// ------ END OF SEND TO LOG FILE --------//
		}
		
		if(updateTouchdownData && PSXInfo.landingScoreTouchdown > 0) {
			//System.out.println("Landing score = " + PSXInfo.landingScore);
			
			try {
				File file = new File("LandingScore.txt");
				FileWriter fw = new FileWriter(file, false);
				fw.write(String.format("%.2f", PSXInfo.landingScore) + "%");
				fw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		PSXInfo.landingPrevNoseGear = PSXInfoNetThread.noseGear;
		
	}

	// UPDATE LABELS
	static void updateLandingColours() {
		if (PSXInfo.landingMainGear >= PSXInfo.highlight) {
			PSXInfo.lblMainGearData.setFont(new Font(labelFont.getName(), Font.BOLD, labelFont.getSize()));
			PSXInfo.lblMainGearDeduct.setFont(new Font(labelFont.getName(), Font.BOLD, labelFont.getSize()));
			PSXInfo.lblMainGearData.setForeground(Color.RED);
			PSXInfo.lblMainGearDeduct.setForeground(Color.RED);
		} else {
			PSXInfo.lblMainGearData.setFont(new Font(labelFont.getName(), Font.PLAIN, labelFont.getSize()));
			PSXInfo.lblMainGearDeduct.setFont(new Font(labelFont.getName(), Font.PLAIN, labelFont.getSize()));
			PSXInfo.lblMainGearData.setForeground(Color.BLACK);
			PSXInfo.lblMainGearDeduct.setForeground(Color.BLACK);
		}

		if (PSXInfo.landingNoseGear >= PSXInfo.highlight || PSXInfoNetThread.noseGear < 0) {
			PSXInfo.lblNoseGearData.setFont(new Font(labelFont.getName(), Font.BOLD, labelFont.getSize()));
			PSXInfo.lblNoseGearDeduct.setFont(new Font(labelFont.getName(), Font.BOLD, labelFont.getSize()));
			PSXInfo.lblNoseGearData.setForeground(Color.RED);
			PSXInfo.lblNoseGearDeduct.setForeground(Color.RED);
		} else {
			PSXInfo.lblNoseGearData.setFont(new Font(labelFont.getName(), Font.PLAIN, labelFont.getSize()));
			PSXInfo.lblNoseGearDeduct.setFont(new Font(labelFont.getName(), Font.PLAIN, labelFont.getSize()));
			PSXInfo.lblNoseGearData.setForeground(Color.BLACK);
			PSXInfo.lblNoseGearDeduct.setForeground(Color.BLACK);
		}

		if(scoreOffsetAndThreshold && (PSXInfo.landingOffset >= PSXInfo.highlight || PSXInfo.landingDistance >= PSXInfo.highlight)) {
			if (PSXInfo.landingOffset >= PSXInfo.highlight) {
				PSXInfo.lblOffsetData.setFont(new Font(labelFont.getName(), Font.BOLD, labelFont.getSize()));
				PSXInfo.lblOffsetDeduct.setFont(new Font(labelFont.getName(), Font.BOLD, labelFont.getSize()));
				PSXInfo.lblOffsetData.setForeground(Color.RED);
				PSXInfo.lblOffsetDeduct.setForeground(Color.RED);
			}
			
			if (PSXInfo.landingDistance >= PSXInfo.highlight) {
				PSXInfo.lblDistanceData.setFont(new Font(labelFont.getName(), Font.BOLD, labelFont.getSize()));
				PSXInfo.lblDistanceDeduct.setFont(new Font(labelFont.getName(), Font.BOLD, labelFont.getSize()));
				PSXInfo.lblDistanceData.setForeground(Color.RED);
				PSXInfo.lblDistanceDeduct.setForeground(Color.RED);
			}
		} else {
			PSXInfo.lblOffsetData.setFont(new Font(labelFont.getName(), Font.PLAIN, labelFont.getSize()));
			PSXInfo.lblOffsetDeduct.setFont(new Font(labelFont.getName(), Font.PLAIN, labelFont.getSize()));
			PSXInfo.lblOffsetData.setForeground(Color.BLACK);
			PSXInfo.lblOffsetDeduct.setForeground(Color.BLACK);
			
			PSXInfo.lblDistanceData.setFont(new Font(labelFont.getName(), Font.PLAIN, labelFont.getSize()));
			PSXInfo.lblDistanceDeduct.setFont(new Font(labelFont.getName(), Font.PLAIN, labelFont.getSize()));
			PSXInfo.lblDistanceData.setForeground(Color.BLACK);
			PSXInfo.lblDistanceDeduct.setForeground(Color.BLACK);
		}

		if (PSXInfo.landingPitch >= PSXInfo.highlight) {
			PSXInfo.lblPitchData.setFont(new Font(labelFont.getName(), Font.BOLD, labelFont.getSize()));
			PSXInfo.lblPitchDeduct.setFont(new Font(labelFont.getName(), Font.BOLD, labelFont.getSize()));
			PSXInfo.lblPitchData.setForeground(Color.RED);
			PSXInfo.lblPitchDeduct.setForeground(Color.RED);
		} else {
			PSXInfo.lblPitchData.setFont(new Font(labelFont.getName(), Font.PLAIN, labelFont.getSize()));
			PSXInfo.lblPitchDeduct.setFont(new Font(labelFont.getName(), Font.PLAIN, labelFont.getSize()));
			PSXInfo.lblPitchData.setForeground(Color.BLACK);
			PSXInfo.lblPitchDeduct.setForeground(Color.BLACK);
		}

		if (PSXInfo.landingBank >= PSXInfo.highlight) {
			PSXInfo.lblBankData.setFont(new Font(labelFont.getName(), Font.BOLD, labelFont.getSize()));
			PSXInfo.lblBankDeduct.setFont(new Font(labelFont.getName(), Font.BOLD, labelFont.getSize()));
			PSXInfo.lblBankData.setForeground(Color.RED);
			PSXInfo.lblBankDeduct.setForeground(Color.RED);
		} else {
			PSXInfo.lblBankData.setFont(new Font(labelFont.getName(), Font.PLAIN, labelFont.getSize()));
			PSXInfo.lblBankDeduct.setFont(new Font(labelFont.getName(), Font.PLAIN, labelFont.getSize()));
			PSXInfo.lblBankData.setForeground(Color.BLACK);
			PSXInfo.lblBankDeduct.setForeground(Color.BLACK);
		}

		if (PSXInfo.landingCrab >= PSXInfo.highlight) {
			PSXInfo.lblCrabData.setFont(new Font(labelFont.getName(), Font.BOLD, labelFont.getSize()));
			PSXInfo.lblCrabDeduct.setFont(new Font(labelFont.getName(), Font.BOLD, labelFont.getSize()));
			PSXInfo.lblCrabData.setForeground(Color.RED);
			PSXInfo.lblCrabDeduct.setForeground(Color.RED);
		} else {
			PSXInfo.lblCrabData.setFont(new Font(labelFont.getName(), Font.PLAIN, labelFont.getSize()));
			PSXInfo.lblCrabDeduct.setFont(new Font(labelFont.getName(), Font.PLAIN, labelFont.getSize()));
			PSXInfo.lblCrabData.setForeground(Color.BLACK);
			PSXInfo.lblCrabDeduct.setForeground(Color.BLACK);
		}

		if (PSXInfo.landingSpeed >= PSXInfo.highlight) {
			PSXInfo.lblSpeedData.setFont(new Font(labelFont.getName(), Font.BOLD, labelFont.getSize()));
			PSXInfo.lblSpeedDeduct.setFont(new Font(labelFont.getName(), Font.BOLD, labelFont.getSize()));
			PSXInfo.lblSpeedData.setForeground(Color.RED);
			PSXInfo.lblSpeedDeduct.setForeground(Color.RED);
		} else {

			PSXInfo.lblSpeedData.setFont(new Font(labelFont.getName(), Font.PLAIN, labelFont.getSize()));
			PSXInfo.lblSpeedDeduct.setFont(new Font(labelFont.getName(), Font.PLAIN, labelFont.getSize()));
			PSXInfo.lblSpeedData.setForeground(Color.BLACK);
			PSXInfo.lblSpeedDeduct.setForeground(Color.BLACK);
		}

		if (PSXInfo.landingDecelleration >= PSXInfo.highlight) {
			PSXInfo.lblDecelData.setFont(new Font(labelFont.getName(), Font.BOLD, labelFont.getSize()));
			PSXInfo.lblDecelDeduct.setFont(new Font(labelFont.getName(), Font.BOLD, labelFont.getSize()));
			PSXInfo.lblDecelData.setForeground(Color.RED);
			PSXInfo.lblDecelDeduct.setForeground(Color.RED);
		} else {

			PSXInfo.lblDecelData.setFont(new Font(labelFont.getName(), Font.PLAIN, labelFont.getSize()));
			PSXInfo.lblDecelDeduct.setFont(new Font(labelFont.getName(), Font.PLAIN, labelFont.getSize()));
			PSXInfo.lblDecelData.setForeground(Color.BLACK);
			PSXInfo.lblDecelDeduct.setForeground(Color.BLACK);
		}

		if (PSXInfo.landingAvgOffset >= PSXInfo.highlight) {
			PSXInfo.lblAvgOffsetData.setFont(new Font(labelFont.getName(), Font.BOLD, labelFont.getSize()));
			PSXInfo.lblAvgOffsetDeduct.setFont(new Font(labelFont.getName(), Font.BOLD, labelFont.getSize()));
			PSXInfo.lblAvgOffsetData.setForeground(Color.RED);
			PSXInfo.lblAvgOffsetDeduct.setForeground(Color.RED);
		} else {

			PSXInfo.lblAvgOffsetData.setFont(new Font(labelFont.getName(), Font.PLAIN, labelFont.getSize()));
			PSXInfo.lblAvgOffsetDeduct.setFont(new Font(labelFont.getName(), Font.PLAIN, labelFont.getSize()));
			PSXInfo.lblAvgOffsetData.setForeground(Color.BLACK);
			PSXInfo.lblAvgOffsetDeduct.setForeground(Color.BLACK);
		}

		if (PSXInfo.landingNoseTransitionTime >= PSXInfo.highlight) {
			PSXInfo.lblNoseTransitionTimeData.setFont(new Font(labelFont.getName(), Font.BOLD, labelFont.getSize()));
			PSXInfo.lblNoseTransitionTimeDeduct.setFont(new Font(labelFont.getName(), Font.BOLD, labelFont.getSize()));
			PSXInfo.lblNoseTransitionTimeData.setForeground(Color.RED);
			PSXInfo.lblNoseTransitionTimeDeduct.setForeground(Color.RED);
		} else {

			PSXInfo.lblNoseTransitionTimeData.setFont(new Font(labelFont.getName(), Font.PLAIN, labelFont.getSize()));
			PSXInfo.lblNoseTransitionTimeDeduct.setFont(new Font(labelFont.getName(), Font.PLAIN, labelFont.getSize()));
			PSXInfo.lblNoseTransitionTimeData.setForeground(Color.BLACK);
			PSXInfo.lblNoseTransitionTimeDeduct.setForeground(Color.BLACK);
		}

	}

	static void setScoreOffsetThreshold(boolean b) {
		scoreOffsetAndThreshold = b;
	}
	
	static int getNauticalMilesToMetres(double miles) {
		int metres = (int) Math.round(miles * 1852);
		return metres;
	}

}
