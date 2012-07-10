/*******************************************************************************
 * Copyright (c) 2010 Australian Nuclear Science and Technology Organisation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *    Norman Xiong (nxi@Bragg Institute) - initial API and implementation
 ******************************************************************************/
package org.gumtree.vis.awt;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;

import org.gumtree.vis.core.internal.StaticValues;
import org.gumtree.vis.interfaces.IDataset;
import org.gumtree.vis.interfaces.IPlot;
import org.gumtree.vis.mask.AbstractMask;
import org.gumtree.vis.mask.ChartMaskingUtilities;
import org.gumtree.vis.mask.ChartTransferableWithMask;
import org.gumtree.vis.mask.IMaskEventListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ExposedMouseWheelHandler;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.Axis;
import org.jfree.chart.axis.AxisSpace;
import org.jfree.chart.axis.AxisUtilities;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.event.ChartChangeEvent;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.PaintScaleLegend;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.title.Title;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.ExtensionFileFilter;

/**
 * @author nxi
 *
 */
public abstract class JChartPanel extends ChartPanel implements IPlot {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4383623034527952722L;
    public static final String SELECT_MASK_COMMAND = "SELECT_MASK";
    public static final String DESELECT_MASK_COMMAND = "SELECT_NONE";
	private static final Cursor WAIT_CURSOR = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
    protected static final Cursor defaultCursor = Cursor.getPredefinedCursor(
    		Cursor.DEFAULT_CURSOR);
    public static final String REMOVE_SELECTED_MASK_COMMAND = "REMOVE_SELECTED_MASK";
    protected static int maskingKeyMask = InputEvent.SHIFT_MASK;
    protected static int maskingExclusiveMask = InputEvent.ALT_MASK;
    protected static int maskingSelectionMask = InputEvent.SHIFT_MASK;
    private boolean autoUpdate = true;

    private boolean isMouseWheelEnabled = true;
    protected Color[] inclusiveMaskColor;
    protected Color[] exclusiveMaskColor;
    private int horizontalTraceLocation;
	private int verticalTraceLocation;
    private boolean isToolTipFollowerEnabled = true;
    private boolean isMaskingEnabled = true;
    private double chartX;
    private double chartY;
    private int maskDragIndicator = Cursor.DEFAULT_CURSOR;

    //Popup Menu
    private JMenu maskManagementMenu;
    private JMenuItem removeSelectedMaskMenuItem;
    private LinkedHashMap<AbstractMask, Color> maskList;
    private AbstractMask selectedMask;
    private List<IMaskEventListener> maskEventListeners = new ArrayList<IMaskEventListener>();
    
    
    /**
	 * @param chart
	 */
	public JChartPanel(JFreeChart chart) {
		this(
	            chart,
	            StaticValues.PANEL_WIDTH,
	            StaticValues.PANEL_HEIGHT,
	            StaticValues.PANEL_MINIMUM_DRAW_WIDTH,
	            StaticValues.PANEL_MINIMUM_DRAW_HEIGHT,
	            StaticValues.PANEL_MAXIMUM_DRAW_WIDTH,
	            StaticValues.PANEL_MAXIMUM_DRAW_HEIGHT,
	            true,
	            true,  // properties
	            true,  // save
	            true,  // print
	            true,  // zoom
	            true   // tooltips
	        );
	}

	/**
	 * @param chart
	 * @param useBuffer
	 */
	public JChartPanel(JFreeChart chart, boolean useBuffer) {
		this(
	            chart,
	            StaticValues.PANEL_WIDTH,
	            StaticValues.PANEL_HEIGHT,
	            StaticValues.PANEL_MINIMUM_DRAW_WIDTH,
	            StaticValues.PANEL_MINIMUM_DRAW_HEIGHT,
	            StaticValues.PANEL_MAXIMUM_DRAW_WIDTH,
	            StaticValues.PANEL_MAXIMUM_DRAW_HEIGHT,
	            useBuffer,
	            true,  // properties
	            true,  // save
	            true,  // print
	            true,  // zoom
	            true   // tooltips
	        );
	}

	/**
	 * @param chart
	 * @param properties
	 * @param save
	 * @param print
	 * @param zoom
	 * @param tooltips
	 */
	public JChartPanel(JFreeChart chart, boolean properties, boolean save,
			boolean print, boolean zoom, boolean tooltips) {
		this(chart,
				StaticValues.PANEL_WIDTH,
				StaticValues.PANEL_HEIGHT,
				StaticValues.PANEL_MINIMUM_DRAW_WIDTH,
				StaticValues.PANEL_MINIMUM_DRAW_HEIGHT,
				StaticValues.PANEL_MAXIMUM_DRAW_WIDTH,
				StaticValues.PANEL_MAXIMUM_DRAW_HEIGHT,
				true,
				properties,
				save,
				print,
				zoom,
				tooltips
		);
	}

	/**
	 * @param chart
	 * @param width
	 * @param height
	 * @param minimumDrawWidth
	 * @param minimumDrawHeight
	 * @param maximumDrawWidth
	 * @param maximumDrawHeight
	 * @param useBuffer
	 * @param properties
	 * @param save
	 * @param print
	 * @param zoom
	 * @param tooltips
	 */
	public JChartPanel(JFreeChart chart, int width, int height,
			int minimumDrawWidth, int minimumDrawHeight, int maximumDrawWidth,
			int maximumDrawHeight, boolean useBuffer, boolean properties,
			boolean save, boolean print, boolean zoom, boolean tooltips) {
		this(chart, width, height, minimumDrawWidth, minimumDrawHeight,
				maximumDrawWidth, maximumDrawHeight, useBuffer, properties,
				true, save, print, zoom, tooltips);
	}

