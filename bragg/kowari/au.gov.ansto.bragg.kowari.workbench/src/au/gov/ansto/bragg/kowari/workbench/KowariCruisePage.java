package au.gov.ansto.bragg.kowari.workbench;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.gumtree.ui.cruise.ICruisePanelPage;

import au.gov.ansto.bragg.kowari.workbench.internal.Activator;


public class KowariCruisePage implements ICruisePanelPage {

	@Override
	public String getName() {
		return "Kowari";
	}

	@Override
	public Composite createNormalWidget(Composite parent) {
		KowariCruisePageWidget widget = new KowariCruisePageWidget(parent,
				SWT.NONE);
		ContextInjectionFactory.inject(widget, Activator.getDefault()
				.getEclipseContext());
		return widget.render();
	}

	@Override
	public Composite createFullWidget(Composite parent) {
		KowariCruisePageWidget widget = new KowariCruisePageWidget(parent,
				SWT.NONE);
		ContextInjectionFactory.inject(widget, Activator.getDefault()
				.getEclipseContext());
		return widget.render();
	}

}
