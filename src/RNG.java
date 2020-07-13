import java.util.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.concurrent.*;
import javax.swing.border.*;
import javax.swing.text.*;

// Hold logical contents of JPanels
interface Content {
	public Color getColor();
	public void processInput();
	public String getText(String mode);
}

// Link Front-end and logical classes
interface Handler {
	public void processInput(Content c);
	public void updateDisplay();
}

class Utilities {
	// Halt execution for ms milliseconds
	public static void sleep(int ms) {
		try {
			Thread.sleep(ms);
		} catch(InterruptedException e) {
			System.out.print("Error sleeping");
		}
	}

	// Return a Color object for rgb triplet specified in [0,255]
	public static Color makeColor(double r, double g, double b) {
		return new Color((float) (r/255.0),(float) (g/255.0),(float) (b/255.0));
	}

	// Return a readable text colour against background c
	public static Color contrastingColor(Color c) {
		if(c == Color.BLACK) {
			return Color.WHITE;
		} else {
			return Color.BLACK;
		}
	}

	// Add the text s to pane in specified format
	public static void addStyledText(JTextPane pane, String s, int id, String font, int fontSize, Color color, boolean bold, boolean italic, boolean center) {
		StyledDocument doc = pane.getStyledDocument();
		SimpleAttributeSet attributeSet = new SimpleAttributeSet();
		String uniqueID = Integer.toString(id);
		Style style = pane.addStyle(uniqueID, null);
		if(center) {
			StyleConstants.setAlignment(attributeSet, StyleConstants.ALIGN_CENTER);
		}
		if(bold) {
			StyleConstants.setBold(style, true);
		}
		if(italic) {
			StyleConstants.setItalic(style, true);
		}
		StyleConstants.setFontFamily(style, font);
		StyleConstants.setFontSize(style, fontSize);
		StyleConstants.setForeground(style, color);

		try {doc.insertString(doc.getLength(), s, style); }
    catch (BadLocationException e){}

		doc.setParagraphAttributes(0, doc.getLength() - 1, attributeSet, false);
	}

	public static void addHistoryText(JTextPane pane, Bingo b, int fontSize) {
		int tens;
		addStyledText(pane, new String("History: "), 0, "Courier New", fontSize, Color.BLACK, false, false, false);
		for(Integer i : b.getHistory()) {
			tens = (i - 1) / 10;
			addStyledText(pane, new String(String.format("%2d", i)+" "), 0, "Courier New", fontSize, b.getColor(tens), false, false, false);
		}
	}

	// Return readable string representation of time specified in ms
	public static String displayTime(long timeUsed) {
		long secondsUsed = timeUsed / 1000;
		long minutesUsed = secondsUsed / 60;
		secondsUsed = secondsUsed % 60;
		long millisecondsUsed = timeUsed % 1000;
		return timeString(minutesUsed, secondsUsed, millisecondsUsed);
	}

	// Return readable string representation of time specified in m/s/ms
	public static String timeString(long m, long s, long ms) {
		if(m > 0) {
			 return new String(Long.toString(m) + "m "+ Long.toString(s) + "s " + Long.toString(ms) + "ms");
		}
		return new String(Long.toString(s) + "s " + Long.toString(ms) + "ms");
	}

}

// A window, holding an arbitrary grid of panels + optionally a menu
class CustomFrame extends JFrame {

	private int width;
	private int height;
	private String title;
	private CustomPanel contentPanel;
	private int panelID;

	// Construct a frame to hold content of x*y pixels with title t
	public CustomFrame(int x, int y, String t) {
		width = x+50;
		height = y+50;
		title = t;
		panelID = 0;
		setTitle(title);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setSize(x, y);

		this.contentPanel = new CustomPanel(x, y, this, panelID, null);
		add(contentPanel);
	}

	// Setting mainWindow=true closes application when window is closed
	public CustomFrame(int x, int y, String t, boolean mainWindow) {
		this(x, y, t);
		if(mainWindow) {
			setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		} else {
			setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		}
	}

	public CustomPanel getContentPanel() {
		return this.contentPanel;
	}

