/*****************************************************************************
 * Copyright (c) 2007 Australian Nuclear Science and Technology Organisation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tony Lam (Bragg Institute) - initial API and implementation
 *****************************************************************************/

package au.gov.ansto.bragg.quokka.ui.workflow;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.BeansObservables;
import org.eclipse.core.databinding.conversion.NumberToStringConverter;
import org.eclipse.core.databinding.conversion.StringToNumberConverter;
import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.core.databinding.observable.map.IObservableMap;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.databinding.viewers.ObservableMapLabelProvider;
import org.eclipse.jface.databinding.viewers.ViewersObservables;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.statushandlers.StatusManager;
import org.gumtree.gumnix.sics.core.SicsCore;
import org.gumtree.gumnix.sics.io.SicsIOException;
import org.gumtree.scripting.IScriptBlock;
import org.gumtree.scripting.IScriptExecutor;
import org.gumtree.scripting.ScriptBlock;
import org.gumtree.scripting.ScriptExecutorStateEvent;
import org.gumtree.service.eventbus.IEventHandler;
import org.gumtree.ui.scripting.viewer.ICommandLineViewer;
import org.gumtree.ui.util.SafeUIRunner;
import org.gumtree.util.ILoopExitCondition;
import org.gumtree.util.LoopRunner;
import org.gumtree.util.PlatformUtils;
import org.gumtree.util.eclipse.WorkspaceUtils;
import org.gumtree.widgets.swt.util.UIResources;
import org.gumtree.workflow.ui.AbstractTaskView;
import org.gumtree.workflow.ui.ITaskView;
import org.gumtree.workflow.ui.tasks.ScriptEngineTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import ucar.atd.dorade.ScanMode;

import au.gov.ansto.bragg.quokka.experiment.model.InstrumentConfig;
import au.gov.ansto.bragg.quokka.experiment.model.InstrumentConfigTemplate;
import au.gov.ansto.bragg.quokka.experiment.model.PropertyList;
import au.gov.ansto.bragg.quokka.experiment.model.ScanMode;
import au.gov.ansto.bragg.quokka.experiment.util.ExperimentModelUtils;
import au.gov.ansto.bragg.quokka.ui.internal.Activator;
import au.gov.ansto.bragg.quokka.ui.internal.InternalImage;
import au.gov.ansto.bragg.quokka.ui.internal.SystemProperties;

import com.ibm.icu.text.DecimalFormat;

public class ConfigurationTask extends AbstractExperimentTask {

	private static Logger logger = LoggerFactory.getLogger(ConfigurationTask.class);
	
	@Override
	protected ITaskView createViewInstance() {
		return new ConfigurationTaskView();
	}
	
	class ConfigurationTaskView extends AbstractTaskView {
		
		private TableViewer tableViewer;

		private org.gumtree.scripting.IScriptExecutor executor;
		
		private org.gumtree.service.eventbus.IEventHandler<ScriptExecutorStateEvent> executorEventHandler;
		