	/**
	 * @param chart
	 * @param width
	 * @param height
	 * @param minimumDrawWidth
	 * @param minimumDrawHeight
	 * @param maximumDrawWidth
	 * @param maximumDrawHeight
	 * @param useBuffer
	 * @param properties
	 * @param copy
	 * @param save
	 * @param print
	 * @param zoom
	 * @param tooltips
	 */
	public JChartPanel(JFreeChart chart, int width, int height,
			int minimumDrawWidth, int minimumDrawHeight, int maximumDrawWidth,
			int maximumDrawHeight, boolean useBuffer, boolean properties,
			boolean copy, boolean save, boolean print, boolean zoom,
			boolean tooltips) {
		super(chart, width, height, minimumDrawWidth, minimumDrawHeight,
				maximumDrawWidth, maximumDrawHeight, useBuffer, properties,
				copy, save, print, zoom, tooltips);
		maskList = new LinkedHashMap<AbstractMask, Color>();
		addMouseWheelListener(new ExposedMouseWheelHandler(this));
	}

	@Override
	public void processMouseWheelEvent(MouseWheelEvent event) {
		if (isMouseWheelEnabled) {
			for (MouseWheelListener listener: getMouseWheelListeners()) {
				listener.mouseWheelMoved(event);
			}
		}
	}
	
	@Override
	public void setMouseWheelEnabled(boolean flag) {
		isMouseWheelEnabled = flag;
	}
	
	@Override
	public boolean isMouseWheelEnabled() {
		return isMouseWheelEnabled;
	}

	public abstract void moveSelectedMask(int keyCode);

	public void setDataset(IDataset dataset) {
		getXYPlot().setDataset(dataset);
	}

	public ValueAxis getHorizontalAxis() {
		return getChart().getXYPlot().getDomainAxis();
	}

	public XYPlot getXYPlot(){
		return getChart().getXYPlot();
	}
	
	public TextTitle getTitle() {
		return getChart().getTitle();
	}

	public ValueAxis getVerticalAxis() {
		return getXYPlot().getRangeAxis();
	}

	public void setHorizontalAxisFlipped(boolean isFlipped) {
		getXYPlot().getDomainAxis().setInverted(isFlipped);
	}

	public void setVerticalAxisFlipped(boolean isFlipped) {
		getXYPlot().getRangeAxis().setInverted(isFlipped);
	}

	public void restoreHorizontalBounds() {
		restoreAutoDomainBounds();
	}

	public void restoreVerticalBounds() {
		restoreAutoRangeBounds();
	}

	public void zoomInHorizontal(double x, double y) {
		zoomInDomain(x, y);
	}

	public void zoomInVertical(double x, double y) {
		zoomInRange(x, y);
	}

	public void zoomOutHorizontal(double x, double y) {
		zoomOutDomain(x, y);
	}

	public void zoomOutVertical(double x, double y) {
		zoomOutRange(x, y);
	}
	
	@Override
	public IDataset getDataset() {
		return (IDataset) getXYPlot().getDataset();
	}

//	@Override
	public void setBackgroundColor(Color color) {
		getChart().setBackgroundPaint(color);
		setBackground(color);
	}

	@Override
	public void setHorizontalZoomable(boolean isZoomable) {
		setDomainZoomable(isZoomable);
	}

	@Override
	public void setVerticalZoomable(boolean isZoomable) {
		setRangeZoomable(isZoomable);
	}

    protected void createMaskColors(boolean inverted) {
    	int numberOfMaskColors = StaticValues.NUMBER_OF_MASK_COLORS;
    	inclusiveMaskColor = new Color[numberOfMaskColors];
    	exclusiveMaskColor = new Color[numberOfMaskColors];
    	int interval = 155 / numberOfMaskColors;
    	for (int i = 0; i < numberOfMaskColors; i++) {
    		int value = 255 - i * interval;
    		if (inverted) {
        		inclusiveMaskColor[i] = new Color(0, value, 0, 75);
    			exclusiveMaskColor[i] = new Color(value, value, value, 75);
    		} else {
        		inclusiveMaskColor[i] = new Color(0, value, 0, 30);
    			exclusiveMaskColor[i] = new Color(0, 0, value, 30);
    		}
    	}
	}
    
	protected Color getNextMaskColor(boolean isInclusive){
    	Color[] colorSeries = isInclusive ? inclusiveMaskColor : exclusiveMaskColor;
    	for (int i = 0; i < StaticValues.NUMBER_OF_MASK_COLORS; i++) {
    		boolean isUsed = false;
    		for (AbstractMask mask : getMasks()) {
    			if (colorSeries[i].equals(maskList.get(mask))) {
    				isUsed = true;
    				break;
    			}
    		}
    		if (!isUsed) {
    			return colorSeries[i];
    		}
    	}
    	Color lastColor = null;
    	for (int i = getMasks().size() - 1; i >= 0; i--) {
    		AbstractMask mask = getMasks().get(i);
    		if (mask.isInclusive() == isInclusive) {
    			lastColor = maskList.get(getMasks().get(i));
    			break;
    		}
    	}
    	int nextColorIndex = 0;
    	for (int i = 0; i < StaticValues.NUMBER_OF_MASK_COLORS; i++) {
    		if (colorSeries[i].equals(lastColor)) {
    			nextColorIndex = i + 1;
    			if (nextColorIndex >= StaticValues.NUMBER_OF_MASK_COLORS) {
    				nextColorIndex = 0;
    			}
    		}
    	}
    	return colorSeries[nextColorIndex];
    }
    
