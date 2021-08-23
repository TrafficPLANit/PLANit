package org.planit.assignment.ltm.sltm;

import org.planit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.planit.utils.network.layer.macroscopic.MacroscopicLinkSegments;

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
  private double[] emptySegmentArray;

  /**
   * create a copy of the empty doubles array
   * 
   * @return empty doubles array the size of the emptySegmentArray provided to the constructor
   */
  protected double[] createEmptyLinkSegmentDoubleArray() {
    return emptySegmentArray.clone();
  }

  /**
   * swap the two 2Darray references
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
      flowPcuHArray[lsId] = Math.min(flowPcuHArray[lsId], linkSegment.computeCapacityPcuH());
    }
  }

  /**
   * Constructor
   * 
   * @param emptySegmentArray empty array used to initialize data stores
   */
  public LinkSegmentData(double[] emptySegmentArray) {
    this.emptySegmentArray = emptySegmentArray;
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