	// Update display of window and all child elements
	public void display() {
		pack();
		setVisible(true);
		contentPanel.display();
	}

	// Close progamatically
	public void close() {
		dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public String getTitle() {
		return title;
	}

	public void setFrameBackground(Color c) {
		contentPanel.setBackground(c);
	}

	// Get sub-panel of contentPanel with id=i
	public CustomPanel getPanel(int i) {
		return contentPanel.getPanel(i);
	}

	// Add new sub-panel to contentPanel
	public int addPanel(int x, int y, int gridwidth, int gridheight, int w, int h, Content c) {
		panelID++;
		contentPanel.addPanel(x, y, gridwidth, gridheight, w, h, this, panelID, c);
		return panelID;
	}

	public int addPanel(int x, int y, int w, int h, Content c) {
		panelID++;
		contentPanel.addPanel(x, y, w, h, this, panelID, c);
		return panelID;
	}

	// Add a button to sub-panel with id=id
	public void addButton(int id, String s) {
		getPanel(id).addButton(s);
	}

	// Tell relevant component to process input
	public void processInput(int id) {
		getPanel(id).getContent().processInput();
	}

}

// Used for main (contentPanel) and its grid of sub-panels
class CustomPanel extends JPanel {
	private int width;
	private int height;
	private GridBagConstraints constraints; // For arranging layout
	private ArrayList<CustomPanel> panels;
	private JButton button;	// May be null
	private CustomFrame root;
	private CustomPanel parent;
	private int myID;
	private Content content; // Used for output+input handling
	private Border border;
	private JTextPane textPane;

	public CustomPanel(int x, int y, CustomFrame f, int id, Content c) {
		width = x;
		height = y;
		root = f;
		myID = id;
		content = c;
		setLayout(new GridBagLayout());
		constraints = new GridBagConstraints();
		if(myID == 0) {
			panels = new ArrayList<CustomPanel>();
		} else {
			border = BorderFactory.createLineBorder(Color.WHITE);
		}
	}

	public void addContent(Content c) {
		content = c;
	}

