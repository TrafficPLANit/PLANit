package org.planit.demand;

import java.util.logging.Logger;

import org.ojalgo.array.Array2D;
import org.ojalgo.structure.Access1D;
import org.planit.utils.Pair;

/**
 * Carry demand based on origin-destination trip matrix in PCU/h
 * 
 * @author markr
 *
 */
public class MatrixDemand extends ODDemand {

    /**
     * Logger for this class
     */
    private static final Logger LOGGER = Logger.getLogger(MatrixDemand.class.getName());

    /**
     * Iterator over entries masking its matrix based container
     * 
     * @author markr
     */
    protected class MatrixDemandIterator implements ODDemandIterator {

        private int originId = 0;
        private int destinationId = 0;
        private int currentOriginId = 1;
        private int currentDestinationId = 1;
        private int beyondEndID = numberOfTravelAnalysisZones - 1;
        boolean hasNext = numberOfTravelAnalysisZones > 0;

        /**
         * Tests whether this iterator has more object to find
         * 
         * @return true if more object available, false otherwise
         */
        @Override
        public boolean hasNext() {
            return hasNext;
        }

        /**
         * Returns the value in the next cell of the origin-demand matrix
         * 
         * @return the value of the demand in the next cell
         */
        @Override
        public Double next() {
            currentDestinationId = destinationId + 1;
            currentOriginId = originId + 1;
            double value = get(originId, destinationId);
            if (destinationId < beyondEndID) {
                ++destinationId;
            } else if (originId < beyondEndID) {
                ++originId;
                destinationId = 0;
            } else {
                hasNext = false;
            }
            return value;
        }

        /**
         * Returns the origin row of the current cell
         * 
         * @return row number of origin of current cell
         */
        @Override
        public int getCurrentOriginId() {
            return currentOriginId - 1;
        }

        /**
         * Returns the destination column of the current cell
         * 
         * @return column number of destination of the current cell
         */
        @Override
        public int getCurrentDestinationId() {
            return currentDestinationId - 1;
        }

        /**
         * Returns the origin and destination of the current cell as a Pair
         * 
         * @return Pair containing the origin row and destination column of the current
         *         cell
         */
        @Override
        public Pair<Integer, Integer> getCurrentODPair() {
            return new Pair<Integer, Integer>(currentOriginId, currentDestinationId);
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
     * Constructor for matrix based od demand
     * 
     * @param numberOfTravelAnalysisZones
     *            number of travel analysis zones
     */
    public MatrixDemand(int numberOfTravelAnalysisZones) {
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
     * Fill a row of trip values in PCU. If the mode of the matrix has a PCU greater
     * than 1, the mode based trips are less than the provided PCU trips
     * 
     * @param originZone
     *            origin zone
     * @param pcuODTripFlowRates
     *            od trip flow rate in PCU/h
     */
    public void fillRowInPCU(long originZone, Access1D<Double> pcuODTripFlowRates) {
        demandMatrixContents.fillRow(originZone, pcuODTripFlowRates);
    }

    /**
     * Generate the iterator for this MatrixDemand object
     * 
     * @return iterator for this MatrixDemand object
     */
    @Override
    public ODDemandIterator iterator() {
        return new MatrixDemandIterator();
    }

}
