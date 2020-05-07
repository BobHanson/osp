package test;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.WindowConstants;

import swingjs.api.js.HTML5Video;

/**
 * Test of <video> tag.
 * 
 * @author RM
 *
 */
public class Test_Video {

	public static void main(String[] args) {
		new Test_Video();
	}
	private HTML5Video jsvideo;

	public Test_Video() {
		JFrame main = new JFrame();
		main.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		ImageIcon icon;

		
		
		boolean testRemote = false;// setting this true requires Java 9
		boolean isJS = /** @j2sNative true || */
				false;
		boolean asBytes = true;

		isDiscrete = true;
		
		
		
		String video = (
		// "test/jmoljana_960x540.png");
		// fails"test/EnergyTransfer.mp4"
		//"test/jmoljana.mp4",
		"test/duet.mp4");
		
		
//		URL videoURL;
//		try {
//			videoURL = new URL("https://chemapps.stolaf.edu/test/duet.mp4");
//		} catch (MalformedURLException e1) {
//			videoURL = null;
//		}
		
		
		if (!isJS) {
			icon = new ImageIcon("src/test/video_image.png");
		} else if (asBytes) {
			try {
				byte[] bytes;
//				if (testRemote) {
//					bytes = videoURL.openStream().readAllBytes();// Argh! Java 9!
//				} else {
					bytes = Files.readAllBytes(new File(video).toPath());
//				}
				icon = new ImageIcon(bytes, "jsvideo");
			} catch (IOException e1) {
				icon = null;
			}
//		} else if (testRemote) {
//			icon = new ImageIcon(videoURL, "jsvideo");
		} else {
			icon = new ImageIcon(video, "jsvideo");
		}
		JLabel label = new JLabel(icon);
		
		int w = 1920 / 4;
		Dimension dim = new Dimension(w, w * 9 / 16);
		
		addLayerPane(label, main, dim);
		jsvideo = (HTML5Video) label.getClientProperty("jsvideo");
		HTML5Video.addActionListener(jsvideo, new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				String event = e.getActionCommand();
				Object[] sources = (Object[]) e.getSource();
				HTML5Video target = (HTML5Video) sources[0];
				Object jsevent = sources[1];
				System.out.println(event + " " + target + " " + jsevent);
			}
			 
		});

