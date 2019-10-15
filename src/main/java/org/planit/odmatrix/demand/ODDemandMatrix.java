package org.planit.odmatrix.demand;

import org.ojalgo.array.Array2D;
import org.planit.demands.ODDemand;
import org.planit.odmatrix.ODMatrixIterator;
import org.planit.odmatrix.ODMatrixIteratorImpl;

/**
 * Carry demand based on origin-destination trip matrix in PCU/h
 * 
 * @author markr
 *
 */
public class ODDemandMatrix extends ODDemand {

    /**
     * Number of travel analysis zones considered in od demand matrix
     */
    protected final int numberOfTravelAnalysisZones;

    /**
     * the trips of this matrix
     */
    protected final Array2D<Double> demandMatrixContents;

    /**
     * Constructor for matrix based OD demand matrix
     * 
     * @param numberOfTravelAnalysisZones  number of travel analysis zones
     */
    public ODDemandMatrix(int numberOfTravelAnalysisZones) {
        super();
        this.numberOfTravelAnalysisZones = numberOfTravelAnalysisZones;
        this.demandMatrixContents = Array2D.PRIMITIVE32.makeZero(numberOfTravelAnalysisZones,
                numberOfTravelAnalysisZones);
    }

    /**
     * Set a value (od flow) on a specified row and column
     * 
     * @param originZone
     *            the origin zone
     * @param destinationZone
     *            the destination zone
     * @param odTripFlowRate
     *            to be set, in PCU/h
     */
    public void set(long originZone, long destinationZone, double odTripFlowRate) {
        if ((originZone == destinationZone) && (odTripFlowRate > 0.0)) {
            // adjust demand from any origin to itself to be zero
            demandMatrixContents.set(originZone, destinationZone, 0.0);
        } else {
            demandMatrixContents.set(originZone, destinationZone, odTripFlowRate);
        }
    }

    /**
     * Collect a value (odFlow) from a specified row and column
     * 
     * @param originZone
     *            the origin zone
     * @param destinationZone
     *            the destination zone
     * @return odTripFlowRate for this OD pair, in PCU/h
     */
    public double get(long originZone, long destinationZone) {
        return demandMatrixContents.get(originZone, destinationZone);
    }

    /**
     * Generate the iterator for this ODDemandMatrix object
     * 
     * @return iterator for this ODDemandMatrix object
     */
    @Override
    public ODMatrixIterator iterator() {
    	return new ODMatrixIteratorImpl(demandMatrixContents, numberOfTravelAnalysisZones);
    }
    
    @Override
    public int getNumberOfTravelAnalysisZones() {
    	return numberOfTravelAnalysisZones;
    }

}
