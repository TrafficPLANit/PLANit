package org.planit.assignment.eltm;

import java.io.Serializable;

import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegment;

/**
 * LinkSegment boundary of a link segment.
 * 
 * @author markr
 */
public class LinkSegmentBoundary implements Serializable {

  /** generated UID */
  private static final long serialVersionUID = -309881548757350032L;

  /**
   * Location types for a link segment boundary
   * 
   * @author markr
   */
  public enum Location {
    UPSTREAM,
    DOWNSTREAM
  }

  /** the link segment of this boundary */
  final protected MacroscopicLinkSegment linkSegment;

  /** the location of this boundary on the link segment */
  final protected Location boundaryLocation;

  /**
   * Constructor
   * 
   * @param linkSegment of this boundary
   * @param boundaryLocation on the link segment
   */
  LinkSegmentBoundary(MacroscopicLinkSegment linkSegment, Location boundaryLocation) {
    this.linkSegment = linkSegment;
    this.boundaryLocation = boundaryLocation;
  }

  /**
   * Collect the boundary location of this instance
   * 
   * @return boundary location
   */
  public Location getLocation() {
    return boundaryLocation;
  }

  /**
   * collect the opposite location of this boundary. So when upstream we return downstream and vice
   * versa
   * 
   * @return opposite boundary location
   */
  public Location getOppositeBoundaryLocation() {
    return (getLocation() == Location.UPSTREAM) ? Location.DOWNSTREAM : Location.UPSTREAM;
  }
}
