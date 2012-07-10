/******************************************************************************* 
* Copyright (c) 2008 Australian Nuclear Science and Technology Organisation.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0 
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
* 
* Contributors: 
*    Norman Xiong (nxi@Bragg Institute) - initial API and implementation
*******************************************************************************/
package org.gumtree.data.nexus.fitting;

import java.io.IOException;

import org.gumtree.data.nexus.INXdata;
import org.gumtree.data.nexus.fitting.StaticField.FunctionType;


/**
 * @author nxi
 * Created on 24/06/2008
 */
public class CubicFitter extends Fitter {

	private String cubic1DFuncionText = 
		"a*x[0]*x[0]*x[0]+b*x[0]*x[0]+c*x[0]+d";
	/**
	 * 
	 */
	public CubicFitter() {
		fitter = fitFactory.createFitter(fitterType.getValue(), enginType.name());
		setFunctionType(FunctionType.Cubic);
	}

	public CubicFitter(int dimension) throws FitterException{
		this();
		setDimension(dimension);
		switch (dimension) {
		case 1:
			setFunctionText(cubic1DFuncionText);
			addParameter("a");
			addParameter("b");
			addParameter("c");
			addParameter("d");
			break;
		default:
			break;
		}
		createFitFunction();
	}
	
	public CubicFitter(INXdata data) throws DimensionNotSupportedException, 
	IOException, FitterException{
		this(data.getSignal().getRank());
		createHistogram(data);
	}
	/* (non-Javadoc)
	 * @see au.gov.ansto.bragg.freehep.jas3.core.Fitter#setParameters()
	 */
	@Override
	public void setParameters() {
		// TODO Auto-generated method stub

	}

}
