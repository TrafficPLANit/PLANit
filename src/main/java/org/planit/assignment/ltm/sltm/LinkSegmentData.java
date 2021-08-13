package org.planit.assignment.ltm.sltm;

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
   * Constructor
   * 
   * @param emptySegmentArray empty array used to initialize data stores
   */
  public LinkSegmentData(double[] emptySegmentArray) {
    this.emptySegmentArray = emptySegmentArray;
  }

}
