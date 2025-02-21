package org.goplanit.converter.network;

import org.goplanit.utils.macroscopic.MacroscopicConstants;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.macroscopic.AccessGroupProperties;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;

import java.util.Objects;
import java.util.TreeMap;

/**
 * Uitlities that may be helpful for network converter readers
 *
 * @author markr
 */
public class NetworkConverterReaderUtils {

  public static final double DEFAULT_LANE_CAPACITY_PCUH = MacroscopicConstants.CAPACITY_2000_PCU_HOUR_LANE;

  /** Create an estimate for the number of lanes given a certain capacity using
   * {@link #DEFAULT_LANE_CAPACITY_PCUH} and rounding upward
   *
   * @param linkCapacityPcuH to use
   * @return number of lanes estimate
   */
  public static int computeNumLaneEstimate(double linkCapacityPcuH) {
    if(linkCapacityPcuH > DEFAULT_LANE_CAPACITY_PCUH) {
      return (int) Math.ceil(linkCapacityPcuH/ DEFAULT_LANE_CAPACITY_PCUH);
    }
    return 1;
  }

  /**
   * When wanting to check if a link segment type is already present without regenerating an instance
   * hash contents. Then when a new instance is created, use this function to feed to the
   * source id tracker map wrapper to access entries base don the same approach.
   *
   * @param lsType to use
   * @return hash
   */
  public static long computeLinkSegmentTypeHashExcludingIds(MacroscopicLinkSegmentType lsType){
    // set should be ordered to enforce exact same result each time
    return computeLinkSegmentTypeHashExcludingIds(
            lsType.getExplicitCapacityPerLaneOrDefault(),
            lsType.getExplicitMaximumDensityPerLaneOrDefault(),
            lsType.getAccessProperties());
  }

  /**
   * When wanting to check if a link segment type is already present without generating an instance (yet)
   * hash contents. use this function to generate a has for comparison.
   *
   * @param capacityPerLane to use
   * @param densityPerLane to use
   * @param accessProperties to use
   * @return hash
   */
  public static long computeLinkSegmentTypeHashExcludingIds(
          double capacityPerLane, double densityPerLane, TreeMap<Mode, AccessGroupProperties> accessProperties){
    // set should be ordered to enforce exact same result each time
    return Objects.hash(
            capacityPerLane,
            densityPerLane,
            accessProperties);
  }
}
