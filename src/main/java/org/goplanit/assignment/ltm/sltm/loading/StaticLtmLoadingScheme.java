package org.goplanit.assignment.ltm.sltm.loading;

/**
 * Defines the different types of solution scheme variations that exist and can be applied (progressively) during an sLTM network loading approach
 * 
 * @author markr
 *
 */
public enum StaticLtmLoadingScheme {

  //@formatter:off
  NONE("NONE"), 
  POINT_QUEUE_BASIC("POINT QUEUE - BLIEMER_ET_AL_2014"),
  POINT_QUEUE_ADVANCED("POINT QUEUE ADV. - RAADSEN_AND_BLIEMER_2021"),
  PHYSICAL_QUEUE_BASIC("PHYSICAL QUEUE - PRAADSEN_AND_BLIEMER_2021"),
  PHYSICAL_QUEUE_EXT_A("PHYSICAL QUEUE EXT. A - RAADSEN_AND_BLIEMER_2021"),
  PHYSICAL_QUEUE_EXT_B("PHYSICAL QUEUE EXT. B - RAADSEN_AND_BLIEMER_2021"),
  PHYSICAL_QUEUE_EXT_C("PHYSICAL QUEUE EXT. C - RAADSEN_AND_BLIEMER_2021");

  /** value of the type */
  private final String value;

  /**
   * Constructor
   * 
   * @param value of the type referring to the paper it is proposed and/or the extension type
   */
  StaticLtmLoadingScheme(final String value) {
    this.value = value;
  }
  
  /** The value of the type
   * 
   * @return value
   */
  public String getValue() {
    return value;
  }

  /** Verify if the chosen method is any of the point queue ones
   * 
   * @return true if a point queue approach, false otherwise
   */
  public boolean isPointQueue() {
    return this.equals(POINT_QUEUE_BASIC) || this.equals(POINT_QUEUE_ADVANCED);
  }

  /** Verify if the chosen method is any of the physical queue options
   * 
   * @return true if a physical queue approach, false otherwise
   */  
  public boolean isPhysicalQueue() {
    return !isPointQueue();
  }
}
