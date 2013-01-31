/*******************************************************************************
 * Copyright (c) 2007 Australian Nuclear Science and Technology Organisation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tony Lam (Bragg Institute) - initial API and implementation
 *******************************************************************************/

package au.gov.ansto.bragg.nbi.ui.widgets;

import javax.inject.Inject;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.BeansObservables;
import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.gumtree.service.dataaccess.IDataAccessManager;
import org.gumtree.ui.widgets.ScalableImageDisplayWidget;

import au.gov.ansto.bragg.nbi.ui.internal.Activator;

public class HMImageDisplayWidget extends ScalableImageDisplayWidget {
	
	private HMImageMode imageMode;
	
	public HMImageDisplayWidget(Composite parent, int style) {
		super(parent, style);
	}
	
	protected void widgetDispose() {
		imageMode = null;
		super.widgetDispose();
	}
	
	protected Composite createImageArea() {
		GridLayoutFactory.swtDefaults().numColumns(6).margins(0, 0).applyTo(this);
		
		// Mode
		Label label = getToolkit().createLabel(this, "Mode: ");
		label.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT));
		GridDataFactory.swtDefaults().align(SWT.END, SWT.CENTER).grab(true, false).applyTo(label);
		ComboViewer comboViewer = new ComboViewer(this, SWT.READ_ONLY);
		comboViewer.setContentProvider(new ArrayContentProvider());
		comboViewer.setLabelProvider(new LabelProvider());
		comboViewer.setInput(getImageMode().getValues());
		comboViewer.setSelection(new StructuredSelection(getImageMode()));
		comboViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				// Change display mode
				setImageMode((HMImageMode) ((IStructuredSelection) event.getSelection()).getFirstElement());
				// Update NOW
				Job job = new Job(HMImageDisplayWidget.class.getName()) {
					protected IStatus run(IProgressMonitor monitor) {
						try {
							// Get data (one off)
							pullData();
						} catch (Exception e) {
							return new Status(IStatus.ERROR, Activator.PLUGIN_ID,
									"Failed to fetch data.", e);
						}
						return Status.OK_STATUS;
					}			
				};
				job.setSystem(true);
				job.schedule();
			}
		});
		
		// Separator
		label = getToolkit().createLabel(this, "");
		GridDataFactory.swtDefaults().hint(65, SWT.DEFAULT).applyTo(label);
		
		// Refresh
		label = getToolkit().createLabel(this, "Refresh: ");
		label.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT));
		final Text refreshText = getToolkit().createText(this, "");
		Realm.runWithDefault(SWTObservables.getRealm(Display.getDefault()), new Runnable() {
			public void run() {
				DataBindingContext bindingContext = new DataBindingContext();
				bindingContext.bindValue(SWTObservables.observeText(refreshText,
						SWT.Modify), BeansObservables.observeValue(
								HMImageDisplayWidget.this, "refreshDelay"), new UpdateValueStrategy(), new UpdateValueStrategy());
			}
		});
		GridDataFactory.swtDefaults().hint(50, SWT.DEFAULT).applyTo(refreshText);
		label = getToolkit().createLabel(this, "sec");
		label.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT));
		
		Composite imageArea = getToolkit().createComposite(this);
		
		imageArea.setLayout(new FillLayout());
		GridLayoutFactory.fillDefaults().margins(0, 0).spacing(0, 0).applyTo(imageArea);
		GridDataFactory.swtDefaults().align(SWT.CENTER, SWT.CENTER).grab(true, true).span(6, 1).applyTo(imageArea);	
		
		return imageArea;
	}

	public HMImageMode getImageMode() {
		if (imageMode == null) {
			setImageMode(DefaultHMImageMode.values()[0]);
		}
		return imageMode;
	}
	
	public void setImageMode(HMImageMode imageMode) {
		this.imageMode = imageMode;
	}
	
	public String getDataURI() {
		return super.getDataURI() + getImageMode().getQuery();
	}
	
	@Inject
	public void setDataAccessManager(IDataAccessManager dataAccessManager) {
		super.setDataAccessManager(dataAccessManager);
	}
	
	@Override
	public void update() {
		setWidth(getBounds().width);
		setHeight(getBounds().height);
		super.update();
	}
	
	@Override
	public void redraw() {
		// TODO Auto-generated method stub
		super.redraw();
	}
	
	@Override
	public void layout() {
		// TODO Auto-generated method stub
		super.layout();
	}
}
