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

import java.awt.Color;
import java.awt.Graphics;
import java.beans.PropertyChangeEvent;
import java.util.HashMap;

import javax.swing.JMenu;

import org.opensourcephysics.controls.XMLControlElement;
import org.opensourcephysics.display.DrawingPanel;
import org.opensourcephysics.display.Interactive;
import org.opensourcephysics.media.core.Filter;
import org.opensourcephysics.media.core.MediaRes;
import org.opensourcephysics.media.core.PerspectiveFilter;
import org.opensourcephysics.media.core.TPoint;

/**
 * This is a track used for autotracking perspective filter corners.
 *
 * @author Douglas Brown
 */
public class PerspectiveTrack extends TTrack {
	
	static int n=0;
	static HashMap<Filter, PerspectiveTrack> filterMap = new HashMap<Filter, PerspectiveTrack>();
	
	PerspectiveFilter filter;
	String filterState;
	
	/**
	 * Constructor requires a PerspectiveFilter to control.
	 *
	 * @param filter the filter
	 */
	public PerspectiveTrack(PerspectiveFilter filter) {
		this.filter = filter;
		filterMap.put(filter, this);
		this.viewable = false;
		CircleFootprint c = (CircleFootprint) CircleFootprint.getFootprint("CircleFootprint.Circle"); //$NON-NLS-1$
		c.setColor(filter.getColor());
		c.setSpotShown(false);
		c.setAlpha(0);
    setFootprints(new Footprint[] {c});
    String letter = alphabet.substring(n, n+1);
    setName(MediaRes.getString("Filter.Perspective.Title").toLowerCase()+" "+letter); //$NON-NLS-1$ //$NON-NLS-2$
    Step step = new PerspectiveStep(this, 0, 0, 0);
    step.setFootprint(getFootprint());
    steps = new StepArray(step);
    filter.addPropertyChangeListener(PROPERTY_TTRACK_COLOR, this); //$NON-NLS-1$
    filter.addPropertyChangeListener(PROPERTY_TTRACK_VISIBLE, this); //$NON-NLS-1$
    filter.addPropertyChangeListener(Filter.PROPERTY_FILTER_ENABLED, this); //$NON-NLS-1$
    filter.addPropertyChangeListener(Filter.PROPERTY_FILTER_TAB, this); //$NON-NLS-1$
    filter.addPropertyChangeListener(PerspectiveFilter.PROPERTY_PERSPECTIVEFILTER_CORNERLOCATION, this); //$NON-NLS-1$
    filter.addPropertyChangeListener(PerspectiveFilter.PROPERTY_PERSPECTIVEFILTER_FIXED, this); //$NON-NLS-1$
	}
	
  @Override
	protected void dispose() {
  	super.dispose();
		filterMap.remove(filter);
    filter.removePropertyChangeListener(PROPERTY_TTRACK_COLOR, this); //$NON-NLS-1$
    filter.removePropertyChangeListener(PROPERTY_TTRACK_VISIBLE, this); //$NON-NLS-1$
    filter.removePropertyChangeListener(Filter.PROPERTY_FILTER_ENABLED, this); //$NON-NLS-1$
    filter.removePropertyChangeListener(Filter.PROPERTY_FILTER_TAB, this); //$NON-NLS-1$
    filter.removePropertyChangeListener(PerspectiveFilter.PROPERTY_PERSPECTIVEFILTER_CORNERLOCATION, this); //$NON-NLS-1$
    filter.removePropertyChangeListener(PerspectiveFilter.PROPERTY_PERSPECTIVEFILTER_FIXED, this); //$NON-NLS-1$
  	filter = null;
  }

