package org.goplanit.assignment.ltm.eltm.event;

import java.io.Serializable;

import org.goplanit.assignment.ltm.eltm.LinkSegmentBoundary.Location;
import org.goplanit.utils.cumulatives.CumulativePoint;
import org.goplanit.utils.event.EventType;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.id.IdAble;

/**
 * A release event represents the arrival of a trigger event at the opposite link boundary from where it originated. Additional information with this event is to be provided by
 * subclassing it
 * 
 * @author markr
 *
 */
public class ReleaseEvent extends CumulativeEvent {

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
    return (TriggerEvent) getSource();
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
