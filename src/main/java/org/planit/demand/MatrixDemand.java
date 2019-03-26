package org.planit.demand;

import org.ojalgo.array.Array2D;
import org.ojalgo.structure.Access1D;
import org.planit.utils.Pair;

/**
 * Carry demand based on origin-destination trip matrix in pcu/h 
 * @author markr
 *
 */
public class MatrixDemand extends ODDemand {
	
	/**
	 * Iterator over entries masking its matrix based container
	 * @author markr
	 */
	protected class MatrixDemandIterator implements ODDemandIterator{
		
		private int originId = 0;
		private int destinationId = 0;
		private int currentOriginId = 1;
		private int currentDestinationId = 1;
		private int beyondEndID = numberOfTravelAnalysisZones-1;
		boolean hasNext = numberOfTravelAnalysisZones>0;
		
		@Override
		public boolean hasNext() {
			return hasNext;
		}

		@Override
		public Double next() {
			currentDestinationId = destinationId + 1;
			currentOriginId = originId + 1;
			double value = get(originId, destinationId);
			if (destinationId < beyondEndID) {
				++destinationId;
			} else if ( originId < beyondEndID) {
				++originId;
				destinationId = 0;
			} else {
				hasNext = false;
			}
			return value;
		}
		
		@Override
		public int getCurrentOriginId() {
			return currentOriginId;
		}
		
		@Override
		public int getCurrentDestinationId() {
			return currentDestinationId;
		}
		
		@Override
		public Pair<Integer, Integer> getCurrentODPair() {
			return new Pair<Integer,Integer>(currentOriginId,currentDestinationId);
		}		
	}
	
	
	/**
	 * Number of travel analysis zones considered in od demand matrix
	 */
	protected final int numberOfTravelAnalysisZones;
	
	/**
	 * the trips of this matrix
	 */
	protected final Array2D<Double> demandMatrixContents;	
		
	/**
	 * constructor for matrix based od demand
	 */
	public MatrixDemand(int numberOfTravelAnalysisZones) {
		super();
		this.numberOfTravelAnalysisZones = numberOfTravelAnalysisZones;
		this.demandMatrixContents= Array2D.PRIMITIVE32.makeZero(numberOfTravelAnalysisZones, numberOfTravelAnalysisZones);
	}
	
	/** Set a value (od flow) on a specified row and column
	 * @param originZone
	 * @param destinationZone
	 * @param odTripFlowRate, desired od trip flow in pcu/h
	 */
	public void set(long originZone, long destinationZone, double odTripFlowRate) {
		if ((originZone == destinationZone) && (odTripFlowRate > 0.0)) {
			//adjust demand from any origin to itself to be zero
			demandMatrixContents.set(originZone, destinationZone, 0.0);
		} else {
			demandMatrixContents.set(originZone, destinationZone, odTripFlowRate);
		}
	}
	
	/** Collect a value (odFlow) from a specified row and column
	 * @param originZone
	 * @param destinationZone
	 */
	public double get(long originZone, long destinationZone) {
		return demandMatrixContents.get(originZone, destinationZone);
	}
	
	/** Fill a row of trip values in pcu, i.e. if the mode of the matrix has a pcu>1 than the mode based trips are 
	 * indeed less than the provided pcu trips
	 * @param row
	 * @param pcuODTripFlowRates, od trip flow rate in pcu/h
	 */
	public void fillRowInPCU(long originZone, Access1D<Double> pcuODTripFlowRates) {
		demandMatrixContents.fillRow(originZone, pcuODTripFlowRates);
	}

	@Override
	public ODDemandIterator iterator() {
		return new MatrixDemandIterator();
	}

}
