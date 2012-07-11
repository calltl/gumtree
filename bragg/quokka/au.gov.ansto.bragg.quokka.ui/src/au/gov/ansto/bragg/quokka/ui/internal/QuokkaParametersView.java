/*******************************************************************************
 * Copyright (c) 2009 Australian Nuclear Science and Technology Organisation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	Based on AnalysisParametersView by Norman Xiong.
 *  Paul Hathaway, August 2009
 *******************************************************************************/
package au.gov.ansto.bragg.quokka.ui.internal;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.ViewPart;
import org.gumtree.data.Factory;
import org.gumtree.data.interfaces.IDataset;
import org.gumtree.data.interfaces.IGroup;
import org.slf4j.LoggerFactory;

import au.gov.ansto.bragg.cicada.core.Algorithm;
import au.gov.ansto.bragg.cicada.core.AlgorithmConfiguration;
import au.gov.ansto.bragg.cicada.core.AlgorithmInput;
import au.gov.ansto.bragg.cicada.core.Format;
import au.gov.ansto.bragg.cicada.core.exception.FailedToExecuteException;
import au.gov.ansto.bragg.cicada.core.exception.NoneAlgorithmException;
import au.gov.ansto.bragg.cicada.core.exception.TransferFailedException;
import au.gov.ansto.bragg.cicada.core.exception.TunerNotReadyException;
import au.gov.ansto.bragg.datastructures.util.ConverterLib;
import au.gov.ansto.bragg.kakadu.core.AlgorithmTask;
import au.gov.ansto.bragg.kakadu.core.AlgorithmTaskStatusListener;
import au.gov.ansto.bragg.kakadu.core.DataSourceManager;
import au.gov.ansto.bragg.kakadu.core.DataSourceManager.DataSelectionListener;
import au.gov.ansto.bragg.kakadu.core.OperationManager;
import au.gov.ansto.bragg.kakadu.core.UIAlgorithmManager;
import au.gov.ansto.bragg.kakadu.core.data.Operation;
import au.gov.ansto.bragg.kakadu.core.data.OperationDataListener;
import au.gov.ansto.bragg.kakadu.core.data.OperationParameter;
import au.gov.ansto.bragg.kakadu.ui.Activator;
import au.gov.ansto.bragg.kakadu.ui.ProjectManager;
import au.gov.ansto.bragg.kakadu.ui.SWTResourceManager;
import au.gov.ansto.bragg.kakadu.ui.editors.AlgorithmTaskEditor;
import au.gov.ansto.bragg.kakadu.ui.editors.BooleanOperationParameterEditor;
import au.gov.ansto.bragg.kakadu.ui.editors.DefaultOperationParameterEditor;
import au.gov.ansto.bragg.kakadu.ui.editors.NumericOperationParameterEditor;
import au.gov.ansto.bragg.kakadu.ui.editors.OperationParameterEditor;
import au.gov.ansto.bragg.kakadu.ui.editors.OptionOperationParameterEditor;
import au.gov.ansto.bragg.kakadu.ui.editors.RegionOperationParameterEditor;
import au.gov.ansto.bragg.kakadu.ui.editors.StepDirectionOperationParameterEditor;
import au.gov.ansto.bragg.kakadu.ui.editors.TextOperationParameterEditor;
import au.gov.ansto.bragg.kakadu.ui.editors.UriOperationParameterEditor;
import au.gov.ansto.bragg.kakadu.ui.plot.PlotManager;
import au.gov.ansto.bragg.kakadu.ui.region.RegionParameterManager;
import au.gov.ansto.bragg.kakadu.ui.util.DisplayManager;
import au.gov.ansto.bragg.kakadu.ui.util.Util;
import au.gov.ansto.bragg.kakadu.ui.views.ButtonClickListener;
import au.gov.ansto.bragg.process.port.Tuner;

/**
 * The view displays operation parameters for all operations in the 
 * current AlgorithmTask.
 */
public class QuokkaParametersView extends ViewPart {

	protected final static String TUNER_EFFICIENCY_MAP_URI = "frame.efficiency.mapUri";
	protected final static String TUNER_BACKGROUND_MAP_URI = "frame.scatterBackgroundUri";
	private final static String KAKADU_UI_CONFIGURATION_FILENAME = "kakaduUI.Configuration.xml";
	private final static String CURRENT_DIR_OBJECT_NAME = "currentDir";

	private final static String[] SAVE_TUNERS = new String[]{
		TUNER_BACKGROUND_MAP_URI,
		TUNER_EFFICIENCY_MAP_URI
	};

	private String tunerEfficiencyMapUri;
	private String tunerBackgroundMapUri;

