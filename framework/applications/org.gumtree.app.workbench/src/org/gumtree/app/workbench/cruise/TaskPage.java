package org.gumtree.app.workbench.cruise;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.gumtree.ui.cruise.ICruisePanelPage;
import org.gumtree.ui.tasklet.support.TaskletManagerViewer;
import org.gumtree.ui.util.workbench.WorkbenchUtils;

@SuppressWarnings("restriction")
public class TaskPage implements ICruisePanelPage {

	@Override
	public String getName() {
		return "Task";
	}

	@Override
	public Composite create(Composite parent) {
		// Initialise
		IEclipseContext context = WorkbenchUtils.getWorkbenchContext()
				.createChild();
		context.set(MWindow.class, WorkbenchUtils.getActiveMWindow());

		// Create viewer
		TaskletManagerViewer viewer = new TaskletManagerViewer(parent,
				SWT.NONE);
		ContextInjectionFactory.inject(viewer, context);

		return viewer;
	}

}