	protected void addMaskMenu(int x, int y) {
	       if (this.removeSelectedMaskMenuItem != null) {
	        	boolean isRemoveMenuEnabled = false;
	        	if (this.selectedMask != null) {
	        		Rectangle2D screenMask = ChartMaskingUtilities.getMaskFramework(
	        				selectedMask, getScreenDataArea(), getChart());
	        		if (screenMask.contains(x, y)) {
	        			isRemoveMenuEnabled = true;
	        		}
	        	}
	        	this.removeSelectedMaskMenuItem.setEnabled(isRemoveMenuEnabled);
	        	if (isRemoveMenuEnabled) {
	        		removeSelectedMaskMenuItem.setVisible(true);
	        		removeSelectedMaskMenuItem.setText("Remove " + selectedMask.getName());
	        	} else {
	        		//        		removeSelectedMaskMenuItem.setText("Mask Management");
	        		removeSelectedMaskMenuItem.setVisible(false);
	        	}
	        }
	        maskManagementMenu.removeAll();
	        if (maskList.size() > 0) {
	        	maskManagementMenu.setEnabled(true);
	        	JMenuItem selectNoneMaskItem = new JRadioButtonMenuItem();
	        	selectNoneMaskItem.setText("Select None");
	        	selectNoneMaskItem.setActionCommand(DESELECT_MASK_COMMAND);
	        	selectNoneMaskItem.addActionListener(this);
	        	maskManagementMenu.add(selectNoneMaskItem);
	        	boolean isInShade = false;
	        	for (AbstractMask mask : maskList.keySet()) {
	        		Rectangle2D screenMask = ChartMaskingUtilities.getMaskFramework(
	        				mask, getScreenDataArea(), getChart());
	        		if (screenMask.contains(x, y)) {
	        			JMenuItem selectMaskItem = new JRadioButtonMenuItem();
	        			selectMaskItem.setText("Select " + mask.getName());
	        			selectMaskItem.setActionCommand(SELECT_MASK_COMMAND 
	        					+ "-" + mask.getName());
	        			if (mask == selectedMask) {
	        				selectMaskItem.setSelected(true);
	        			}
	        			selectMaskItem.addActionListener(this);
	        			maskManagementMenu.add(selectMaskItem);
	        			isInShade = true;
	        		}
	        	}
	        	if (isInShade) {
	        		if (selectedMask == null) {
	        			selectNoneMaskItem.setSelected(true);
	        		}
	        	} else {
	        		for (AbstractMask mask : getMasks()) {
	        			JMenuItem selectMaskItem = new JRadioButtonMenuItem();
	        			selectMaskItem.setText("Select " + mask.getName());
	        			selectMaskItem.setActionCommand(SELECT_MASK_COMMAND 
	        					+ "-" + mask.getName());
	        			if (mask == selectedMask) {
	        				selectMaskItem.setSelected(true);
	        			}
	        			selectMaskItem.addActionListener(this);
	        			maskManagementMenu.add(selectMaskItem);
	        		}
	        		selectNoneMaskItem.setSelected(selectedMask == null);
	        	}
	        } else {
	        	maskManagementMenu.setEnabled(false);
	        }
	}

	@Override
	public void paintComponent(Graphics g) {
//		long time = System.currentTimeMillis();
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g.create();
//		ChartMaskingUtilities.drawDomainMask(g2, getScreenDataArea(), maskList, 
//				selectedMask, getChart());
		if (isMaskingEnabled) {
			ChartMaskingUtilities.drawMasks(g2, getScreenDataArea(), maskList, 
					selectedMask, getChart());
		}
		if (getHorizontalAxisTrace()) {
			drawHorizontalAxisTrace(g2, horizontalTraceLocation);
		}
		if (getVerticalAxisTrace()) {
			drawVerticalAxisTrace(g2, verticalTraceLocation);
		}
		if (isToolTipFollowerEnabled) {
			drawToolTipFollower(g2, horizontalTraceLocation, verticalTraceLocation);
		}
//		long diff = System.currentTimeMillis() - time;
//		if (diff > 100) {
//			System.out.println("refreshing cost: " + diff);
//		}
	}

	/**
     * Draws a vertical line used to trace the mouse position to the horizontal
     * axis.
     *
     * @param g2 the graphics device.
     * @param x  the x-coordinate of the trace line.
     */
    private void drawHorizontalAxisTrace(Graphics2D g2, int x) {

    	Rectangle2D dataArea = getScreenDataArea();
    	if (((int) dataArea.getMinX() < x) && (x < (int) dataArea.getMaxX())) {
    		g2.setPaint(getAxisTraceColor());
    		g2.setStroke(new BasicStroke(0.25f));
        	g2.draw(new Line2D.Float(x,
                    (int) dataArea.getMinY(), x, (int) dataArea.getMaxY()));
    	}
    }