	protected Composite parametersComposite;
	protected Composite parameterEditorsHolderComposite;
	protected Composite parameterGroupButtonsComposite;
	protected Button defaultParametersButton;
	protected Button applyParametersButton;
	protected Button revertParametersButton;
	protected Button configurationButton;
	protected SelectionListener configurationSelectionListener;
	private Menu configurationMenu;
	private MenuItem saveMenuItem;
	protected Button startOperationButton;	
	protected ScrolledComposite parameterEditorsScrolledComposite;
	protected AlgorithmTaskStatusListener statusListener;
	private static List<ButtonClickListener> stopButtonListenerList;
	boolean isOperationPrepared = false;
	private FormToolkit toolkit;
	private Form form;

	public QuokkaParametersView() {
	}

	protected final OperationParameterEditor.ChangeListener parameterEditorChangeListener = new OperationParameterEditor.ChangeListener() {
		public void dataChanged(Object oldData, Object newData) {
			updateParametersButtons();
			try {
				for (Operation operation : operations) {
					currentTask.applyParameterChangesForAllDataItems(operation);
				}
			} catch (Exception e) {
				handleException(e);
			}

		}
	};
	
	protected final SelectionListener applyParametersListener = new SelectionListener() {
		public void widgetDefaultSelected(SelectionEvent e) {
//			applyParameters();
		}
		public void widgetSelected(SelectionEvent e) {
			applyParameters();

		}
	};
	
	//Associate the view with active AlgorithmTaskEditor
	final IPartListener2 catchAlgorithmTaskEditorPartListener = new IPartListener2(){

		public void partActivated(IWorkbenchPartReference partRef) {
			final IWorkbenchPart part = partRef.getPart(false);
			if (part != null && part instanceof AlgorithmTaskEditor) {
				final AlgorithmTask algorithmTask = ((AlgorithmTaskEditor) part).getAlgorithmTask();
				if (QuokkaParametersView.this.currentTask != algorithmTask) {
					setAlgorithmTask(algorithmTask);
				}
			}
		}

		public void partBroughtToTop(IWorkbenchPartReference partRef) {
		}

		public void partClosed(IWorkbenchPartReference partRef) {
			final IWorkbenchPart part = partRef.getPart(false);
			if (part != null && part instanceof AlgorithmTaskEditor) {
				setAlgorithmTask(null);
			}
		}

		public void partDeactivated(IWorkbenchPartReference partRef) {
		}

		public void partHidden(IWorkbenchPartReference partRef) {
		}

		public void partInputChanged(IWorkbenchPartReference partRef) {
		}

		public void partOpened(IWorkbenchPartReference partRef) {
		}

		public void partVisible(IWorkbenchPartReference partRef) {
		}
		
	};

	/**
	 * The listener is used to load OperationParameters objects to editors if DataItem index was changed.
	 */
	final AlgorithmTask.AlgorithmTaskListener algorithmTaskListener = new AlgorithmTask.AlgorithmTaskListener() {
		public void selectedDataItemIndexChanged(AlgorithmTask algorithmTask, int newIndex) {
			loadParameters();
		}
	};

	
	protected AlgorithmTask currentTask;
	protected List<Operation> operations;
	private Composite parent;
	protected final Map<String, List<OperationParameterEditor>> 
		parameterEditorsMap = new HashMap<String, List<OperationParameterEditor>>();

