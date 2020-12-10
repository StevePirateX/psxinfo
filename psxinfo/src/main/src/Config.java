package src;

import java.awt.Point;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

public class Config extends PSXInfo {

	// Window Position
	public static String getPosition() {
		Point p = frmPsxinfo.getLocationOnScreen();
		windowPosition[0] = (int) p.getX();
		windowPosition[1] = (int) p.getY();
		return windowPosition[0] + ";" + windowPosition[1];
	}

	// Save and load to the properties file

	public static void loadProperties() throws IOException {
		Properties defaultProps = new Properties();
		defaultProps.setProperty("psxhost", "localhost");
		defaultProps.setProperty("psxport", "10747");
		defaultProps.setProperty("psxAutoConnect", "false");
		defaultProps.setProperty("minimise", "false");
		defaultProps.setProperty("autoReconnect", "true");
		defaultProps.setProperty("printEnabled", "false");
		defaultProps.setProperty("windowPosition", windowPosition[0] + ";" + windowPosition[1]);
		defaultProps.setProperty("scoreOffsetAndThreshold", "false");
		
		configProps = new Properties(defaultProps);
		InputStream inputStream = new FileInputStream(configFile);

		configProps.load(inputStream);
		inputStream.close();

		LandingCalc.setScoreOffsetThreshold(Boolean.parseBoolean(configProps.getProperty("scoreOffsetAndThreshold")));
	}

	public static void saveProperties() {
		try {
			configProps.setProperty("psxhost", psxhost);
			configProps.setProperty("psxport", txt_HostPort.getText());
			configProps.setProperty("psxAutoConnect", String.valueOf(Options.cbxAutoConnect.isSelected()));
			configProps.setProperty("minimise", String.valueOf(Options.cbxMinimise.isSelected()));
			configProps.setProperty("autoReconnect", String.valueOf(Options.cbxAutoReconnect.isSelected()));
			configProps.setProperty("printEnabled", String.valueOf(Options.cbxPrintEnabled.isSelected()));
			configProps.setProperty("windowPosition", getPosition());
			configProps.setProperty("scoreOffsetAndThreshold", String.valueOf(Options.cbxOffsetThreshold.isSelected()));
			
			File f = new File(configFileName);
			OutputStream out = new FileOutputStream(f);
			configProps.store(out, "PSX Info Config File");
			out.close();
			System.out.println("Config saved");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void getProperties() {

	}

}
