package org.planit.od.odmatrix.demand;

import org.planit.od.odmatrix.ODMatrix;
import org.planit.zoning.Zoning;

/**
 * This class handles the OD demand matrix.
 * 
 * @author gman6028
 *
 */
public class ODDemandMatrix extends ODMatrix {

	/**
	 * Constructor
	 * 
	 * @param zones holds the zones defined in the network
	 */
	public ODDemandMatrix(Zoning.Zones zones) {
		super(zones);
	}

}