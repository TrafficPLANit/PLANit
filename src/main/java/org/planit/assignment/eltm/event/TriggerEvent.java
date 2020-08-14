package org.planit.assignment.eltm.event;

import java.io.Serializable;

import org.djutils.event.EventType;
import org.planit.assignment.eltm.LinkSegmentBoundary;
import org.planit.utils.cumulatives.CumulativePoint;

/**
 * A trigger event emanates from a link boundary (its source) with a particular cumulative value and time and potentially other information which should be constructed by
 * subclassing this class
 * 
 * @author markr
 *
 */
public abstract class TriggerEvent extends CumulativeEvent {

  /** generated UID */
  private static final long serialVersionUID = -5918858456054294507L;

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
    return (LinkSegmentBoundary) getSourceId();
  }
}
