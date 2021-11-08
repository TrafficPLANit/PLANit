package org.goplanit.assignment.ltm.sltm;

import java.util.Arrays;

import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegments;

/**
 * POJO to store Link segment based variables by the link segment internal id for sLTM link segment based data.
 * 
 * @author markr
 *
 */
public abstract class LinkSegmentData {

  /**
   * Empty array for quick memory based copying of doubles
   */
  private double[] initialStateSegmentArray;

  /**
   * create a copy of the initial state doubles array
   * 
   * @return initial state doubles array the size of the emptySegmentArray provided to the constructor
   */
  protected double[] createinitialStateLinkSegmentDoubleArray() {
    return initialStateSegmentArray.clone();
  }

  /**
   * Swap the two 2Darray references
   * 
   * @param index   first row index to swap contents for
   * @param index2  second row index to swap contents for
   * @param array2d 2d array to swap contents for based on two row indices
   */
  protected static void swap(int index, int index2, double[][] array2d) {
    double[] dummy = array2d[index];
    array2d[index] = array2d[index2];
    array2d[index2] = dummy;
  }

  /**
   * Reduce all provided link segments' flows to capacity
   * 
   * @param flowPcuHArray to apply limit on (pcuPerHour)
   * @param linkSegments  to use
   */
  protected void limitFlowsToCapacity(double[] flowPcuHArray, final MacroscopicLinkSegments linkSegments) {
    for (MacroscopicLinkSegment linkSegment : linkSegments) {
      int lsId = (int) linkSegment.getId();
      flowPcuHArray[lsId] = Math.min(flowPcuHArray[lsId], linkSegment.getCapacityOrDefaultPcuH());
    }
  }

  /**
   * Constructor
   * 
   * @param initialStateSegmentArray empty array used to initialize data stores
   */
  public LinkSegmentData(double[] initialStateSegmentArray) {
    this.initialStateSegmentArray = initialStateSegmentArray;
  }

  /**
   * Constructor
   * 
   * @param linkSegmentsSize to use for initial state array
   * @param initialValue     to use for the initial state arrays entries
   */
  public LinkSegmentData(int linkSegmentsSize, double initialValue) {
    this.initialStateSegmentArray = new double[linkSegmentsSize];
    Arrays.fill(initialStateSegmentArray, initialValue);
  }

  /**
   * copy from origin to destination assuming entire array is to be copied and both arrays are of equal size
   * 
   * @param originArray      copy from
   * @param destinationArray copy to
   */
  public static void copyTo(double[] originArray, double[] destinationArray) {
    System.arraycopy(originArray, 0, destinationArray, 0, originArray.length);
  }

}
