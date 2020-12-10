package src;

import java.awt.Toolkit;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

public class About extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1294159563661850183L;
	private final JPanel contentPanel = new JPanel();

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (InstantiationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IllegalAccessException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (UnsupportedLookAndFeelException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		//SwingUtilities.updateComponentTreeUI(contentPanel);
		
		try {
			About dialog = new About();
			dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create the dialog.
	 */
	public About() {
		setIconImage(Toolkit.getDefaultToolkit().getImage(About.class.getResource("/resources/about.png")));
		setResizable(false);
		setModal(true);
		//setIconImage(Toolkit.getDefaultToolkit().getImage(About.class.getResource("/windowBuilder/resources/095_chart.png")));
		setTitle(" About");
		setAlwaysOnTop(true);
		setBounds(100, 100, 458, 443);
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		GroupLayout groupLayout = new GroupLayout(getContentPane());
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addComponent(contentPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addComponent(contentPanel, GroupLayout.PREFERRED_SIZE, 385, Short.MAX_VALUE)
					.addContainerGap())
		);
		
		JEditorPane dtrpnpsxinfoStevenBrown = new JEditorPane();
		dtrpnpsxinfoStevenBrown.setBackground(getBackground());
		dtrpnpsxinfoStevenBrown.setHighlighter(null);
		dtrpnpsxinfoStevenBrown.setEditable(false);
		dtrpnpsxinfoStevenBrown.setContentType("text/html");
		dtrpnpsxinfoStevenBrown.setText("<center><h1>PSXInfo " + PSXInfo.version + "<br>Build: " + PSXInfo.build + "</br></h1>\r\nSteven Brown</br></center>\r\n<br>\r\n<p>PSXInfo features a touchdown scoring system for PSX. It shows what your main gear landing rate was and if the aircraft is still in a servicable state plus more. i.e. no pod stikes, tailstrikes etc - Is the aircraft in a state that it can take off again and this is determined using the indicators on PSX's touchdown page.</p>\r\n<p>PSXInfo also has an optional Expert Mode which means you need to land the aircraft on the runway as accurately as you can. This allows the software to ignore the scenery discrepancies which would result in a poor score when landing visually.</p>\r\n<p>\r\n\r\nEnjoy! :)");
		GroupLayout gl_contentPanel = new GroupLayout(contentPanel);
		gl_contentPanel.setHorizontalGroup(
			gl_contentPanel.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_contentPanel.createSequentialGroup()
					.addContainerGap()
					.addComponent(dtrpnpsxinfoStevenBrown, GroupLayout.DEFAULT_SIZE, 409, Short.MAX_VALUE)
					.addGap(13))
		);
		gl_contentPanel.setVerticalGroup(
			gl_contentPanel.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_contentPanel.createSequentialGroup()
					.addContainerGap()
					.addComponent(dtrpnpsxinfoStevenBrown, GroupLayout.DEFAULT_SIZE, 386, Short.MAX_VALUE)
					.addContainerGap())
		);
		contentPanel.setLayout(gl_contentPanel);
		getContentPane().setLayout(groupLayout);
	}
}
