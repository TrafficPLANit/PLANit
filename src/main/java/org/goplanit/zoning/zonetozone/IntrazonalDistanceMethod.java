package org.goplanit.zoning.zonetozone;

/**
 * How the value of a trip within one zone is inferred in a skim, see {@link OdSkimBuilderSettings}
 * <p>
 * A skim between zones says nothing about a trip that starts and ends in the same zone, yet such trips are common in
 * a fine zoning and need a length and a time all the same.
 * </p>
 *
 * @author markr
 */
public enum IntrazonalDistanceMethod {

  /** a factor times the mean skim value to the zone's nearest other zones, following the network's own distances or
   * times, and needing no polygon */
  NEAREST_NEIGHBOURS,

  /** 0.52 times the square root of the zone's area, the mean distance between two points drawn at random in a square
   * of that area, taken at the mode's straight line speed in a time skim */
  AREA
}
