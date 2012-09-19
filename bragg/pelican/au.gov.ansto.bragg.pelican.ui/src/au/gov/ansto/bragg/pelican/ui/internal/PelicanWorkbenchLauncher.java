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

package au.gov.ansto.bragg.pelican.ui.internal;

import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.WorkbenchWindow;
import org.gumtree.ui.service.launcher.AbstractLauncher;
import org.gumtree.ui.service.launcher.LauncherException;
import org.gumtree.ui.service.multimonitor.IMultiMonitorManager;
import org.gumtree.ui.service.multimonitor.support.MultiMonitorManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PelicanWorkbenchLauncher extends AbstractLauncher {

	
	private static final String ID_PERSPECTIVE_SCRIPTING = "au.gov.ansto.bragg.nbi.ui.scripting.ScriptingPerspective";
	
	private static final String ID_PERSPECTIVE_EXPERIMENT = "au.gov.ansto.bragg.pelican.ui.TCLRunnerPerspective";
	
	// Use the default as buffer to hold the editor
	private static final String ID_PERSPECTIVE_DEFAULT = "au.gov.ansto.bragg.nbi.ui.EmptyPerspective";
	
	private static Logger logger = LoggerFactory.getLogger(PelicanWorkbenchLauncher.class);
	
	
	public PelicanWorkbenchLauncher() {
	}

	public void launch() throws LauncherException {
		{			
			// TODO: move this logic to experiment UI manager service
			IWorkbenchWindow activeWorkbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			if (activeWorkbenchWindow instanceof WorkbenchWindow) {
				((WorkbenchWindow) activeWorkbenchWindow).setCoolBarVisible(false);
			}
//			IMultiMonitorManager mmManager = ServiceUtils.getService(IMultiMonitorManager.class);
			IMultiMonitorManager mmManager = new MultiMonitorManager();
			// Attempt to close intro
//			IIntroPart introPart = PlatformUI.getWorkbench().getIntroManager().getIntro();
//			PlatformUI.getWorkbench().getIntroManager().closeIntro(introPart);
			
//			InstrumentDashboardLauncher launcher = new InstrumentDashboardLauncher();
//			launcher.launch(0);
			
			mmManager.showPerspectiveOnOpenedWindow(ID_PERSPECTIVE_EXPERIMENT, 0, 0, mmManager.isMultiMonitorSystem());
			
//			if (PlatformUI.getWorkbench().getWorkbenchWindowCount() < 2) {
//				// open new window as editor buffer
//				mmManager.openWorkbenchWindow(ID_PERSPECTIVE_DEFAULT, 1, true);
//			}
//			mmManager.showPerspectiveOnOpenedWindow(ID_PERSPECTIVE_SCRIPTING, 1, 1, mmManager.isMultiMonitorSystem());

		}
	}

}
