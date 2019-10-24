package org.planit.od.odmatrix;

import org.ojalgo.array.Array2D;
import org.planit.od.ODData;
import org.planit.zoning.Zone;
import org.planit.zoning.Zoning;

/**
 * This class contains common methods for handling origin-demand matrices.
 * 
 * @author gman6028
 *
 */
public abstract class ODMatrix implements ODData<Double> {

    /**
     * holder for zones considered in the matrix
     */
    protected Zoning.Zones zones;

    /**
     * matrix of data values
     */
    protected Array2D<Double> matrixContents;

    /**
     * Constructor for matrix based OD demand matrix
     * 
     * @param zones holder for zones considered in the matrix
     */
    public ODMatrix(Zoning.Zones zones) {
        this.zones = zones;
        int numberOfTravelAnalysisZones = zones.getNumberOfZones();
        this.matrixContents = Array2D.PRIMITIVE32.makeZero(numberOfTravelAnalysisZones,
                numberOfTravelAnalysisZones);
    }
    
	/**
	 * Sets the value for a specified origin and destination
	 * 
	 * @param origin specified origin
	 * @param destination specified destination
	 * @param value value at the specified cell
	 */
	public void setValue(Zone origin, Zone destination, Double value) {
		long originId = origin.getId();
		long destinationId = destination.getId();
        if (originId == destinationId) {
            // demand or cost from any origin to itself must be zero
            matrixContents.set(originId, destinationId, 0.0);
        } else {
            matrixContents.set(originId, destinationId, value);
        }
    }
    
	/**
	 * Returns the value for a specified origin and destination
	 * 
	 * @param origin specified origin
	 * @param destination specified destination
	 * @return value at the specified cell
	 */
    public Double getValue(Zone origin, Zone destination) {
		long originId = origin.getId();
		long destinationId = destination.getId();
        return matrixContents.get(originId, destinationId);
    }

	/**
	 * Returns the number of zones contained in the object
	 *  
	 * @return number of zones in the object
	 */
    public int getNumberOfTravelAnalysisZones() {
    	return zones.getNumberOfZones();
    }
    
	/**
	 * Returns an iterator which can iterate through all the origin-destination cells in the matrix
	 * 
	 * @return iterator through all the origin-destination cells
	 */
    public ODMatrixIterator iterator() {
    	return new ODMatrixIterator(matrixContents, zones);
    }
}