	/**
	 * Responds to property change events.
	 *
	 * @param e the property change event
	 */
	@Override
	public void propertyChange(PropertyChangeEvent e) {
		switch (e.getPropertyName()) {
		case Filter.PROPERTY_FILTER_COLOR:
			setColor((Color) e.getNewValue());
			break;
		case Filter.PROPERTY_FILTER_ENABLED:
		case Filter.PROPERTY_FILTER_TAB:
		case PROPERTY_TTRACK_VISIBLE:
			if (trackerPanel.getSelectedTrack() == this) {
				trackerPanel.setSelectedPoint(null);
				trackerPanel.selectedSteps.clear();
			}
			boolean visible = filter.hasInspector() && filter.getInspector().isVisible();
			if (visible) {
				trackerPanel.setSelectedTrack(this);
			} else {
				trackerPanel.setSelectedTrack(null);
			}
			break;
		case PerspectiveFilter.PROPERTY_PERSPECTIVEFILTER_FIXED:
			Undo.postFilterEdit(trackerPanel, filter,  new XMLControlElement((String) e.getOldValue()));
			break;
		case PerspectiveFilter.PROPERTY_PERSPECTIVEFILTER_CORNERLOCATION:
			PerspectiveFilter.Corner filtercorner = (PerspectiveFilter.Corner) e.getNewValue();
			int i = filter.getCornerIndex(filtercorner);
			int n = trackerPanel.getFrameNumber();
			if (filter.isInputEnabled() && i < 4) {
				getStep(n).points[i].setXY(filtercorner.getX(), filtercorner.getY());
			}
			break;
		case TrackerPanel.PROPERTY_TRACKERPANEL_SELECTEDTRACK:
			if (e.getNewValue() == this) {
				if (filter.hasInspector() && !filter.getInspector().isVisible()) {
					filter.getInspector().setVisible(true);
				}
			}
		break;
		case TrackerPanel.PROPERTY_TRACKERPANEL_SELECTEDPOINT:
			if (e.getOldValue() != null && filterState != null) {
				TPoint p = (TPoint) e.getOldValue();
				if (p instanceof PerspectiveFilter.Corner) {
					Undo.postFilterEdit(trackerPanel, filter, new XMLControlElement(filterState));
					filterState = null;
				}
			}
			if (e.getNewValue() != null) {
				if (e.getNewValue() instanceof PerspectiveFilter.Corner && filterState == null) {
					filterState = new XMLControlElement(filter).toXML();
				}
			}
			break;
		}
	}
  
  /**
   * Finds the interactive drawable object located at the specified
   * pixel position.
   *
   * @param panel the drawing panel
   * @param xpix the x pixel position on the panel
   * @param ypix the y pixel position on the panel
   * @return the first step TPoint that is hit
   */
  @Override
public Interactive findInteractive(
         DrawingPanel panel, int xpix, int ypix) {
  	partName = null;
  	hint = null;
  	return null;
  }
  
	/**
	 * Prepares menu items and returns a new menu. Subclasses should override this
	 * method and add track-specific menu items.
	 *
	 * @param trackerPanel the tracker panel
	 * @return a menu
	 */
	@Override
	public JMenu getMenu(TrackerPanel trackerPanel, JMenu menu) {
		if (menu == null)
			menu = new JMenu();
		menu.setText(getName("track")); //$NON-NLS-1$
		menu.setIcon(getFootprint().getIcon(21, 16));
		return menu;
	}
	
	/**
	 * Gets a message about this track to display in a message box.
	 *
	 * @return the message
	 */
  @Override
	public String getMessage() {
  	String s = MediaRes.getString("Filter.Perspective.Title").toLowerCase(); //$NON-NLS-1$
  	if (partName != null) 
    	s += " "+partName; //$NON-NLS-1$
    if (isLocked()) {
      hint = TrackerRes.getString("TTrack.Locked.Hint"); //$NON-NLS-1$
    }
    if (Tracker.showHints && hint != null) 
    	s += " ("+hint+")"; //$NON-NLS-1$ //$NON-NLS-2$
    return s;
	}
	
