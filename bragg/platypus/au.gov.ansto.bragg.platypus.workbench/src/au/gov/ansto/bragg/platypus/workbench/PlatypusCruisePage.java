package au.gov.ansto.bragg.platypus.workbench;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.gumtree.ui.cruise.ICruisePanelPage;

public class PlatypusCruisePage implements ICruisePanelPage {

	@Override
	public String getName() {
		return "Platypus";
	}

	@Override
	public Composite create(Composite parent) {
		return new PlatypusCruisePageWidget(parent, SWT.NONE);
	}

}
