package org.planit.assignment.eltm.event;

import org.planit.utils.cumulatives.CumulativePoint;
import org.planit.utils.event.EventType;

/**
 * A release flow event represents a flow rate that is being released on a link boundary on the opposite border from where it was triggered through a trigger event.
 * 
 * @author markr
 */
public class ReleaseFlowEvent extends ReleaseEvent {

  /** event type to identify ltm events representing a change in flow that is being released */
  public static final EventType LTM_RELEASE_FLOW_EVENT = new EventType("LTM.EVENT.RELEASE.FLOW");

  /**
   * Constructor
   * 
   * @param sourceTriggerEvent the source trigger this release event originated from (its source)
   * @param cumulativePoint    the cumulative reference point
   * @param flowRatePcuPerHour the flow rate
   */
  protected ReleaseFlowEvent(TriggerFlowEvent sourceTriggerEvent, CumulativePoint cumulativePoint, double flowRatePcuPerHour) {
    super(LTM_RELEASE_FLOW_EVENT, sourceTriggerEvent, cumulativePoint, flowRatePcuPerHour);
  }

  /**
   * Create a new release flow event being release on the opposite boundary of the source trigger
   * 
   * @param sourceTriggerEvent the source trigger this release event originated from (its source)
   * @param cumulativePoint    the cumulative reference point
   * @param flowRatePcuPerHour the flow rate
   * @return created release flow event
   */
  public static ReleaseFlowEvent createReleaseFlowEvent(TriggerFlowEvent sourceTriggerEvent, CumulativePoint cumulativePoint, double flowRatePcuPerHour) {
    return new ReleaseFlowEvent(sourceTriggerEvent, cumulativePoint, flowRatePcuPerHour);
  }

  // getters - setters

  /**
   * @return the pcuFowRatePerHour
   */
  public double getPcuFowRatePerHour() {
    return getEventContentByIndex(2);
  }
}