	public void createPartControl(Composite composite) {
		SWTResourceManager.registerResourceUser(composite);
		
		toolkit = new FormToolkit(composite.getDisplay());
		form = toolkit.createForm(composite);
		this.parent = form.getBody();
		
		parent.setLayout(new FillLayout());
		parent.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		parent.setBackgroundMode(SWT.INHERIT_FORCE);
		parametersComposite = new Composite(parent, SWT.NONE);
		GridData data = new GridData ();
		data.horizontalAlignment = GridData.FILL;
		data.verticalAlignment = GridData.FILL;
		data.grabExcessHorizontalSpace = true;
		data.grabExcessVerticalSpace = true;
		parametersComposite.setLayoutData (data);
		GridLayout parametersCompositeGridLayout = new GridLayout();
		parametersCompositeGridLayout.numColumns = 1;
		parametersCompositeGridLayout.verticalSpacing = 0;
		parametersCompositeGridLayout.marginHeight = 3;
		parametersCompositeGridLayout.marginWidth = 3;
		parametersComposite.setLayout(parametersCompositeGridLayout);
		
		parameterEditorsScrolledComposite = new ScrolledComposite(parametersComposite, SWT.V_SCROLL | SWT.H_SCROLL);
		parameterEditorsScrolledComposite.setExpandHorizontal(true);
		parameterEditorsScrolledComposite.setExpandVertical(true);
		data = new GridData ();
		data.horizontalAlignment = GridData.FILL;
		data.verticalAlignment = GridData.FILL;
		data.grabExcessHorizontalSpace = true;
		data.grabExcessVerticalSpace = true;
		parameterEditorsScrolledComposite.setLayoutData (data);
		
		parameterEditorsHolderComposite = new Composite(parameterEditorsScrolledComposite, SWT.NONE);
		final GridLayout parameterEditorHolderCompositeGridLayout = new GridLayout();
		parameterEditorHolderCompositeGridLayout.marginHeight = 0;
		parameterEditorHolderCompositeGridLayout.marginWidth = 0;
		parameterEditorHolderCompositeGridLayout.verticalSpacing = 3;
		parameterEditorHolderCompositeGridLayout.horizontalSpacing = 0;
		parameterEditorsHolderComposite.setLayout(parameterEditorHolderCompositeGridLayout);
		
		parameterEditorsScrolledComposite.setContent(parameterEditorsHolderComposite);
		
		Label parameterGroupSeparator = new Label(parametersComposite, SWT.SEPARATOR | SWT.HORIZONTAL);
		data = new GridData ();
		data.horizontalAlignment = GridData.FILL;
		data.grabExcessHorizontalSpace = true;
		parameterGroupSeparator.setLayoutData (data);

		parameterGroupButtonsComposite = new Composite(parametersComposite, SWT.NONE);
		data = new GridData ();
		data.horizontalAlignment = GridData.FILL;
		data.grabExcessHorizontalSpace = true;
		parameterGroupButtonsComposite.setLayoutData (data);
		GridLayout parameterGroupButtonsCompositeGridLayout = new GridLayout();
		parameterGroupButtonsCompositeGridLayout.numColumns = 5;
		parameterGroupButtonsCompositeGridLayout.marginWidth = 0;
		parameterGroupButtonsCompositeGridLayout.marginHeight = 0;
		parameterGroupButtonsCompositeGridLayout.marginTop = 3;
		parameterEditorHolderCompositeGridLayout.marginLeft = 0;
		parameterEditorHolderCompositeGridLayout.marginRight = 0;
		parameterEditorHolderCompositeGridLayout.horizontalSpacing = 0;
		parameterGroupButtonsComposite.setLayout(parameterGroupButtonsCompositeGridLayout);

		configurationButton = new Button(parameterGroupButtonsComposite, SWT.PUSH);
		data = new GridData ();
		data.horizontalIndent = 0;
		data.horizontalSpan = 0;
		
		data.horizontalAlignment = GridData.FILL;
		data.grabExcessHorizontalSpace = true;
		configurationButton.setText("Config");
		configurationButton.setToolTipText("Save or load configuration");
		configurationButton.setLayoutData(data);
		configurationButton.setImage(SWTResourceManager.getImage("icons/algorithm_config.gif", 
				parent));
		configurationButton.setAlignment(GridData.HORIZONTAL_ALIGN_BEGINNING);
		configurationMenu = new Menu (configurationButton.getShell(), SWT.POP_UP);

		defaultParametersButton = new Button(parameterGroupButtonsComposite, SWT.PUSH);
		defaultParametersButton.setImage(Activator.getImageDescriptor("icons/reload_page16x16.png").createImage());
		defaultParametersButton.setToolTipText("Load default values");
		data = new GridData ();
//		data.grabExcessHorizontalSpace = true;
		defaultParametersButton.setLayoutData (data);
		
		applyParametersButton = new Button(parameterGroupButtonsComposite, SWT.PUSH);
//		applyParametersButton.setText("Apply");
		applyParametersButton.setImage(Activator.getImageDescriptor("icons/apply16x16.png").createImage());
		applyParametersButton.setToolTipText("Apply the change");
		data = new GridData ();
		applyParametersButton.setLayoutData (data);
		applyParametersButton.setEnabled(false);
		
		revertParametersButton = new Button(parameterGroupButtonsComposite, SWT.PUSH);
//		revertParametersButton.setText("Undo");
		revertParametersButton.setImage(Activator.getImageDescriptor("icons/undo16x16.png").createImage());
		revertParametersButton.setToolTipText("Undo the change");
		data = new GridData ();
		revertParametersButton.setLayoutData (data);
		revertParametersButton.setEnabled(false);

		Composite controlGroupButtonsComposite = new Composite(parametersComposite, SWT.NONE);
		data = new GridData ();
		data.horizontalAlignment = GridData.FILL;
		data.verticalAlignment = GridData.FILL;
		data.grabExcessHorizontalSpace = true;
		controlGroupButtonsComposite.setLayoutData (data);
		GridLayout controlGroupButtonsCompositeGridLayout = new GridLayout();
		controlGroupButtonsCompositeGridLayout.numColumns = 2;
		controlGroupButtonsCompositeGridLayout.marginWidth = 0;
		controlGroupButtonsCompositeGridLayout.marginHeight = 0;
		controlGroupButtonsCompositeGridLayout.marginTop = 3;
		controlGroupButtonsComposite.setLayout(controlGroupButtonsCompositeGridLayout);
		
		startOperationButton = new Button(parameterGroupButtonsComposite, SWT.PUSH);
		data = new GridData ();
		data.horizontalAlignment = GridData.FILL;
		data.grabExcessHorizontalSpace = true;
		startOperationButton.setLayoutData(data);
		startOperationButton.setText("Relaunch");
		startOperationButton.setToolTipText("Rerun the Algorithm for selected Data Item");
		startOperationButton.setImage(Activator.getImageDescriptor("icons/Play-Normal-16x16.png").createImage());

		initConfigurationMenu();
		initListeners();
		
		setAlgorithmTask(ProjectManager.getCurrentAlgorithmTask());
		loadEfficiencyMapConfiguration();
		loadCurrentDir();
		setVisibility();
	}

