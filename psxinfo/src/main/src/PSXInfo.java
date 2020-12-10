package src;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.border.EtchedBorder;

public class PSXInfo {

	// --------VARIABLE DECLARATIONS--------//
	static String version = "1.5";
	static String build = "201210";
	

	static LandingCalc landingCalc; 														// Contains the calculation methods for scoring the landing
	static PSXInfoNetThread netThread; 														// Network PSX thread
	static Options options = new Options();													// Options window to define settings for application
	static int[] defaultWindowPosition = {300, 200};										// Default position of main window [x , y]
	static int[] windowPosition = {defaultWindowPosition[0], defaultWindowPosition[1]};		// Window position of main window [x, y]
	static String configFileName = "config.cfg";											// Filename of the configuration file
	static File configFile = new File(configFileName);										// Config file
	static String touchdownLogFileName = "TouchdownLog.txt";								// Keeps a log of all the touchdowns with times and deductions. Used to confirm the score
	static Properties configProps;															// Properties that store all the configuration of the application
	static boolean isConnected = false; 													// Variable to test if add-on is isConnected to PSX

	// UI & PSX Connection Variables
	static JFrame frmPsxinfo;
	private static JPanel statusPanel;
	private static JPanel mainPanel;
	private static JPanel landingScoreMetricPanel;
	private static JLabel lblRate;
	private static JMenuItem optionsMenuItem;
	static Font labels;
	static JButton btnConnect = new JButton("Connect");
	static JLabel lab_Conn = new JLabel("DISCONNECTED");
	static JLabel lab_Lat = new JLabel("Latitude: --- (°)");
	static JLabel lab_Lon = new JLabel("Longitude: --- (°)");
	static JLabel lab_Alt = new JLabel("Altitude: --- (ft)");
	static JLabel lab_Hdg = new JLabel("Heading: --- (°)");
	static JLabel lab_Ias = new JLabel("IAS: --- (kts)");
	static JLabel lab_Wnd = new JLabel("Wind: --- ° / --- kts");
	static JLabel lab_Eta = new JLabel("ETA: ---- z");
	static JLabel lab_Dep = new JLabel("Origin: ---- / ---");
	static JLabel lab_Arr = new JLabel("Destination: ---- / ---");
	static JLabel lbl_Landing = new JLabel("Landing Score: ---");
	static JLabel lbl_Serviceable = new JLabel("SERVICEABLE");

	static JLabel lblMainGearDeduct;
	static JLabel lblMainGearData;
	static JLabel lblNoseGearData;
	static JLabel lblNoseGearDeduct;
	static JLabel lblOffsetData;
	static JLabel lblOffsetDeduct;
	static JLabel lblDistanceData;
	static JLabel lblDistanceDeduct;
	static JLabel lblPitchData;
	static JLabel lblPitchDeduct;
	static JLabel lblBankData;
	static JLabel lblBankDeduct;
	static JLabel lblCrabData;
	static JLabel lblCrabDeduct;
	static JLabel lblSpeedData;
	static JLabel lblSpeedDeduct;
	static JLabel lblDecel;
	static JLabel lblDecelData;
	static JLabel lblDecelDeduct;
	static JLabel lblAvgOffset;
	static JLabel lblAvgOffsetData;
	static JLabel lblAvgOffsetDeduct;
	static JLabel lblNoseTransitionTime;
	static JLabel lblNoseTransitionTimeData;
	static JLabel lblNoseTransitionTimeDeduct;
	static JLabel lblEmptyLandingScore;

	static boolean autoConnect; 															// Automatically connect to PSX on start up
	static boolean minimise; 																// Automatically minimise on start up
	static boolean autoReconnect;
	static boolean autoConnecting = false;
	static boolean printLanding; 															// Print landing data on touchdown to PSX printer
	static int highlight = 10; 																// This is the score of which, if above, the deduction turns red

	// Defaults when disconnected
	static String lab_LatDefault = lab_Lat.getText();
	static String lab_LonDefault = lab_Lon.getText();
	static String lab_AltDefault = lab_Alt.getText();
	static String lab_HdgDefault = lab_Hdg.getText();
	static String lab_IasDefault = lab_Ias.getText();
	static String lab_WndDefault = lab_Wnd.getText();
	static String lab_EtaDefault = lab_Eta.getText();
	static String lab_DepRwyDefault = lab_Dep.getText();
	static String lab_ArrRwyDefault = lab_Arr.getText();

	// Server Settings
	static final boolean TO_SERVER = true, FROM_SERVER = !TO_SERVER;
	static String psxhost;
	static int psxport;

	// PSX Variables
	// Check Variables.txt in PSX installation for Q codes re LcduTitle, LcduLine1s, LcduLine1b etc.:
	static int qsPiBaHeAlTas = 121; 														// PSX position altitude etc
	static int qsFuelQty = 438; 															// PSX FuelQty
	static int qsFreqsAntenna = 112; 														// PSX Radio frequencies
	static int qsActiveRoute = 373; 														// PSX Active route (route 1, route 2 or no route)
	static int qsRteData = 376; 															// PSX Route Data (e.g. departure airport, arrival airport, runway etc)
	static int StartPiBaHeAlVsTasYw = 122; 													// Aircraft position at the start of situation or when manually changed
	static int FmcDepRwys1 = 376; 															// Departure runway route 1
	static int FmcDepRwys2 = 383;															// Departure runway route 2
	static int qiActDestEta = 247;															// ETA of destination
	static int MiscFltData = 483;															// Miscellaneous flight data
	static int qiAcftHeight = 219; 															// Aircraft height above GROUND