	/**
     * Draws a horizontal line used to trace the mouse position to the vertical
     * axis.
     *
     * @param g2 the graphics device.
     * @param y  the y-coordinate of the trace line.
     */
    private void drawVerticalAxisTrace(Graphics2D g2, int y) {

        Rectangle2D dataArea = getScreenDataArea();
        if (((int) dataArea.getMinY() < y) && (y < (int) dataArea.getMaxY())) {
        	g2.setPaint(getAxisTraceColor());
        	g2.setStroke(new BasicStroke(0.25f));
        	g2.draw(new Line2D.Float((int) dataArea.getMinX(), y, (int) dataArea.getMaxX(),
        			y));
        }
    }

    protected abstract void drawToolTipFollower(Graphics2D g2, int x, int y);

	public List<AbstractMask> getMasks() {
		return new ArrayList<AbstractMask>(maskList.keySet());
	}
	
	protected LinkedHashMap<AbstractMask, Color> getMaskMap() {
		return maskList;
	}
	
	protected abstract Color getAxisTraceColor();
	
	public boolean isToolTipFollowerEnabled() {
		return isToolTipFollowerEnabled;
	}
	
	/**
	 * @return the horizontalTraceLocation
	 */
	protected int getHorizontalTraceLocation() {
		return horizontalTraceLocation;
	}

	/**
	 * @param horizontalTraceLocation the horizontalTraceLocation to set
	 */
	protected void setHorizontalTraceLocation(int horizontalTraceLocation) {
		this.horizontalTraceLocation = horizontalTraceLocation;
	}

	/**
	 * @return the verticalTraceLocation
	 */
	protected int getVerticalTraceLocation() {
		return verticalTraceLocation;
	}

	/**
	 * @param verticalTraceLocation the verticalTraceLocation to set
	 */
	protected void setVerticalTraceLocation(int verticalTraceLocation) {
		this.verticalTraceLocation = verticalTraceLocation;
	}

	@Override
    public void doCopy() {
        final Clipboard systemClipboard
                = Toolkit.getDefaultToolkit().getSystemClipboard();
        Rectangle2D screenArea = getScreenDataArea();
        final ChartTransferableWithMask selection = new ChartTransferableWithMask(
        		getChart(), getWidth(), getHeight(), screenArea, maskList);
        //TODO: the below command take too long to run. 6 seconds for Wombat data. 
        Cursor currentCursor = getCursor();
        setCursor(WAIT_CURSOR);
        systemClipboard.setContents(selection, null);
        setCursor(currentCursor);
    }

//	@Override
//	public void doSaveAs() throws IOException {
//
//		Display.getDefault().asyncExec(new Runnable() {
//			
//			Shell shell;
//			
//			private void handleException(Exception e) {
//				if (shell != null) {
//					MessageDialog.openError(shell, "Failed to Save", "failed to save " +
//							"the image: " + e.getMessage());
//					
//				}
//			}
//			
//			@Override
//			public void run() {
//				try {
//					shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
//				}catch (Exception e) {
//					e.printStackTrace();
//				}
//				if (shell != null) {
//					FileDialog fileDialog = new FileDialog(shell, SWT.SAVE);
//					String[] extensions = {"*.png", "*.jpg"};
//					String[] typeNames = {"PNG IMAGE FILE",  "JPEG IMAGE FILE"};
//					String systemSavePath = System.getProperty("SYSTEM_SAVE_PATH");
//					if (systemSavePath != null) {
//						fileDialog.setFilterPath(systemSavePath);
//					}
//					fileDialog.setFilterExtensions(extensions);
//					fileDialog.setFilterNames(typeNames);
//					String filename = fileDialog.open();
//
//					if (filename != null) {
////						int filterIndex = fileDialog.getFilterIndex();
////						if (filterIndex == 0) {
////							if (!filename.endsWith(".png")) {
////								filename = filename + ".png";
////							} 
////							try {
////				           		ChartMaskingUtilities.writeChartAsPNG(new File(filename), getChart(), 
////			            				getWidth(), getHeight(), null, getScreenDataArea(), 
////			            				getMasks());
////						} catch (IOException e) {
////								handleException(e);
////							}
////						} else if (filterIndex == 1) {
////							if (!filename.endsWith(".jpg")) {
////								filename = filename + ".jpg";
////							}
////							try {
////			            		ChartMaskingUtilities.writeChartAsJPEG(new File(filename), getChart(),
////			            				getWidth(), getHeight(), null, getScreenDataArea(), getMasks());
////							} catch (IOException e) {
////								handleException(e);
////							}
////						}
//						int filterIndex = fileDialog.getFilterIndex();
//						String fileType = "png";
//						if (filterIndex == 0) {
//							fileType = "png";
//						} else if (filterIndex == 1) {
//							fileType = "jpg";
//						}
//						try {
//							saveTo(filename, fileType);
//						} catch (IOException e) {
//							handleException(e);
//						}
//						System.setProperty("SYSTEM_SAVE_PATH", fileDialog.getFilterPath());
//					}
//				} else {
//					try {
//						superDoSaveAs();
//					} catch (IOException e) {
//						handleException(e);
//					}
//				}
//				
//			}
//
//		});
//	}
	