	public GridBagConstraints getConstraints() {
		return constraints;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public int getID() {
		return myID;
	}

	public Content getContent() {
		return content;
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(width, height);
	}

	// Update graphics for this and all children
	public void display() {
		setSize(getPreferredSize());
		setVisible(true);
		if(myID == 0) {
			// contentPanel
			for(CustomPanel p : panels) {
				p.display();
			}
		} else {
			if((content != null) && ((button != null) || (textPane != null))) {
				String text;
				if(myID == 1) {
					Color color = content.getColor();
					text = content.getText("ball");
					button.setFont(new Font("Helvetica", Font.BOLD, 420));
					// System.out.println(text);
					button.setText(text);
					button.setOpaque(true);
					button.setContentAreaFilled(true);
					button.setBorderPainted(true);
					button.setForeground(color);
					button.setVisible(true);
				} else if(myID == 2) {
					if(textPane == null) {
						remove(button);
						int twidth = (int) getPreferredSize().getWidth();
						int theight = (int) getPreferredSize().getHeight();
						textPane = new JTextPane() {
							@Override
							public Dimension getPreferredSize() {
								return new Dimension(twidth,theight);
							};
						};
						add(textPane);
					}
					textPane.setText("");
					if(content instanceof Bingo) {
						Utilities.addHistoryText(textPane, (Bingo) content, 20);
					}
					// text = content.getText("history");
					// Utilities.addStyledText(textPane, text, 0, "Courier New", 20, Color.BLACK, false, false, false);
				} else {
					text = content.getText("info");
					button.setFont(new Font("Helvetica", Font.ITALIC, 48));
					// System.out.println(text);
					button.setText(text);
					button.setOpaque(true);
					button.setContentAreaFilled(true);
					button.setBorderPainted(true);
					// button.setForeground(color);
					button.setVisible(true);
				}
				// Color color = content.getColor();
				// Color buttonColor = Utilities.contrastingColor(color);
				// setBackground(color);


			}

			// if(button != null) {
			// 	if(content instanceof MenuButton) {
			// 		// Menu buttons have text
			// 		button.setOpaque(true);
			// 		button.setContentAreaFilled(true);
			// 		button.setBorderPainted(true);
			// 		button.setForeground(buttonColor);
			// 		button.setVisible(true);
			// 	} else {
			// 		// ...generic Content doesn't
			// 		setBorder(border);
			// 		button.setOpaque(false);
			// 		button.setContentAreaFilled(false);
			// 		button.setBorderPainted(false);
			// 		button.setForeground(buttonColor);
			// 		button.setVisible(true);
			// 	}
			// }
		}
	}

	// Add and attach a sub-panel. x,y are relative positions not pixels
	public void addPanel(int x, int y, int gridwidth, int gridheight, int w, int h, CustomFrame f, int id, Content c) {
		CustomPanel newPanel = new CustomPanel(w, h, f, id, c);
		constraints.gridx = x;
		constraints.gridy = y;
		constraints.gridwidth = gridwidth;
		constraints.gridheight = gridheight;
		add(newPanel, constraints);
		panels.add(newPanel);
	}

	public void addPanel(int x, int y, int w, int h, CustomFrame f, int id, Content c) {
		CustomPanel newPanel = new CustomPanel(w, h, f, id, c);
		constraints.gridx = x;
		constraints.gridy = y;
		add(newPanel, constraints);
		panels.add(newPanel);
	}

	public ArrayList<CustomPanel> getPanels() {
		return panels;
	}

	public CustomPanel getPanel(int id) {
		assert ((id >= 1) && (id <= panels.size()) && (myID == 0));
		return panels.get(id - 1); // contentPanel has id=0, first inner panel has id=1
	}

	// Add a button and give it a listener for input
	public void addButton(String text) {
		assert (myID != 0);
		// if(content instanceof MenuButton) {
			button = new JButton(text);
		// } else {
		// 	button = new JButton();
		// }
		button.setFocusable(false);
		if(content != null) {
			button.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e){
					content.processInput();
	      }
	    });
		}
		button.setPreferredSize(getPreferredSize());
		add(button);
	}

}

class GUI implements Handler {

	CustomFrame window;
	// Bingo bingo;