	// Current status of the aircraft
	static int acftHeight = 0;
	static int activeRoute;
	static int altitude;
	static String arrRwy = "---";
	static int avgEgt;
	static float avgN1;
	static double comm1 = 199.998;
	static double comm2 = 199.998;
	static int destEta;
	static String depRwy = "---";
	static String destination = "----";
	static String eta = "----";
	static float flightHdg; 																// Displayed heading on webpage - what is set and autopilot references
	static double fuelQty = 0;
	static float ias;
	static float latitude;
	static float longitude;
	static float magHdg;
	static int magtruehdg; 																	// 0 if set to mag/norm hdg or 1 if true heading
	static float magVar = 0f;
	static float oat;
	static int onGround;
	static String origin = "----";
	static float prevlatitude = 0;
	static float prevlongitude = 0;
	static String route = "";
	static int rwyHeadingDegrees = -1;
	static float rwyLatitude = 0f;
	static float rwyLongitude = 0f;
	static float tas;
	static float trueHdg;
	static int update = 0;
	static int updateOccur = 0; 															// 1 if update occurs
	static int updateStart = 0; 															// So PSXInfo looks for the next update and doesn't include the current one
	static int windDir = 0;
	static int windSpd = 0;
	
	// Touchdown Variables
	static double landingPrevMainGear = -1;
	static double landingPrevNoseGear = -1;
	static double landingMainGear = 0;
	static double landingNoseGear = 0;
	static double landingOffset = 0;
	static double landingDistance = 0;
	static double landingPitch = 0;
	static double landingBank = 0;
	static double landingCrab = 0;
	static double landingSpeed = 0;
	static double landingDecelleration = 0;
	static double landingMainNoseTime = 0;
	static double landingNoseTransitionTime = 0;
	static double landingMaxDecel = 0;
	static double landingAvgOffset = 0;
	static double landingScoreTouchdown = 0; 												// Initial touchdown parameters
	static double landingScoreContinual = 0; 												// After the touchdown
	static double landingScore = -1;
	static double prevLandingScore = -1; 													// Landing score value that goes on the website - this isn't updated when the

	// Landing score settings
	static int noseTransitionTime = 6; 														// seconds
	static int maxTaxiSpeed = 20; 															// knots
	static int maxExitSpeed = 60; 															// High speed exit in kts
	/* 	
	 * Referenced by Jeppesen - High Speed Taxi-way / Turnoff in the glossary - page
	 * 12 (pdf page 24)
	 * http://ww1.jeppesen.com/documents/aviation/business/ifr-paper-services/glossary-legends.pdf
	*/
	
	static JTextField txt_HostIP; 															// Text field to define PSX host IP
	static JTextField txt_HostPort; 														// Text field to define PSX host port
	static boolean printEnabled = false;													// Will PSXInfo print to PSX's printer

	public static SwingWorker<Object, String> swingWorker = null;

	public static void aircraftService(boolean log) {
		PSXInfo.setLabel_Service(true);														// Reset the servicable label
		PSXInfoNetThread.bounced = false;													// Aircraft is no longer defined as had bounced
		PSXInfoNetThread.hasAlreadyLandedMain = false;										// Main gear no longer considered as "landed"
		PSXInfoNetThread.wingStrike = false;												// No wing strike
		PSXInfoNetThread.tailStrike = false;												// No tail strike

		sendToServer("Qs144=0;6;-99"); 														// Nose Gear
		sendToServer("Qs217=0;6;-99"); 														// Pod strike 1
		sendToServer("Qs218=0;6;-99"); 														// Pod strike 2
		sendToServer("Qs219=0;6;-99"); 														// Pod strike 3
		sendToServer("Qs220=0;6;-99"); 														// Pod strike 4
		sendToServer("Qi130=1"); 															// Tell PSX that some malfunction details have changed
		PSXInfoNetThread.noseCollapse = false;												// Has the nose collapsed										
		PSXInfoNetThread.cause = 0; 														// Aircraft serviceable and licence unrevoked
		sendToServer("Qs418="); 															// Clear custom LWR EICAS Messages
		sendToServer("Qs422="); 															// Clear custom LWR EICAS Messages
		lbl_Landing.setForeground(Color.BLACK);												// Return the landing score label to normal colour

		if (log) {
			File BouncedLocalFile = new File(PSXInfo.touchdownLogFileName);
			Timestamp timestamp = new Timestamp(System.currentTimeMillis());
			try {
				FileWriter fw = new FileWriter(BouncedLocalFile, true);
				fw.write(timestamp + "\t\t--- AIRCRAFT SERVICED --- AIRCRAFT SERVICED ---\n");
				fw.close();
			} catch (IOException ioe) {
				System.err.println("IOException: " + ioe.getMessage());
			}
		}
	}

