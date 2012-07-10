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

package org.gumtree.ui.scripting.viewer;

import org.gumtree.service.eventbus.Event;

public class CommandlLineViewerEvent extends Event {

	public CommandlLineViewerEvent(ICommandLineViewer viewer) {
		super(viewer);
	}

	public ICommandLineViewer getPublisher() {
		return (ICommandLineViewer) super.getPublisher();
	}
	
}
