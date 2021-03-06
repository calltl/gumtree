/*******************************************************************************
 * Copyright (c) 2007 Australian Nuclear Science and Technology Organisation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     nxi (Bragg Institute) - initial API and implementation
 *******************************************************************************/

package au.gov.ansto.bragg.dingo.ui.internal;

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.WorkbenchWindow;
import org.gumtree.ui.service.launcher.AbstractLauncher;
import org.gumtree.ui.service.launcher.LauncherException;
import org.gumtree.ui.service.multimonitor.IMultiMonitorManager;
import org.gumtree.ui.service.multimonitor.support.MultiMonitorManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DingoWorkbenchLauncher extends AbstractLauncher {

	
	private static final String ID_PERSPECTIVE_SICS = "au.gov.ansto.bragg.nbi.ui.SICSExperimentPerspective";
	private static final String ID_PERSPECTIVE_SCRIPTING = "au.gov.ansto.bragg.nbi.ui.scripting.ScriptingPerspective";
	private static final String ID_PERSPECTIVE_DEFAULT = "au.gov.ansto.bragg.nbi.ui.EmptyPerspective";
	
	private static Logger logger = LoggerFactory.getLogger(DingoWorkbenchLauncher.class);
	
//	private static boolean isCoolBarVisable = true;
	
	public DingoWorkbenchLauncher() {
	}

	public void launch() throws LauncherException {
		{			
			// TODO: move this logic to experiment UI manager service
			
			final IWorkbenchWindow activeWorkbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			final IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
			for (IWorkbenchWindow window : windows){
				if (window != null && window != activeWorkbenchWindow) {
					window.close();
				}
			}
			if (activeWorkbenchWindow instanceof WorkbenchWindow) {
//				activeWorkbenchWindow.getActivePage().closeAllPerspectives(true, false);
				IWorkbenchPage[] pages = activeWorkbenchWindow.getPages();
				for (IWorkbenchPage page : pages) {
					try {
						if (!ID_PERSPECTIVE_SICS.equals(page.getPerspective().getId())){
							activeWorkbenchWindow.getActivePage().closePerspective(page.getPerspective(), false, true);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			IMultiMonitorManager mmManager = new MultiMonitorManager();
			// Prepare status in screen 1 (maximised)

			mmManager.showPerspectiveOnOpenedWindow(ID_PERSPECTIVE_SICS, 0, 0, mmManager.isMultiMonitorSystem());
//			activeWorkbenchWindow.getActivePage().setEditorAreaVisible(false);
			if (PlatformUI.getWorkbench().getWorkbenchWindowCount() < 2) {
				// open new window as editor buffer
				mmManager.openWorkbenchWindow(ID_PERSPECTIVE_DEFAULT, 1, true);
			}
//			// position it
			mmManager.showPerspectiveOnOpenedWindow(ID_PERSPECTIVE_SCRIPTING, 1, 1, mmManager.isMultiMonitorSystem());

		}
	}

}
