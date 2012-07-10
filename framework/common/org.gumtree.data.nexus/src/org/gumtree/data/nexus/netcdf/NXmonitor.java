/*******************************************************************************
 * Copyright (c) 2010 Australian Nuclear Science and Technology Organisation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *    Norman Xiong (nxi@Bragg Institute) - initial API and implementation
 ******************************************************************************/
package org.gumtree.data.nexus.netcdf;

import org.gumtree.data.impl.netcdf.NcDataset;
import org.gumtree.data.impl.netcdf.NcGroup;
import org.gumtree.data.interfaces.IGroup;
import org.gumtree.data.nexus.INXmonitor;

import ucar.nc2.Group;

/**
 * Netcdf implementation of NeXus NXmonitor.
 * @author nxi
 * 
 */
public class NXmonitor extends NXGroup implements INXmonitor {

	/**
	 * Constructor with initialisation.
	 * 
	 * @param dataset
	 *            NcDataset object
	 * @param parent
	 *            NcGroup object
	 * @param shortName
	 *            String value
	 * @param updateParent
	 *            if parent will know this as a child group
	 */
	public NXmonitor(final NcDataset dataset, final NcGroup parent,
			final String shortName, final boolean updateParent) {
		super(dataset, parent, shortName, updateParent);
	}

	/**
	 * Construct an empty instance under a parent group.
	 * 
	 * @param parent
	 *            NcGroup object
	 * @param shortName
	 *            String value
	 */
	public NXmonitor(final NcGroup parent, final String shortName) {
		super(parent, shortName);
		addStringAttribute(NXConstants.GLOBAL_NEXUS_CLASS_LABEL,
				NXConstants.MONITOR_NEXUS_CLASS_VALUE);
	}

	/**
	 * @param dataset
	 * @param parent
	 * @param shortName
	 */
	// public NXmonitor(NcDataset dataset, Group parent, String shortName) {
	// super(dataset, parent, shortName);
	// // TODO Auto-generated constructor stub
	// }

	/**
	 * Copy constructor from a Netcdf group.
	 * 
	 * @param from
	 *            Netcdf Group
	 * @param dataset
	 *            NcDataset object
	 */
	public NXmonitor(final Group from, final NcDataset dataset) {
		super(from, dataset);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Copy constructor from a GDM IGroup object.
	 * 
	 * @param from
	 *            GDM IGroup object
	 */
	public NXmonitor(final IGroup from) {
		super(from);
		// TODO Auto-generated constructor stub
	}

}