	private void setVisibility() {
		configurationButton.setVisible(false);
		defaultParametersButton.setVisible(false);
		revertParametersButton.setVisible(false);
		applyParametersButton.setText("Apply");
	}

	private void initConfigurationMenu(){
		MenuItem loadMenuItem = new MenuItem (configurationMenu, SWT.PUSH);
		loadMenuItem.setText("Load configuration from");
		loadMenuItem.addSelectionListener(new SelectionListener(){

			public void widgetDefaultSelected(SelectionEvent arg0) {
				
			}

			public void widgetSelected(SelectionEvent e) {
				URI fileURI = null;
				try{
//						File draPath = new File(UIAlgorithmManager.getAlgorithmManager().getAlgorithmSetPath());
						File draPath = new File(".");
						String path = Util.getFilenameFromShell(e.widget.getDisplay().
								getActiveShell(), "*.xml", "'xml' data file", draPath);
						if (path == null) return;
						fileURI = (new File(path)).toURI();
				}catch (Exception ex) {
					handleException(ex);
				}
				try {
					for (AlgorithmInput algorithmInput : currentTask.getAlgorithmInputs()){
						algorithmInput.getAlgorithm().loadConfiguration(fileURI);
					}
				} catch (Exception e1) {
					handleException(e1);
				}
				updateAllOperationParameters();
				try {
					currentTask.runAlgorithm();
				} catch (NoneAlgorithmException e1) {
					handleException(e1);
				}
			}

			
		});
				
		saveMenuItem = new MenuItem (configurationMenu, SWT.PUSH);
		saveMenuItem.setText("Save");
		saveMenuItem.addSelectionListener(new SelectionListener(){

			public void widgetDefaultSelected(SelectionEvent arg0) {
				
			}

			public void widgetSelected(SelectionEvent e) {
				URI fileURI = null;
				Algorithm currentAlgorithm = null;
				try{
					currentAlgorithm = currentTask.getSelectedAlgorithmInput().getAlgorithm();
					AlgorithmConfiguration configuration = currentAlgorithm.getConfiguration();
					if (configuration != null && configuration.getPath() != null){
						fileURI = configuration.getPath();
					} else{
						File draPath = new File(".");
						String path = Util.saveFilenameFromShell(e.widget.getDisplay().
								getActiveShell(), "xml", "'xml' data file", draPath);
						if (path == null) return;
						fileURI = (new File(path)).toURI();
					}
				}catch (Exception ex) {
					handleException(ex);
				}
				try {
					String filename = (new File(fileURI)).getName().trim();
					filename = filename.substring(0, filename.indexOf("."));
					currentAlgorithm.exportConfiguration(fileURI, filename);
				} catch (Exception e1) {
					handleException(e1);
				}
			}
			
		});

		MenuItem saveAsMenuItem = new MenuItem (configurationMenu, SWT.PUSH);
		saveAsMenuItem.setText("Save as");
		saveAsMenuItem.addSelectionListener(new SelectionListener(){

			public void widgetDefaultSelected(SelectionEvent arg0) {
				
			}

			public void widgetSelected(SelectionEvent e) {
//				File draPath = new File(UIAlgorithmManager.getAlgorithmManager().getAlgorithmSetPath());
				File draPath = new File(".");
				String path = Util.saveFilenameFromShell(e.widget.getDisplay().
						getActiveShell(), "xml", "'xml' data file", draPath);
				if (path == null) return;
				final URI fileURI = (new File(path)).toURI();

				try {
					String filename = (new File(fileURI)).getName().trim();
					filename = filename.substring(0, filename.indexOf("."));
					UIAlgorithmManager.getAlgorithmManager().getCurrentInput().getAlgorithm().
						exportConfiguration(fileURI, filename);
				} catch (Exception e1) {
					handleException(e1);
				}
			}
			
		});
	}

	protected void updateAllOperationParameters() {
		currentTask.updateOperationParameters();
		updateParametersData();
	}

	public void runAlgorithmWithSelectedData() throws Exception {
		// Action: interrupt SICS
		for (Operation operation : operations) 
			currentTask.applyParameterChangesForAllDataItems(operation);
		if (currentTask.getAlgorithm().hasInPort())
			currentTask.changeAlgorithmInput(DataSourceManager.getSelectedFile().getDataItems().get(0));
		saveEfficiencyMapTuner();
		currentTask.runAlgorithm();
	}
	
	private void loadDataUpdateListener() {
		final PlotDataUpdateListener plotDataUpdateListener = new PlotDataUpdateListener();
		final OperationManager operationManager = currentTask.getOperationManager(
				currentTask.getSelectedDataItemIndex());
		final List<Operation> operations = operationManager.getOperations();
		for (Operation operation : operations) {
			operation.addOperationDataListener(plotDataUpdateListener);
		}
	}

