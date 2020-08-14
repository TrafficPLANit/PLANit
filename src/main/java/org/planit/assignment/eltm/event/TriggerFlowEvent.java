package org.planit.assignment.eltm.event;

import org.djutils.event.EventType;
import org.planit.assignment.eltm.LinkSegmentBoundary;
import org.planit.utils.cumulatives.CumulativePoint;

/**
 * A trigger flow event represents a flow rate that is being triggered exogenously, typically by a node on either side of the link segment. Hence, that the source of this event is
 * a link segment boundary
 * 
 * @author markr
 *
 */
public class TriggerFlowEvent extends TriggerEvent {

  /** generated UID */
  private static final long serialVersionUID = -913914782289041442L;

  /**
   * event type to identify ltm events representing a change in flow that is exogenously triggered
   */
  public static final EventType LTM_TRIGGER_FLOW_EVENT = new EventType("LTM.EVENT.TRIGGER.FLOW");

  /**
   * Constructor
   * 
   * @param linkSegmentBoundary the link segment boundary this flow emanates from
   * @param cumulativePoint     the cumulative reference point
   * @param flowRatePcuPerHour  the flow rate
   */
  protected TriggerFlowEvent(LinkSegmentBoundary linkSegmentBoundary, CumulativePoint cumulativePoint, double flowRatePcuPerHour) {
    super(LTM_TRIGGER_FLOW_EVENT, linkSegmentBoundary, cumulativePoint, flowRatePcuPerHour);
  }

  /**
   * Create a new trigger flow event emanating from the passing in link boundary at a given cumulative point and flow rate
   * 
   * @param linkBoundarySource link segment boundary
   * @param cumulativePoint    cumulative point to use for trigger
   * @param flowRatePcuPerHour flow rate for the event
   * @return created trigger flow event
   */
  public static TriggerFlowEvent createTriggerFlowEvent(LinkSegmentBoundary linkBoundarySource, CumulativePoint cumulativePoint, double flowRatePcuPerHour) {
    return new TriggerFlowEvent(linkBoundarySource, cumulativePoint, flowRatePcuPerHour);
  }

  // getters - setters

  /**
   * @return the pcuFowRatePerHour
   */
  public double getPcuFowRatePerHour() {
    return getEventContentByIndex(2);
  }

}
