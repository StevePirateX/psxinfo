package src;

public class NoseTransitionTime extends Thread {
	protected volatile boolean exit = false;
	long timeDifference;
	
	
	
	public void run() {
		while(!exit) {
			// System.out.println("Nose Transition calculating");
			
			if(LandingCalc.noseGearLandingTime == 0 && LandingCalc.mainGearLandingTime > 0) {
				timeDifference = System.currentTimeMillis() - LandingCalc.mainGearLandingTime;
			}else {
				timeDifference = LandingCalc.noseGearLandingTime - LandingCalc.mainGearLandingTime;
			}
		
			//System.out.println("Nose Time: " + LandingCalc.noseGearLandingTime + "\tMain Time: " + LandingCalc.mainGearLandingTime + " \tTime Difference: " + timeDifference);
			double time = Double.valueOf(timeDifference);
			time = time / 1000.0;
			
			// Set Data Label
			String s = String.format("%.1f", time);
			PSXInfoNetThread.noseTransitionTime = time;
			PSXInfo.lblNoseTransitionTimeData.setText(s + " s");
			
			// Set Deduction Label
			//double i = Double.valueOf(String.valueOf(time)); //Math.round(
			double noseTransDeduct = LandingCalc.getNoseTransitionScore(time);
			//System.out.println(noseTransDeduct);
			PSXInfo.landingNoseTransitionTime = noseTransDeduct;
			PSXInfo.lblNoseTransitionTimeDeduct.setText(
					String.format("%.1f", PSXInfo.landingNoseTransitionTime));

			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void exit(){
		//System.out.println("Stopping Nose Transition Thread");
        exit = true;
        LandingCalc.timer = null;
        LandingCalc.t = null;
    }
	
	
	
	
	
	
}
		