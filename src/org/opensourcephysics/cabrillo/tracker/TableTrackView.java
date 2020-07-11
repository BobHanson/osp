/*
 * The tracker package defines a set of video/image analysis tools
 * built on the Open Source Physics framework by Wolfgang Christian.
 *
 * Copyright (c) 2019  Douglas Brown
 *
 * Tracker is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Tracker is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Tracker; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston MA 02111-1307 USA
 * or view the license online at <http://www.gnu.org/copyleft/gpl.html>
 *
 * For additional Tracker information and documentation, please see
 * <http://physlets.org/tracker/>.
 */
package org.opensourcephysics.cabrillo.tracker;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Enumeration;
import java.util.EventObject;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.AbstractCellEditor;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

import org.opensourcephysics.controls.OSPLog;
import org.opensourcephysics.controls.XMLControlElement;
import org.opensourcephysics.display.DataFunction;
import org.opensourcephysics.display.DataTable;
import org.opensourcephysics.display.Dataset;
import org.opensourcephysics.display.DatasetManager;
import org.opensourcephysics.display.DisplayRes;
import org.opensourcephysics.display.MeasuredImage;
import org.opensourcephysics.display.OSPFrame;
import org.opensourcephysics.display.OSPRuntime;
import org.opensourcephysics.display.ResizableIcon;
import org.opensourcephysics.display.TeXParser;
import org.opensourcephysics.media.core.NumberField;
import org.opensourcephysics.media.core.VideoClip;
import org.opensourcephysics.tools.DataRefreshTool;
import org.opensourcephysics.tools.DataTool;
import org.opensourcephysics.tools.FontSizer;
import org.opensourcephysics.tools.FunctionPanel;
import org.opensourcephysics.tools.FunctionTool;
import org.opensourcephysics.tools.LocalJob;
import org.opensourcephysics.tools.ToolsRes;

/**
 * A JScrollPane that presents a table view of a track on a TrackerPanel. 
 * The class maintains the table as well as the associated column views JDialog.
 *
 * @author Douglas Brown
 * @author John Welch
 */
@SuppressWarnings("serial")
public class TableTrackView extends TrackView {

	// static fields
	static final String DEFINED_AS = ": "; //$NON-NLS-1$
	static final Icon skipsOffIcon = new ResizableIcon(Tracker.getClassResource("resources/images/skips_on.gif")); //$NON-NLS-1$
	static final Icon skipsOnIcon = new ResizableIcon(Tracker.getClassResource("resources/images/skips_off.gif")); //$NON-NLS-1$

	// data model

	/**
	 * DataManager for all track data -- ALL columns
	 */
	private DatasetManager trackDataManager;

	/**
	 * DataManager for all table data -- just the VISIBLE columns
	 */
	private DatasetManager dataTableManager;

	// internal column model
	
	/**
	 * primary indicator of visibility; shared with TableTView.Loader
	 * 
	 */
	BitSet bsCheckBoxes = new BitSet();
	
	private int colCount;
	private int datasetCount;
	private Map<String, Integer> htNames;
	private String[] aNames;

	/**
	 * set to false during loading
	 */
	private boolean refreshing = true;
	
	public void setRefreshing(boolean b) {
		refreshing = b;		
	}

	private boolean highlightVisible = true;
	private boolean refreshed = false;
	private int leadCol;

	final private Font font = new JTextField().getFont();
	final private BitSet highlightFrames = new BitSet();
	final private BitSet highlightRows = new BitSet(); 
	final private ArrayList<String> textColumnNames = new ArrayList<String>();
	final protected Set<String> textColumnsVisible = new TreeSet<String>();
	final private Map<String, TableCellRenderer> degreeRenderers = new HashMap<String, TableCellRenderer>();
	
	/**
	 * used when sorting
	 */
	final private TreeSet<Double> selectedIndepVarValues = new TreeSet<Double>();
	

	// GUI

	/**
	 * initially false; set to true once createGUI() has run
	 */
	private boolean haveMenuItems;
		
	/**
	 * the JTable
	 */
	protected TrackDataTable dataTable;
	private TextColumnTableModel textColumnModel;
	private TextColumnEditor textColumnEditor;

	/**
	 * for super.toolbarComponents 
	 */
	private JButton columnsDialogButton, gapsButton;
	
	private ColumnsDialog columnsDialog;


	// popup GUI (lazy)

	private JPopupMenu popup;
	private JMenu textColumnMenu, deleteTextColumnMenu, renameTextColumnMenu;
	private JMenuItem createTextColumnItem;
	private JMenuItem dataToolItem, dataBuilderItem, deleteDataFunctionItem;
	private JMenu numberMenu;
	private JMenuItem goToFrameItem, formatDialogItem, setUnitsItem, showUnitsItem;
	private JMenu copyDataMenu;
	private JMenuItem copyDataRawItem, copyDataFormattedItem;
	private JMenu setDelimiterMenu;
	private JMenuItem copyImageItem, snapshotItem, printItem, helpItem;


