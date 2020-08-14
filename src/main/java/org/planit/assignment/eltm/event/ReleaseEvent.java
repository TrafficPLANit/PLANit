package org.planit.assignment.eltm.event;

import java.io.Serializable;

import org.djutils.event.EventType;
import org.planit.assignment.eltm.LinkSegmentBoundary.Location;
import org.planit.utils.cumulatives.CumulativePoint;

/**
 * A release event represents the arrival of a trigger event at the opposite link boundary from where it originated. Additional information with this event is to be provided by
 * sublassing it
 * 
 * @author markr
 *
 */
public class ReleaseEvent extends CumulativeEvent {

  /** generated UID */
  private static final long serialVersionUID = -5277263137883207592L;

  /**
   * Constructor
   * 
   * @param type            the release event type
   * @param triggerEvent,   the source of each release event is a trigger event
   * @param cumulativePoint the cumulative reference point
   * @param singleParameter additional parameter
   */
  protected ReleaseEvent(EventType type, TriggerEvent triggerEvent, CumulativePoint cumulativePoint, Serializable singleParameter) {
    super(type, triggerEvent, cumulativePoint, singleParameter);
  }

  /**
   * Collect the source trigger event that is the cause of this release event being scheduled
   * 
   * @return source trigger event
   */
  public TriggerEvent getSourceTriggerEvent() {
    return (TriggerEvent) getSourceId();
  }

  /**
   * collect the link boundary location of this release event, i.e., the opposite link boundary of its source trigger event
   * 
   * @return link boundary location
   */
  public Location getLinkBoundaryLocation() {
    return getSourceTriggerEvent().getLinkBoundary().getOppositeBoundaryLocation();
  }

}
