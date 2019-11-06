package org.planit.od.odmatrix.skim;

import org.planit.od.odmatrix.ODMatrix;
import org.planit.output.enums.ODSkimOutputType;
import org.planit.zoning.Zone;
import org.planit.zoning.Zoning;

/**
 * This class stores an OD Skim matrix.
 * 
 * @author gman6028
 *
 */
public class ODSkimMatrix extends ODMatrix {

//TODO - We may need to add more overloads of the setValue() method below, if different OD skim types need other
//arguments to determine their cell value e.g. mode, route length, toll etc
	
	/**
	 * The ODSkimOutputType for this ODSkimMatrix
	 */
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

	/**
	 * Sets the value for a specified origin and destination 
	 * 
	 * The value to be stored can be dependent on OD skim output type, hence this method
	 * overrides the ODMatrix method.
	 * 
	 * @param origin specified origin
	 * @param destination specified destination
	 * @param value value at the specified cell
	 */
//TODO - At present this method is trivial because we only have NONE and COST as OD skim output types.
//We must update this when more OD skim output types are added.
	@Override
	public void setValue(Zone origin, Zone destination, Double value) {
		long originId = origin.getId();
		long destinationId = destination.getId();
        if (originId == destinationId) {
            // demand or cost from any origin to itself must be zero
            matrixContents.set(originId, destinationId, 0.0);
        } else {
        	switch (odSkimOutputType) {
        	case COST: matrixContents.set(originId, destinationId, value);
        	break;
        	case NONE: matrixContents.set(originId, destinationId, value);
        	break;
        	}
        }
    }
    
}
