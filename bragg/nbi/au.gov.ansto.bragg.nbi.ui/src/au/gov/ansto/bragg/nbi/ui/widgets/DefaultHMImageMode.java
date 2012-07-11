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

public enum DefaultHMImageMode implements HMImageMode {
	
	HISTMEM_XY("Total x-y histogram", "&type=TOTAL_HISTOGRAM_XY"),
	HISTMEM_X("Total x histogram", "&type=TOTAL_HISTOGRAM_X"),
	HISTMEM_Y("Total y histogram", "&type=TOTAL_HISTOGRAM_Y"),
	RATE_XY("x-y Event Rate Map", "&type=RATEMAP_XY"),
	RATE_X("x Event Rate Map", "&type=RATEMAP_X"),
	RATE_Y("y Event Rate Map", "&type=RATEMAP_Y");
	
	private DefaultHMImageMode(String label, String query) {
		this.label = label;
		this.query = query;
	}
	
	public String toString() {
		return getLabel();
	}
	
	public String getLabel() {
		return label;
	}
	
	public String getQuery() {
		return query;
	}
	
	public HMImageMode[] getValues() {
		return DefaultHMImageMode.values();
	}
	
	private String label;
	private String query;
	
}