	/**
	 * Constructs a TrackTableView of the specified track on the specified tracker
	 * panel.
	 *
	 * @param track the track
	 * @param panel the tracker panel
	 * @param view  the TableTView that will display this
	 */
	public TableTrackView(TTrack track, TrackerPanel panel, TableTView view) {
		super(track, panel, view, TView.VIEW_TABLE);
		track.addPropertyChangeListener(TTrack.PROPERTY_TTRACK_TEXTCOLUMN, this);
		textColumnNames.addAll(track.getTextColumnNames());
		// create the DataTable with two OSPTableModels
		// (1) our DatasetManager
		// (2) a text column
		textColumnModel = new TextColumnTableModel();
		textColumnEditor = new TextColumnEditor();
		dataTable = new TrackDataTable();
		trackDataManager = track.getData(trackerPanel);
		dataTableManager = new DatasetManager();
		dataTableManager.setXPointsLinked(true);
		dataTable.add(dataTableManager);
		dataTable.add(textColumnModel);
		setViewportView(dataTable);
		dataTable.setPreferredScrollableViewportSize(new Dimension(160, 200));
		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				dataTable.clearSelection();
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				dataTable.requestFocusInWindow();
			}
		});
		dataTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				dataTable.requestFocusInWindow();
			}
		});
		// add key listener to start editing text column cells with space key
		dataTable.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_SPACE) {
					int row = dataTable.getSelectedRow();
					int col = dataTable.getSelectedColumn();
					dataTable.editCellAt(row, col);
					textColumnEditor.field.selectAll();
					Runnable runner = new Runnable() {
						@Override
						public synchronized void run() {
							textColumnEditor.field.requestFocusInWindow();
						}
					};
					SwingUtilities.invokeLater(runner);
				}
			}

		});

		ListSelectionModel selectionModel = dataTable.getSelectionModel();
		selectionModel.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				selectedIndepVarValues.clear();
				int[] rows = dataTable.getSelectedRows(); // selected view rows
				for (int i = 0; i < rows.length; i++) {
					double val = getIndepVarValueAtRow(rows[i]);
					if (!Double.isNaN(val))
						selectedIndepVarValues.add(val);
				}
			}

		});
		setToolTipText(ToolsRes.getString("DataToolTab.Scroller.Tooltip")); //$NON-NLS-1$
		highlightVisible = !(track instanceof LineProfile);
		setNameMaps();

		// create the GUI
		createGUI();
		// show the track-specified default columns
		boolean useDefault = true;
		for (int i = 0; i < 4; i++) {
			String col = (String) track.getProperty("tableVar" + i); //$NON-NLS-1$
			if (col != null) {
				setVisible(Integer.parseInt(col), true);
				useDefault = false;
			}
		}
		if (useDefault) {
			// show the default columns
			setVisible(0, true);
			setVisible(1, true);
		}
		// set the default number formats, if any
		Class<? extends TTrack> trackType = NumberFormatDialog.getTrackType(track);
		TreeMap<String, String> patterns = trackerPanel.getFormatPatterns(trackType);
		DataTable table = getDataTable();
		for (String name : patterns.keySet()) {
			table.setFormatPattern(name, patterns.get(name));
		}
	}

	private void setNameMaps() {
		htNames = new LinkedHashMap<>();
		ArrayList<Dataset> sets = trackDataManager.getDatasets();
		ArrayList<String> textSet = textColumnNames;
		datasetCount = sets.size();
		colCount = datasetCount + textSet.size();

		aNames = new String[colCount];
		for (int i = 0; i < datasetCount; i++) {
			String name = sets.get(i).getYColumnName();
			aNames[i] = name;
			htNames.put(name, Integer.valueOf(i));
		}
		for (int i = 0, n = textSet.size(); i < n; i++) {
			String name = textSet.get(i);
			aNames[datasetCount + i] = name;
			htNames.put(name, Integer.valueOf(datasetCount + i));
		}

	}

	@Override
	public void refresh(int frameNumber, int mode) {

		// main entry point for a new or revised track -- from TrackChooserTView

//		OSPLog.debug("TableTrackView.refresh " + Integer.toHexString(mode));

		forceRefresh = true; // for now, at least

		if (!forceRefresh && !isRefreshEnabled() || !viewParent.isViewPaneVisible())
			return;

		forceRefresh = false;
		if (Tracker.timeLogEnabled)
			Tracker.logTime(getClass().getSimpleName() + hashCode() + " refresh " + frameNumber); //$NON-NLS-1$
		dataTable.clearSelection();
		TTrack track = getTrack();
		OSPLog.debug("TableTrackView.refresh " + Integer.toHexString(mode) + " track=" + track);
		try {
			trackDataManager = track.getData(trackerPanel);
			// copy datasets into table data based on checkbox states

			ArrayList<Dataset> datasets = trackDataManager.getDatasets();
			int count = datasets.size();
			dataTable.setUnits(datasets.get(0).getXColumnName(), "", track.getDataDescription(0)); //$NON-NLS-1$
			boolean degrees = trackerPanel.getTFrame() != null && !trackerPanel.getTFrame().anglesInRadians;

			dataTableManager.clear();
			int colCount = 0;
			for (int i = bsCheckBoxes.nextSetBit(0); i >= 0 && i < count; i = bsCheckBoxes.nextSetBit(i + 1)) {
				Dataset ds = datasets.get(i);
				String xTitle = ds.getXColumnName();
				String yTitle = ds.getYColumnName();
				double[] yPoints = ds.getYPoints();
				if (setUnitsAndTooltip(yTitle, track.getDataDescription(i + 1), degrees)) {
					// convert values from radians to degrees
					for (int k = 0; k < yPoints.length; k++) {
						if (!Double.isNaN(yPoints[k])) {
							yPoints[k] *= 180 / Math.PI;
						}
					}
				}
				Dataset local = dataTableManager.getDataset(colCount++);
				local.append(ds.getXPointsRaw(), yPoints, ds.getIndex());
				local.setXYColumnNames(xTitle, yTitle);
				local.setYColumnVisible(true);
			}
			for (int i = colCount; i < dataTableManager.getDatasets().size(); i++) {
				dataTableManager.setYColumnVisible(i, false);
			}
			if (colCount == 0) {
				// show independent variable
				Dataset in = datasets.get(0);
				String xTitle = in.getXColumnName();
				Dataset local = dataTableManager.getDataset(colCount++);
				double[] x = in.getXPointsRaw();
				local.append(x, x, in.getIndex());
				local.setXYColumnNames(xTitle, xTitle);
				local.setYColumnVisible(false);
				colCount++;
			}
			dataTable.refreshColumnModel();
			dataTable.refreshTable(mode);
			refreshed = true;
		} catch (Exception e) {
			OSPLog.debug("TableTrackView exception " + e);
		}
		// set the highlighted rows
		highlightFrames.clear();
		if (trackerPanel.selectedSteps.size() > 0) {
			for (Step step : trackerPanel.selectedSteps) {
				if (step.getTrack() != this.getTrack())
					continue;
				highlightFrames.set(step.getFrameNumber());
			}
		} else {
			highlightFrames.set(frameNumber);
		}
		setHighlighted(highlightFrames);
	}

	private boolean setUnitsAndTooltip(String yTitle, String root, boolean degrees) {
		boolean yIsAngle = yTitle.startsWith(Tracker.THETA) || yTitle.startsWith(Tracker.OMEGA)
				|| yTitle.startsWith(Tracker.ALPHA);
		String tooltip = root + " "; //$NON-NLS-1$
		String units = ""; //$NON-NLS-1$
		if (yIsAngle) { // angle columns
			if (yTitle.startsWith(Tracker.THETA)) {
				if (degrees) {
					units = Tracker.DEGREES;
					tooltip += TrackerRes.getString("TableTrackView.Degrees.Tooltip"); //$NON-NLS-1$
				} else {
					tooltip += TrackerRes.getString("TableTrackView.Radians.Tooltip"); //$NON-NLS-1$
				}
			} else if (yTitle.startsWith(Tracker.OMEGA)) {
				if (degrees) {
					tooltip += TrackerRes.getString("TableTrackView.DegreesPerSecond.Tooltip"); //$NON-NLS-1$
				} else {
					tooltip += TrackerRes.getString("TableTrackView.RadiansPerSecond.Tooltip"); //$NON-NLS-1$
				}
			} else if (yTitle.startsWith(Tracker.ALPHA)) {
				if (degrees) {
					tooltip += TrackerRes.getString("TableTrackView.DegreesPerSecondSquared.Tooltip"); //$NON-NLS-1$
				} else {
					tooltip += TrackerRes.getString("TableTrackView.RadiansPerSecondSquared.Tooltip"); //$NON-NLS-1$
				}
			}
			TableCellRenderer precisionRenderer = dataTable.getPrecisionRenderer(yTitle);
			if (degrees) {
				// set default degrees precision
				if (precisionRenderer == null) {
					dataTable.setFormatPattern(yTitle, NumberField.DECIMAL_1_PATTERN);
					degreeRenderers.put(yTitle, dataTable.getPrecisionRenderer(yTitle));
				}
			} else if (precisionRenderer != null) { // radians display
				if (precisionRenderer == degreeRenderers.get(yTitle)) {
					dataTable.setFormatPattern(yTitle, null);
					degreeRenderers.remove(yTitle);
				}
			}
		}
		if ("".equals(tooltip.trim())) //$NON-NLS-1$
			tooltip = ""; //$NON-NLS-1$
		dataTable.setUnits(yTitle, units, tooltip);
		return yIsAngle && degrees;
	}

	/**
	 * Refreshes the GUI.
	 */
	@Override
	void refreshGUI() {
		TTrack track = getTrack();
		columnsDialogButton.setText(TrackerRes.getString("TableTrackView.Button.SelectTableData")); //$NON-NLS-1$
		columnsDialogButton.setToolTipText(TrackerRes.getString("TableTrackView.Button.SelectTableData.ToolTip")); //$NON-NLS-1$
//  	skippedFramesButton.setText(skippedFramesButton.isSelected()?
//  		TrackerRes.getString("TableTrackView.Button.SkippedFrames.On"): //$NON-NLS-1$
//    		TrackerRes.getString("TableTrackView.Button.SkippedFrames.Off")); //$NON-NLS-1$
		gapsButton.setToolTipText(TrackerRes.getString("TableTrackView.Button.SkippedFrames.ToolTip")); //$NON-NLS-1$
//    track.dataValid = false; // triggers data refresh
		trackDataManager = track.getData(trackerPanel);
		// refreshColumnCheckboxes();
		refresh(trackerPanel.getFrameNumber(), DataTable.MODE_TRACK_REFRESH);

		if (columnsDialog == null || !columnsDialog.isVisible())
			return;
		columnsDialog.refreshGUI();

	}

	/**
	 * Gets the datatable.
	 * 
	 * For AutoTracker, NumberformatDialog
	 *
	 * @return the datatable
	 */
	public TrackDataTable getDataTable() {
		return dataTable;
	}

	/**
	 * Gets the toolbar components
	 *
	 * @return an ArrayList of components to be added to a toolbar
	 */
	@Override
	public ArrayList<Component> getToolBarComponents() {
		toolbarComponents.remove(gapsButton);
		// determine if track has gaps
		TTrack track = getTrack();
		if (track instanceof PointMass) {
			PointMass p = (PointMass) track;
			if (p.getGapCount() > 0 || p.skippedSteps.size() > 0) {
				toolbarComponents.add(gapsButton);
			}
		}
		return toolbarComponents;
	}

	/**
	 * Gets the view button
	 *
	 * @return the view button
	 */
	@Override
	public JButton getViewButton() {
		return columnsDialogButton;
	}

	/**
	 * Returns true if this trackview is in a custom state.
	 *
	 * @return true if in a custom state, false if in the default state
	 */
	@Override
	public boolean isCustomState() {
		if (!refreshed) {
			forceRefresh = true;
			refresh(trackerPanel.getFrameNumber(), DataTable.MODE_TRACK_REFRESH);
		}
		// check displayed data columns--default is columns 0 and 1 only
		if (!bsCheckBoxes.get(0) || !bsCheckBoxes.get(1))
			return true;

		// ignore formatting since now handled by NumberFormatSetter
//  	if (dataTable.getFormattedColumnNames().length>0)
//  		return true;

		// check for reordered columns
		TableColumnModel model = dataTable.getColumnModel();
		int count = model.getColumnCount();
		if (count == 0)
			return true; // should never happen...
		int index = model.getColumn(0).getModelIndex();
		for (int i = 1; i < count; i++) {
			if (model.getColumn(i).getModelIndex() < index) {
				return true;
			}
			index = model.getColumn(i).getModelIndex();
		}
		return false;
	}

	/**
	 * Sets the visibility of a dataset specified by index
	 *
	 * @param index   the index of the column
	 * @param visible <code>true</code> to show the dataset column in the table
	 */
	public void setVisible(int index, boolean visible) {
		bsCheckBoxes.set(index, visible);
//		if (index < checkBoxes.length) {
//			checkBoxes[index].setSelected(visible);
//		}
		int n = trackDataManager.getDatasets().size();
		if (index >= n) {
			TTrack track = getTrack();
			String name = track.getTextColumnNames().get(index - n);
			if (visible)
				textColumnsVisible.add(name);
			else
				textColumnsVisible.remove(name); // BH? missing?
		}
		refresh(trackerPanel.getFrameNumber(), DataTable.MODE_COL_SETVISIBLE);
	}

	/**
	 * Sets the visibility of a data or text column specified by name
	 *
	 * @param name    the name of the column
	 * @param visible <code>true</code> to show the column in the table
	 */
	public void setVisible(String name, boolean visible) {
//		for (int i = 0; i < checkBoxes.length; i++) {
//			String s = checkBoxes[i].getActionCommand();
//			if (s.equals(name) || TeXParser.removeSubscripting(s).equals(name)) {
//				setVisible(i, visible);
//				break;
//			}
//		}
		Integer i = htNames.get(name);
		if (i != null)
			setVisible(i.intValue(), visible);
	}

	@Override
	protected void dispose() {
		trackDataManager = null;
		getTrack().removePropertyChangeListener(TTrack.PROPERTY_TTRACK_TEXTCOLUMN, this); // $NON-NLS-1$
		setViewportView(null);
		if (columnsDialog != null) {
			columnsDialog.setVisible(false);
			columnsDialog.dispose();
			columnsDialog = null;
		}
		dataTableManager.clear();
		dataTableManager = null;
		dataTable.dispose();
		dataTable = null;
		viewParent = null;
		super.dispose();
	}

	/**
	 * Sets the highlighted frame numbers.
	 *
	 * @param frameNumbers the frame numbers
	 */
	protected void setHighlighted(BitSet frameNumbers) {
		// assume no highlights
		if (!highlightVisible)
			return;

		// get rows to highlight
		highlightRows.clear();
		for (int i = frameNumbers.nextSetBit(0); i >= 0; i = frameNumbers.nextSetBit(i + 1)) {
			int row = getRowFromFrame(i);
			if (row >= 0 && row < dataTable.getRowCount()) {
				highlightRows.set(i);
			}
		}
		// set highlighted rows if found
		OSPRuntime.postEvent(new Runnable() {
			@Override
			public synchronized void run() {
				dataTable.clearSelection();
				if (highlightRows.isEmpty()) {
					return;
				}
				try {
					dataTable.selectTableRowsBS(highlightRows, 0);
//					for (int i = highlightRows.nextSetBit(0); i >= 0; i = highlightRows.nextSetBit(i + 1)) {
//						dataTable.addRowSelectionInterval(i, row);
//					}
					if (highlightRows.cardinality() == 1) {
						dataTable.scrollRectToVisible(dataTable.getCellRect(highlightRows.nextSetBit(0), 0, true));
					}
				} catch (Exception e) {
				   e.printStackTrace();
				   // occasionally throws exception during loading!
				}
				int cols = dataTable.getColumnCount();
				dataTable.setColumnSelectionInterval(0, cols - 1);
			}
		});

	}

	/**
	 * Gets an array of visible column names.
	 *
	 * @return the visible columns
	 */
	String[] getVisibleColumns() {
		ArrayList<String> list = new ArrayList<String>();
		for (Entry<String, Integer> e : htNames.entrySet()) {
			if (bsCheckBoxes.get(e.getValue()))
				list.add(e.getKey()); // TODO remove all subs?
		}
		return list.toArray(new String[list.size()]);
	}

	/**
	 * Returns the visible column names in the order displayed in the table. Used
	 * for saving/loading xml.
	 *
	 * @return the visible columns in order
	 */
	String[] getOrderedVisibleColumns() {
		// get array of column model indexes in table order
		TableColumnModel model = dataTable.getColumnModel();
		Integer[] modelIndexes = new Integer[model.getColumnCount()];
		for (int i = 0; i < modelIndexes.length; i++) {
			modelIndexes[i] = model.getColumn(i).getModelIndex();
		}
		// get array of visible (dependent variable) column names
		String[] dependentVars = getVisibleColumns();
		// expand array to include independent variable
		String[] columnNames = new String[dependentVars.length + 1];
		TTrack track = getTrack();
		columnNames[0] = track.getDataName(0);
		System.arraycopy(dependentVars, 0, columnNames, 1, dependentVars.length);
		// create array of names in table order
		String[] ordered = new String[columnNames.length];
		for (int i = 0; i < ordered.length; i++) {
			if (i >= modelIndexes.length || modelIndexes[i] >= columnNames.length)
				continue;
			ordered[i] = columnNames[modelIndexes[i]];
		}
		return ordered;
	}

	/**
	 * Gets an array of column names and formats. Used for saving/loading xml
	 *
	 * @return String[][] each element is {colName, format}
	 */
	String[][] getColumnFormats() {
		String[] colNames = dataTable.getFormattedColumnNames();
		String[][] colFormats = new String[colNames.length][2];
		for (int i = 0; i < colNames.length; i++) {
			colFormats[i][0] = colNames[i];
			colFormats[i][1] = dataTable.getFormatPattern(colNames[i]);
		}
		return colFormats;
	}

	/**
	 * Responds to property change events.
	 *
	 * @param e the property change event
	 */
	@Override
	public void propertyChange(PropertyChangeEvent e) {
		TTrack track = getTrack();
		switch (e.getPropertyName()) {
		case TrackerPanel.PROPERTY_TRACKERPANEL_LOADED:
		case TrackerPanel.PROPERTY_TRACKERPANEL_TRACK:
			if (columnsDialog != null && e.getNewValue() == track) {
				refreshColumnDialog(track, true);
			}
			break;
		case TTrack.PROPERTY_TTRACK_TEXTCOLUMN:
			// look for added and removed column names
			String added = null;
			for (String name : track.getTextColumnNames()) {
				if (!textColumnNames.contains(name))
					added = name;
			}
			String removed = null;
			for (String name : textColumnNames) {
				if (!track.getTextColumnNames().contains(name))
					removed = name;
			}
			if (added != null && removed != null) {
				// name changed
				if (textColumnsVisible.contains(removed)) {
					textColumnsVisible.remove(removed);
					textColumnsVisible.add(added);
				}
			}
//  		else if (added!=null) {
//  			// new column is visible by default
//    		textColumnsVisible.add(added);    			
//  		}
			else if (removed != null) {
				textColumnsVisible.remove(removed);
			}
			// else a text entry was changed
			// refresh table and column visibility dialog
			dataTable.refreshTable(DataTable.MODE_COLUMN);
			if (viewParent.getViewType() == TView.VIEW_TABLE) {
				TableTView view = (TableTView) getParent();
				view.refreshColumnsDialog(track, true);
			}
			// update local list of names
			textColumnNames.clear();
			textColumnNames.addAll(track.getTextColumnNames());
			break;
		case TrackerPanel.PROPERTY_TRACKERPANEL_UNITS:
			dataTable.getTableHeader().repaint();
			break;
		default:
			super.propertyChange(e);
			break;
		}
	}

	/**
	 * Creates a snapshot of this view or its parent TViewChooser, if any.
	 */
	public void snapshot() {
		BufferedImage image = new TrackerIO.ComponentImage(TViewChooser.getChooserParent(this)).getImage();
		int w = image.getWidth();
		int h = image.getHeight();
		if ((w == 0) || (h == 0)) {
			return;
		}
		MeasuredImage mi = new MeasuredImage(image, 0, w, h, 0);

		// create ImageFrame using reflection
		OSPFrame frame = null;
		try {
			Class<?> type = Class.forName("org.opensourcephysics.frames.ImageFrame"); //$NON-NLS-1$
			Constructor<?>[] constructors = type.getConstructors();
			for (int i = 0; i < constructors.length; i++) {
				Class<?>[] parameters = constructors[i].getParameterTypes();
				if (parameters.length == 1 && parameters[0] == MeasuredImage.class) {
					frame = (OSPFrame) constructors[i].newInstance(new Object[] { mi });
					break;
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		if (frame == null)
			return;

		frame.setTitle(DisplayRes.getString("Snapshot.Title")); //$NON-NLS-1$
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.setKeepHidden(false);
		FontSizer.setFonts(frame, FontSizer.getLevel());
		frame.pack();
		frame.setVisible(true);
	}

	@Override
	public void setFont(Font font) {
		super.setFont(font);
		if (dataTable != null) {
			dataTable.setRowHeight(font.getSize() + 4);
			dataTable.getTableHeader().setFont(font);
		}
	}

	/**
	 * Gets the frame number for a view row. Returns -1 if not found.
	 *
	 * @param row the table row
	 * @return the frame number
	 */
	protected int getFrameAtRow(int row) {
		// get value of independent variable at row
		double val = getIndepVarValueAtRow(row);
		TTrack track = getTrack();
		String xVar = track.data.getDataset(0).getXColumnName();
		int frameNum = track.getFrameForData(xVar, null, new double[] { val });
		return frameNum;
	}

	/**
	 * Gets the view row for a given frame number. Returns -1 if none found.
	 *
	 * @param row the table row
	 * @return the frame number
	 */
	protected int getRowFromFrame(int frame) {
		// look for "frame" dataset in data
		ArrayList<Dataset> temp = trackDataManager.getDatasets();
		for (int i = 0; i < temp.size(); i++) {
			if ("frame".equals(temp.get(i).getYColumnName())) { //$NON-NLS-1$
				double[] vals = temp.get(i).getYPoints();
				for (int j = 0; j < vals.length; j++) {
					if (vals[j] == frame) {
						return dataTable.getSortedRow(j);
					}
				}
			}
		}
		return -1;
	}

	/**
	 * Gets the independent variable value at a view row.
	 *
	 * @param row the table row
	 * @return the value
	 */
	protected double getIndepVarValueAtRow(int row) {
		int col = dataTable.convertColumnIndexToView(0);
		Double val = null;
		try {
			val = (Double) dataTable.getValueAt(row, col);
		} catch (Exception e) {
		}
		return val == null ? Double.NaN : val;
	}

	/**
	 * Gets the view row at which an independent variable value is found.
	 *
	 * @param indepVarValue the value
	 * @return the view row
	 */
	protected int getRowFromIndepVarValue(double indepVarValue) {
		int col = dataTable.convertColumnIndexToView(0);
		for (int i = 0; i < dataTable.getRowCount(); i++) {
			if (indepVarValue == (Double) dataTable.getValueAt(i, col)) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Gets the selected independent variable values.
	 *
	 * @return double[] of selected values
	 */
	protected double[] getSelectedIndepVarValues() {
		Double[] d = selectedIndepVarValues.toArray(new Double[0]);
		double[] vals = new double[d.length];
		for (int i = 0; i < d.length; i++) {
			vals[i] = d[i];
		}
		return vals;
	}

	/**
	 * Sets the selected independent variable values.
	 *
	 * @param vals the values to select
	 */
	protected void setSelectedIndepVarValues(double[] vals) {
		if (dataTable.getRowCount() < 1) {
			return;
		}
		dataTable.removeRowSelectionInterval(0, dataTable.getRowCount() - 1);
		for (int i = 0; i < vals.length; i++) {
			int row = getRowFromIndepVarValue(vals[i]);
			if (row > -1) {
				dataTable.addRowSelectionInterval(row, row);
			}
		}
	}

	/**
	 * Creates the GUI.
	 */
	protected void createGUI() {
		columnsDialogButton = new TButton() {
			// override getMaximumSize method so has same height as chooser button
			@Override
			public Dimension getMaximumSize() {
				return TViewChooser.getButtonMaxSize(this, 
						super.getMaximumSize(), 
						getMinimumSize().height);

			}
		};
		columnsDialogButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				getOrCreateColumnsDialog(getTrack()).showDialog();
			}
		});
		// create column list
//		refreshColumnCheckboxes();

		// button to show gaps in data (skipped frames)
		gapsButton = new TButton() {
			// override getMaximumSize method so has same height as chooser button
			@Override
			public Dimension getMaximumSize() {
				return TViewChooser.getButtonMaxSize(this, 
						super.getMaximumSize(), 
						getMinimumSize().height);
			}

			@Override
			protected JPopupMenu getPopup() {
				JPopupMenu popup = new JPopupMenu();
				JCheckBoxMenuItem item = new JCheckBoxMenuItem(
						TrackerRes.getString("TableTrackView.MenuItem.Gaps.GapsVisible")); //$NON-NLS-1$
				item.setSelected(gapsButton.isSelected());
				item.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						gapsButton.setSelected(!gapsButton.isSelected());
						dataTable.skippedFramesRenderer.setVisible(gapsButton.isSelected());
						if (gapsButton.isSelected()) {
							dataTable.resetSort();
						}
						dataTable.repaint();
						dataTable.getTableHeader().resizeAndRepaint();
					}
				});
				popup.add(item);
				if (Tracker.enableAutofill) {
					item = new JCheckBoxMenuItem(TrackerRes.getString("TableTrackView.MenuItem.Gaps.AutoFill")); //$NON-NLS-1$
					item.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							PointMass p = (PointMass) TableTrackView.this.getTrack();
							p.setAutoFill(!p.isAutofill);
							p.repaint();
						}
					});
					PointMass p = (PointMass) TableTrackView.this.getTrack();
					item.setSelected(p.isAutofill);
					popup.addSeparator();
					popup.add(item);
				}
				FontSizer.setFonts(popup, FontSizer.getLevel());
				return popup;
			}

		};
		gapsButton.setText(TrackerRes.getString("TableTrackView.Button.Gaps.Text")); //$NON-NLS-1$
		gapsButton.setSelected(Tracker.showGaps);

		// create popup and add menu items
		popup = new JPopupMenu();
		dataTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		dataTable.getTableHeader().setToolTipText(TrackerRes.getString("TableTrackView.Header.Tooltip")); //$NON-NLS-1$
		dataTable.getTableHeader().addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				tableHeaderMousePressed(e);
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2 || OSPRuntime.isPopupTrigger(e))
					return;
				// single click: refresh selected rows
				double[] vals = getSelectedIndepVarValues();
				setSelectedIndepVarValues(vals);
			}
		});
		// data table: add right button mouse listener to copy data and double-click to
		// select all
		dataTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				tableMousePressed(e);
			}
		});
		// override the datatable CTRL-C behavior
		InputMap im = dataTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		KeyStroke k = KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK);
		Action newAction = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				TTrack track = getTrack();
				TrackerIO.copyTable(dataTable, false, track.getName()); // copy raw data
			}
		};
		ActionMap am = dataTable.getActionMap();
		am.put(im.get(k), newAction);
		// override the pageUp and pageDown behaviors
		k = KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0);
		newAction = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!trackerPanel.getPlayer().isEnabled())
					return;
				trackerPanel.getPlayer().back();
			}
		};
		am.put(im.get(k), newAction);
		k = KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, KeyEvent.SHIFT_DOWN_MASK);
		newAction = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!trackerPanel.getPlayer().isEnabled())
					return;
				int n = trackerPanel.getPlayer().getStepNumber() - 5;
				trackerPanel.getPlayer().setStepNumber(n);
			}
		};
		am.put(im.get(k), newAction);
		k = KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0);
		newAction = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!trackerPanel.getPlayer().isEnabled())
					return;
				trackerPanel.getPlayer().step();
			}
		};
		am.put(im.get(k), newAction);
		k = KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, KeyEvent.SHIFT_DOWN_MASK);
		newAction = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!trackerPanel.getPlayer().isEnabled())
					return;
				int n = trackerPanel.getPlayer().getStepNumber() + 5;
				trackerPanel.getPlayer().setStepNumber(n);
			}
		};
		am.put(im.get(k), newAction);
		k = KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0);
		newAction = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!trackerPanel.getPlayer().isEnabled())
					return;
				trackerPanel.getPlayer().setStepNumber(0);
			}
		};
		am.put(im.get(k), newAction);
		k = KeyStroke.getKeyStroke(KeyEvent.VK_END, 0);
		newAction = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!trackerPanel.getPlayer().isEnabled())
					return;
				VideoClip clip = trackerPanel.getPlayer().getVideoClip();
				trackerPanel.getPlayer().setStepNumber(clip.getStepCount() - 1);
			}
		};
		am.put(im.get(k), newAction);
	}

	public void dataToolAction() {
		TTrack track = getTrack();
		DatasetManager toSend = new DatasetManager();
		toSend.setID(trackDataManager.getID());
		toSend.setName(track.getName());
		toSend.setXPointsLinked(true);
		int colCount = 0;
		ArrayList<Dataset> datasets = trackDataManager.getDatasets();
		// always include linked independent variable first
		Dataset next = datasets.get(0);
		XMLControlElement control = new XMLControlElement(next);
		next = toSend.getDataset(colCount++);
		control.loadObject(next, true, true);
		next.setYColumnVisible(false);
		next.setConnected(false);
		next.setMarkerShape(Dataset.NO_MARKER);

//		for (int i = bsCheckBoxes.nextSetBit(0); i >= 0; i = bsCheckBoxes.nextSetBit(i + 1)) {
//			if (i >= datasetCount) {
//				next = track.convertTextToDataColumn(track.getTextColumnNames().get(i - datasetCount));
//				if (next == null)
//					continue;
//			} else {
//				next = datasets.get(i);
//			}

		for (int i = bsCheckBoxes.nextSetBit(0); i >= 0; i = bsCheckBoxes.nextSetBit(i + 1)) {
			if (i >= datasetCount) {
				next = track.convertTextToDataColumn(aNames[i]); // full name
				if (next == null)
					continue;
			} else {
				next = datasets.get(i);
			}
			control = new XMLControlElement(next);
			next = toSend.getDataset(colCount++);
			control.loadObject(next, true, true);
			next.setMarkerColor(track.getColor());
			next.setConnected(true);
			next.setXColumnVisible(false);

		}
		DataTool tool = DataTool.getTool(true);
		tool.setUseChooser(false);
		tool.setSaveChangesOnClose(false);
		DataRefreshTool refresher = DataRefreshTool.getTool(trackDataManager);
		tool.send(new LocalJob(toSend), refresher);
		tool.setVisible(true);
	}

	protected void tableMousePressed(MouseEvent e) {
		if (e.getClickCount() == 2) {
			dataTable.selectAll();
		}
		if (!OSPRuntime.isPopupTrigger(e))
			return;
		getMenuItems();
		java.awt.Point mousePt = e.getPoint();
		int col = dataTable.columnAtPoint(mousePt);
		deleteDataFunctionItem.setActionCommand(""); //$NON-NLS-1$
		// set action command of delete item if data function column selected
		String colName = dataTable.getColumnName(col);
		int index = trackDataManager.getDatasetIndex(colName);
		if (index > -1) {
			Dataset dataset = trackDataManager.getDataset(index);
			if (dataset instanceof DataFunction) {
				deleteDataFunctionItem.setActionCommand(String.valueOf(index));
				String s = TrackerRes.getString("TableTrackView.MenuItem.DeleteDataFunction"); //$NON-NLS-1$
				deleteDataFunctionItem.setText(s + " \"" + colName + "\""); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		// set action command and title of goToFrame item
		int row = dataTable.rowAtPoint(mousePt);
		goToFrameItem.setEnabled(row > -1);
		if (goToFrameItem.isEnabled()) {
			goToFrameItem.setActionCommand(String.valueOf(row));
			String s = TrackerRes.getString("TableTrackView.Popup.Menuitem.GoToStep"); //$NON-NLS-1$
			int frameNum = getFrameAtRow(row);
			VideoClip clip = trackerPanel.getPlayer().getVideoClip();
			int stepNum = clip.frameToStep(frameNum);
			s += " " + stepNum; //$NON-NLS-1$
			goToFrameItem.setText(s);
		}
		getPopup().show(dataTable, e.getX() + 4, e.getY());
	}

	protected void tableHeaderMousePressed(MouseEvent e) {
		java.awt.Point mousePt = e.getPoint();
		int col = dataTable.columnAtPoint(mousePt);
		if (OSPRuntime.isPopupTrigger(e)) {
			getMenuItems();
			if (dataTable.getRowCount() > 0 && dataTable.getSelectedRowCount() == 0) {
				dataTable.setColumnSelectionInterval(col, col);
				dataTable.setRowSelectionInterval(0, dataTable.getRowCount() - 1);
			}
			deleteDataFunctionItem.setActionCommand(""); //$NON-NLS-1$
			// set action command of delete item if data function column selected
			String colName = dataTable.getColumnName(col);
			int index = trackDataManager.getDatasetIndex(colName);
			if (index > -1) {
				Dataset dataset = trackDataManager.getDataset(index);
				if (dataset instanceof DataFunction) {
					deleteDataFunctionItem.setActionCommand(String.valueOf(index));
					String s = TrackerRes.getString("TableTrackView.MenuItem.DeleteDataFunction"); //$NON-NLS-1$
					deleteDataFunctionItem.setText(s + " \"" + colName + "\""); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
			goToFrameItem.setEnabled(false);
			getPopup().show(dataTable.getTableHeader(), e.getX(), e.getY() + 8);
		} else {
			// double-click: select column and all rows
			if (e.getClickCount() == 2) {
				dataTable.setRowSelectionInterval(0, dataTable.getRowCount() - 1); // all rows
				dataTable.setColumnSelectionInterval(col, col);
				leadCol = col;
				// sort by independent variable
				dataTable.sort(0);
			} else if (e.isControlDown()) {
				// control-click: add/remove columns to selection
				if (dataTable.isColumnSelected(col)) {
					dataTable.removeColumnSelectionInterval(col, col);
				} else {
					dataTable.addColumnSelectionInterval(col, col);
					if (dataTable.getSelectedColumns().length == 1) {
						leadCol = col;
					}
				}
			} else if (e.isShiftDown() && dataTable.getSelectedRows().length > 0) {
				// shift-click: extend selection
				if (leadCol < dataTable.getColumnCount()) {
					dataTable.setColumnSelectionInterval(col, leadCol);
				}
			}
		}
	}

	private void getMenuItems() {
		if (haveMenuItems)
			return;
		haveMenuItems = true;

		deleteDataFunctionItem = new JMenuItem();
		deleteDataFunctionItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int index = Integer.parseInt(e.getActionCommand());
				TTrack track = getTrack();
				FunctionTool tool = trackerPanel.getDataBuilder();
				FunctionPanel panel = tool.getPanel(track.getName());
				Dataset dataset = trackDataManager.getDataset(index);
				// next line posts undo edit to FunctionPanel
				if (dataset instanceof DataFunction)
					panel.getFunctionEditor().removeObject((DataFunction) dataset, true);
			}
		});

		goToFrameItem = new JMenuItem();
		goToFrameItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					int row = Integer.parseInt(e.getActionCommand());
					int frameNum = getFrameAtRow(row);
					if (frameNum > -1) {
						VideoClip clip = trackerPanel.getPlayer().getVideoClip();
						int stepNum = clip.frameToStep(frameNum);
						trackerPanel.getPlayer().setStepNumber(stepNum);
					}
				} catch (Exception ex) {
				}
			}
		});
		numberMenu = new JMenu();
		formatDialogItem = new JMenuItem();
		formatDialogItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int[] selected = dataTable.getSelectedColumns();
				String[] selectedNames = new String[selected.length];
				for (int i = 0; i < selectedNames.length; i++) {
					String name = dataTable.getColumnName(selected[i]);
					selectedNames[i] = name;
				}
				NumberFormatDialog dialog = NumberFormatDialog.getNumberFormatDialog(trackerPanel, getTrack(),
						selectedNames);
				dialog.setVisible(true);
			}
		});
		showUnitsItem = new JMenuItem();
		showUnitsItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				trackerPanel.setUnitsVisible(!trackerPanel.isUnitsVisible());
			}
		});
		setUnitsItem = new JMenuItem();
		setUnitsItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				UnitsDialog dialog = trackerPanel.getUnitsDialog();
				dialog.setVisible(true);
			}
		});
		copyDataMenu = new JMenu();
		copyDataRawItem = new JMenuItem(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				TTrack track = getTrack();
				TrackerIO.copyTable(dataTable, false, track.getName());
			}
		});
		copyDataFormattedItem = new JMenuItem(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				TTrack track = getTrack();
				TrackerIO.copyTable(dataTable, true, track.getName());
			}
		});
		final Action setDelimiterAction = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				TrackerIO.setDelimiter(e.getActionCommand());
				refreshGUI();
			}
		};
		setDelimiterMenu = new JMenu(setDelimiterAction);
		TFrame.addMenuListener(setDelimiterMenu, new Runnable() {

			@Override
			public void run() {
				setupDelimiterMenu(setDelimiterAction);
			}

		});
		Action copyImageAction = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// find TViewChooser that owns this view and copy it
				TViewChooser chooser = getOwner();
				if (chooser != null) {
					new TrackerIO.ComponentImage(chooser).copyToClipboard();
				}
			}
		};
		copyImageItem = new JMenuItem(copyImageAction);
		Action snapshotAction = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				snapshot();
			}
		};
		// add and remove text column items
		createTextColumnItem = new JMenuItem();
		createTextColumnItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String name = getUniqueColumnName(null, false);
				TTrack track = getTrack();
				track.addTextColumn(name);
				// new column is visible by default
				textColumnsVisible.add(name);
				// refresh table and column visibility dialog
				dataTable.refreshTable(DataTable.MODE_COLUMN);
				if (viewParent.getViewType() == TView.VIEW_TABLE) {
					((TableTView) viewParent).refreshColumnsDialog(track, true);
				}
			}
		});
		textColumnMenu = new JMenu();
		deleteTextColumnMenu = new JMenu();
		renameTextColumnMenu = new JMenu();

		snapshotItem = new JMenuItem(snapshotAction);
		// add dataBuilder item
		dataBuilderItem = new JMenuItem();
		dataBuilderItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				TTrack track = getTrack();
				trackerPanel.getDataBuilder().setSelectedPanel(track.getName());
				trackerPanel.getDataBuilder().setVisible(true);
			}
		});
		// add dataTool item
		dataToolItem = new JMenuItem();
		dataToolItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				dataToolAction();
			}
		});
		// add print item
		printItem = new JMenuItem();
		printItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// find TViewChooser that owns this view and print it
				TViewChooser chooser = getOwner();
				if (chooser != null) {
					new TrackerIO.ComponentImage(chooser).print();
				}
			}
		});
		// add help item last
		helpItem = new JMenuItem();
		helpItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				TFrame frame = trackerPanel.getTFrame();
				if (frame != null) {
					frame.showHelp("datatable", 0); //$NON-NLS-1$
				}
			}
		});
	}

	protected void setupDelimiterMenu(Action setDelimiterAction) {
	    ButtonGroup delimiterButtonGroup = new ButtonGroup();

		for (String key : TrackerIO.delimiters.keySet()) {
			String delimiter = TrackerIO.delimiters.get(key);
			JMenuItem item = new JRadioButtonMenuItem(key);
			item.setActionCommand(delimiter);
			item.addActionListener(setDelimiterAction);
			delimiterButtonGroup.add(item);
		}
		Action addDelimiterAction = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String delimiter = TrackerIO.delimiter;
				Object response = JOptionPane.showInputDialog(TableTrackView.this,
						TrackerRes.getString("TableTrackView.Dialog.CustomDelimiter.Message"), //$NON-NLS-1$
						TrackerRes.getString("TableTrackView.Dialog.CustomDelimiter.Title"), //$NON-NLS-1$
						JOptionPane.PLAIN_MESSAGE, null, null, delimiter);
				if (response != null) {
					String s = response.toString();
					TrackerIO.setDelimiter(s);
					TrackerIO.addCustomDelimiter(s);
					refreshGUI();
				}
			}
		};
		JMenuItem addDelimiterItem = new JMenuItem(addDelimiterAction);
		Action removeDelimiterAction = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String[] choices = TrackerIO.customDelimiters.values().toArray(new String[1]);
				Object response = JOptionPane.showInputDialog(TableTrackView.this,
						TrackerRes.getString("TableTrackView.Dialog.RemoveDelimiter.Message"), //$NON-NLS-1$
						TrackerRes.getString("TableTrackView.Dialog.RemoveDelimiter.Title"), //$NON-NLS-1$
						JOptionPane.PLAIN_MESSAGE, null, choices, null);
				if (response != null) {
					String s = response.toString();
					TrackerIO.removeCustomDelimiter(s);
					refreshGUI();
				}
			}
		};
		JMenuItem removeDelimiterItem = new JMenuItem(removeDelimiterAction);
		addDelimiterItem.setText(TrackerRes.getString("TableTrackView.MenuItem.AddDelimiter")); //$NON-NLS-1$
		removeDelimiterItem.setText(TrackerRes.getString("TableTrackView.MenuItem.RemoveDelimiter")); //$NON-NLS-1$
		setDelimiterMenu.removeAll();
		String delimiter = TrackerIO.getDelimiter();
		// remove all custom delimiter items from button group
		Enumeration<AbstractButton> en = delimiterButtonGroup.getElements();
		for (; en.hasMoreElements();) {
			JMenuItem item = (JMenuItem) en.nextElement();
			String delim = item.getActionCommand();
			if (!TrackerIO.delimiters.containsValue(delim))
				delimiterButtonGroup.remove(item);
		}
		// add all button group items to menu
		en = delimiterButtonGroup.getElements();
		for (; en.hasMoreElements();) {
			JMenuItem item = (JMenuItem) en.nextElement();
			setDelimiterMenu.add(item);
			if (delimiter.equals(item.getActionCommand()))
				item.setSelected(true);
		}
		// add new custom delimiter items
		boolean hasCustom = !TrackerIO.customDelimiters.isEmpty();
		if (hasCustom) {
			setDelimiterMenu.addSeparator();
			for (String key : TrackerIO.customDelimiters.keySet()) {
				JMenuItem item = new JRadioButtonMenuItem(key);
				item.setActionCommand(TrackerIO.customDelimiters.get(key));
				item.addActionListener(new AbstractAction() {
					@Override
					public void actionPerformed(ActionEvent e) {
						TrackerIO.setDelimiter(e.getActionCommand());
					}
				});
				delimiterButtonGroup.add(item);
				setDelimiterMenu.add(item);
				if (delimiter.equals(item.getActionCommand()))
					item.setSelected(true);
			}
		}
		setDelimiterMenu.addSeparator();
		setDelimiterMenu.add(addDelimiterItem);
		if (hasCustom)
			setDelimiterMenu.add(removeDelimiterItem);
	}

	protected JPopupMenu getPopup() {
		getMenuItems();
		numberMenu.setText(TrackerRes.getString("Popup.Menu.Numbers")); //$NON-NLS-1$
		formatDialogItem.setText(TrackerRes.getString("Popup.MenuItem.Formats") + "..."); //$NON-NLS-1$ //$NON-NLS-2$
		setUnitsItem.setText(TrackerRes.getString("Popup.MenuItem.Units") + "..."); //$NON-NLS-1$ //$NON-NLS-2$
		copyImageItem.setText(TrackerRes.getString("TMenuBar.Menu.CopyImage")); //$NON-NLS-1$
		snapshotItem.setText(DisplayRes.getString("DisplayPanel.Snapshot_menu_item")); //$NON-NLS-1$
		printItem.setText(TrackerRes.getString("TActions.Action.Print")); //$NON-NLS-1$
		helpItem.setText(TrackerRes.getString("Tracker.Popup.MenuItem.Help")); //$NON-NLS-1$
		createTextColumnItem.setText(TrackerRes.getString("TableTrackView.Action.CreateTextColumn.Text")); //$NON-NLS-1$
		textColumnMenu.setText(TrackerRes.getString("TableTrackView.Menu.TextColumn.Text")); //$NON-NLS-1$
		deleteTextColumnMenu.setText(TrackerRes.getString("TableTrackView.Action.DeleteTextColumn.Text")); //$NON-NLS-1$
		renameTextColumnMenu.setText(TrackerRes.getString("TableTrackView.Action.RenameTextColumn.Text")); //$NON-NLS-1$
		dataBuilderItem.setText(TrackerRes.getString("TView.Menuitem.Define")); //$NON-NLS-1$
		dataToolItem.setText(TrackerRes.getString("TableTrackView.Popup.MenuItem.Analyze")); //$NON-NLS-1$
		refreshCopyDataMenu(copyDataMenu);
		popup.removeAll();
		if (goToFrameItem.isEnabled()) {
			popup.add(goToFrameItem);
		}

		TTrack track = getTrack();
		if (track == null) {
			if (trackerPanel.isEnabled("number.formats") || trackerPanel.isEnabled("number.units")) { //$NON-NLS-1$ //$NON-NLS-2$
				if (popup.getComponentCount() > 0)
					popup.addSeparator();
				popup.add(numberMenu);
				numberMenu.removeAll();
				if (trackerPanel.isEnabled("number.formats")) //$NON-NLS-1$
					numberMenu.add(formatDialogItem);
				if (trackerPanel.isEnabled("number.units")) //$NON-NLS-1$
					numberMenu.add(setUnitsItem);
			}
			return popup;
		}
		if (track.trackerPanel != null && track.trackerPanel.isEnabled("edit.copyData")) { //$NON-NLS-1$
			if (popup.getComponentCount() > 0)
				popup.addSeparator();
			popup.add(copyDataMenu);
		}
		if (trackerPanel.isEnabled("number.formats") || trackerPanel.isEnabled("number.units") //$NON-NLS-1$ //$NON-NLS-2$
				&& track.trackerPanel != null) {
			if (popup.getComponentCount() > 0)
				popup.addSeparator();
			popup.add(numberMenu);
			numberMenu.removeAll();
			if (trackerPanel.isEnabled("number.formats")) //$NON-NLS-1$
				numberMenu.add(formatDialogItem);
			if (trackerPanel.isEnabled("number.units")) //$NON-NLS-1$
				numberMenu.add(setUnitsItem);
		}

		// textColumnMenu
		if (trackerPanel.isEnabled("text.columns")) { //$NON-NLS-1$
			textColumnMenu.removeAll();
			if (popup.getComponentCount() > 0)
				popup.addSeparator();
			popup.add(textColumnMenu);
			textColumnMenu.add(createTextColumnItem);
			if (track.getTextColumnNames().size() > 0) {
				deleteTextColumnMenu.removeAll();
				textColumnMenu.add(deleteTextColumnMenu);
				for (String next : track.getTextColumnNames()) {
					JMenuItem item = new JMenuItem(next);
					deleteTextColumnMenu.add(item);
					item.setActionCommand(next);
					item.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							TTrack track = getTrack();
							track.removeTextColumn(e.getActionCommand());
						}
					});
				}
				renameTextColumnMenu.removeAll();
				textColumnMenu.add(renameTextColumnMenu);
				for (String next : track.getTextColumnNames()) {
					JMenuItem item = new JMenuItem(next);
					renameTextColumnMenu.add(item);
					item.setActionCommand(next);
					item.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							String prev = e.getActionCommand();
							String name = getUniqueColumnName(prev, false);
							if (name != null && !name.equals("") && !name.equals(prev)) { //$NON-NLS-1$
								// name has changed
								TTrack track = getTrack();
								track.renameTextColumn(prev, name);
							}
						}
					});
				}
			}
		}
		textColumnMenu.setEnabled(!track.isLocked());

		if (!"".equals(deleteDataFunctionItem.getActionCommand())) { //$NON-NLS-1$
			popup.addSeparator();
			popup.add(deleteDataFunctionItem);
		}

		if (track.trackerPanel != null && track.trackerPanel.isEnabled("edit.copyImage")) { //$NON-NLS-1$
			popup.addSeparator();
			popup.add(copyImageItem);
			popup.add(snapshotItem);
		}
		if (track.trackerPanel != null && (track.trackerPanel.isEnabled("data.builder") //$NON-NLS-1$
				|| track.trackerPanel.isEnabled("data.tool"))) { //$NON-NLS-1$
			popup.addSeparator();
			if (track.trackerPanel.isEnabled("data.builder")) //$NON-NLS-1$
				popup.add(dataBuilderItem);
			if (track.trackerPanel.isEnabled("data.tool")) //$NON-NLS-1$
				popup.add(dataToolItem);
		}
		if (track.trackerPanel != null && track.trackerPanel.isEnabled("file.print")) { //$NON-NLS-1$
			popup.addSeparator();
			popup.add(printItem);
		}
		if (popup.getComponentCount() > 0)
			popup.addSeparator();
		popup.add(helpItem);
		FontSizer.setFonts(popup, FontSizer.getLevel());
		return popup;
	}

	/**
	 * Gets a unique new name for a text column.
	 *
	 * @param previous the previous name (may be null)
	 * @return the new name
	 */
	protected String getUniqueColumnName(String previous, boolean tryAgain) {
		if (previous == null)
			previous = ""; //$NON-NLS-1$
		Object input = null;
		TTrack track = getTrack();
		if (tryAgain) {
			input = JOptionPane.showInputDialog(track.trackerPanel.getTFrame(),
					TrackerRes.getString("TableTrackView.Dialog.NameColumn.TryAgain") + "\n" + //$NON-NLS-1$ //$NON-NLS-2$
							TrackerRes.getString("TableTrackView.Dialog.NameColumn.Message"), //$NON-NLS-1$
					TrackerRes.getString("TableTrackView.Dialog.NameColumn.Title"), //$NON-NLS-1$
					JOptionPane.WARNING_MESSAGE, null, null, previous);
		} else {
			input = JOptionPane.showInputDialog(track.trackerPanel.getTFrame(),
					TrackerRes.getString("TableTrackView.Dialog.NameColumn.Message"), //$NON-NLS-1$
					TrackerRes.getString("TableTrackView.Dialog.NameColumn.Title"), //$NON-NLS-1$
					JOptionPane.QUESTION_MESSAGE, null, null, previous);
		}
		if (input == null) {
			return null;
		}
		String name = ((String) input).trim();
		if (name.equals(previous))
			return name;
		// check name for uniqueness
		boolean unique = true;
		for (String next : getDataColumnNames()) {
			if (next.equals(name)) {
				unique = false;
				break;
			}
		}
		if (unique) {
			for (String next : track.getTextColumnNames()) {
				if (next.equals(name)) {
					unique = false;
					break;
				}
			}
		}
		if (!unique)
			return getUniqueColumnName(previous, true);
		return name;
	}

	/**
	 * Refreshes a menu with appropriate copy data items for this view.
	 *
	 * @param menu the menu to refresh
	 * @return the refreshed menu
	 */
	protected JMenu refreshCopyDataMenu(JMenu menu) {
		getMenuItems();
		menu.removeAll();
		menu.add(copyDataRawItem);
		menu.add(copyDataFormattedItem);
		menu.addSeparator();
		menu.add(setDelimiterMenu);
		if (dataTable.getSelectedRowCount() == 0)
			menu.setText(TrackerRes.getString("TableTrackView.Action.CopyData")); //$NON-NLS-1$
		else
			menu.setText(TrackerRes.getString("TableTrackView.MenuItem.CopySelectedData")); //$NON-NLS-1$
		copyDataRawItem.setText(TrackerRes.getString("TableTrackView.MenuItem.Unformatted")); //$NON-NLS-1$
		copyDataFormattedItem.setText(TrackerRes.getString("TableTrackView.MenuItem.Formatted")); //$NON-NLS-1$
		setDelimiterMenu.setText(TrackerRes.getString("TableTrackView.Menu.SetDelimiter")); //$NON-NLS-1$
		return menu;
	}

	/**
	 * Gets an array of all column names.
	 *
	 * @return the column names
	 */
	protected String[] getDataColumnNames() {
		ArrayList<String> names = new ArrayList<String>();
		// first add independent variable
		Dataset dataset = trackDataManager.getDataset(0);
		String name = dataset.getXColumnName();
		names.add(name);
		// then add other variables
		TTrack track = getTrack();
		ArrayList<Integer> dataOrder = track.getPreferredDataOrder();
		ArrayList<Integer> added = new ArrayList<Integer>();
		// first add in preferred order
		for (int i = 0; i < dataOrder.size(); i++) {
			dataset = trackDataManager.getDataset(dataOrder.get(i));
			name = dataset.getYColumnName();
			names.add(name);
			added.add(dataOrder.get(i));
		}
		// then add any that were missed
		for (int i = 0; i < trackDataManager.getDatasets().size(); i++) {
			if (!added.contains(i)) {
				dataset = trackDataManager.getDataset(i);
				name = dataset.getYColumnName();
				names.add(name);
			}
		}
		return names.toArray(new String[0]);
	}

	/**
	 * A class to provide textColumn data for the dataTable.
	 */
	private class TextColumnTableModel extends DataTable.OSPTableModel {
		@Override
		public String getColumnName(int col) {
			int i = 0;
			for (String name : getTrack().getTextColumnNames()) {
				if (textColumnsVisible.contains(name)) {
					if (i++ == col)
						return name;
				}
			}
			return "unknown"; //$NON-NLS-1$
		}

		@Override
		public int getRowCount() {
			return dataTableManager.getRowCount();
		}

		@Override
		public int getColumnCount() {
			return textColumnsVisible.size();
		}

		@Override
		public Object getValueAt(int row, int col) {
			String columnName = getColumnName(col);
			TTrack track = getTrack();
			// convert row to frame number
			// DatasetManager data = track.getData(track.trackerPanel);
			int index = trackDataManager.getDatasetIndex("frame"); //$NON-NLS-1$
			if (index > -1) {
				double frame = trackDataManager.getDataset(index).getYPoints()[row];
				return track.getTextColumnEntry(columnName, (int) frame);
			}
			// if no frame numbers defined (eg line profile), use row number
			return track.getTextColumnEntry(columnName, row);
		}

		/**
		 * Sets the value at the given cell.
		 *
		 * @param value the value
		 * @param row   the row index
		 * @param col   the column index
		 */
		@Override
		public void setValueAt(Object value, int row, int col) {
			String columnName = getColumnName(col);
			TTrack track = getTrack();
			// convert row to frame number
			// DatasetManager data = track.getData(track.trackerPanel);
			int index = trackDataManager.getDatasetIndex("frame"); //$NON-NLS-1$
			if (index > -1) {
				double frame = trackDataManager.getDataset(index).getYPoints()[row];
				if (track.setTextColumnEntry(columnName, (int) frame, (String) value)) {
					trackerPanel.changed = true;
				}
				return;
			}
			// if no frame numbers defined (eg line profile), use row number
			if (track.setTextColumnEntry(columnName, row, (String) value)) {
				trackerPanel.changed = true;
			}
		}

		@Override
		public boolean isCellEditable(int row, int col) {
			return !getTrack().isLocked();
		}

		@Override
		public Class<?> getColumnClass(int col) {
			return String.class;
		}

		@Override
		public String toString() {
			return "TableTrackView.TextColumnTableModel n=" + textColumnNames.size() + " vis="
					+ textColumnsVisible.size();
		}

	}

	/**
	 * A cell editor for textColumn cells.
	 */
	class TextColumnEditor extends AbstractCellEditor implements TableCellEditor {
		Color defaultEditingColor;
		JPanel panel = new JPanel(new BorderLayout());
		JTextField field = new JTextField();

		// Constructor.
		TextColumnEditor() {
			defaultEditingColor = field.getSelectionColor();
			panel.add(field, BorderLayout.CENTER);
			panel.setOpaque(false);
			field.setBorder(BorderFactory.createEmptyBorder(0, 1, 1, 0));
			field.addKeyListener(new KeyAdapter() {
				@Override
				public void keyPressed(KeyEvent e) {
					if (e.getKeyCode() == KeyEvent.VK_ENTER) {
						stopCellEditing();
					} else if (field.isEnabled()) {
						field.setBackground(Color.yellow);
					}
				}

			});
			field.addFocusListener(new FocusAdapter() {
				@Override
				public void focusLost(FocusEvent e) {
					// request focus immediately to keep it
					field.requestFocusInWindow();
				}
			});
		}

		// Gets the component to be displayed while editing.
		@Override
		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row,
				int column) {
			field.setBackground(Color.white);
			field.setSelectionColor(defaultEditingColor);
			field.setEditable(true);
			if (value == null)
				value = ""; //$NON-NLS-1$
			field.setText(value.toString());
			return panel;
		}

		// Determines when editing starts.
		@Override
		public boolean isCellEditable(EventObject e) {
			if (e == null || e instanceof MouseEvent) {
				TTrack track = getTrack();
				return !track.isLocked();
			}
			return false;
		}

		// Called when editing is completed.
		@Override
		public Object getCellEditorValue() {
			dataTable.requestFocusInWindow();
			if (field.getBackground() != Color.white) {
				field.setBackground(Color.white);
			}
			return field.getText();
		}

	}

	// the default table cell renderer when no PrecisionRenderer is used
	class NumberFieldRenderer extends NumberField implements TableCellRenderer {

		DefaultTableCellRenderer defaultRenderer;

		public NumberFieldRenderer() {
			super(1);
			defaultRenderer = new DefaultTableCellRenderer();
			defaultRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			Component c = defaultRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
					column);
			if (value instanceof Double && c instanceof JLabel) {
				// show number as formatted by this NumberField
				setValue((Double) value);
				((JLabel) c).setText(getText());
			}
			return c;
		}

	}

	// the default table cell renderer when no PrecisionRenderer is used
	class SkippedFramesRenderer implements TableCellRenderer {

		TableCellRenderer baseRenderer;
		Border belowBorder, aboveBorder;
		boolean visible = Tracker.showGaps;

		public SkippedFramesRenderer() {
			belowBorder = BorderFactory.createMatteBorder(0, 0, 1, 0, Color.red);
			Border space = BorderFactory.createEmptyBorder(0, 1, 0, 1);
			belowBorder = BorderFactory.createCompoundBorder(belowBorder, space);
			aboveBorder = BorderFactory.createMatteBorder(1, 0, 0, 0, Color.red);
			space = BorderFactory.createEmptyBorder(0, 1, 1, 1);
			aboveBorder = BorderFactory.createCompoundBorder(aboveBorder, space);
		}

		public void setBaseRenderer(TableCellRenderer renderer) {
			baseRenderer = renderer;
		}

		public void setVisible(boolean vis) {
			visible = vis;
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			Component c = baseRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			if (visible) {
				// add red above or below border to identify skipped frames
				TTrack track = getTrack();
				if (track instanceof PointMass) {
					PointMass p = (PointMass) track;
					if (p.trackerPanel != null) {
						VideoClip clip = p.trackerPanel.getPlayer().getVideoClip();
						int frameNum = getFrameAtRow(row);
						int stepNum = clip.frameToStep(frameNum);
						for (int i : p.skippedSteps) {
							if (stepNum + 1 == i) {
								((JLabel) c).setBorder(belowBorder);
							} else if (stepNum - 1 == i) {
								((JLabel) c).setBorder(aboveBorder);
							}
						}
					}
				}
			}
			return c;
		}

	}

	class TrackDataTable extends DataTable {

		NumberFieldRenderer numberFieldRenderer = new NumberFieldRenderer();
		SkippedFramesRenderer skippedFramesRenderer = new SkippedFramesRenderer();

		TrackDataTable() {
			super();
			TableCellRenderer renderer = getTableHeader().getDefaultRenderer();
			if (renderer instanceof DataTable.HeaderRenderer) {
				renderer = ((DataTable.HeaderRenderer) renderer).getBaseRenderer();
			}
			TableCellRenderer headerRenderer = new HeaderUnitsRenderer(this, renderer);
			getTableHeader().setDefaultRenderer(headerRenderer);
		}

		@Override
		public void refreshTable(int mode) {
			super.refreshTable(mode, true);
			refreshToolbar();
		}

		@Override
		public TableCellEditor getCellEditor(int row, int column) {
			// only text columns are editable, so always return textColumnEditor
			return textColumnEditor;
		}

		@Override
		public boolean isCellEditable(int row, int col) {
			return dataTableModel.getColumnClass(convertColumnIndexToModel(col)).equals(String.class);
		}

		@Override
		public TableCellRenderer getDefaultRenderer(Class<?> type) {
			if (type.isAssignableFrom(Double.class)) {
				return numberFieldRenderer;
			}
			return super.getDefaultRenderer(type);
		}

		@Override
		public TableCellRenderer getCellRenderer(int row, int column) {
			TableCellRenderer renderer = super.getCellRenderer(row, column);
			skippedFramesRenderer.setBaseRenderer(renderer);
			return skippedFramesRenderer;
		}

		@Override
		public void sort(int col) {
			if (col > 0 && gapsButton.isSelected()) {
				gapsButton.doClick(0);
			}
			super.sort(col);
		}

	}

	public class HeaderUnitsRenderer extends DataTable.HeaderRenderer {

		public HeaderUnitsRenderer(DataTable table, TableCellRenderer renderer) {
			table.super(renderer);
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int col) {
			TTrack track = getTrack();
			if (track.trackerPanel != null && value instanceof String) {
				String var = (String) value;// ((JLabel) c).getText();//textLine.getText();
				String units = track.trackerPanel.getUnits(track, var);
				if (units.length() > 0) {// !"".equals(units)) { //$NON-NLS-1$
//					if (OSPRuntime.isMac()) {
//						var = TeXParser.removeSubscripting(var);
//					}
					value = var + " (" + units.trim() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
//					if (OSPRuntime.isMac()) {
					// if (c instanceof JLabel) {
					// ((JLabel) c).setText(var);
					// }
//					}
					// textLine.setText(var);
				}
			}
			return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
		}

	}

	public void refreshToolbar() {
		if (OSPRuntime.isJS)
			return;

		if (trackerPanel.getTFrame() != null) {
			TViewChooser owner = getOwner();
			if (owner != null)
				owner.refreshToolbar();
		}
	}

	/**
	 * For TableTView popup menu
	 * 
	 * @param popup
	 */
	public void addColumnItems(JPopupMenu popup) {
		getPopup(); // refreshes menu items
		popup.add(createTextColumnItem);
		if (deleteTextColumnMenu.getMenuComponentCount() > 0) {
			popup.add(deleteTextColumnMenu);
			popup.add(renameTextColumnMenu);
		}
	}

	private ColumnsDialog getOrCreateColumnsDialog(TTrack track) {
		TFrame frame = trackerPanel.getTFrame();
		if (frame != null) {
			if (columnsDialog == null)
				columnsDialog = new ColumnsDialog(frame, track);
			else if (columnsDialog.track != track)
				columnsDialog.rebuild(track);
			else
				columnsDialog.refreshButtonPanel();				
		}
		return columnsDialog;
	}

	public void refreshColumnDialog(TTrack track, boolean onlyIfVisible) {			
		if (track == null) {
			if (columnsDialog != null) {
				columnsDialog.getContentPane().removeAll();
				columnsDialog.setVisible(false);
			}
			return;
		}
		if (onlyIfVisible && columnsDialog == null || !columnsDialog.isVisible())
			return;
		getOrCreateColumnsDialog(track);
		columnsDialog.showDialog();
	}

	
	public boolean setDialogVisible(boolean dialogVisible, boolean dialogLastVisible) {
		if (dialogVisible) {
			if (columnsDialog != null)
				columnsDialog.setVisible(dialogLastVisible);
			return dialogLastVisible;
		} else {
			boolean vis = dialogVisible;
			if (columnsDialog != null)
				columnsDialog.setVisible(false);
			return vis;
		}
	}

	public void buildForNewFunction() {
		if (columnsDialog != null) {
			columnsDialog.refreshState();
			columnsDialog.setPortPosition();
		}
	}

	private class ColumnsDialog extends JDialog {

		// GUI

		private JScrollPane columnsScroller;
		private JCheckBox[] checkBoxes;
		private JPanel columnsPanel;

		private JLabel trackLabel;
		private JButton defineButton, closeButton, textColumnButton;
		private JPanel buttonPanel;

		private TTrack track;

		@Override
		public void setVisible(boolean vis) {
			if (vis) {
				refreshState();
			}
			super.setVisible(vis);
		}

		private ActionListener cbActionListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doCheckBoxAction(Integer.parseInt(((JCheckBox) e.getSource()).getActionCommand()));
			}
		};

		private boolean haveGUI;

		private ColumnsDialog(TFrame frame, TTrack track) {
			super(frame, false);
			this.track = track;
			rebuild(track);
		}

		private void doCheckBoxAction(int i) {
			bsCheckBoxes.set(i, checkBoxes[i].isSelected());
			trackerPanel.changed = true;
			if (refreshing)
				TableTrackView.this.refresh(trackerPanel.getFrameNumber(), DataTable.MODE_TRACK_STATE);
			trackerPanel.changed = true; // BH ? a second time?
		}

		private void createGUI() {
			haveGUI = true;
			columnsPanel = new JPanel();
			columnsPanel.setBackground(Color.WHITE);
			columnsPanel.setLayout(new GridLayout(0, 4));
			columnsScroller = new JScrollPane(columnsPanel);
			javax.swing.border.Border empty = BorderFactory.createEmptyBorder(0, 3, 0, 2);
			javax.swing.border.Border etched = BorderFactory.createEtchedBorder();
			columnsScroller.setBorder(BorderFactory.createCompoundBorder(empty, etched));
			// button to open column selection dialog box
			setResizable(false);
			JPanel contentPane = new JPanel();
			contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.PAGE_AXIS));
			setContentPane(contentPane);
			// create close button
			closeButton = new JButton(TrackerRes.getString("Dialog.Button.Close")); //$NON-NLS-1$
			closeButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					setVisible(false);
				}
			});
			// create data function tool action
			// create define button
			defineButton = new JButton(TrackerRes.getString("TView.Menuitem.Define")); //$NON-NLS-1$
			defineButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					if (track != null) {
						trackerPanel.getDataBuilder().setSelectedPanel(track.getName());
						trackerPanel.getDataBuilder().setVisible(true);
					}
				}
			});
			defineButton.setToolTipText(TrackerRes.getString("Button.Define.Tooltip")); //$NON-NLS-1$
			// create text column button
			textColumnButton = new JButton(TrackerRes.getString("TableTrackView.Menu.TextColumn.Text")); //$NON-NLS-1$
			textColumnButton.setToolTipText(TrackerRes.getString("TableTrackView.Menu.TextColumn.Tooltip")); //$NON-NLS-1$
			textColumnButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					// show popup menu
					JPopupMenu popup = new JPopupMenu();
					addColumnItems(popup);
					FontSizer.setFonts(popup, FontSizer.getLevel());
					popup.show(textColumnButton, 0, textColumnButton.getHeight());
				}
			});

			buttonPanel = new JPanel();
			// will be filled in below
			
			// create track label
			trackLabel = new JLabel();
			trackLabel.setBorder(BorderFactory.createEmptyBorder(7, 0, 6, 0));
			trackLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		}

		private void refreshButtonPanel() {
			// refresh button panel
			buttonPanel.removeAll();
			if (trackerPanel.isEnabled("data.builder")) //$NON-NLS-1$
				buttonPanel.add(defineButton);
			if (trackerPanel.isEnabled("text.columns")) //$NON-NLS-1$
				buttonPanel.add(textColumnButton);
			buttonPanel.add(closeButton);
		}

		private void refreshGUI() {
			if (!haveGUI)
				return;
			FontSizer.setFonts(this);
			closeButton.setText(TrackerRes.getString("Dialog.Button.Close")); //$NON-NLS-1$
			defineButton.setText(TrackerRes.getString("TView.Menuitem.Define")); //$NON-NLS-1$
			defineButton.setToolTipText(TrackerRes.getString("Button.Define.Tooltip")); //$NON-NLS-1$
			setTitle(TrackerRes.getString("TableTView.Dialog.TableColumns.Title")); //$NON-NLS-1$
			textColumnButton.setText(TrackerRes.getString("TableTrackView.Menu.TextColumn.Text")); //$NON-NLS-1$
			textColumnButton.setToolTipText(TrackerRes.getString("TableTrackView.Menu.TextColumn.Tooltip")); //$NON-NLS-1$
		}

		/**
		 * Refreshes the column visibility checkboxes.
		 */
		private void refreshState() {
			if (!haveGUI) 
				createGUI();
			refreshButtonPanel();
			if (checkBoxes == null || colCount > checkBoxes.length)
				checkBoxes = new JCheckBox[colCount];
			for (int i = 0; i < colCount; i++) {
				String name = (i < datasetCount ? trackDataManager.getDataset(i).getYColumnName()
						: textColumnNames.get(i - datasetCount));
				if (checkBoxes[i] == null) {
					checkBoxes[i] = new JCheckBox();
					checkBoxes[i].setBackground(Color.white);
					checkBoxes[i].setBorder(BorderFactory.createEmptyBorder(1, 5, 1, 0));
					checkBoxes[i].setActionCommand("" + i);
					checkBoxes[i].setToolTipText(track.getDataDescription(i + 1));
					checkBoxes[i].addActionListener(cbActionListener);
					checkBoxes[i].setOpaque(false);
				}
				checkBoxes[i].setFont(font);
				checkBoxes[i].setSelected(bsCheckBoxes.get(i));
				checkBoxes[i].setName(name);
				checkBoxes[i].setText(TeXParser.removeSubscripting(name));
			}
			columnsPanel.removeAll();
			ArrayList<Integer> dataOrder = track.getPreferredDataOrder();
			BitSet bsMissed = new BitSet();
			bsMissed.set(0, colCount);
			// first add in preferred order
			for (int i = 0, n = dataOrder.size(); i < n; i++) {
				int pt = dataOrder.get(i);
				columnsPanel.add(checkBoxes[pt]);
				bsMissed.clear(pt);
			}
			// then add any that were missed
			for (int i = bsMissed.nextSetBit(0); i >= 0; i = bsMissed.nextSetBit(i + 1)) {
				columnsPanel.add(checkBoxes[i]);
			}
			refreshGUI();
		}

		private void setPortPosition() {
			JViewport port = columnsScroller.getViewport();
			Dimension dim = port.getViewSize();
			int offset = port.getExtentSize().height;
			port.setViewPosition(new Point(0, dim.height - offset));
		}

		private void rebuild(TTrack track) {
			FontSizer.setFonts(this);
			this.track = track;
			setResizable(true);
			getContentPane().removeAll();
			refreshState();
			trackLabel.setIcon(track.getFootprint().getIcon(21, 16));
			trackLabel.setText(track.getName());
			textColumnButton.setEnabled(!track.isLocked());
			add(trackLabel, BorderLayout.NORTH);
			add(columnsScroller, BorderLayout.CENTER);
			add(buttonPanel, BorderLayout.SOUTH);
			Dimension dim = getContentPane().getPreferredSize();
			getContentPane().setPreferredSize(new Dimension(dim.width, Math.min(dim.height, 300)));
			pack();		
//			repaint();			
		}

		/**
		 * Displays the dialog box for selecting data columns.
		 *
		 * @param track the track
		 */
		protected void showDialog() {
			// refresh dialog
			Point p0 = new Frame().getLocation();
			if (getLocation().x == p0.x) {
				// center dialog on the screen
				Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
				int x = (dim.width - getBounds().width) / 2;
				int y = (dim.height - getBounds().height) / 2;
				setLocation(x, y);
			}
			if (!isVisible())
				setVisible(true);
		}

		@Override
		public void dispose() {
			columnsPanel.removeAll();

		}

	}

}