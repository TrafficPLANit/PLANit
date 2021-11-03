package org.goplanit.assignment.ltm.eltm.event;

import java.io.Serializable;

import org.goplanit.assignment.ltm.eltm.LinkSegmentBoundary;
import org.goplanit.utils.cumulatives.CumulativePoint;
import org.goplanit.utils.event.EventType;

/**
 * A trigger event emanates from a link boundary (its source) with a particular cumulative value and time and potentially other information which should be constructed by
 * subclassing this class
 * 
 * @author markr
 *
 */
public abstract class TriggerEvent extends CumulativeEvent {

  /**
   * Constructor
   * 
   * @param type                the trigger event type
   * @param linkBoundarySource, the source of each trigger is a link boundary
   * @param cumulativePoint     the cumulative reference point
   * @param singleParameter     additional parameter
   */
  protected TriggerEvent(EventType type, LinkSegmentBoundary linkBoundarySource, CumulativePoint cumulativePoint, Serializable singleParameter) {
    super(type, linkBoundarySource, cumulativePoint, singleParameter);
  }

  /**
   * @return the link boundary this event emanates from, i.e., the source
   */
  public LinkSegmentBoundary getLinkBoundary() {
    return (LinkSegmentBoundary) getSource();
  }
}
