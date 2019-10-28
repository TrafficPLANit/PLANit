package org.planit.od.odmatrix.skim;

import org.planit.od.odmatrix.ODMatrix;
import org.planit.output.enums.ODSkimOutputType;
import org.planit.zoning.Zoning;

/**
 * This class stores an OD Skim matrix.
 * 
 * @author gman6028
 *
 */
public class ODSkimMatrix extends ODMatrix {
	
	private final ODSkimOutputType odSkimOutputType;
	
	/**
	 * Constructor
	 * 
	 * @param zones holding the zones in the network
	 * @param odSkimOutputType the skim output type for this OD skim matrix
	 */
	public ODSkimMatrix(Zoning.Zones zones, ODSkimOutputType odSkimOutputType) {
		super(zones);
		this.odSkimOutputType = odSkimOutputType;
	}
	
	/**
	 * Returns the type of the current OD skim matrix
	 * 
	 * @return the OD skim matrix type for the current OD skim matrix
	 */
	public ODSkimOutputType getOdSkimOutputType() {
		return odSkimOutputType;
	}

}
