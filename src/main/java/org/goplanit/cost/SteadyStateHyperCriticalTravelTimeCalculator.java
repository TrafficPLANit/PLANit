package org.goplanit.cost;

import org.goplanit.supply.fundamentaldiagram.FundamentalDiagram;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;

import java.util.logging.Logger;

/**
 * Convenience class that packs the functionality to compute hyper critical costs for
 * steady state travel times
 *
 * <p>HyperCritical delay equates to (timePeriod duration/2) * (inflowRateLane/outflowRateLane - 1)</p>
 * <p>HyperCritical delay derivative equates to (timePeriod duration/2) * (1/outflowRate)</p>
 */
public final class SteadyStateHyperCriticalTravelTimeCalculator {

  private static final Logger LOGGER = Logger.getLogger(SteadyStateHyperCriticalTravelTimeCalculator.class.getCanonicalName());

  /**
   * Compute hyper critical delay
   *
   * @param inflowRatePcuHour   to use
   * @param outflowRatePcuHour  to use
   * @param numberOfLanes       to use
   * @param timePeriodHours     to use
   * @return delay, zero if no delay
   */
  public static double computeHyperCriticalDelay(
      double inflowRatePcuHour, double outflowRatePcuHour, int numberOfLanes, double timePeriodHours){

    double hyperCriticalDelay = 0;

    double inflowPcuHLane = inflowRatePcuHour/numberOfLanes;
    double outflowPcuHLane = outflowRatePcuHour/numberOfLanes;
    if (Precision.positive(inflowRatePcuHour)) {
      /* average hyper critical delay */
      if (Precision.smaller(outflowPcuHLane, inflowPcuHLane)) {
        if (!Precision.positive(outflowRatePcuHour)) {
          LOGGER.warning(String.format("No outflow while positive inflow (%.2f) " +
                  "-> infinite travel time, this is unlikely", inflowRatePcuHour));
          return Double.POSITIVE_INFINITY;
        }

        // hyperCriticalDelay = (excess inflow rate * 1/2* duration)/outflow rate)
        hyperCriticalDelay = ((inflowPcuHLane - outflowPcuHLane) * 0.5 * timePeriodHours / outflowPcuHLane);
      }
    }
    return hyperCriticalDelay;
  }

  /**
   * Simple derivative of steady state travel time delay based on (timePeriod duration/2) * (1/outflowRate)
   * assuming both inflow and outflow rate are positive
   *
   * @param uncongested flag indicating if link is congested or not, if false, zero is returned
   * @param mode to use
   * @param linkSegment to use
   * @param inFlowRatePcuH inflow on link (across all lanes)
   * @param outflowRatePcuH outflow on link (across all lanes)
   * @param timePeriodHours time period in hours
   * @return computed derivative, if outflow is not positive derivative is set to zero or in case of a zero inflow as
   * well, set to infinite.
   */
  public static double computeDTravelTimeDFlow(
      boolean uncongested,
      double inFlowRatePcuH,
      double outflowRatePcuH,
      double timePeriodHours) {

    double hyperDerivative = 0.0;

    /* hyperCriticalDelay derivative */
    if(!uncongested) {
      if (outflowRatePcuH>0) {
        /* congested derivative (T/2)*(1/v) */
        hyperDerivative =  0.5 * timePeriodHours / outflowRatePcuH;
      } else {
        /* avoid division by zero, if no outflow rate but congested, it is undesirable to use this link
        (if it has any flow), we return infinity, or let congested portion have no impact if empty (or unknown)*/
        hyperDerivative = inFlowRatePcuH > 0.0 ? Double.POSITIVE_INFINITY : 0;
      }
    }

    return hyperDerivative;
  }

}