  /**
   * Gets the step associated with a TPoint.
   *
   * @param p a TPoint
   * @param trackerPanel the tracker panel holding the TPoint
   * @return the step associated with the TPoint
   */
  @Override
public Step getStep(TPoint p, TrackerPanel trackerPanel) {
    if (p instanceof PerspectiveFilter.Corner) {
    	PerspectiveFilter.Corner corner = (PerspectiveFilter.Corner)p;
    	int i = filter.getCornerIndex(corner);
    	// set the hint
    	if (i>-1) {
      	partName = getTargetDescription(i);
      	hint = TrackerRes.getString("PerspectiveTrack.Corner.Hint"); //$NON-NLS-1$
    		return getStep(trackerPanel.getFrameNumber());
    	}
    }
    return super.getStep(p, trackerPanel);
  }

  /**
   * Deletes a step. This removes the perspective filter key frame data.
   *
   * @param n the frame number
   * @return the deleted step
   */
  @Override
public Step deleteStep(int n) {
    if (locked) return null;
    TPoint p = trackerPanel.getSelectedPoint();
    if (p instanceof PerspectiveFilter.Corner) {
    	PerspectiveFilter.Corner corner = (PerspectiveFilter.Corner)p;
    	filter.deleteKeyFrame(n, corner);
    	TFrame.repaintT(trackerPanel);
    }
    Step step = getStep(n);
    return step;
  }

  /**
   * Used by autoTracker to mark a step at a match target position. 
   * 
   * @param n the frame number
   * @param x the x target coordinate in image space
   * @param y the y target coordinate in image space
   * @return the TPoint that was automarked
   */
	@Override
  public TPoint autoMarkAt(int n, double x, double y) {
		int index = getTargetIndex();
		PerspectiveStep step = (PerspectiveStep)getStep(n);
		step.points[index].setXY(x, y);
  	filter.setCornerLocation(n, index, x, y);
  	return getMarkedPoint(n, index);
  }
  
  /**
   * Used by autoTracker to get the marked point for a given frame and index. 
   * 
   * @param n the frame number
   * @param index the index
   * @return the step TPoint at the index
   */
  @Override
public TPoint getMarkedPoint(int n, int index) {
  	Step step = getStep(n);
  	return step.points[index];
  }
  
  /**
   * Sets the target index for the autotracker.
   *
   * @param p a TPoint associated with this track
   */
	@Override
  protected void setTargetIndex(TPoint p) {
		Step step = getStep(p, trackerPanel);
		if (step!=null)
  		setTargetIndex(step.getPointIndex(p));  	
  }

  /**
   * Returns a description of a target point with a given index.
   *
   * @param pointIndex the index
   * @return the description
   */
	@Override
  protected String getTargetDescription(int pointIndex) {
		if (pointIndex<4) {
			return TrackerRes.getString("PerspectiveTrack.Corner.Input")+" "+pointIndex; //$NON-NLS-1$ //$NON-NLS-2$
		}
		return TrackerRes.getString("PerspectiveTrack.Corner.Output")+" "+(pointIndex-4); //$NON-NLS-1$ //$NON-NLS-2$
	}

  /**
   * Determines if the given point index is autotrackable.
   *
   * @param pointIndex the points[] index
   * @return true if autotrackable
   */
	@Override
  protected boolean isAutoTrackable(int pointIndex) {
  	return pointIndex<4;
  }
  
  /**
   * Determines if at least one point in this track is autotrackable.
   *
   * @return true if autotrackable
   */
	@Override
  protected boolean isAutoTrackable() {
  	return true;
  }
  

	
	@Override
  public void draw(DrawingPanel panel, Graphics _g) {
  }

	@Override
	public int getStepLength() {
		return 4;
	}

	@Override
	public int getFootprintLength() {
		return 1;
	}

	@Override
	public Step createStep(int n, double x, double y) {
		autoMarkAt(n, x, y);
		return getStep(n);
	}
	
	@Override
  public void remark(TrackerPanel trackerPanel) {
  }



}