	public GUI() {
		Color pastelBlue = Utilities.makeColor(175.0, 218.0, 240.0);

		CustomFrame frame = new CustomFrame(500, 250, "Choose numbers", false);
		CustomPanel panel = frame.getContentPanel();
		GridBagConstraints constraints = panel.getConstraints();

		// Make first slider for min value
		JPanel sliderPanel = new JPanel() {
			@Override
			public Dimension getPreferredSize() {
				return new Dimension(500,100);
			};
		};
		constraints.gridx = 0;
		constraints.gridy = 0;
		panel.add(sliderPanel, constraints);

		JSlider slider = new JSlider(0, 10) {
			@Override
			public Dimension getPreferredSize() {
				return new Dimension(500,50);
			};
		};
		slider.setFont(new Font("Helvetica", Font.BOLD, 16));
		// slider.setMinorTickSpacing(1);
		slider.setMajorTickSpacing(1);
		slider.setSnapToTicks(true);
		slider.setPaintTicks(true);
		slider.setPaintLabels(true);
		slider.setValue(1);

		slider.setFocusable(false);

		sliderPanel.add(slider);

		JTextPane sliderHelp = new JTextPane() {
			@Override
			public Dimension getPreferredSize() {
				return new Dimension(500,50);
			};
		};
		sliderHelp.setEditable(false);

		String text = "Smallest number";

		String font = "Helvetica";
		int fontSize = 16;
		Utilities.addStyledText(sliderHelp, text, 0, font, fontSize, Color.BLACK, true, false, true);

		sliderPanel.add(sliderHelp);

		// Make seconds slider for max
		JPanel sliderPanel2 = new JPanel() {
			@Override
			public Dimension getPreferredSize() {
				return new Dimension(500,100);
			};
		};
		constraints.gridx = 0;
		constraints.gridy = 1;
		panel.add(sliderPanel2, constraints);

		JSlider slider2 = new JSlider(10, 120) {
			@Override
			public Dimension getPreferredSize() {
				return new Dimension(500,50);
			};
		};
		slider2.setFont(new Font("Helvetica", Font.BOLD, 16));
		slider2.setMinorTickSpacing(1);
		slider2.setMajorTickSpacing(10);
		slider2.setSnapToTicks(true);
		slider2.setPaintTicks(true);
		slider2.setPaintLabels(true);
		slider2.setValue(90);

		slider2.setFocusable(false);

		sliderPanel2.add(slider2);

		JTextPane sliderHelp2 = new JTextPane() {
			@Override
			public Dimension getPreferredSize() {
				return new Dimension(500,50);
			};
		};
		sliderHelp2.setEditable(false);

		String text2 = "Biggest number";

		Utilities.addStyledText(sliderHelp2, text2, 0, font, fontSize, Color.BLACK, true, false, true);

		sliderPanel2.add(sliderHelp2);

		// Make bottom panel for start button
		JPanel confirmPanel = new JPanel() {
			@Override
			public Dimension getPreferredSize() {
				return new Dimension(500,50);
			};
		};
		constraints.gridx = 0;
		constraints.gridy = 3;
		panel.add(confirmPanel, constraints);

		JButton button = new JButton("Start") {
			@Override
			public Dimension getPreferredSize() {
				return new Dimension(200,40);
			};
		};
		button.setFont(new Font("Helvetica", Font.BOLD, 24));
		button.setFocusable(false);

		GUI ptr = this;

		button.setActionCommand("Start");
		// ActionListener creates grid window with current slider settings in
		// a new thread
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				(new Thread(new Runnable(){
   				public void run(){
       			ptr.run(slider.getValue(), slider2.getValue());
   				}
				})).start();

				frame.close();

      }
		});

		confirmPanel.add(button);

		// Enforce uniform background colour
		sliderPanel.setBackground(pastelBlue);
		sliderPanel2.setBackground(pastelBlue);
		sliderHelp.setBackground(pastelBlue);
		sliderHelp2.setBackground(pastelBlue);
		confirmPanel.setBackground(pastelBlue);

		frame.display();

	}

	public void run(int min, int max) {
		assert(min < max);
		// System.out.println("Running, min="+Integer.toString(min)+", max="+Integer.toString(max));

		Bingo bingo = new Bingo(min, max, this);

		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int w = (int) screenSize.getWidth();
		int h = (int) screenSize.getHeight();
		int shortDim = (w < h) ? w : h;
		double scale = 0.9;
		int dim = (int)(((double) shortDim) * scale);
		int bigPanelHeight = (int) ((double) dim * 0.6);
		int smallPanelHeight = (int) ((double) dim * 0.175);
		// int windowDimension = (int) ((float) screenSize.getHeight()*0.9);

		window = new CustomFrame(dim, dim, "Bingo", true);

		CustomPanel panel = window.getContentPanel();
		GridBagConstraints constraints = panel.getConstraints();

		int id1 = window.addPanel(0, 0, dim, bigPanelHeight, bingo);
		window.addButton(id1, "");

		// CustomPanel bigPanel = new CustomPanel(dim, bigPanelHeight, window, 1, bingo);
		// // {
		// // 	@Override
		// // 	public Dimension getPreferredSize() {
		// // 		return new Dimension(dim,bigPanelHeight);
		// // 	};
		// // };
		// constraints.gridx = 0;
		// constraints.gridy = 0;
		// panel.add(bigPanel, constraints);
		// bigPanel.setBackground(Color.RED);
		//
		// // bigPanel.addContent(bingo);
		// bigPanel.addButton("");

		int id2 = window.addPanel(0, 1, dim, smallPanelHeight, bingo);
		window.addButton(id2, "");
		// window.addButton(id1, "");

		// JPanel historyPanel = new JPanel() {
		// 	@Override
		// 	public Dimension getPreferredSize() {
		// 		return new Dimension(dim,smallPanelHeight);
		// 	};
		// };
		// constraints.gridx = 0;
		// constraints.gridy = 1;
		// panel.add(historyPanel, constraints);
		// historyPanel.setBackground(Color.BLUE);

		int id3 = window.addPanel(0, 2, dim, smallPanelHeight, bingo);
		window.addButton(id3, "");

		// JPanel infoPanel = new JPanel() {
		// 	@Override
		// 	public Dimension getPreferredSize() {
		// 		return new Dimension(dim,smallPanelHeight);
		// 	};
		// };
		// constraints.gridx = 0;
		// constraints.gridy = 2;
		// panel.add(infoPanel, constraints);
		// infoPanel.setBackground(Color.GREEN);

		window.display();

	}


	public void processInput(Content c) {
		// if(c instanceof Bingo) {
		// 	Bingo b = (Bingo) c;
		// }
		updateDisplay();
	}

	public void updateDisplay() {
		window.display();
	}

}