		public void createPartControl(final Composite parent) {
			parent.setLayout(new GridLayout(3, false));
			executor = getContext().getSingleValue(IScriptExecutor.class);
			Realm.runWithDefault(SWTObservables.getRealm(Display.getDefault()), new Runnable() {
				public void run() {
					/*********************************************************
					 * Configuration list
					 *********************************************************/
					Composite configArea = getToolkit().createComposite(parent);
					GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.FILL).grab(false, false).applyTo(configArea);
					createConfigArea(configArea);

					/*********************************************************
					 * Selection buttons
					 *********************************************************/
					Composite selectionArea = getToolkit().createComposite(parent);
					GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.FILL).grab(false, false).applyTo(selectionArea);
					createSelectionArea(selectionArea);
					
					/*********************************************************
					 * Script area + transmission settings
					 *********************************************************/
					Composite scriptArea = getToolkit().createComposite(parent);
					GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(false, false).applyTo(scriptArea);
					createScriptArea(scriptArea);
					
					parent.layout();
				}
			});
		}

		public void setFocus() {
			tableViewer.getTable().setFocus();
		}
		
		private void createConfigArea(Composite parent) {
			parent.setLayout(new GridLayout(2, false));

			Label label = getToolkit().createLabel(parent, "Configurations");
			label.setFont(UIResources.getDefaultFont(SWT.BOLD));
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).span(2, 1).applyTo(label);
			
			tableViewer = new TableViewer(parent, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION | SWT.V_SCROLL);
			Table table = tableViewer.getTable();
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).span(2, 1).hint(120, 120).applyTo(table);
			// Create a standard content provider
			ObservableListContentProvider contentProvider = new ObservableListContentProvider();
			tableViewer.setContentProvider(contentProvider);
			// And a standard label provider that maps columns
			IObservableMap[] attributeMaps = BeansObservables.observeMaps(contentProvider.getKnownElements(), InstrumentConfig.class, new String[] { "name" });
			tableViewer.setLabelProvider(new ObservableMapLabelProvider(attributeMaps));
			// Now set the Viewer's input
			tableViewer.setInput(new WritableList(getExperiment().getInstrumentConfigs(), InstrumentConfig.class));
			// Update selection
			if (tableViewer.getTable().getItemCount() > 0) {
				// select first one
				tableViewer.setSelection(new StructuredSelection(tableViewer.getElementAt(0)));
			}
			
			Button addButton = getToolkit().createButton(parent, "", SWT.PUSH);
			addButton.setImage(InternalImage.ADD.getImage());
			addButton.addSelectionListener(new SelectionAdapter() {
				@SuppressWarnings("unchecked")
				public void widgetSelected(SelectionEvent e) {
					// Prepare configuration templates
					List<InstrumentConfigTemplate> standardTemplates = (List<InstrumentConfigTemplate>) getContext().get("configTemplate");
					List<InstrumentConfigTemplate> templates = new ArrayList<InstrumentConfigTemplate>();
					if (standardTemplates != null) {
						templates.addAll(standardTemplates);
					}
					// Launch dialog
					Shell parentShell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
					// [GUMTREE-653] Config dialog based on workspace
					ConfigSelectionDialog dialog = new ConfigSelectionDialog(parentShell);
					IFolder configFolder = WorkspaceUtils.getFolder(SystemProperties.CONFIG_FOLDER.getValue());
					try {
						configFolder.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
					} catch (CoreException e1) {
					}
					dialog.setBaseDirectory(configFolder);
					int result = dialog.open();
					if (result == Window.OK) {
						if (dialog.isNewConfig()) {
							addNewConfig();
						} else {
							IFile selectedConfig = dialog.getSelectedConfig();
							
							// replace au.gov.ansto.bragg.quokka2.experiment.model.InstrumentConfigTemplate
							// with au.gov.ansto.bragg.quokka.experiment.model.InstrumentConfigTemplate

							BufferedReader reader = null;
							try {
								reader = new BufferedReader(new FileReader(new File(selectedConfig.getLocationURI())));
								StringBuilder stringBuilder = new StringBuilder();
				
								String line;
								String newLine = System.getProperty("line.separator");
								while ((line = reader.readLine()) != null) {
									stringBuilder.append(line
										.replace(
											"au.gov.ansto.bragg.quokka2.experiment.model.InstrumentConfigTemplate",
											"au.gov.ansto.bragg.quokka.experiment.model.InstrumentConfigTemplate")
										.replace(
											"startingAtteunation",
											"startingAttenuation"));
									stringBuilder.append(newLine);
							    }
								
								Object object = ExperimentModelUtils.getXStream().fromXML(stringBuilder.toString());
								if (object instanceof InstrumentConfigTemplate) {
									// "group" is relative folder of selected configuration
									String group = configFolder.getLocationURI().relativize(selectedConfig.getParent().getLocationURI()).getPath();
									addConfigFromTemplate((InstrumentConfigTemplate) object, group);
								}
							} catch (Exception ex) {
								logger.error("Failed to load config template from " + selectedConfig.getLocationURI().toString());
							} finally {
								if (reader != null)
									try {
										reader.close();
									} catch (IOException e1) {
										e1.printStackTrace();
									}
							}

						}
					}
				}
			});
			
			Button removeButton = getToolkit().createButton(parent, "", SWT.PUSH);
			removeButton.setImage(InternalImage.DELETE.getImage());
			removeButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					InstrumentConfig config = (InstrumentConfig) ((IStructuredSelection) tableViewer.getSelection()).getFirstElement();
					removeConfig(config);
				}
			});
		}
		
		private void createSelectionArea(Composite parent) {
			parent.setLayout(new GridLayout());
			
			/*****************************************************************
			 * Up button
			 *****************************************************************/
			Button upButton = getToolkit().createButton(parent, "", SWT.PUSH);
			GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.END).grab(false, true).applyTo(upButton);
			upButton.setImage(InternalImage.UP.getImage());
			upButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					InstrumentConfig config = (InstrumentConfig) ((IStructuredSelection) tableViewer.getSelection()).getFirstElement();
					PropertyList<InstrumentConfig> configs = getExperiment().getInstrumentConfigs(); 
					int index = configs.indexOf(config);
					if (configs.size() > 1 && index > 0) {
						configs.swap(index, index - 1);
						// Refresh UI			
						tableViewer.setInput(new WritableList(getExperiment().getInstrumentConfigs(), InstrumentConfig.class));
						// Give focus
						tableViewer.setSelection(new StructuredSelection(config));
					}
				}
			});

			/*****************************************************************
			 * Down button
			 *****************************************************************/
			Button downButton = getToolkit().createButton(parent, "", SWT.PUSH);
			GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.BEGINNING).grab(false, true).applyTo(downButton);
			downButton.setImage(InternalImage.DOWN.getImage());
			downButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					InstrumentConfig config = (InstrumentConfig) ((IStructuredSelection) tableViewer.getSelection()).getFirstElement();
					PropertyList<InstrumentConfig> configs = getExperiment().getInstrumentConfigs(); 
					int index = configs.indexOf(config);
					if (configs.size() > 1 && index != configs.size() - 1) {
						configs.swap(index, index + 1);
						// Refresh UI			
						tableViewer.setInput(new WritableList(getExperiment().getInstrumentConfigs(), InstrumentConfig.class));
						// Give focus
						tableViewer.setSelection(new StructuredSelection(config));
					}
				}
			});
		}
		
		private void FillGroupCombo(Combo group, IFolder folder, String prefix) {
			try {
				for (IResource member : folder.members()) {
					if (member instanceof IFolder) {
						String fullName = prefix + member.getName();
						group.add(fullName);
						FillGroupCombo(group, (IFolder)member, fullName + "/");
					}
				}
			} catch (CoreException e) {
			}
		}
		
		private void createScriptArea(final Composite parent) {
			parent.setLayout(new GridLayout());
			DataBindingContext bindingContext = new DataBindingContext();
			// bind widget to the name of the current selection
			final IObservableValue selection = ViewersObservables.observeSingleSelection(tableViewer);
			
			/*****************************************************************
			 * Configuration group
			 *****************************************************************/
			Group configurationGroup = new Group(parent, SWT.NONE);
			configurationGroup.setText("Configuration");
			getToolkit().adapt(configurationGroup);
			configurationGroup.setLayout(new GridLayout(5, false));
			GridDataFactory.fillDefaults().applyTo(configurationGroup);
			
			// Name
			Label label = getToolkit().createLabel(configurationGroup, "Name: ", SWT.RIGHT);
			GridDataFactory.fillDefaults().align(SWT.END, SWT.BEGINNING).hint(80, SWT.DEFAULT).applyTo(label);
			
			final Text nameText = getToolkit().createText(configurationGroup, "", SWT.BORDER);
			GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).hint(150, SWT.DEFAULT).applyTo(nameText);
			bindingContext.bindValue(
					SWTObservables.observeText(nameText, SWT.Modify),
					BeansObservables.observeDetailValue(Realm.getDefault(), selection, "name", String.class));
			nameText.setEnabled(false);
			
			// Lock/Unlock
			final Label lockLabel = getToolkit().createLabel(configurationGroup, "Locked", SWT.RIGHT);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).grab(true, false).applyTo(lockLabel);
			
			final Button lockButton = getToolkit().createButton(configurationGroup, "", SWT.TOGGLE);
			lockButton.setImage(InternalImage.LOCK.getImage());
			
			final Button saveConfigButton = getToolkit().createButton(configurationGroup, "", SWT.PUSH);
			saveConfigButton.setImage(InternalImage.SAVE.getImage());
			saveConfigButton.setEnabled(false);
			
			// Group
			label = getToolkit().createLabel(configurationGroup, "Group: ", SWT.RIGHT);
			GridDataFactory.fillDefaults().align(SWT.END, SWT.BEGINNING).hint(80, SWT.DEFAULT).applyTo(label);

			final Combo groupText = new Combo(configurationGroup, SWT.DROP_DOWN | SWT.BORDER);
			GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).hint(150, SWT.DEFAULT).applyTo(groupText);
			bindingContext.bindValue(
					SWTObservables.observeSelection(groupText),
					BeansObservables.observeDetailValue(Realm.getDefault(), selection, "group", String.class));
			groupText.setEnabled(false);
			
			IFolder configFolder = WorkspaceUtils.getFolder(SystemProperties.CONFIG_FOLDER.getValue());
			try {
				configFolder.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
				FillGroupCombo(groupText, configFolder, "");
			} catch (CoreException e1) {
			}
			
			// fill space after combo
			label = getToolkit().createLabel(configurationGroup, "");
			GridDataFactory.fillDefaults().span(3, 1).applyTo(label);
			
			/*****************************************************************
			 * Script editing
			 *****************************************************************/
			// Script area
			label = getToolkit().createLabel(configurationGroup, "Script: ", SWT.RIGHT);
			GridDataFactory.fillDefaults().align(SWT.END, SWT.BEGINNING).hint(80, SWT.DEFAULT).applyTo(label);
			TabFolder tabFolder = new TabFolder(configurationGroup, SWT.NONE);
			GridDataFactory.fillDefaults().hint(500, 220).span(4, 1).applyTo(tabFolder);
			
			/*****************************************************************
			 * Init script tab
			 *****************************************************************/
			// Holder
			TabItem initItem = new TabItem(tabFolder, SWT.NONE);
			initItem.setText("Initialise");
			Composite initArea = getToolkit().createComposite(tabFolder);
			GridLayoutFactory.swtDefaults().margins(0, 0).numColumns(2).equalWidth(false).applyTo(initArea);
			
			// Text editor
			final Text initScriptText = getToolkit().createText(initArea, "", SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
			initItem.setControl(initArea);
			bindingContext.bindValue(
					SWTObservables.observeText(initScriptText, SWT.Modify),
					BeansObservables.observeDetailValue(Realm.getDefault(), selection, "initScript", String.class));
			initScriptText.setEditable(false);
			GridDataFactory.swtDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).span(2, 1).applyTo(initScriptText);
			
			// Control
			final ProgressBar initDriveProgressBar = new ProgressBar(initArea, SWT.INDETERMINATE);
			initDriveProgressBar.setVisible(false);
			GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(initDriveProgressBar);
			final Button initControlButton = getToolkit().createButton(initArea, "Test Drive", SWT.PUSH);
			GridDataFactory.swtDefaults().align(SWT.END, SWT.TOP).applyTo(initControlButton);
			
			/*****************************************************************
			 * Pre-transmission tab
			 *****************************************************************/
			TabItem preTransmissionItem = new TabItem(tabFolder, SWT.NONE);
			preTransmissionItem.setText("Pre-transmission");
			
			Composite preTransmissionArea = getToolkit().createComposite(tabFolder);
			GridLayoutFactory.fillDefaults().margins(0, 0).numColumns(2).applyTo(preTransmissionArea);
			
			final Text preTransmissionScriptText = getToolkit().createText(preTransmissionArea, "", SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
			preTransmissionItem.setControl(preTransmissionArea);
			bindingContext.bindValue(
					SWTObservables.observeText(preTransmissionScriptText, SWT.Modify),
					BeansObservables.observeDetailValue(Realm.getDefault(), selection, "preTransmissionScript", String.class));
			preTransmissionScriptText.setEditable(false);
			GridDataFactory.swtDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).span(2, 1).applyTo(preTransmissionScriptText);
			
			final ProgressBar transDriveProgressBar = new ProgressBar(preTransmissionArea, SWT.INDETERMINATE);
			GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(transDriveProgressBar);
			final Button transControlButton = getToolkit().createButton(preTransmissionArea, "Test Drive", SWT.PUSH);
			
			/*****************************************************************
			 * Pre-scattering tab
			 *****************************************************************/
			TabItem preScatteringItem = new TabItem(tabFolder, SWT.NONE);
			preScatteringItem.setText("Pre-scattering");
			Composite preScatteringArea = getToolkit().createComposite(tabFolder);
			GridLayoutFactory.fillDefaults().margins(0, 0).numColumns(3).applyTo(preScatteringArea);
			preScatteringItem.setControl(preScatteringArea);
			
			final Button useManualAttenuationAlogrithmButton = getToolkit().createButton(preScatteringArea, "Use manual attenuation algorithm from: ", SWT.CHECK);
			bindingContext.bindValue(
					SWTObservables.observeSelection(useManualAttenuationAlogrithmButton),
					BeansObservables.observeDetailValue(selection, "useManualAttenuationAlgorithm", boolean.class));
			useManualAttenuationAlogrithmButton.setEnabled(false);
			
			final Text startingAttenuationText = getToolkit().createText(preScatteringArea, "", SWT.SINGLE);
			GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(startingAttenuationText);
			bindingContext.bindValue(
					SWTObservables.observeText(startingAttenuationText, SWT.Modify),
					BeansObservables.observeDetailValue(Realm.getDefault(), selection, "startingAttenuation", int.class));
			startingAttenuationText.setEditable(false);
			
			final Text preScatteringScriptText = getToolkit().createText(preScatteringArea, "", SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).span(3, 1).applyTo(preScatteringScriptText);
			bindingContext.bindValue(
					SWTObservables.observeText(preScatteringScriptText, SWT.Modify),
					BeansObservables.observeDetailValue(Realm.getDefault(), selection, "preScatteringScript", String.class));
			preScatteringScriptText.setEditable(false);

			final ProgressBar scattDriveProgressBar = new ProgressBar(preScatteringArea, SWT.INDETERMINATE);
			GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(2, 1).applyTo(scattDriveProgressBar);
			final Button scattControlButton = getToolkit().createButton(preScatteringArea, "Test Drive", SWT.PUSH);
			
			/*****************************************************************
			 * Button logic
			 *****************************************************************/
			if (executor != null) {
				initControlButton.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						if (executor.isBusy()) {
							try {
								SicsCore.getSicsController().interrupt();
							} catch (SicsIOException e1) {
								logger.error("Cannot interrupt SICS from test drive button", e);
							}
						} else {
							startConfigDrive("print('### Test drive configuration " + nameText.getText() + " (init) ###')\n" +
//									"from bragg.quokka import quokka\n" +
//									"from bragg.quokka.quokka import *\n" + 
									initScriptText.getText());
						}
					}
				});
				transControlButton.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						if (executor.isBusy()) {
							try {
								SicsCore.getSicsController().interrupt();
							} catch (SicsIOException e1) {
								logger.error("Cannot interrupt SICS from test drive button", e);
							}
						} else {
							// Run init + transmission mode
							startConfigDrive("print('### Test drive configuration " + nameText.getText() + " (init + transmission) ###')\n" + initScriptText.getText() + "\n" + preTransmissionScriptText.getText());
						}
					}
				});
				scattControlButton.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						if (executor.isBusy()) {
							try {
								SicsCore.getSicsController().interrupt();
							} catch (SicsIOException e1) {
								logger.error("Cannot interrupt SICS from test drive button", e);
							}
						} else {
							// Run init + scattering mode
							startConfigDrive("print('### Test drive configuration " + nameText.getText() + " (init + scattering) ###')\n" + initScriptText.getText() + "\n" + preScatteringScriptText.getText());
						}
					}
				});
				
				// Monitor to make UI changes
				executorEventHandler = new IEventHandler<ScriptExecutorStateEvent>() {
					@Override
					public void handleEvent(ScriptExecutorStateEvent event) {
						if (event.isBusy()) {
							SafeUIRunner.asyncExec(new SafeRunnable() {
								public void run() throws Exception {
									initDriveProgressBar.setVisible(true);
									transDriveProgressBar.setVisible(true);
									scattDriveProgressBar.setVisible(true);
									initControlButton.setText("Interrupt");
									transControlButton.setText("Interrupt");
									scattControlButton.setText("Interrupt");
								}
							});
						} else {
							SafeUIRunner.asyncExec(new SafeRunnable() {
								public void run() throws Exception {
									initDriveProgressBar.setVisible(false);
									transDriveProgressBar.setVisible(false);
									scattDriveProgressBar.setVisible(false);
									initControlButton.setText("Test Drive");
									transControlButton.setText("Test Drive");
									scattControlButton.setText("Test Drive");
								}
							});
						}
					}
				};
				PlatformUtils.getPlatformEventBus().subscribe(executor, executorEventHandler);
			}
			
			/*****************************************************************
			 * Transmission settings
			 *****************************************************************/
			Group transmissionGroup = new Group(parent, SWT.NONE);
			transmissionGroup.setText("Transmission");
			getToolkit().adapt(transmissionGroup);
			transmissionGroup.setLayout(new GridLayout(4, false));
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(transmissionGroup);
			
			// Mode
			String[] availableModes = new String[3];
			availableModes[0] = ScanMode.TIME.getLabel();
			availableModes[1] = ScanMode.COUNTS.getLabel();
			availableModes[2] = ScanMode.BM1.getLabel();
			
			label = getToolkit().createLabel(transmissionGroup, "Mode: ", SWT.RIGHT);
			GridDataFactory.fillDefaults().align(SWT.END, SWT.BEGINNING).hint(80, SWT.DEFAULT).applyTo(label);
			final ComboViewer transModeCombo = new ComboViewer(transmissionGroup, SWT.READ_ONLY);
			GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).hint(150, SWT.DEFAULT).applyTo(transModeCombo.getCombo());
			transModeCombo.setContentProvider(new ArrayContentProvider());
			transModeCombo.setLabelProvider(new LabelProvider());
			transModeCombo.setInput(availableModes);
			// SWT view bug ... need to manually layout this combo
			transModeCombo.getCombo().pack();
			transmissionGroup.layout();
			
