package org.planit.odmatrix.skim;

import org.ojalgo.array.Array2D;
import org.planit.odmatrix.ODMatrixIterator;
import org.planit.odmatrix.ODMatrixIteratorImpl;

public class ODSkimMatrix {
	
    /**
     * Number of travel analysis zones considered in skim matrix
     */
    protected final int numberOfTravelAnalysisZones;

    /**
     * the trips of this matrix
     */
    protected Array2D<Double> odSkimMatrixContents;

	
	public ODSkimMatrix(int numberOfTravelAnalysisZones) {
        this.numberOfTravelAnalysisZones = numberOfTravelAnalysisZones;
        this.odSkimMatrixContents = Array2D.PRIMITIVE32.makeZero(numberOfTravelAnalysisZones,
                numberOfTravelAnalysisZones);
	}
	
	public void setValue(int originZone, int destinationZone, double tripCost) {
		odSkimMatrixContents.set(originZone, destinationZone, tripCost);
	}
	
    /**
     * Generate the iterator for this ODSkimMatrix object
     * 
     * @return iterator for this ODSkimMatrix object
     */
     public ODMatrixIterator iterator() {
    	return new ODMatrixIteratorImpl(odSkimMatrixContents, numberOfTravelAnalysisZones);
    }
     
}