class Bingo implements Content {
	private ArrayList<Integer> balls;
	private ArrayList<Integer> history;
	private Random randomGen;
	private int lastDrawn;
	private String ballText;
	private String historyText;
	private String infoText;
	private Handler handler;
	private int initialCount;

	public Bingo(int min, int max, Handler h) {
		this.balls = new ArrayList<Integer>();
		this.history = new ArrayList<Integer>();
		for(int i=min; i<=max; i++) {
			balls.add(i);
		}
		initialCount = (max - min) + 1; // Inclusive
		lastDrawn = -2;
		genBallText();
		genInfoText(balls.size());
		genHistoryText();
		randomGen = new Random();
		handler = h;
	}

	private void genInfoText(int currentBalls) {
		infoText = new String("Drawn: "+Integer.toString(initialCount - currentBalls)+"\t\tRemaining: "+Integer.toString(currentBalls));
	}

	private void genBallText() {
		switch(lastDrawn) {
			case -2:
				ballText = "";
				return;
			case -1:
				ballText = "-";
				return;
			default:
				ballText = Integer.toString(lastDrawn);
		}
	}

	private void genHistoryText() {
		Collections.sort(history);
		StringBuilder b = new StringBuilder();
		b.append("History:\n");
		for(Integer i : history) {
			b.append(String.format("%2d", i));
			b.append(" ");
		}
		historyText = b.toString();
	}

	public ArrayList<Integer> getHistory() {
		return history;
	}

	public boolean isEmpty() {
		return (balls.size() == 0);
	}

	public int draw() {
		if(isEmpty()) {
			lastDrawn = -1;
			return lastDrawn;
		} else {
			int i = randomGen.nextInt(balls.size());
			int ball = balls.remove(i);
			lastDrawn = ball;
			history.add(ball);
			return ball;
		}
	}

	public String getText(String mode) {
		if(mode == "ball") {
			return ballText;
		} else if(mode == "history") {
			return historyText;
		} else if(mode == "info") {
			return infoText;
		}
		return "";
	}

	public Color getColor() {
		if(lastDrawn < 0) {
			return Color.BLACK;
		}
		int tens = (lastDrawn - 1) / 10;
		return getColor(tens);
	}

	public Color getColor(int x) {
		switch(x) {
			case 0:
				return Color.RED;
			case 1:
				return Color.ORANGE;
			case 2:
				return Utilities.makeColor(181, 181, 5);
			case 3:
				return Color.GREEN;
			case 4:
				return Color.CYAN;
			case 5:
				return Color.BLUE;
			case 6:
				return Color.MAGENTA;
			case 7:
				return Color.PINK;
			case 8:
				return Color.GRAY;
			case 9:
				return Color.DARK_GRAY;
			case 10:
				return Color.LIGHT_GRAY;
			default:
				return Color.BLACK;
		}
	}

	public void processInput() {
		draw();
		genBallText();
		genInfoText(balls.size());
		genHistoryText();

		handler.processInput(this);
	}
}


public class RNG {

	public static void main(String[] args) {
		GUI g = new GUI();
	}
}