//			final Combo transModeCombo = new Combo(transmissionGroup, SWT.READ_ONLY);
//			transModeCombo.setItems(InstrumentConfig.modeList.toArray(new String[InstrumentConfig.modeList.size()]));
//			bindingContext.bindList(SWTObservables.observeItems(transModeCombo), BeansObservables.observeDetailList(Realm.getDefault(), selection, "availableModes", String.class), null, null);
//			bindingContext.bindValue(SWTObservables.observeSelection(transModeCombo), BeansObservables.observeDetailValue(Realm.getDefault(), selection, "transmissionMode", String.class), null, null);
			tableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					Object selection = ((IStructuredSelection) event.getSelection()).getFirstElement();
					if (selection instanceof InstrumentConfig) {
						InstrumentConfig config = (InstrumentConfig) selection;
						transModeCombo.setSelection(new StructuredSelection(config.getTransmissionMode().getLabel()));
					} else {
						transModeCombo.setSelection(new StructuredSelection(new Object[0]));
					}
				}				
			});
			transModeCombo.addSelectionChangedListener(new ISelectionChangedListener() {
				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					if (selection.getValue() instanceof InstrumentConfig) {
						InstrumentConfig config = (InstrumentConfig) selection.getValue();
						
						String selection = ((IStructuredSelection)event.getSelection()).getFirstElement().toString();
						if ((selection != null) && !selection.isEmpty()) {
							ScanMode mode = ScanMode.fromLabel(selection);
							config.setTransmissionMode(mode);
						}
					}
				}
			});
			
			// Preset
			label = getToolkit().createLabel(transmissionGroup, "Preset: ", SWT.RIGHT);
			GridDataFactory.fillDefaults().align(SWT.END, SWT.BEGINNING).hint(80, SWT.DEFAULT).applyTo(label);
			Text transPresetText = getToolkit().createText(transmissionGroup, "", SWT.BORDER);
			GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).hint(150, SWT.DEFAULT).applyTo(transPresetText);
			// [GT-111] Support scientific notation
			UpdateValueStrategy targetToModelStrategy = new UpdateValueStrategy();
			DecimalFormat numberFormat = new DecimalFormat();
			numberFormat.setScientificNotation(true);
