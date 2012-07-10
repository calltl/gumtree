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

package org.gumtree.workflow.ui.viewer2;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class WorkflowContextViewer extends AbstractWorkflowViewerComponent {

	public WorkflowContextViewer(Composite parent, int style) {
		super(parent, style);
	}

	protected void componentDispose() {
	}

	protected void createUI() {
	}
	
	protected void refreshUI() {
		if (isDisposed()) {
			return;
		}
		for (Control child : getChildren()) {
			child.dispose();
		}
		createUI();
	}

}
