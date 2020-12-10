package src;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.TitledBorder;

public class Options extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8578973490647968311L;

	protected static JCheckBox cbxAutoConnect;
	protected static JCheckBox cbxMinimise;
	protected static JCheckBox cbxAutoReconnect;
	protected static JCheckBox cbxPrintEnabled;
	protected static JCheckBox cbxOffsetThreshold;

	private static JButton okButton;
	private JPanel buttonPane;
	private JButton cancelButton;
	

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

		} catch (Throwable e) {
			e.printStackTrace();
		}
		try {
			Options dialog = new Options();
			dialog.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
			dialog.setVisible(true);
			dialog.getRootPane().setDefaultButton(okButton);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create the dialog.
	 */
	public Options() {
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent arg0) {
				setFields();
				dispose();
			}
		});
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

		} catch (Throwable e) {
			e.printStackTrace();
		}
		setModal(true);
		setAlwaysOnTop(true);
		setIconImage(Toolkit.getDefaultToolkit().getImage(Options.class.getResource("/resources/settings.png")));
		setTitle("Options");
		setBounds(100, 100, 352, 303);
		getContentPane().setLayout(null);
		{
			TitledBorder border = new TitledBorder("FTP Settings");
			border.setTitleJustification(TitledBorder.LEFT);
			border.setTitlePosition(TitledBorder.TOP);
		}
		{
			TitledBorder borderFileSettings = new TitledBorder("File Settings");
			borderFileSettings.setTitleJustification(TitledBorder.LEFT);
			borderFileSettings.setTitlePosition(TitledBorder.TOP);
		}

		JLabel lblPsxinfoOptions = new JLabel("PSXInfo Options");
		lblPsxinfoOptions.setHorizontalAlignment(SwingConstants.CENTER);
		lblPsxinfoOptions.setBounds(10, 11, 316, 19);
		lblPsxinfoOptions.setFont(
				lblPsxinfoOptions.getFont().deriveFont(lblPsxinfoOptions.getFont().getStyle() | Font.BOLD, 15f));
		getContentPane().add(lblPsxinfoOptions);

		JPanel panel = new JPanel();
		panel.setSize(new Dimension(290, 235));
		panel.setMinimumSize(new Dimension(290, 235));
		panel.setPreferredSize(new Dimension(100, 200));
		panel.setBounds(10, 41, 316, 166);
		getContentPane().add(panel);
		panel.setBorder(new TitledBorder(null, "General Settings", TitledBorder.LEFT, TitledBorder.TOP, null, null));
		GridBagLayout gbl_panel = new GridBagLayout();
		gbl_panel.columnWidths = new int[] {200};
		gbl_panel.rowHeights = new int[] {25, 25, 25, 25, 0};
		gbl_panel.columnWeights = new double[] { 0.0 };
		gbl_panel.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0 };
		panel.setLayout(gbl_panel);

		cbxAutoConnect = new JCheckBox("Connect to PSX on start up");
		GridBagConstraints gbc_cbxAutoConnect = new GridBagConstraints();
		gbc_cbxAutoConnect.insets = new Insets(0, 0, 5, 0);
		gbc_cbxAutoConnect.anchor = GridBagConstraints.NORTHWEST;
		gbc_cbxAutoConnect.gridx = 0;
		gbc_cbxAutoConnect.gridy = 0;
		panel.add(cbxAutoConnect, gbc_cbxAutoConnect);

		cbxMinimise = new JCheckBox("Minimise on start up");
		GridBagConstraints gbc_cbxMinimise = new GridBagConstraints();
		gbc_cbxMinimise.anchor = GridBagConstraints.NORTHWEST;
		gbc_cbxMinimise.insets = new Insets(0, 0, 5, 0);
		gbc_cbxMinimise.gridx = 0;
		gbc_cbxMinimise.gridy = 1;
		panel.add(cbxMinimise, gbc_cbxMinimise);

		cbxAutoReconnect = new JCheckBox("Reconnect on error");
		GridBagConstraints gbc_cbxAutoReconnect = new GridBagConstraints();
		gbc_cbxAutoReconnect.anchor = GridBagConstraints.WEST;
		gbc_cbxAutoReconnect.insets = new Insets(0, 0, 5, 0);
		gbc_cbxAutoReconnect.gridx = 0;
		gbc_cbxAutoReconnect.gridy = 2;
		panel.add(cbxAutoReconnect, gbc_cbxAutoReconnect);

		cbxPrintEnabled = new JCheckBox("Print landing results to cockpit printer");
		GridBagConstraints gbc_cbxPrintEnabled = new GridBagConstraints();
		gbc_cbxPrintEnabled.anchor = GridBagConstraints.WEST;
		gbc_cbxPrintEnabled.insets = new Insets(0, 0, 5, 0);
		gbc_cbxPrintEnabled.gridx = 0;
		gbc_cbxPrintEnabled.gridy = 3;
		panel.add(cbxPrintEnabled, gbc_cbxPrintEnabled);
		
		cbxOffsetThreshold = new JCheckBox("Include offset & threshold distance in score (Expert Pilot)");
		GridBagConstraints gbc_cbxOffsetThreshold = new GridBagConstraints();
		gbc_cbxOffsetThreshold.anchor = GridBagConstraints.WEST;
		gbc_cbxOffsetThreshold.gridx = 0;
		gbc_cbxOffsetThreshold.gridy = 4;
		panel.add(cbxOffsetThreshold, gbc_cbxOffsetThreshold);
		{
			buttonPane = new JPanel();
			buttonPane.setBounds(10, 218, 316, 33);
			getContentPane().add(buttonPane);
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			{
				okButton = new JButton("OK");
				okButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent arg0) {

						try {
							Config.saveProperties();
							LandingCalc.scoreOffsetAndThreshold = Boolean.parseBoolean(Config.configProps.getProperty("scoreOffsetAndThreshold"));
						} catch (Exception e) {
							// TODO Auto-generated catch block
							JOptionPane.showMessageDialog(null, "Unable to save config file");
							e.printStackTrace();
						}
						
						if(LandingCalc.scoreOffsetAndThreshold) {
								PSXInfo.setLabel_OffsetDeduct("---");
								PSXInfo.setLabel_ThresholdDeduct("---");
								PSXInfo.lblOffsetDeduct.setFont(new Font(LandingCalc.labelFont.getName(), Font.PLAIN, LandingCalc.labelFont.getSize()));
								PSXInfo.lblDistanceDeduct.setFont(new Font(LandingCalc.labelFont.getName(), Font.PLAIN, LandingCalc.labelFont.getSize()));
								PSXInfo.lblOffsetDeduct.setForeground(Color.BLACK);
								PSXInfo.lblDistanceDeduct.setForeground(Color.BLACK);
						} else {
							PSXInfo.setLabel_OffsetDeduct("N/A");
							PSXInfo.setLabel_ThresholdDeduct("N/A");
							PSXInfo.lblOffsetDeduct.setFont(new Font(LandingCalc.labelFont.getName(), Font.PLAIN, LandingCalc.labelFont.getSize()));
							PSXInfo.lblDistanceDeduct.setFont(new Font(LandingCalc.labelFont.getName(), Font.PLAIN, LandingCalc.labelFont.getSize()));
							PSXInfo.lblOffsetDeduct.setForeground(Color.BLACK);
							PSXInfo.lblDistanceDeduct.setForeground(Color.BLACK);
						}

						dispose();
					}
				});
				okButton.setActionCommand("OK");
				buttonPane.add(okButton);
				getRootPane().setDefaultButton(okButton);
			}
			{
				cancelButton = new JButton("Cancel");
				cancelButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent arg0) {
						setFields();
						dispose();

					}
				});
				cancelButton.setActionCommand("Cancel");
				buttonPane.add(cancelButton);
			}
		}

		setFields();

		testConnection();

	}

	// Set text fields
	public static void setFields() {
		cbxAutoConnect.setSelected(PSXInfo.autoConnect);
		cbxMinimise.setSelected(PSXInfo.minimise);
		cbxAutoReconnect.setSelected(PSXInfo.autoReconnect);
		cbxPrintEnabled.setSelected(PSXInfo.printEnabled);
		cbxOffsetThreshold.setSelected(LandingCalc.scoreOffsetAndThreshold);
	}

	public void testConnection() {

		/*
		 * if (PSXInfo.isConnected) { System.out.println("PSX isConnected? " +
		 * PSXInfo.isConnected); //true txt_ftpServer.setEnabled(false);
		 * txt_ftpPort.setEnabled(false); txt_ftpUser.setEnabled(false);
		 * txt_ftpPass.setEnabled(false); txt_folderPath.setEnabled(false);
		 * txt_fileName.setEnabled(false); } else if (!PSXInfo.isConnected){
		 * System.out.println("PSX isConnected? " + PSXInfo.isConnected); //false
		 * txt_ftpServer.setEnabled(true); txt_ftpPort.setEnabled(true);
		 * txt_ftpUser.setEnabled(true); txt_ftpPass.setEnabled(true);
		 * txt_folderPath.setEnabled(true); txt_fileName.setEnabled(true); }
		 */
	}
}