//			numberFormat.setExponentSignAlwaysShown(true);
			targetToModelStrategy.setConverter(StringToNumberConverter.toLong(numberFormat, true));
			UpdateValueStrategy modelToTargetStrategy = new UpdateValueStrategy();
			modelToTargetStrategy.setConverter(NumberToStringConverter.fromLong(numberFormat, true));
			bindingContext.bindValue(
					SWTObservables.observeText(transPresetText, SWT.Modify),
					BeansObservables.observeDetailValue(Realm.getDefault(), selection, "transmissionPreset", long.class),
					targetToModelStrategy,
					modelToTargetStrategy);
			
			/*****************************************************************
			 * Scattering settings
			 *****************************************************************/
			Group scatteringGroup = new Group(parent, SWT.NONE);
			scatteringGroup.setText("Scattering");
			getToolkit().adapt(scatteringGroup);
			scatteringGroup.setLayout(new GridLayout(4, false));
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(scatteringGroup);
			
			// Mode
			label = getToolkit().createLabel(scatteringGroup, "Mode: ", SWT.RIGHT);
			GridDataFactory.fillDefaults().align(SWT.END, SWT.BEGINNING).hint(80, SWT.DEFAULT).applyTo(label);
			final ComboViewer scatteringModeCombo = new ComboViewer(scatteringGroup, SWT.READ_ONLY);
			GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).hint(150, SWT.DEFAULT).applyTo(scatteringModeCombo.getCombo());
			scatteringModeCombo.setContentProvider(new ArrayContentProvider());
			scatteringModeCombo.setLabelProvider(new LabelProvider());
			scatteringModeCombo.setInput(availableModes);
			// SWT view bug ... need to manually layout this combo
			scatteringModeCombo.getCombo().pack();
			scatteringGroup.layout();
			
			tableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					Object selection = ((IStructuredSelection) event.getSelection()).getFirstElement();
					if (selection instanceof InstrumentConfig) {
						InstrumentConfig config = (InstrumentConfig) selection;
						scatteringModeCombo.setSelection(new StructuredSelection(config.getMode().getLabel()));
					} else {
						scatteringModeCombo.setSelection(new StructuredSelection(new Object[0]));
					}
				}				
			});
			scatteringModeCombo.addSelectionChangedListener(new ISelectionChangedListener() {
				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					if (selection.getValue() instanceof InstrumentConfig) {
						InstrumentConfig config = (InstrumentConfig)selection.getValue();

						String selection = ((IStructuredSelection)event.getSelection()).getFirstElement().toString();
						if ((selection != null) && !selection.isEmpty()) {
							ScanMode mode = ScanMode.fromLabel(selection);
							config.setMode(mode);
						}
					}
				}
			});
			
			// Default preset
			label = getToolkit().createLabel(scatteringGroup, "Preset: ", SWT.RIGHT);
			GridDataFactory.fillDefaults().align(SWT.END, SWT.BEGINNING).hint(80, SWT.DEFAULT).applyTo(label);
			Text defaultSettingText = getToolkit().createText(scatteringGroup, "", SWT.BORDER);
			GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).hint(150, SWT.DEFAULT).applyTo(defaultSettingText);
			bindingContext.bindValue(
					SWTObservables.observeText(defaultSettingText, SWT.Modify),
					BeansObservables.observeDetailValue(Realm.getDefault(), selection, "defaultSetting", long.class),
					targetToModelStrategy,
					modelToTargetStrategy);
			
			/*****************************************************************
			 * File association 
			 *****************************************************************/
			Group fileAssociationGroup = new Group(parent, SWT.NONE);
			fileAssociationGroup.setText("File Association");
			getToolkit().adapt(fileAssociationGroup);
			fileAssociationGroup.setLayout(new GridLayout(3, false));
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(fileAssociationGroup);
			
			label = getToolkit().createLabel(fileAssociationGroup, "Empty Beam Transmission: ");
			Text emptyBeamTransmissionText = getToolkit().createText(fileAssociationGroup, "");
			GridDataFactory.fillDefaults().grab(true, false).applyTo(emptyBeamTransmissionText);
			bindingContext.bindValue(
					SWTObservables.observeText(emptyBeamTransmissionText, SWT.Modify),
					BeansObservables.observeDetailValue(selection, "emptyBeamTransmissionDataFile", String.class));
			Button loadFromReportButton = getToolkit().createButton(fileAssociationGroup, "Load from Report", SWT.PUSH);
			loadFromReportButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					InstrumentConfig config = (InstrumentConfig) selection.getValue();
					if (config == null) {
						return;
					}
					Shell parentShell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
					LoadReportDialog dialog = new LoadReportDialog(parentShell, config);
					dialog.open();
				}
			});
			
			label = getToolkit().createLabel(fileAssociationGroup, "Empty Cell Transmission: ");
			Text emptyCellTransmissionText = getToolkit().createText(fileAssociationGroup, "");
			GridDataFactory.fillDefaults().grab(true, false).applyTo(emptyCellTransmissionText);
			bindingContext.bindValue(
					SWTObservables.observeText(emptyCellTransmissionText, SWT.Modify),
					BeansObservables.observeDetailValue(selection, "emptyCellTransmissionDataFile", String.class));
			Button clearAllButton = getToolkit().createButton(fileAssociationGroup, "Clear All", SWT.PUSH);
			GridDataFactory.fillDefaults().applyTo(clearAllButton);
			clearAllButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					InstrumentConfig config = (InstrumentConfig) selection.getValue();
					if (config != null) {
						// Clear model
						config.setEmptyBeamTransmissionDataFile(null);
						config.setEmptyCellTransmissionDataFile(null);
						config.setEmptyCellScatteringDataFile(null);
					}
				}
			});
			
			label = getToolkit().createLabel(fileAssociationGroup, "Empty Cell Scattering: ");
			Text emptyCellScatteringText = getToolkit().createText(fileAssociationGroup, "");
			GridDataFactory.fillDefaults().grab(true, false).applyTo(emptyCellScatteringText);
			bindingContext.bindValue(
					SWTObservables.observeText(emptyCellScatteringText, SWT.Modify),
					BeansObservables.observeDetailValue(selection, "emptyCellScatteringDataFile", String.class));
			label = getToolkit().createLabel(fileAssociationGroup, "");
			
			/*****************************************************************
			 * Restore button
			 *****************************************************************/
			Button restoreButton = getToolkit().createButton(parent, "Restore Script", SWT.PUSH);
			GridDataFactory.fillDefaults().align(SWT.END, SWT.CENTER).grab(true, false).applyTo(restoreButton);
			restoreButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					InstrumentConfig config = (InstrumentConfig) ((IStructuredSelection) tableViewer.getSelection()).getFirstElement();
					restoreScriptFromTemplate(config);
				}
			});
			
			/*****************************************************************
			 * Lock / unlock logic
			 *****************************************************************/
			lockButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent event) {
					if (lockButton.getSelection()) {
						PasswordRequestDialog dialog = new PasswordRequestDialog(parent.getShell());
						if (dialog.open() == PasswordRequestDialog.OK) {
							if ("quokkarox".equals(dialog.getPassword())) {
								lockButton.setImage(InternalImage.UNLOCK.getImage());
								lockLabel.setText("Unlocked");
								saveConfigButton.setEnabled(true);
								nameText.setEnabled(true);
								groupText.setEnabled(true);
								initScriptText.setEditable(true);
								preTransmissionScriptText.setEditable(true);
								preScatteringScriptText.setEditable(true);
								startingAttenuationText.setEditable(true);
								useManualAttenuationAlogrithmButton.setEnabled(true);
							} else {
						        MessageBox messageBox = new MessageBox(parent.getShell(), SWT.OK | SWT.ICON_INFORMATION);
						        messageBox.setText("Information");
						        messageBox.setMessage("Access Denied");
						        messageBox.open();
							}
						}
					} else {
						lockButton.setImage(InternalImage.LOCK.getImage());
						lockLabel.setText("Locked");
						saveConfigButton.setEnabled(false);
						nameText.setEnabled(false);
						groupText.setEnabled(false);
						initScriptText.setEditable(false);
						preTransmissionScriptText.setEditable(false);
						preScatteringScriptText.setEditable(false);
						startingAttenuationText.setEditable(false);
						useManualAttenuationAlogrithmButton.setEnabled(false);
					}
				}
			});
			
			/*****************************************************************
			 * Save logic
			 *****************************************************************/
			saveConfigButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent event) {
					// name (nameText) must be specified; group is optional
					
					String name = nameText.getText();
					String group = groupText.getText();
					
					if (name == null || name.length() == 0) {
						MessageDialog nameErrorDialog = new MessageDialog(
							parent.getShell(),
							"Warning",
							null,
							"Please specify a valid name for the current configuration.",
							MessageDialog.WARNING,
							new String[] { IDialogConstants.OK_LABEL },
							0);
						nameErrorDialog.open();
						return;
					}
					
					if (group != null) {
						group = group.replace('\\', '/').trim();
						if (group.contains("//") || !group.matches("[a-zA-Z0-9_/ ]*")) {
							MessageDialog nameErrorDialog = new MessageDialog(
								parent.getShell(),
								"Warning",
								null,
								"Please specify a valid group for the current configuration.",
								MessageDialog.WARNING,
								new String[] { IDialogConstants.OK_LABEL },
								0);
							nameErrorDialog.open();
							return;
						}
						
						// trim all (sub)groups 
						if (group.contains("/")) {
							StringBuilder correctedGroup = new StringBuilder();
							for (String subGroup : group.split("/"))
								if (subGroup.length() > 0)
									correctedGroup.append(subGroup.trim()).append('/');
							
							// remove '/' at end
							if (correctedGroup.length() != 0)
								correctedGroup.deleteCharAt(correctedGroup.length() - 1);
							
							group = correctedGroup.toString();
						}
					}
					
					final IObservableValue configDescription = BeansObservables.observeDetailValue(
							Realm.getDefault(), selection, "description", String.class);
					
					Object oldDescription = configDescription.getValue();
					SaveConfigDialog saveConfigDialog = new SaveConfigDialog(parent.getShell(), configDescription);
										
					if (saveConfigDialog.open() == Window.OK) {
						
						InstrumentConfigTemplate template = ((InstrumentConfig)selection.getValue()).generateTemplate();

						// save to workspace
						IFolder configFolder = WorkspaceUtils.getFolder(SystemProperties.CONFIG_FOLDER.getValue());
						File templatesFolder = new File(configFolder.getLocationURI().getPath(), group);
						if (!templatesFolder.exists())
							templatesFolder.mkdirs();

						File templateFile = new File(templatesFolder, template.getName() + ".xml");
						try {
							if (!templateFile.exists())
								templateFile.createNewFile();

							template.setFile(templateFile);
							FileWriter writer = new FileWriter(templateFile);
							ExperimentModelUtils.getXStream().toXML(template, writer);
							writer.flush();
							writer.close();
							
							MessageDialog okDialog = new MessageDialog(
									parent.getShell(),
									"Information",
									null,
									"The current configuration was successfully saved.",
									MessageDialog.INFORMATION,
									new String[] { IDialogConstants.OK_LABEL },
									0);
							okDialog.open();
							
						} catch (IOException e) {
							StatusManager.getManager().handle(
								new Status(
									IStatus.ERROR,
									Activator.PLUGIN_ID,
									IStatus.OK,
									"Error occured when saving configuration " + template.getName(),
									null),
								StatusManager.SHOW);
							logger.error("Failed to save config to " + templateFile.getName(), e);
						}
						
						// update group combo
						try {
							if (groupText.getItemCount() != 0)
								groupText.removeAll();
							configFolder.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
							FillGroupCombo(groupText, configFolder, "");
							groupText.setText(group);
						} catch (CoreException e1) {
						}
					} else {
						// reset description
						configDescription.setValue(oldDescription);
					}
				}
			});
		}
		
		private void addNewConfig() {
			InstrumentConfig newConfig = new InstrumentConfig();
			newConfig.setName("New");
			getExperiment().getInstrumentConfigs().add(newConfig);
			// Refresh UI
			tableViewer.setInput(new WritableList(getExperiment().getInstrumentConfigs(), InstrumentConfig.class));
			// Give focus
			tableViewer.setSelection(new StructuredSelection(newConfig));
		}
		
		private void addConfigFromTemplate(InstrumentConfigTemplate configTemplate, String group) {
			if (configTemplate == null) {
				return;
			}
			InstrumentConfig config = new InstrumentConfig();
			config.setTemplate(configTemplate);
			config.setGroup(group);
			getExperiment().getInstrumentConfigs().add(config);
			// Refresh UI
			tableViewer.setInput(new WritableList(getExperiment().getInstrumentConfigs(), InstrumentConfig.class));
			// Give focus
			tableViewer.setSelection(new StructuredSelection(config));
		}
		
		private void removeConfig(InstrumentConfig config) {
			config.setGroup("");
			getExperiment().getInstrumentConfigs().remove(config);			
			// Refresh UI
			tableViewer.setInput(new WritableList(getExperiment().getInstrumentConfigs(), InstrumentConfig.class));
			// Reselect the first element
			Object element = tableViewer.getElementAt(0);
			if (element != null) {
				tableViewer.setSelection(new StructuredSelection(element));
			}
		}
		
		private void startConfigDrive(final String script) {
			final IScriptExecutor executor = getContext().getSingleValue(IScriptExecutor.class);
			if (executor == null) {
				// Error
				return;
			}
			
			// Clear console if viewer is one of the pre-registered console
			final ICommandLineViewer viewer = (ICommandLineViewer) getContext().get(ScriptEngineTask.CONTEXT_KEY_SCRIPT_VIEWER);
			if (viewer != null) {
				SafeUIRunner.asyncExec(new SafeRunnable() {
					public void run() throws Exception {
						viewer.clearConsole();
					}
				});
			}
			
			// Make sure the executor is not busy in first 5 sec
			LoopRunner.run(new ILoopExitCondition() {
				@Override
				public boolean getExitCondition() {
					return !executor.isBusy();
				}
			}, 5000, 10);

			// Run Script
			IScriptBlock block = new ScriptBlock() {
				public String getScript() {
					return script;
				}
			};
			executor.runScript(block);
			
		}
		
		private void restoreScriptFromTemplate(InstrumentConfig config) {
			if (config == null || config.getTemplate() == null) {
				return;
			}
			config.restoreScripts();
		}
		
		public void dispose() {
			if (executor != null && executorEventHandler != null) {
				PlatformUtils.getPlatformEventBus().unsubscribe(executor, executorEventHandler);
			}
			executorEventHandler = null;
			executor = null;
			super.dispose();
		}
		
	}
	
	class PasswordRequestDialog extends Dialog {
	    private Text passwordField;
	    private String passwordString;

	    public PasswordRequestDialog(Shell parentShell) {
	        super(parentShell);
	    }

	    public String getPassword() {
	        return passwordString;
	    }

	    @Override
	    protected void configureShell(Shell newShell)  {
	        super.configureShell(newShell);
	        newShell.setText("Please enter password");
	    }

	    @Override
	    protected Control createDialogArea(Composite parent) {
	        Composite comp = (Composite) super.createDialogArea(parent);

	        GridLayout layout = (GridLayout) comp.getLayout();
	        layout.numColumns = 2;

	        Label passwordLabel = new Label(comp, SWT.RIGHT);
	        passwordLabel.setText("Password: ");
	        passwordField = new Text(comp, SWT.SINGLE | SWT.BORDER | SWT.PASSWORD);

	        GridData data = new GridData(SWT.FILL, SWT.CENTER, true, false);
	        passwordField.setLayoutData(data);

	        return comp;
	    }

	    @Override
	    protected void okPressed() {
	        passwordString = passwordField.getText();
	        super.okPressed();
	    }

	    @Override
	    protected void cancelPressed() {
	        passwordField.setText("");
	        super.cancelPressed();
	    }
	}
}