	private final class PlotDataUpdateListener implements OperationDataListener {
		public void outputDataUpdated(final Operation operation, IGroup oldData, final IGroup newData) {
			try {
				PlotManager.updatePlots(currentTask, operation.getName());
			} catch (TunerNotReadyException e) {
				handleException(e);
			} catch (TransferFailedException e) {
				handleException(e);
			} catch (NoneAlgorithmException e) {
				handleException(e);
			} catch (FailedToExecuteException e) {
				handleException(e);
			}
		}
	}
	private void preparePlot(){
		final OperationManager operationManager = currentTask.getOperationManager(currentTask.getSelectedDataItemIndex());
		final List<Operation> operations = operationManager.getOperations();
		List<Operation> autoPlotOperationList = new ArrayList<Operation>();
		for (Operation operation : operations){
			if (operation.hasAutoPlotSink())
				autoPlotOperationList.add(operation);
		}
		final IWorkbenchWindow workbenchWindow = getSite().getWorkbenchWindow();
		if (autoPlotOperationList.size() == 0){
			final Operation lastOperation = operations.get(operations.size() - 1);
			lastOperation.addOperationDataListener(new OperationDataListener() {
				public void outputDataUpdated(final Operation operation, 
						IGroup oldData, IGroup newData) {
					final OperationDataListener finalListener = this;
					DisplayManager.getDefault().asyncExec(new Runnable() {
						public void run() {
							//plot result of last operation
							PlotManager.plotOperation(currentTask, lastOperation, 
									workbenchWindow);
							operation.removeOperationDataListener(finalListener);
						}
					});
				}

			});	
		}else{
			for (Operation operation : autoPlotOperationList){
				final Operation autoPlotOperation = operation;
				autoPlotOperation.addOperationDataListener(new OperationDataListener() {
					public void outputDataUpdated(final Operation operation, 
							IGroup oldData, 
							IGroup newData) {
						final OperationDataListener finalListener = this;
						DisplayManager.getDefault().asyncExec(new Runnable() {
							public void run() {
								//plot result of last operation
								PlotManager.plotOperation(currentTask, autoPlotOperation, 
										workbenchWindow);
								operation.removeOperationDataListener(finalListener);
							}
						});
					}
				});					
			}
		}
		isOperationPrepared = true;
	}
	private void initListeners() {
		defaultParametersButton.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
			}
			public void widgetSelected(SelectionEvent e) {
				try {
					Runtime.getRuntime().exec("X:/gumtree/releases/apps/wombat/wombat.exe");
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				loadDefaulParameters();
			}
		});
		
		revertParametersButton.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
			}
			public void widgetSelected(SelectionEvent e) {
				revertParameters();
			}
		});
		applyParametersButton.addSelectionListener(applyParametersListener);
		
		startOperationButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				try{
					runAlgorithmWithSelectedData();
				} catch (Exception e1) {
					e1.printStackTrace();
					LoggerFactory.getLogger(QuokkaParametersView.class).error(
							"Failed to run the algorithm.", e);
				}
			}
		});
		
		configurationSelectionListener = new SelectionListener(){

			public void widgetDefaultSelected(SelectionEvent arg0) {
			}

			public void widgetSelected(SelectionEvent arg0) {
				AlgorithmConfiguration configuration = null;
				try {
					configuration = currentTask.getAlgorithmInputs().get(0)
					.getAlgorithm().getConfiguration();
					if (configuration != null && configuration.getPath() != null)
						saveMenuItem.setText("Save to " + configuration.getName());
					else
						saveMenuItem.setText("Save");
				} catch (Exception e) {
					handleException(e);
				}
				Rectangle rect = configurationButton.getBounds ();
				final Point location = configurationButton.getLocation();
				final Point size = configurationButton.getSize();
				Point pt = new Point (rect.x, rect.y
						+ rect.height
				);
				pt = configurationButton.getParent().toDisplay (pt);
				configurationMenu.setLocation (pt.x, pt.y);
				configurationMenu.setVisible (true);
//				}
			}
			
		};
		configurationButton.addSelectionListener(configurationSelectionListener);
	
		//the PartListener listener must be added after active workbenchPage was created.
		//to ensure it the operation should be performed in asyncExec block  
		DisplayManager.getDefault().asyncExec(new Runnable() {
			public void run() {
				IWorkbenchWindow workbenchWindow = Activator.getDefault().getWorkbench().getActiveWorkbenchWindow();
				final IWorkbenchPage workbenchPage = workbenchWindow.getActivePage();
			}
		});

		DataSourceManager.addSelectionChangedListener(new DataSelectionListener(){

			public void dataSelectionChanged() {
				try {
					runAlgorithmWithSelectedData();
				} catch (Exception e) {
					e.printStackTrace();
					LoggerFactory.getLogger(QuokkaParametersView.class).error(
							"Failed to run the algorithm.", e);
				}
			}

			public void allDataRemoved() {
				currentTask.clear();
			}
			
		});
	}

	protected void loadDefaulParameters() {
		for (Operation operation : operations) {
			operation.resetParametersToDefault();
		}
		
		updateParametersData();
	}

	private void revertParameters() {
		for (Operation operation : operations) {
			operation.revertParametersChanges();
		}
		updateParametersData();
	}
	
	protected void applyParameters() {

		boolean isParametersChanged = false;

		//check is changed parameters available
		for (Operation operation : operations) {
			if (operation.isParametersChanged()) {
				isParametersChanged = true;
				break;
			}
		}

		//apply changed parameters
		try {
			for (Operation operation : operations) {
				currentTask.applyParameterChangesForAllDataItems(operation);
			}
		} catch (Exception e) {
			handleException(e);
		}

		//OperationManager for selected data item
		OperationManager operationManager = currentTask.getOperationManager(currentTask.getSelectedDataItemIndex());

		//detect first no actual Operation data to run the algorithm from the operation.
		Operation lastReprocessableOperation = AlgorithmTask.getOperationChainHead(
				operationManager.getOperations());
		//run Algorithm from first operation with not actual data

		if (lastReprocessableOperation != null) {
	
					System.out.println("CHECK TIMING RUN algorithm from operation*************" 
							+ lastReprocessableOperation.getName());
					try {
						saveEfficiencyMapTuner();
						currentTask.runAlgorithmFromOperation(lastReprocessableOperation);
					} catch (TunerNotReadyException e) {
						handleException(e);
					} catch (TransferFailedException e) {
						handleException(e);
					}
			
		}else{
			try {
				currentTask.runAlgorithmFromOperation(operations.get(0));
//				runAlgorithmWithSelectedData();
			} catch (Exception e) {
				e.printStackTrace();
				LoggerFactory.getLogger(QuokkaParametersView.class).error(
						"Failed to run the algorithm.", e);
			}
		}
		
		updateParametersData();
	}
	/**
	 * Updates parameter editors with data from holders. 
	 */
	protected void updateParametersData() {
		for (Operation operation : operations) {
			//update existed editors with parameters of selected operation
			final List<OperationParameter> parameters = operation.getParameters();
			final List<OperationParameterEditor> parameterEditorList = parameterEditorsMap.get(operation.getName());
			for (int i = 0; i < parameters.size(); i++) {
				OperationParameter parameter = parameters.get(i);
				final OperationParameterEditor operationParameterEditor = parameterEditorList.get(i);
				operationParameterEditor.setOperationParameter(parameter);
				operationParameterEditor.loadData();
			}
		}
	}
	
	public void setFocus() {
		
	}
	
	public void setAlgorithmTask(AlgorithmTask algorithmTask) {
		clearView();
		
		if (this.currentTask != null) {
			this.currentTask.removeAlgorithmTaskListener(algorithmTaskListener);
			this.currentTask.removeStatusListener(statusListener);
		}
		
		this.currentTask = algorithmTask;
		
		if (algorithmTask != null) {
			loadParameters();
			algorithmTask.addAlgorithmTaskListener(algorithmTaskListener);
		}
		
		
		
	}

	private void loadParameters() {
		//TODO check is operation manager initialized if no then add listener to update the view late.
		operations = currentTask.getOperationManager(currentTask.getSelectedDataItemIndex()).getOperations();
		if (parameterEditorsMap.isEmpty()) {
			for (Operation operation : operations) {
				addOperationParameters(operation);
			}
		} else {
			updateParametersData();
		}
		updateParametersButtons();

		parameterEditorsScrolledComposite.setMinSize(parameterEditorsHolderComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
	}

	protected void addOperationParameters(final Operation operation) {

			//create a new group of parameters
		if (operation.getParameters().size() > 0){
			Group parameterEditorsGroup = new Group(parameterEditorsHolderComposite, SWT.NONE);
			parameterEditorsGroup.setText(operation.getUILabel());
			parameterEditorsGroup.setLayoutData(new GridData(GridData.FILL, SWT.CENTER, true, false));
			GridLayout parameterEditorsCompositeGridLayout = new GridLayout();
			parameterEditorsCompositeGridLayout.numColumns = 2;
			parameterEditorsCompositeGridLayout.marginWidth = 2;
			parameterEditorsCompositeGridLayout.marginBottom = 0;
			parameterEditorsCompositeGridLayout.marginHeight = 2;
			parameterEditorsCompositeGridLayout.marginTop = 0;
			parameterEditorsCompositeGridLayout.verticalSpacing = 2;
			parameterEditorsGroup.setLayout(parameterEditorsCompositeGridLayout);

			//register new list of parameter editors
			final ArrayList<OperationParameterEditor> parameterEditorList = new ArrayList<OperationParameterEditor>();
			parameterEditorsMap.put(operation.getName(), parameterEditorList);

			if (operation != null) {
				for (OperationParameter operationParameter : operation.getParameters()) {
					OperationParameterEditor operationParameterEditor;
					switch (operationParameter.getType()) {
					case Text:
						operationParameterEditor = new TextOperationParameterEditor(
								operationParameter,
								parameterEditorsGroup);
						break;
					case Number:
						operationParameterEditor = new NumericOperationParameterEditor(
								operationParameter,
								parameterEditorsGroup);
						break;
					case Boolean:
						operationParameterEditor = new BooleanOperationParameterEditor(
								operationParameter,
								parameterEditorsGroup);
						break;
					case Uri:
						operationParameterEditor = new UriOperationParameterEditor(
								operationParameter,
								parameterEditorsGroup);
						break;
					case Region:
						operationParameterEditor = new RegionOperationParameterEditor(
								operationParameter,
								parameterEditorsGroup);
						((RegionOperationParameterEditor) operationParameterEditor).setRegionParameter(
								((RegionParameterManager) getAlgorithmTask().getRegionParameterManager()
										).findParameter(operation));
						break;
					case Option:
						operationParameterEditor = new OptionOperationParameterEditor(
								operationParameter,
								parameterEditorsGroup);
						break;
					case StepDirection:
						operationParameterEditor = new StepDirectionOperationParameterEditor(
								operationParameter,
								parameterEditorsGroup);								
						break;
					default:
						operationParameterEditor = new DefaultOperationParameterEditor(
								operationParameter,
								parameterEditorsGroup);
					break;
					}

					operationParameterEditor.addChangeListener(parameterEditorChangeListener);
					operationParameterEditor.addApplyParameterListener(applyParametersListener);
					parameterEditorList.add(operationParameterEditor);
				}
			}

			//define parameterEditorComposite which contains parameter editors of selected operation
			parameterEditorsGroup.layout();

			parameterEditorsHolderComposite.layout();
		}
	}

	private void updateParametersButtons() {
		boolean isParametersChanged = false;
		boolean isDefaultParametersLoaded = true;
		for (Operation operation : operations) {
			operation.updateStatus();
			isParametersChanged |= operation.isParametersChanged();
			isDefaultParametersLoaded &= operation.isDefaultParametersLoaded();
		}
		
		if (!applyParametersButton.isDisposed())
			applyParametersButton.setEnabled(isParametersChanged);
		if (!revertParametersButton.isDisposed())
			revertParametersButton.setEnabled(isParametersChanged);
		if (!defaultParametersButton.isDisposed())
			defaultParametersButton.setEnabled(!isDefaultParametersLoaded);
		
	}

	protected void handleException(Throwable throwable) {
		throwable.printStackTrace();
		showErrorMessage(throwable.getMessage());
	}
	
	private void showErrorMessage(String message) {
		MessageDialog.openError(
			parent.getShell(),
			"Operation Parameters View",
			message);
	}
	
	private void clearView() {
		for (List<OperationParameterEditor> list : parameterEditorsMap.values()) {
			for (OperationParameterEditor operationParameterEditor : list) {
				operationParameterEditor.removeChangeListener(parameterEditorChangeListener);
			}
		}
		parameterEditorsMap.clear();
		
		if (!parameterEditorsHolderComposite.isDisposed()) {
			for (Control control : parameterEditorsHolderComposite
					.getChildren()) {
				if (!control.isDisposed()) {
					control.dispose();
				}
			}
		}
	}
	
	public void dispose() {
		clearView();
		
		super.dispose();

		IWorkbenchWindow workbenchWindow = Activator.getDefault().getWorkbench().getActiveWorkbenchWindow();
		final IWorkbenchPage workbenchPage = workbenchWindow.getActivePage();
		
		if (workbenchPage != null) {
			workbenchPage.removePartListener(catchAlgorithmTaskEditorPartListener);
		}
		saveCurrentDir();
		currentTask.clear();
		DataSourceManager.getInstance().removeAll();
	}

	public static void subscribeStopButtonListener(ButtonClickListener listener){
		if (stopButtonListenerList == null)
			stopButtonListenerList = new ArrayList<ButtonClickListener>();
		stopButtonListenerList.add(listener);
	}
	
	public static void unsubscribeStopButtonListener(ButtonClickListener listener){
		if (stopButtonListenerList != null)
			stopButtonListenerList.remove(listener);
	}

	protected boolean loadSelectedDataItem(){
//		List<DataItem> dataItemList = DataSourceManager.getInstance().get;
		return false;
	}
	
	protected void saveEfficiencyMapTuner(){
		Algorithm currentAlgorithm = currentTask.getSelectedAlgorithmInput().getAlgorithm();
		Tuner tuner = currentAlgorithm.findTuner(TUNER_EFFICIENCY_MAP_URI);
		if (tuner == null) return;
		Object value = tuner.getSignal();
		if (value == null)return;
		if (tunerEfficiencyMapUri == null || 
			!tunerEfficiencyMapUri.equals(String.valueOf(value))) {
			saveConfiguration(getSaveTunerNames());
		}
		tunerEfficiencyMapUri = String.valueOf(value);
	}
	
	protected String[] getSaveTunerNames(){
		return SAVE_TUNERS;
	}
	
	protected void saveConfiguration(String... tunerNames){
		String filename = null;
		URI fileURI = null;
		Algorithm current = null;
		try{
			current = currentTask.getSelectedAlgorithmInput().getAlgorithm();
			IPath stateLocation = Activator.getDefault().getStateLocation();
			if (stateLocation == null) {
				return;
			}
			filename = current.getName() + ".xml";
			stateLocation = stateLocation.append(filename);
			fileURI = ConverterLib.path2URI(stateLocation.toFile().getAbsolutePath());
		}catch (Exception ex) {
			handleException(ex);
		}
		try {
			current.exportPartialConfiguration(fileURI, filename, tunerNames);
		} catch (Exception e1) {
			handleException(e1);
		}
	}
	
	protected void saveTuners(String... names) {
		String filename = null;
		URI fileURI = null;
		Algorithm current = null;
		try{
			current = currentTask.getSelectedAlgorithmInput().getAlgorithm();
			IPath stateLocation = Activator.getDefault().getStateLocation();
			if (stateLocation == null) {
				return;
			}
			filename = current.getName() + ".xml";
			stateLocation = stateLocation.append(filename);
			fileURI = ConverterLib.path2URI(stateLocation.toFile().getAbsolutePath());
		}catch (Exception ex) {
			handleException(ex);
		}
		try {
			current.exportPartialConfiguration(fileURI, filename, names);
		} catch (Exception e1) {
			handleException(e1);
		}
	}
	
	protected void saveTuners() {
		this.saveTuners(SAVE_TUNERS);
	}
	
	private void loadEfficiencyMapConfiguration(){
		String filename = null;
		URI fileURI = null;
		Algorithm currentAlgorithm = null;
		try{
//			AlgorithmConfiguration configuration = UIAlgorithmManager.getAlgorithmManager().
//			getCurrentInput().getAlgorithm().getConfiguration();
			currentAlgorithm = currentTask.getAlgorithm();
			IPath stateLocation = Activator.getDefault().getStateLocation();
			if (stateLocation == null)
				return;
			filename = currentAlgorithm.getName() + ".xml";
			stateLocation = stateLocation.append(filename);
			File configurationFile = stateLocation.toFile();
			if (configurationFile == null || !configurationFile.exists())
				return;
			fileURI = ConverterLib.path2URI(configurationFile.getAbsolutePath());
		}catch (Exception ex) {
			handleException(ex);
		}
		try {
			for (AlgorithmInput algorithmInput : currentTask.getAlgorithmInputs()){
				algorithmInput.getAlgorithm().loadConfiguration(fileURI);
			}
		} catch (Exception e1) {
			handleException(e1);
		}
		updateAllOperationParameters();
	}
	
	private void saveCurrentDir(){
		IGroup dirGroup = null;
		try {
			dirGroup = Factory.createGroup(CURRENT_DIR_OBJECT_NAME);
			dirGroup.addDataItem(Factory.createDataItem(dirGroup, CURRENT_DIR_OBJECT_NAME, Factory.createArray(
					Util.getCurrentDir().getAbsolutePath().toCharArray())));
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (dirGroup == null)
			return;
		URI fileURI = null;
		String filename = null;
		try{
			IPath stateLocation = Activator.getDefault().getStateLocation();
			if (stateLocation == null)
				return;
			filename = KAKADU_UI_CONFIGURATION_FILENAME;
			stateLocation = stateLocation.append(filename);
			File configurationFile = stateLocation.toFile();
			fileURI = ConverterLib.path2URI(configurationFile.getAbsolutePath());
			UIAlgorithmManager.getAlgorithmManager().getExporter(Format.xml).signalExport(dirGroup, fileURI);
		}catch (Exception ex) {
			handleException(ex);
		}
	}
	
	private void loadCurrentDir(){
		URI fileURI = null;
		IGroup dirGroup = null;
		String filename = null;
		try{
			IPath stateLocation = Activator.getDefault().getStateLocation();
			if (stateLocation == null) {
				return;
			}
			filename = KAKADU_UI_CONFIGURATION_FILENAME;
			stateLocation = stateLocation.append(filename);
			File configurationFile = stateLocation.toFile();
			if (!configurationFile.exists())
				return;
			fileURI = ConverterLib.path2URI(configurationFile.getAbsolutePath());
			IDataset dataset = Factory.createDatasetInstance(fileURI);
			dirGroup = dataset.getRootGroup().findGroup(CURRENT_DIR_OBJECT_NAME);
			Util.setCurrentDir(new File(dirGroup.findDataItem(CURRENT_DIR_OBJECT_NAME).getData().toString()));
		} catch (Exception ex) {
			handleException(ex);
		}
	}
	
	protected AlgorithmTask getAlgorithmTask(){
		return currentTask;
	}
}
