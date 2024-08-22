package org.goplanit.supply.fundamentaldiagram;

import org.goplanit.utils.macroscopic.MacroscopicConstants;

/**
 * Utilities for fundamental diagrams
 */
public class FundamentalDiagramUtils {

  /**
   * Infer capacity point by finding intersection point between critical vehicle speed (at capacity) coming from origin
   * and the backward wave speed coming from jam density. this gives critical density point which gives capacity when
   * multiplied with critical vehicle speed.
   *
   * @param criticalVehicleSpeedKmH to use
   * @param backwardWaveSpeedKmh to use
   * @param jamDensityPcuKmLane to use
   * @return capacityPcuH (single lane)
   */
  public static double computeCapacityPcuHLaneFrom(double criticalVehicleSpeedKmH, double backwardWaveSpeedKmh, double jamDensityPcuKmLane){
    double kCrit =
            -((jamDensityPcuKmLane*backwardWaveSpeedKmh)
                    /
                    (criticalVehicleSpeedKmH - backwardWaveSpeedKmh));
    return kCrit * criticalVehicleSpeedKmH;
  }

  /**
   * Compute the backward wave speed that goes with a given critical and jam density
   * <p>
   *   backward wave speed = capacity/(criticalDensity-jamDensity)
   * </p>
   *
   * @param capacityPcuHour the capacity
   * @param criticalDensity to use
   * @param jamDensity to use
   * @return proposed backward wave speed
   */
  public static double computeBackwardWaveSpeedKmHFor(double capacityPcuHour, double criticalDensity, double jamDensity) {
    return capacityPcuHour/(criticalDensity - jamDensity);
  }
}
