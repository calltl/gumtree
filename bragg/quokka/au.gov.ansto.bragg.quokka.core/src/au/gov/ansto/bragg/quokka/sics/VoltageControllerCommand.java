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

package au.gov.ansto.bragg.quokka.sics;

public enum VoltageControllerCommand {

	UP("up"), DOWN("down"), STOP("stop");

	private VoltageControllerCommand(String command) {
		this.command = command;
	}

	public String getCommand() {
		return command;
	}

	private String command;
}