	@Override
    public void doSaveAs() throws IOException {

        JFileChooser fileChooser = new JFileChooser();
        String currentDirectory = System.getProperty(StaticValues.SYSTEM_SAVE_PATH_LABEL);
        if (currentDirectory != null) {
        	File savePath = new File(currentDirectory);
        	if (savePath.exists() && savePath.isDirectory()) {
        		fileChooser.setCurrentDirectory(savePath);
        	}
        }
        ExtensionFileFilter ascFilter = new ExtensionFileFilter("Text_Files", ".txt");
        ExtensionFileFilter jpgFilter = new ExtensionFileFilter("JPG_Image_Files", ".jpg");
        ExtensionFileFilter pngFilter = new ExtensionFileFilter("PNG_Image_Files", ".png");
        fileChooser.addChoosableFileFilter(pngFilter);
        fileChooser.addChoosableFileFilter(jpgFilter);
        fileChooser.addChoosableFileFilter(ascFilter);
        fileChooser.setFileFilter(jpgFilter);
        int option = fileChooser.showSaveDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
        	String filename = fileChooser.getSelectedFile().getPath();
        	String selectedDescription = fileChooser.getFileFilter().getDescription();
        	String fileExtension = StaticValues.DEFAULT_IMAGE_FILE_EXTENSION;
        	if (selectedDescription.toLowerCase().contains("png")) {
        		fileExtension = "png";
        		if (!filename.toLowerCase().endsWith(".png")) {
        			filename = filename + ".png";
        		}
        	} else if (selectedDescription.toLowerCase().contains("jpg")) {
        		fileExtension = "jpg";
        		if (!filename.toLowerCase().endsWith(".jpg")) {
        			filename = filename + ".jpg";
        		}
        	} else if (selectedDescription.toLowerCase().contains("text")) {
        		fileExtension = "txt";
        		if (!filename.toLowerCase().endsWith(".txt")) {
        			filename = filename + ".txt";
        		}
        	}
        	File selectedFile = new File(filename);
        	int confirm = JOptionPane.YES_OPTION;
        	if (selectedFile.exists()) {
        		confirm = JOptionPane.showConfirmDialog(this, selectedFile.getName() + " exists, overwrite?", 
        				"Confirm Overwriting", JOptionPane.YES_NO_OPTION);
        	}
        	if (confirm == JOptionPane.YES_OPTION) {
        		saveTo(filename, fileExtension);
        		System.setProperty(StaticValues.SYSTEM_SAVE_PATH_LABEL, 
        				fileChooser.getSelectedFile().getParent());
        	}
        }
	}
	
	public void saveTo(String filename, String fileType) 
	throws IOException{
		int filterIndex = 0;
		if (fileType != null) {
			if (fileType.toLowerCase().contains("png")) {
				filterIndex = 0;
			} else if (fileType.toLowerCase().contains("jpg") || 
					fileType.toLowerCase().contains("jpeg")) {
				filterIndex = 1;
			} else if (fileType.toLowerCase().contains("txt")) {
				filterIndex = 2;
			} 
		}
		if (filterIndex == 0) {
			ChartMaskingUtilities.writeChartAsPNG(new File(filename), getChart(), 
					getWidth(), getHeight(), null, getScreenDataArea(), 
					maskList);
		} else if (filterIndex == 1) {
			ChartMaskingUtilities.writeChartAsJPEG(new File(filename), getChart(),
					getWidth(), getHeight(), null, getScreenDataArea(), maskList);
		} else if (filterIndex == 2) {
			FileWriter fw = new FileWriter(filename);
			BufferedWriter writer = new BufferedWriter (fw);
			saveAsText(writer);
			writer.close();
			fw.close();
		}
	}
	
	protected abstract void saveAsText(BufferedWriter writer) throws IOException;
	
	
 	/**
     * Prints the chart on a single page.
     *
     * @param g  the graphics context.
     * @param pf  the page format to use.
     * @param pageIndex  the index of the page. If not <code>0</code>, nothing
     *                   gets print.
     *
     * @return The result of printing.
     */
    @Override
    public int print(Graphics g, PageFormat pf, int pageIndex) {

        if (pageIndex != 0) {
            return NO_SUCH_PAGE;
        }
        Graphics2D g2 = (Graphics2D) g;
        double x = pf.getImageableX();
        double y = pf.getImageableY();
        double w = pf.getImageableWidth();
        double h = pf.getImageableHeight();
        double screenWidth = getWidth();
        double screenHeight = getHeight();
        double widthRatio = w / screenWidth;
        double heightRatio = h / screenHeight;
        double overallRatio = 1;
        overallRatio = widthRatio < heightRatio ? widthRatio : heightRatio;
        Rectangle2D printArea = new Rectangle2D.Double(x, y, screenWidth * overallRatio, 
        		screenHeight * overallRatio);
        draw(g2, printArea, 0, 0);
        return PAGE_EXISTS;
    }

    @Override
    public void createChartPrintJob() {
    	setCursor(WAIT_CURSOR);
    	PrinterJob job = PrinterJob.getPrinterJob();
    	PageFormat pf = job.defaultPage();
    	PageFormat pf2 = job.pageDialog(pf);
    	if (pf2 != pf) {
    		job.setPrintable(this, pf2);
    		try {
    			job.print();
    		}
    		catch (PrinterException e) {
    			JOptionPane.showMessageDialog(this, e);
    		} finally {
    			setCursor(defaultCursor);
    		}
    	}
    	setCursor(defaultCursor);
    }
    
    protected void selectMask(String maskName) {
    	if (maskName == null) {
    		selectedMask = null;
    	} else {
    		for (AbstractMask mask : getMasks()) {
    			if (maskName.equals(mask.getName())) {
    				selectedMask = mask;
    				break;
    			}
    		}
    	}
    }
   
    public void removeSelectedMask() {
    	if (selectedMask != null) {
    		maskList.remove(selectedMask);
    		final AbstractMask toRemove = selectedMask;
    		selectedMask = null;
    		repaint();
    		fireMaskRemovalEvent(toRemove);
    	}
	}

	@Override
	protected JPopupMenu createPopupMenu(boolean properties, boolean copy,
			boolean save, boolean print, boolean zoom) {
		JPopupMenu menu = super.createPopupMenu(properties, copy, save, print, zoom);
        
        this.removeSelectedMaskMenuItem = new JMenuItem();
        this.removeSelectedMaskMenuItem.setActionCommand(REMOVE_SELECTED_MASK_COMMAND);
        this.removeSelectedMaskMenuItem.addActionListener(this);
        menu.addSeparator();
        menu.add(removeSelectedMaskMenuItem);
        maskManagementMenu = new JMenu("Mask Management");
        menu.add(maskManagementMenu);

        return menu;
	}
	
	@Override
	protected void displayPopupMenu(int x, int y) {
		addMaskMenu(x, y);
		super.displayPopupMenu(x, y);
	}
	
	@Override
	public void actionPerformed(ActionEvent event) {
		String command = event.getActionCommand();
		if (command.equals(REMOVE_SELECTED_MASK_COMMAND)) {
        	removeSelectedMask();
        	repaint();
        } else if (command.equals(DESELECT_MASK_COMMAND)) {
        	selectMask(Double.NaN, Double.NaN);
        	repaint();
        } else if (command.startsWith(SELECT_MASK_COMMAND)) {
        	String[] commands = command.split("-", 2);
        	if (commands.length > 1) {
        		selectMask(commands[1]);
            	repaint();
        	}
        } else {
        	super.actionPerformed(event);
        }
	}
	
	protected abstract void selectMask(double x, double y);

	/**
	 * @return the selectedMask
	 */
	public AbstractMask getSelectedMask() {
		return selectedMask;
	}

	/**
	 * @param selectedMask the selectedMask to set
	 */
	public void setSelectedMask(AbstractMask selectedMask) {
		this.selectedMask = selectedMask;
	}

	/**
	 * @return the maskDragIndicator
	 */
	protected int getMaskDragIndicator() {
		return maskDragIndicator;
	}

	/**
	 * @param maskDragIndicator the maskDragIndicator to set
	 */
	protected void setMaskDragIndicator(int maskDragIndicator) {
		this.maskDragIndicator = maskDragIndicator;
	}

	/**
	 * @return the chartX
	 */
	public double getChartX() {
		return chartX;
	}

	/**
	 * @param chartX the chartX to set
	 */
	protected void setChartX(double chartX) {
		this.chartX = chartX;
	}

	/**
	 * @return the chartY
	 */
	public double getChartY() {
		return chartY;
	}

	/**
	 * @param chartY the chartY to set
	 */
	protected void setChartY(double chartY) {
		this.chartY = chartY;
	}
	
	@Override
    public void setCursor(Cursor arg0) {
    	super.setCursor(arg0);
    	getParent().setCursor(arg0);
    }
	
	@Override
	public void mouseMoved(MouseEvent e) {
//        if (isMaskingEnabled() && (e.getModifiers() & maskingKeyMask) != 0) {
		if (isMaskingEnabled()) {
        	int cursorType = findSelectedMask(e.getX(), e.getY());
        	setCursor(Cursor.getPredefinedCursor(cursorType));
        } else if (getCursor() != defaultCursor) {
        	setCursor(defaultCursor);
        }
	}
	
	protected abstract int findSelectedMask(int x, int y);
	
    protected Point2D translateScreenToChart(Point2D point) {
        EntityCollection entities = getChartRenderingInfo().getEntityCollection();
        ChartEntity entity = entities.getEntity(point.getX(), point.getY());
        if (entity instanceof XYItemEntity) {
        	XYDataset dataset = ((XYItemEntity) entity).getDataset();
        	int item = ((XYItemEntity) entity).getItem();
        	double chartX = dataset.getXValue(0, item);
        	double chartY = dataset.getYValue(0, item);
//        	double chartZ = ((XYZDataset) dataset).getZValue(0, item);
        	return new Point2D.Double(chartX, chartY);
        } 
        return null;
	}
    
    /**
     * Returns a point based on (x, y) but constrained to be within the bounds
     * of the given rectangle.  This method could be moved to JCommon.
     *
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     * @param area  the rectangle (<code>null</code> not permitted).
     *
     * @return A point within the rectangle.
     */
    protected Point2D getPointInRectangle(int x, int y, Rectangle2D area) {
        double xx = Math.max(area.getMinX(), Math.min(x, area.getMaxX()));
        double yy = Math.max(area.getMinY(), Math.min(y, area.getMaxY()));
        return new Point2D.Double(xx, yy);
    }
    
    public void setToolTipFollowerEnabled(boolean enabled){
    	isToolTipFollowerEnabled = enabled;
    }
    
    @Override
    public void mouseExited(MouseEvent e) {
    	setHorizontalTraceLocation(-1);
    	setVerticalTraceLocation(-1);
    	repaint();
    	super.mouseExited(e);
    }

    @Override
    public Image getImage() {
		BufferedImage image = new BufferedImage(getWidth(), getHeight(), 
				BufferedImage.TYPE_INT_RGB);
		Graphics2D g2 = image.createGraphics();
//		gc2.setBackground(Color.white);
		g2.setPaint(Color.white);
		g2.fill(new Rectangle2D.Double(0, 0, getWidth(), getHeight()));
		if (getChart() != null) {
			Image chartImage = getChart().createBufferedImage((int) getWidth(),
					(int) getHeight());
			g2.drawImage(chartImage, 0, 0, this);
			ChartMaskingUtilities.drawMasks(g2, getScreenDataArea(), maskList, 
					null, getChart());
		}
		g2.dispose();
		return image;
    }
    
    @Override
    public void draw(Graphics2D g2, Rectangle2D area, 
    		double shiftX, double shiftY) {
//    	g2.setPaint(Color.white);
//		g2.fill(new Rectangle2D.Double(0, 0, getWidth(), getHeight()));
//		if (getChart() != null) {
////			Image chartImage = getChart().createBufferedImage((int) area.getWidth(),
////					(int) area.getHeight());
////			g2.drawImage(chartImage, (int) area.getMinX(), (int) area.getMinY(), 
////					this);
//			getChart().draw(g2, area, getAnchor(), null);
//			ChartMaskingUtilities.drawMasks(g2, getScreenDataArea(), maskList, 
//					null, getChart());
//		}
        double widthRatio = area.getWidth() / getWidth();
        double heightRatio = area.getHeight() / getHeight();
        double overallRatio = 1;
        overallRatio = widthRatio < heightRatio ? widthRatio : heightRatio;

        XYPlot plot = (XYPlot) getChart().getPlot();
        Font domainFont = plot.getDomainAxis().getLabelFont();
        int domainSize = domainFont.getSize();
        Font rangeFont = plot.getRangeAxis().getLabelFont();
        int rangeSize = rangeFont.getSize();
        TextTitle titleBlock = getChart().getTitle();
        Font titleFont = null;
        int titleSize = 0;
        if (titleBlock != null) {
        	titleFont = titleBlock.getFont();
        	titleSize = titleFont.getSize();
        	getChart().getTitle().setFont(titleFont.deriveFont(
        			(float) (titleSize * overallRatio)));
        }
        Font domainScaleFont = plot.getDomainAxis().getTickLabelFont();
        int domainScaleSize = domainScaleFont.getSize();
        Font rangeScaleFont = plot.getRangeAxis().getTickLabelFont();
        int rangeScaleSize = rangeScaleFont.getSize();
        plot.getDomainAxis().setLabelFont(domainFont.deriveFont(
        		(float) (domainSize * overallRatio)));
        plot.getRangeAxis().setLabelFont(rangeFont.deriveFont(
        		(float) (rangeSize * overallRatio)));
        plot.getDomainAxis().setTickLabelFont(domainScaleFont.deriveFont(
        		(float) (domainScaleSize * overallRatio)));
        plot.getRangeAxis().setTickLabelFont(rangeScaleFont.deriveFont(
        		(float) (rangeScaleSize * overallRatio)));
        LegendTitle legend = getChart().getLegend();
        Font legendFont = null;
        int legendFontSize = 0;
        if (legend != null) {
        	legendFont = legend.getItemFont();
        	legendFontSize = legendFont.getSize();
        	legend.setItemFont(legendFont.deriveFont(
        			(float) (legendFontSize * overallRatio)));
        }
        
        Rectangle2D chartArea = (Rectangle2D) area.clone();
        getChart().getPadding().trim(chartArea);
        if (titleBlock != null) {
        	AxisUtilities.trimTitle(chartArea, g2, titleBlock, titleBlock.getPosition());
        }
        
        Axis scaleAxis = null;
        Font scaleAxisFont = null;
        int scaleAxisFontSize = 0;
        for (Object object : getChart().getSubtitles()) {
        	Title title = (Title) object;
        	if (title instanceof PaintScaleLegend) {
        		scaleAxis = ((PaintScaleLegend) title).getAxis();
        		scaleAxisFont = scaleAxis.getTickLabelFont();
        		scaleAxisFontSize = scaleAxisFont.getSize();
        		scaleAxis.setTickLabelFont(scaleAxisFont.deriveFont(
        				(float) (scaleAxisFontSize * overallRatio)));
        	}
        	AxisUtilities.trimTitle(chartArea, g2, title, title.getPosition());
        }
        AxisSpace axisSpace = AxisUtilities.calculateAxisSpace(
        		getChart().getXYPlot(), g2, chartArea);
        Rectangle2D dataArea = axisSpace.shrink(chartArea, null);
        getChart().getXYPlot().getInsets().trim(dataArea);
        getChart().getXYPlot().getAxisOffset().trim(dataArea);
        
//        Rectangle2D screenArea = getScreenDataArea();
//        Rectangle2D visibleArea = getVisibleRect();
//        Rectangle2D printScreenArea = new Rectangle2D.Double(screenArea.getMinX() * overallRatio + x, 
//        		screenArea.getMinY() * overallRatio + y, 
//        		printArea.getWidth() - visibleArea.getWidth() + screenArea.getWidth(), 
//        		printArea.getHeight() - visibleArea.getHeight() + screenArea.getHeight());

        getChart().draw(g2, area, getAnchor(), null);
        ChartMaskingUtilities.drawMasks(g2, dataArea, 
        		maskList, null, getChart(), overallRatio);
        plot.getDomainAxis().setLabelFont(domainFont);
        plot.getRangeAxis().setLabelFont(rangeFont);
        if (titleBlock != null) {
        	titleBlock.setFont(titleFont);
        }
        if (legend != null) {
        	legend.setItemFont(legendFont);
        }
        plot.getDomainAxis().setTickLabelFont(domainScaleFont);
        plot.getRangeAxis().setTickLabelFont(rangeScaleFont);
        if (scaleAxis != null) {
        	scaleAxis.setTickLabelFont(scaleAxisFont);
        }
//        System.out.println("print " + titleBlock.getText() + 
//        		" at [" + area.getX() + ", " + area.getY() + ", " + 
//        		area.getWidth() + ", " + area.getHeight() + "]");
    }
    
    @Override
    public void updatePlot() {
    	setRefreshBuffer(true);
    	repaint();
    }
    
    @Override
    public void updateLabels() {
    	XYPlot xyPlot = getChart().getXYPlot();
    	XYDataset xyDataset = xyPlot.getDataset();
    	if (xyDataset instanceof IDataset) {
    		IDataset dataset = (IDataset) xyDataset;
    		try{
    			String title = "";
    			if (dataset.getXTitle() != null) {
    				title += dataset.getXTitle();
    			}
    			if (dataset.getXUnits() != null) {
    				title += " (" + dataset.getXUnits() + ")";
    			}
    			xyPlot.getDomainAxis().setLabel(title);
    			title = "";
    			if (dataset.getYTitle() != null) {
    				title += dataset.getYTitle();
    			}
    			if (dataset.getYUnits() != null) {
    				title += " (" + dataset.getYUnits() + ")";
    			}
    			xyPlot.getRangeAxis().setLabel(title);
    			title = "";
    			if (dataset.getTitle() != null) {
    				title = dataset.getTitle();
    			}
    			getChart().getTitle().setText(title);
    		} catch (Exception e) {
			}
    	}
    }
    
    @Override
    public void addMask(AbstractMask mask) {
    	Color newColor = getNextMaskColor(mask.isInclusive());
//    	mask.setFillColor(newColor);
    	if (maskList.containsKey(mask)) {
    		return;
    	}
    	maskList.put(mask, newColor);
    	fireMaskCreationEvent(mask);
    }
    
    @Override
    public void addMasks(List<AbstractMask> maskList) {
    	for (AbstractMask mask : maskList) {
    		addMask(mask);
    	}
    }
    
    @Override
    public void setMaskingEnabled(boolean enabled) {
    	isMaskingEnabled = enabled;
    }
    
    @Override
    public boolean isMaskingEnabled() {
    	return isMaskingEnabled;
    }
    
    @Override
    public void removeMask(AbstractMask mask) {
    	if (selectedMask == mask) {
    		selectedMask = null;
    	}
    	if (maskList.containsKey(mask)) {
    		maskList.remove(mask);
    		fireMaskRemovalEvent(mask);
    	}
    }
    
    @Override
    public boolean isHorizontalAxisFlipped() {
    	return getXYPlot().getDomainAxis().isInverted();
    }
    
    @Override
    public boolean isVerticalAxisFlipped() {
    	return getXYPlot().getRangeAxis().isInverted();
    }

    @Override
    public void addMaskEventListener(IMaskEventListener listener) {
    	maskEventListeners.add(listener);
    }
    
    @Override
    public void removeMaskEventListener(IMaskEventListener listener) {
    	maskEventListeners.remove(listener);
    }
    
    protected List<IMaskEventListener> getMaskEventListeners() {
    	return maskEventListeners;
    }

	protected void fireMaskUpdateEvent(AbstractMask mask) {
		for (IMaskEventListener listener : maskEventListeners) {
			listener.maskUpdated(mask);
		}
	}
	
	protected void fireMaskCreationEvent(AbstractMask mask) {
		for (IMaskEventListener listener : maskEventListeners) {
			listener.maskAdded(mask);
		}
	}

	protected void fireMaskRemovalEvent(AbstractMask mask) {
		for (IMaskEventListener listener : maskEventListeners) {
			listener.maskRemoved(mask);
		}
	}

	/**
	 * @param autoUpdate the autoUpdate to set
	 */
	public void setAutoUpdate(boolean autoUpdate) {
		this.autoUpdate = autoUpdate;
	}

	/**
	 * @return the autoUpdate
	 */
	public boolean isAutoUpdate() {
		return autoUpdate;
	}

	@Override
	public void chartChanged(ChartChangeEvent event) {
		if (autoUpdate) {
			super.chartChanged(event);
		}
	}
	
	@Override
	public void setPlotTitle(String title) {
		JFreeChart chart = getChart();
		chart.setTitle(title);
	}
	
	@Override
	public void cleanUp() {
		
	}
}