//		try {
//			jsvideo.play();
//		} catch (Throwable e) {
//			System.out.println("couldn't play (yet)" + e);
//		}
//
		addControls(label, main);
		main.pack();
		main.setVisible(true);

		HTML5Video.setProperty(jsvideo, "currentTime", 0);

		showAllProperties();
	}
	
	private void showProperty(String key) {
		System.out.println(key +"=" + HTML5Video.getProperty(jsvideo, key));
	}
	
	
	private void showAllProperties() {
		for (int i = 0; i < allprops.length; i++)
			showProperty(allprops[i]);		
	}
	
	private static String[] allprops = {
			"audioTracks",//
			"autoplay",//
			"buffered",//
			"controller",//
			"controls",//
			"controlsList",//
			"crossOrigin",//
			"currentSrc",//
			"currentTime",//
			"defaultMuted",//
			"defaultPlaybackRate",//
			"disableRemotePlayback",//
			"duration",//
			"ended",//
			"error",//
			"loop",//
			"mediaGroup",//
			"mediaKeys",//
			"mozAudioCaptured",//
			"mozFragmentEnd",//
			"mozFrameBufferLength",//
			"mozSampleRate",//
			"muted",//
			"networkState",//
			"paused",//
			"playbackRate",//
			"played",//
			"preload",//
			"preservesPitch",//
			"readyState",//
			"seekable",//
			"seeking",//
			"sinkId",//
			"src",//
			"srcObject",//
			"textTracks",//
			"videoTracks",//
			"volume",//
			"initialTime",//
			"mozChannels",//

	};
	
	
	double vt, vt0;
	long t0 = 0;
	double duration;
	int totalTime;
	int delay = 33;
	Timer timer;
	Object[] playListener;
	
	private void playVideoDiscretely(HTML5Video v) {
		vt0 = vt = HTML5Video.getCurrentTime(v);
		t0 = System.currentTimeMillis() - (int) (vt * 1000);
		duration = ((Double) HTML5Video.getProperty(v, "duration")).doubleValue();
		if (vt >= duration) {
			vt = 0;
			playing = false;
		}
		
		ActionListener listener = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				System.out.print("\0");
				vt += delay/1000.0;
				System.out.println("setting time to " + vt);
				HTML5Video.setCurrentTime(v, vt);
				long dt = System.currentTimeMillis() - t0;
				double dtv = vt-vt0;
				System.out.println((dtv - dt/1000.) + 
						" " + HTML5Video.getProperty(jsvideo, "paused") +
						" " + HTML5Video.getProperty(jsvideo, "seeking"));
				if (vt >= duration) {
					playing = false;
					removePlayListener(v);
				}
			}
			
		};
		playListener = HTML5Video.addActionListener(v, listener, "canplaythrough");
		t0 = System.currentTimeMillis();
		listener.actionPerformed(null);
	}
	
	protected void removePlayListener(HTML5Video v) {
		if (playListener != null)
			HTML5Video.removeActionListener(v, playListener);
		playListener = null;
	}
	
	private JLayeredPane layerPane;
	private JPanel drawLayer;

	private List<Point> points = new ArrayList<Point>();


	protected boolean isDiscrete;


	protected boolean playing;

	
	private void addLayerPane(JLabel label, JFrame main, Dimension dim) {
		label.setPreferredSize(dim);
		label.setMinimumSize(dim);
		label.setMaximumSize(dim);
		label.setBounds(0,0, dim.width, dim.height);

		
		drawLayer = new JPanel() {
			@Override
			public void paintComponent(Graphics g) {
				super.paintComponent(g); // takes care of the background clearing
				g.setColor(Color.red);
				for (int i = 0, n = points.size(); i < n; i++) {
					Point p = points.get(i);
					g.drawLine(p.x-5, p.y, p.x+5, p.y);
					g.drawLine(p.x, p.y-5, p.x, p.y+5);
				}
			}
		};
		drawLayer.setBounds(0,0, dim.width, dim.height);
		drawLayer.setOpaque(false);
		//drawLayer.setBackground(new Color(255,255,255,0));
		// note -- setting this to opaque/{255 255 255 0} does not quite work as expected. 
		// rather than paint a white background over the image in Java, Java paints
		// the default background color. This of course makes no sense; I consider it a Java bug.
		// 
		drawLayer.putClientProperty("jscanvas", "true");

		drawLayer.addMouseListener(new MouseAdapter() {
			
			@Override
			public void mouseClicked(MouseEvent e) {
				System.out.println("Draw layer mouse click " + e.getX() + " " + e.getY());
				points.add(new Point(e.getX(), e.getY()));
				drawLayer.repaint();
			}
		});

		layerPane = new JLayeredPane();
		layerPane.setMinimumSize(dim);
		layerPane.setPreferredSize(dim);
		layerPane.setMaximumSize(dim);

		layerPane.add(drawLayer, JLayeredPane.PALETTE_LAYER);
		layerPane.add(label, JLayeredPane.DEFAULT_LAYER);

		JPanel p = new JPanel();
		p.add(layerPane, BorderLayout.CENTER);
		main.add(p, BorderLayout.CENTER);
		System.out.println(main.getLayeredPane());
		System.out.println(layerPane);
	}

	private void addControls(JLabel label, JFrame main) {

		JCheckBox discrete = new JCheckBox("discrete");

		JButton play = new JButton("play");
		play.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (playing)
					return;
				isDiscrete = discrete.isSelected();
				try {
					playing = true;
					if (isDiscrete) {
						playVideoDiscretely(jsvideo);
					} else {
						jsvideo.play();
					}
				} catch (Throwable e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}

		});
		JButton pause = new JButton("pause");
		pause.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					playing = false;
					duration = 0; // turns off timer 
					jsvideo.pause();
					removePlayListener(jsvideo);
				} catch (Throwable e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}

		});
		
		JButton clear = new JButton("clear");
		clear.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				points.clear();
				drawLayer.repaint();
			}

		});
	
		JButton undo = new JButton("undo");
		undo.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (points.size() > 0)
					points.remove(points.size() - 1);
				drawLayer.repaint();
			}

		});
	
		JButton show = new JButton("show");
		show.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				showAllProperties();
			}

		});
	

		JPanel controls = new JPanel();
		controls.add(new JLabel("click to mark     "));
		controls.add(discrete);
		controls.add(play);
		controls.add(pause);
		controls.add(undo);
		controls.add(clear);
		controls.add(show);
		main.add(controls, BorderLayout.SOUTH);
	}
	
//	Event Name 	Fired When
//	audioprocess 	The input buffer of a ScriptProcessorNode is ready to be processed.
//	canplay 	The browser can play the media, but estimates that not enough data has been loaded to play the media up to its end without having to stop for further buffering of content.
//	canplaythrough 	The browser estimates it can play the media up to its end without stopping for content buffering.
//	complete 	The rendering of an OfflineAudioContext is terminated.
//	durationchange 	The duration attribute has been updated.
//	emptied 	The media has become empty; for example, this event is sent if the media has already been loaded (or partially loaded), and the load() method is called to reload it.
//	ended 	Playback has stopped because the end of the media was reached.
//	loadeddata 	The first frame of the media has finished loading.
//	loadedmetadata 	The metadata has been loaded.
//	pause 	Playback has been paused.
//	play 	Playback has begun.
//	playing 	Playback is ready to start after having been paused or delayed due to lack of data.
//	progress 	Fired periodically as the browser loads a resource.
//	ratechange 	The playback rate has changed.
//	seeked 	A seek operation completed.
//	seeking 	A seek operation began.
//	stalled 	The user agent is trying to fetch media data, but data is unexpectedly not forthcoming.
//	suspend 	Media data loading has been suspended.
//	timeupdate 	The time indicated by the currentTimeattribute has been updated.
//	volumechange 	The volume has changed.
//	waiting 	Playback has stopped because of a temporary lack of data


}