	static void updatetimes() {
		JLabel lblFlightStatus = new JLabel("Current Status");
		lblFlightStatus.setFont(new Font("Tahoma", Font.BOLD, 16));
		lblFlightStatus.setHorizontalAlignment(SwingConstants.CENTER);
		GroupLayout gl_statusPanel = new GroupLayout(statusPanel);
		gl_statusPanel.setHorizontalGroup(gl_statusPanel.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_statusPanel.createSequentialGroup().addContainerGap()
						.addGroup(gl_statusPanel.createParallelGroup(Alignment.LEADING)
								.addComponent(lblFlightStatus, GroupLayout.DEFAULT_SIZE, 185, Short.MAX_VALUE)
								.addComponent(lab_Wnd).addComponent(lab_Eta).addComponent(lab_Arr).addComponent(lab_Dep)
								.addComponent(lab_Ias).addComponent(lab_Hdg).addComponent(lab_Alt).addComponent(lab_Lon)
								.addComponent(lab_Lat, GroupLayout.PREFERRED_SIZE, 122, GroupLayout.PREFERRED_SIZE))
						.addContainerGap()));
		gl_statusPanel.setVerticalGroup(gl_statusPanel.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_statusPanel.createSequentialGroup().addContainerGap().addComponent(lblFlightStatus)
						.addGap(9).addComponent(lab_Lat).addPreferredGap(ComponentPlacement.RELATED)
						.addComponent(lab_Lon).addPreferredGap(ComponentPlacement.RELATED).addComponent(lab_Alt)
						.addPreferredGap(ComponentPlacement.RELATED).addComponent(lab_Hdg)
						.addPreferredGap(ComponentPlacement.RELATED).addComponent(lab_Ias)
						.addPreferredGap(ComponentPlacement.RELATED).addComponent(lab_Dep)
						.addPreferredGap(ComponentPlacement.RELATED).addComponent(lab_Arr)
						.addPreferredGap(ComponentPlacement.RELATED).addComponent(lab_Eta)
						.addPreferredGap(ComponentPlacement.RELATED).addComponent(lab_Wnd).addContainerGap()));
		statusPanel.setLayout(gl_statusPanel);
	}


	public static void connect() {
		swingWorker = new SwingWorker<Object, String>() {
			protected Object doInBackground() throws Exception {
				try {
					PSXInfo.setBtnConnectText("Disconnect");
					// System.out.println("Button Pressed, autoConnecting: " + autoConnecting);
					psxhost = txt_HostIP.getText();
					psxport = Integer.parseInt(txt_HostPort.getText());

					if (!isConnected) {
						netThread = new PSXInfoNetThread(psxhost, psxport);
						netThread.start();
						TimeUnit.MILLISECONDS.sleep(250);
					} else if (netThread != null) {
						netThread.finalJobs();
					}
				} catch (Exception e) {
					if (!autoConnecting) {
						PSXInfo.setBtnConnectText("Connect");
						e.printStackTrace();
						System.out.println("ERROR, autoConnecting: " + autoConnecting);
						JOptionPane.showMessageDialog(null, "Please enter a valid IP Address & Port", "ERROR",
								JOptionPane.ERROR_MESSAGE);
					}
				}
				return null;
			}

		};
		swingWorker.execute(); // Run the code to connect to PSX
	}

	// --------------------------//
	// ----- MAIN METHOD -------//
	// -------------------------//
	public static void main(String[] args) throws InstantiationException {

		Options.setFields();
		mainPanel = new JPanel();
		mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));
		lab_Conn.setBounds(235, 11, 209, 18);
		lab_Conn.setHorizontalAlignment(SwingConstants.CENTER);
		lab_Conn.setForeground(Color.RED);
		lab_Conn.setFont(new Font("Tahoma", Font.BOLD, 18));
		btnConnect.setBounds(235, 40, 209, 27);
		btnConnect.setFont(new Font("Tahoma", Font.PLAIN, 15));

		btnConnect.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				// System.out.println("Autoconnecting: " + autoConnecting);
				if (autoConnecting) {
					autoConnecting = false;
					connectingUI(1);
				} else {
					// System.out.println("Is connected: " + isConnected);
					if (!isConnected)
						PSXInfo.connectingUI(2); // Set UI for Connecting
					connect();

				}
			}
		});

		// Creating the main application frame
		frmPsxinfo = new JFrame("Precision Simulator Website Uplink " + version);
		frmPsxinfo.getContentPane().setPreferredSize(new Dimension(504, 460));
		frmPsxinfo.setName("PSXInfo");
		frmPsxinfo.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmPsxinfo.setIconImage(
				Toolkit.getDefaultToolkit().getImage(PSXInfo.class.getResource("/resources/mainApplication.png")));
		frmPsxinfo.setTitle("PSXInfo - PSX Landing Score");

		GroupLayout groupLayout = new GroupLayout(frmPsxinfo.getContentPane());
		groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup().addContainerGap()
						.addComponent(mainPanel, GroupLayout.DEFAULT_SIZE, 484, Short.MAX_VALUE).addContainerGap()));
		groupLayout.setVerticalGroup(groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup().addContainerGap()
						.addComponent(mainPanel, GroupLayout.DEFAULT_SIZE, 458, Short.MAX_VALUE).addContainerGap()));
		mainPanel.setLayout(null);
		mainPanel.add(lab_Conn);
		mainPanel.add(btnConnect);

		txt_HostIP = new JTextField();
		txt_HostIP.setBounds(76, 8, 133, 25);
		txt_HostIP.setText(psxhost);
		mainPanel.add(txt_HostIP);
		txt_HostIP.setColumns(10);

		JLabel lblHostIp = new JLabel("Host IP:");
		lblHostIp.setBounds(10, 11, 56, 25);
		mainPanel.add(lblHostIp);

		JLabel lblHostPort = new JLabel("Host Port:");
		lblHostPort.setBounds(10, 42, 61, 25);
		mainPanel.add(lblHostPort);

		txt_HostPort = new JTextField();
		txt_HostPort.setBounds(76, 42, 133, 25);

		mainPanel.add(txt_HostPort);
		txt_HostPort.setColumns(10);
		frmPsxinfo.getContentPane().setLayout(groupLayout);
		frmPsxinfo.pack();

		// Menus Bar
		JMenuBar menuBar = new JMenuBar();
		frmPsxinfo.setJMenuBar(menuBar);

		JMenu fileMenu = new JMenu("File");
		menuBar.add(fileMenu);

		optionsMenuItem = new JMenuItem("Options");
		optionsMenuItem.setIcon(new ImageIcon(PSXInfo.class.getResource("/resources/setting.png")));
		fileMenu.add(optionsMenuItem);

		JMenuItem exitMenuItem = new JMenuItem("Exit");
		exitMenuItem.setIcon(new ImageIcon(PSXInfo.class.getResource("/resources/power.png")));
		fileMenu.add(exitMenuItem);

		JMenu helpMenu = new JMenu("Help");
		menuBar.add(helpMenu);

		JMenuItem aboutMenuItem = new JMenuItem("About");
		aboutMenuItem.setIcon(new ImageIcon(PSXInfo.class.getResource("/resources/about.png")));
		helpMenu.add(aboutMenuItem);

		try {
			Config.loadProperties();
			System.out.println("Saved Properties Loaded");
		} catch (IOException ex) {
			JOptionPane.showMessageDialog(null, "The config.cfg file does not exist, default properties loaded.");
			System.out.println("Default Properties loaded");
		}
		psxhost = configProps.getProperty("psxhost");
		psxport = Integer.parseInt(configProps.getProperty("psxport"));
		autoConnect = Boolean.parseBoolean(configProps.getProperty("psxAutoConnect"));
		minimise = Boolean.parseBoolean(configProps.getProperty("minimise"));
		autoReconnect = Boolean.parseBoolean(configProps.getProperty("autoReconnect"));
		printEnabled = Boolean.parseBoolean(configProps.getProperty("printEnabled"));
		windowPosition[0] = Integer.parseInt(configProps.getProperty("windowPosition").split(";")[0]);
		windowPosition[1] = Integer.parseInt(configProps.getProperty("windowPosition").split(";")[1]);
		touchdownLogFileName = "TouchdownLog.txt";

		txt_HostIP.setText(configProps.getProperty("psxhost"));
		txt_HostPort.setText(configProps.getProperty("psxport"));

		JPanel landingScorePanel = new JPanel();
		landingScorePanel.setToolTipText("Landing");
		landingScorePanel.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		landingScorePanel.setBounds(0, 86, 298, 299);
		mainPanel.add(landingScorePanel);

		lbl_Landing.setHorizontalAlignment(SwingConstants.CENTER);
		lbl_Landing.setFont(new Font("Tahoma", Font.BOLD, 16));
		lbl_Serviceable.setBounds(0, 396, 485, 14);
		mainPanel.add(lbl_Serviceable);

		lbl_Serviceable.setForeground(Color.decode("#00AA00"));
		lbl_Serviceable.setFont(new Font("Tahoma", Font.BOLD, 16));
		lbl_Serviceable.setHorizontalAlignment(SwingConstants.CENTER);
		lbl_Serviceable.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				aircraftService(true);

			}
		});

		landingScoreMetricPanel = new JPanel();
		landingScoreMetricPanel.setBorder(null);
		GroupLayout gl_landingScorePanel = new GroupLayout(landingScorePanel);
		gl_landingScorePanel.setHorizontalGroup(gl_landingScorePanel.createParallelGroup(Alignment.TRAILING)
				.addComponent(lbl_Landing, GroupLayout.DEFAULT_SIZE, 294, Short.MAX_VALUE)
				.addGroup(gl_landingScorePanel.createSequentialGroup().addGap(10)
						.addComponent(landingScoreMetricPanel, GroupLayout.DEFAULT_SIZE, 274, Short.MAX_VALUE).addGap(10)));
		gl_landingScorePanel.setVerticalGroup(gl_landingScorePanel.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_landingScorePanel.createSequentialGroup().addContainerGap().addComponent(lbl_Landing)
						.addPreferredGap(ComponentPlacement.RELATED)
						.addComponent(landingScoreMetricPanel, GroupLayout.DEFAULT_SIZE, 260, Short.MAX_VALUE).addContainerGap()));

		lblRate = new JLabel("Data");
		lblRate.setHorizontalAlignment(SwingConstants.CENTER);

		JLabel lblDeduction = new JLabel("Deduction");
		lblDeduction.setHorizontalAlignment(SwingConstants.CENTER);

		JLabel lblMainGear = new JLabel("Main Gear");

		JLabel lblOffset = new JLabel("Offset from Centreline at TD");

		JLabel lblDistance = new JLabel("Distance from Threshold");

		JLabel lblPitch = new JLabel("Pitch");

		JLabel lblBank = new JLabel("Bank");

		JLabel lblCrab = new JLabel("Crab");

		JLabel lblSpeed = new JLabel("Speed Deviation");

		lblMainGearData = new JLabel("--- fpm");
		lblMainGearData.setHorizontalAlignment(SwingConstants.CENTER);

		lblNoseGearData = new JLabel("--- fpm");
		lblNoseGearData.setHorizontalAlignment(SwingConstants.CENTER);

		lblMainGearDeduct = new JLabel("---");
		lblMainGearDeduct.setHorizontalAlignment(SwingConstants.CENTER);

		lblNoseGearDeduct = new JLabel("---");
		lblNoseGearDeduct.setFont(new Font("Tahoma", Font.PLAIN, 11));
		lblNoseGearDeduct.setHorizontalAlignment(SwingConstants.CENTER);

		lblOffsetData = new JLabel("--- m");
		lblOffsetData.setHorizontalAlignment(SwingConstants.CENTER);

		lblOffsetDeduct = new JLabel("");
		if(LandingCalc.scoreOffsetAndThreshold) {
			setLabel_OffsetDeduct("---");
		} else {
			setLabel_OffsetDeduct("N/A");
		}
		lblOffsetDeduct.setHorizontalAlignment(SwingConstants.CENTER);

		lblDistanceData = new JLabel("--- ft");
		lblDistanceData.setHorizontalAlignment(SwingConstants.CENTER);

		lblDistanceDeduct = new JLabel("");
		if(LandingCalc.scoreOffsetAndThreshold) {
			setLabel_ThresholdDeduct("---");
		} else {
			setLabel_ThresholdDeduct("N/A");
		}
		lblDistanceDeduct.setHorizontalAlignment(SwingConstants.CENTER);
		
		lblPitchData = new JLabel("--- \u00B0");
		lblPitchData.setHorizontalAlignment(SwingConstants.CENTER);

		lblBankData = new JLabel("--- \u00B0");
		lblBankData.setHorizontalAlignment(SwingConstants.CENTER);

		lblCrabData = new JLabel("--- \u00B0");
		lblCrabData.setHorizontalAlignment(SwingConstants.CENTER);

		lblSpeedData = new JLabel("--- kts");
		lblSpeedData.setHorizontalAlignment(SwingConstants.CENTER);

		

		lblPitchDeduct = new JLabel("---");
		lblPitchDeduct.setHorizontalAlignment(SwingConstants.CENTER);

		lblBankDeduct = new JLabel("---");
		lblBankDeduct.setHorizontalAlignment(SwingConstants.CENTER);

		lblCrabDeduct = new JLabel("---");
		lblCrabDeduct.setHorizontalAlignment(SwingConstants.CENTER);

		lblSpeedDeduct = new JLabel("---");
		lblSpeedDeduct.setHorizontalAlignment(SwingConstants.CENTER);

		JLabel lblNoseGear = new JLabel("Nose Gear");

		lblDecel = new JLabel("Max Change in Deceleration");

		lblDecelData = new JLabel("--- m/s\u00B3");
		lblDecelData.setHorizontalAlignment(SwingConstants.CENTER);

		lblDecelDeduct = new JLabel("---");
		lblDecelDeduct.setHorizontalAlignment(SwingConstants.CENTER);

		lblAvgOffsetDeduct = new JLabel("---");
		lblAvgOffsetDeduct.setHorizontalAlignment(SwingConstants.CENTER);

		lblAvgOffsetData = new JLabel("--- m");
		lblAvgOffsetData.setHorizontalAlignment(SwingConstants.CENTER);

		lblAvgOffset = new JLabel("Avg Offset from Centreline");

		lblEmptyLandingScore = new JLabel("");

		lblNoseTransitionTimeData = new JLabel("--- s");
		lblNoseTransitionTimeData.setHorizontalAlignment(SwingConstants.CENTER);

		lblNoseTransitionTime = new JLabel("Nose Transition Time");

		lblNoseTransitionTimeDeduct = new JLabel("---");
		lblNoseTransitionTimeDeduct.setHorizontalAlignment(SwingConstants.CENTER);
		GroupLayout gl_landingScoreMetricPanel = new GroupLayout(landingScoreMetricPanel);
		gl_landingScoreMetricPanel.setHorizontalGroup(gl_landingScoreMetricPanel.createParallelGroup(Alignment.TRAILING).addGroup(gl_landingScoreMetricPanel
				.createSequentialGroup().addContainerGap()
				.addGroup(gl_landingScoreMetricPanel.createParallelGroup(Alignment.LEADING).addGroup(gl_landingScoreMetricPanel
						.createSequentialGroup().addGroup(gl_landingScoreMetricPanel.createParallelGroup(Alignment.LEADING)
								.addComponent(lblCrab, GroupLayout.DEFAULT_SIZE, 137, Short.MAX_VALUE)
								.addComponent(lblBank, GroupLayout.DEFAULT_SIZE, 137, Short.MAX_VALUE)
								.addComponent(lblOffset, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE,
										Short.MAX_VALUE)
								.addComponent(lblNoseGear, GroupLayout.DEFAULT_SIZE, 137, Short.MAX_VALUE)
								.addComponent(lblMainGear, GroupLayout.DEFAULT_SIZE, 137, Short.MAX_VALUE)
								.addComponent(lblEmptyLandingScore, GroupLayout.DEFAULT_SIZE, 137, Short.MAX_VALUE)
								.addComponent(lblPitch, GroupLayout.DEFAULT_SIZE, 137, Short.MAX_VALUE)
								.addComponent(lblDistance, GroupLayout.DEFAULT_SIZE, 137, Short.MAX_VALUE)
								.addComponent(lblSpeed, GroupLayout.DEFAULT_SIZE, 137, Short.MAX_VALUE))
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(gl_landingScoreMetricPanel.createParallelGroup(Alignment.TRAILING)
								.addComponent(lblNoseGearData, GroupLayout.DEFAULT_SIZE, 57, Short.MAX_VALUE)
								.addComponent(lblRate, GroupLayout.DEFAULT_SIZE, 57, Short.MAX_VALUE)
								.addComponent(lblMainGearData, GroupLayout.DEFAULT_SIZE, 57, Short.MAX_VALUE)
								.addComponent(lblOffsetData, GroupLayout.DEFAULT_SIZE, 57, Short.MAX_VALUE)
								.addComponent(lblDistanceData, GroupLayout.DEFAULT_SIZE, 57, Short.MAX_VALUE)
								.addComponent(lblPitchData, GroupLayout.DEFAULT_SIZE, 57, Short.MAX_VALUE)
								.addComponent(lblBankData, GroupLayout.DEFAULT_SIZE, 57, Short.MAX_VALUE)
								.addComponent(lblCrabData, GroupLayout.DEFAULT_SIZE, 57, Short.MAX_VALUE)
								.addComponent(lblSpeedData, GroupLayout.DEFAULT_SIZE, 57, Short.MAX_VALUE))
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(gl_landingScoreMetricPanel.createParallelGroup(Alignment.LEADING)
								.addComponent(lblSpeedDeduct, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 48,
										Short.MAX_VALUE)
								.addComponent(lblCrabDeduct, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 48,
										Short.MAX_VALUE)
								.addComponent(lblBankDeduct, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 48,
										Short.MAX_VALUE)
								.addComponent(lblPitchDeduct, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 48,
										Short.MAX_VALUE)
								.addComponent(lblDistanceDeduct, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 48,
										Short.MAX_VALUE)
								.addComponent(lblOffsetDeduct, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 48,
										Short.MAX_VALUE)
								.addComponent(lblNoseGearDeduct, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 48,
										Short.MAX_VALUE)
								.addComponent(lblDeduction, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE,
										GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
								.addComponent(lblMainGearDeduct, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 48,
										Short.MAX_VALUE)))
						.addGroup(Alignment.TRAILING,
								gl_landingScoreMetricPanel.createSequentialGroup()
										.addComponent(lblDecel, GroupLayout.DEFAULT_SIZE, 137, Short.MAX_VALUE)
										.addPreferredGap(ComponentPlacement.RELATED)
										.addComponent(lblDecelData, GroupLayout.DEFAULT_SIZE, 57, Short.MAX_VALUE)
										.addPreferredGap(ComponentPlacement.RELATED)
										.addComponent(lblDecelDeduct, GroupLayout.DEFAULT_SIZE, 48, Short.MAX_VALUE))
						.addGroup(Alignment.TRAILING,
								gl_landingScoreMetricPanel.createSequentialGroup()
										.addComponent(lblAvgOffset, GroupLayout.DEFAULT_SIZE, 137, Short.MAX_VALUE)
										.addPreferredGap(ComponentPlacement.RELATED)
										.addComponent(lblAvgOffsetData, GroupLayout.DEFAULT_SIZE, 57, Short.MAX_VALUE)
										.addPreferredGap(ComponentPlacement.RELATED).addComponent(lblAvgOffsetDeduct,
												GroupLayout.DEFAULT_SIZE, 48, Short.MAX_VALUE))
						.addGroup(Alignment.TRAILING, gl_landingScoreMetricPanel.createSequentialGroup()
								.addComponent(lblNoseTransitionTime, GroupLayout.DEFAULT_SIZE, 137, Short.MAX_VALUE)
								.addPreferredGap(ComponentPlacement.RELATED)
								.addComponent(lblNoseTransitionTimeData, GroupLayout.PREFERRED_SIZE, 57,
										GroupLayout.PREFERRED_SIZE)
								.addPreferredGap(ComponentPlacement.RELATED).addComponent(lblNoseTransitionTimeDeduct,
										GroupLayout.DEFAULT_SIZE, 48, Short.MAX_VALUE)))
				.addContainerGap()));
		gl_landingScoreMetricPanel.setVerticalGroup(gl_landingScoreMetricPanel.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_landingScoreMetricPanel.createSequentialGroup().addContainerGap()
						.addGroup(gl_landingScoreMetricPanel.createParallelGroup(Alignment.BASELINE).addComponent(lblEmptyLandingScore)
								.addComponent(lblRate).addComponent(lblDeduction))
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(gl_landingScoreMetricPanel.createParallelGroup(Alignment.BASELINE).addComponent(lblNoseGearDeduct)
								.addComponent(lblNoseGearData).addComponent(lblNoseGear))
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(gl_landingScoreMetricPanel.createParallelGroup(Alignment.BASELINE).addComponent(lblMainGearDeduct)
								.addComponent(lblMainGearData).addComponent(lblMainGear))
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(gl_landingScoreMetricPanel.createParallelGroup(Alignment.BASELINE).addComponent(lblOffsetDeduct)
								.addComponent(lblOffsetData).addComponent(lblOffset))
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(gl_landingScoreMetricPanel.createParallelGroup(Alignment.BASELINE).addComponent(lblDistanceDeduct)
								.addComponent(lblDistanceData).addComponent(lblDistance))
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(gl_landingScoreMetricPanel.createParallelGroup(Alignment.BASELINE).addComponent(lblPitchDeduct)
								.addComponent(lblPitchData).addComponent(lblPitch))
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(gl_landingScoreMetricPanel.createParallelGroup(Alignment.BASELINE).addComponent(lblBankDeduct)
								.addComponent(lblBankData).addComponent(lblBank))
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(gl_landingScoreMetricPanel.createParallelGroup(Alignment.BASELINE).addComponent(lblCrabDeduct)
								.addComponent(lblCrabData).addComponent(lblCrab))
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(gl_landingScoreMetricPanel.createParallelGroup(Alignment.BASELINE).addComponent(lblSpeedDeduct)
								.addComponent(lblSpeedData).addComponent(lblSpeed))
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(gl_landingScoreMetricPanel.createParallelGroup(Alignment.BASELINE).addComponent(lblDecelDeduct)
								.addComponent(lblDecelData).addComponent(lblDecel))
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(gl_landingScoreMetricPanel.createParallelGroup(Alignment.BASELINE).addComponent(lblAvgOffsetDeduct)
								.addComponent(lblAvgOffsetData).addComponent(lblAvgOffset))
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(gl_landingScoreMetricPanel.createParallelGroup(Alignment.BASELINE)
								.addComponent(lblNoseTransitionTimeDeduct).addComponent(lblNoseTransitionTimeData)
								.addComponent(lblNoseTransitionTime))
						.addContainerGap(21, Short.MAX_VALUE)));
		landingScoreMetricPanel.setLayout(gl_landingScoreMetricPanel);
		landingScorePanel.setLayout(gl_landingScorePanel);

		statusPanel = new JPanel();
		statusPanel.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		statusPanel.setBounds(308, 86, 177, 229);
		mainPanel.add(statusPanel);

		

		Options.cbxAutoConnect.setSelected(Boolean.parseBoolean(configProps.getProperty("psxAutoConnect")));
		Options.cbxMinimise.setSelected(Boolean.parseBoolean(configProps.getProperty("minimise")));
		Options.cbxAutoReconnect.setSelected(Boolean.parseBoolean(configProps.getProperty("autoReconnect")));
		Options.cbxPrintEnabled.setSelected(Boolean.parseBoolean(configProps.getProperty("printEnabled")));
		Options.cbxOffsetThreshold.setSelected(Boolean.parseBoolean(configProps.getProperty("scoreOffsetAndThreshold")));
		// saveProperties();

		// Screen width
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

		if (windowPosition[0] < 0 || windowPosition[0] >= screenSize.getWidth() || windowPosition[1] < 0
				|| windowPosition[1] > screenSize.getHeight()) {
			windowPosition[0] = defaultWindowPosition[0];
			windowPosition[1] = defaultWindowPosition[1];
		}

		frmPsxinfo.setLocation(windowPosition[0], windowPosition[1]);
		updatetimes();

		// ---------- MENU BUTTON ACTIONS --------//
		// Options
		optionsMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
					protected Void doInBackground() throws Exception {
						options.setVisible(true);
						return null;
					}
				};

				worker.execute();
			}
		});

		// Exit
		exitMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				try {
					Config.saveProperties();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					JOptionPane.showMessageDialog(null, "Unable to save config file");
					e.printStackTrace();
				}
				if(netThread != null)
					netThread.finalJobs();
				netThread = null;
				System.exit(0);
			}
		});

		// About
		aboutMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				About about = new About();
				about.setVisible(true);
			}
		});

		// --------- END OF MENU BUTTON ACTIONS --------//

		frmPsxinfo.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent e) {
				try {
					Config.saveProperties();
				} catch (Exception e1) {
					JOptionPane.showMessageDialog(null, "Unable to save config file");
					e1.printStackTrace();
				}
				if(netThread != null)
					netThread.finalJobs();
				netThread = null;
				System.exit(0);
			}
		});

		frmPsxinfo.pack();
		frmPsxinfo.setVisible(true);

		if (autoConnect) {
			autoConnecting = true;
			PSXInfo.txt_HostIP.setEnabled(false);
			PSXInfo.txt_HostPort.setEnabled(false);
			PSXInfo.lab_Conn.setText("CONNECTING");
			PSXInfo.setBtnConnectText("Disconnect");
			PSXInfo.lab_Conn.setForeground(Color.decode("#FF9900"));
			try {
				TimeUnit.MILLISECONDS.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}


			SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
				@Override
				public Void doInBackground() {
					boolean isFirstTry = true;
					while (!isConnected & autoConnecting) {
						if (autoConnecting) {
							//System.out.println("Initial Autoconnect");
							if (isFirstTry) {
								connect();
								isFirstTry = false;
							} else {
								netThread.run();
								if (isConnected)
									isFirstTry = true;
							}
						}
						try {
							TimeUnit.MILLISECONDS.sleep(2000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}

					}
					return null;
				}
			};
			
			worker.execute();
		}

		try {
			TimeUnit.MILLISECONDS.sleep(2000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}

		if (isConnected) {
			if (minimise)
				frmPsxinfo.setState(Frame.ICONIFIED);
		}

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				if (netThread != null)
					netThread.finalJobs();
			}
		});

	}

	// -------------- CHANGING LABEL METHODS FROM OTHER JAVA FILES -----------//
	static void setLabel_Lat(String s) {
		lab_Lat.setText(s);
	}

	static void setLabel_Lon(String s) {
		lab_Lon.setText(s);
	}

	static void setLabel_Alt(String s) {
		lab_Alt.setText(s);
	}

	static void setLabel_Hdg(String s) {
		lab_Hdg.setText(s);
	}

	static void setLabel_Ias(String s) {
		lab_Ias.setText(s);
	}

	static void setLabel_Wnd(String s) {
		lab_Wnd.setText(s);
	}

	static void setLabel_Eta(String s) {
		lab_Eta.setText(s);
	}

	static void setLabel_Dep(String s) {
		lab_Dep.setText(s);
	}

	static void setLabel_Arr(String s) {
		lab_Arr.setText(s);
	}

	static void setLabel_Ldg(String s) {
		lbl_Landing.setText(s);
	}
	
	static void setLabel_OffsetDeduct(String s) {
		lblOffsetDeduct.setText(s);
	}
	
	static void setLabel_ThresholdDeduct(String s) {
		lblDistanceDeduct.setText(s);
	}

	static void setLabel_Service(boolean serviceable) {
		if (serviceable) {
			lbl_Serviceable.setText("SERVICEABLE");
			lbl_Serviceable.setForeground(Color.decode("#00AA00"));
		} else {
			lbl_Serviceable.setText("UNSERVICEABLE");
			lbl_Serviceable.setForeground(Color.red);
		}

	}

	static void setBtnConnectText(String text) {
		btnConnect.setText(text);
	}
	// -------------- END OF CHANGING LABEL METHODS FROM OTHER JAVA FILES
	// -----------//

	// -------------- THINGS THAT NEED TO HAPPEN WHEN CONNECTING/DISCONNECTING FROM
	// PSX -----------//
	static void connectingUI(int i) {
		// 1 = set disconnected (default), 2 = set connectING, 3 = set isConnected

		if (i == 3) { // When isConnected to PSX
			lab_Conn.setText("CONNECTED");
			lab_Conn.setForeground(Color.decode("#00AA00"));
			setBtnConnectText("Disconnect");
			PSXInfoNetThread.forceRouteUpdate = 1;
			isConnected = true;
			autoConnecting = false;
			txt_HostIP.setEnabled(false);
			txt_HostPort.setEnabled(false);
			sendToServer("demand=Qs483");

		} else if (i == 2) { // When connecting to PSX

			isConnected = false;
			lab_Conn.setText("CONNECTING");
			lab_Conn.setForeground(Color.decode("#FF9900"));
			txt_HostIP.setEnabled(false);
			txt_HostPort.setEnabled(false);

			// mntmOptions.setEnabled(false);

		} else { // When disconnected from PSX
			isConnected = false;
			if (autoConnecting) {
				PSXInfoNetThread.socket = null;
				lab_Conn.setText("CONNECTING");
				lab_Conn.setForeground(Color.decode("#FF9900"));
				txt_HostIP.setEnabled(false);
				txt_HostPort.setEnabled(false);
				// btnPushback.setEnabled(true);

			} else {
				lab_Conn.setText("DISCONNECTED");
				lab_Conn.setForeground(Color.red);
				setBtnConnectText("Connect");
				txt_HostIP.setEnabled(true);
				txt_HostPort.setEnabled(true);

				// Set labels back to default values (i.e. when the application is opened)
				setLabel_Lat(lab_LatDefault);
				setLabel_Lon(lab_LonDefault);
				setLabel_Alt(lab_AltDefault);
				setLabel_Hdg(lab_HdgDefault);
				setLabel_Ias(lab_IasDefault);
				setLabel_Wnd(lab_WndDefault);
				setLabel_Eta(lab_EtaDefault);
				setLabel_Dep(lab_DepRwyDefault);
				setLabel_Arr(lab_ArrRwyDefault);
			}
		}
	}

	synchronized static void sendToServer(String s) {
		if (netThread != null && s != null)
			netThread.send(s);
	}

	public static double roundToHalf(double d) {
		return Math.round(d * 2) / 2.0;
	}
}