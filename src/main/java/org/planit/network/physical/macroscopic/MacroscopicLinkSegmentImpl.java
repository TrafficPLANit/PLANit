package org.planit.network.physical.macroscopic;

import java.util.logging.Logger;

import org.planit.exceptions.PlanItException;
import org.planit.network.physical.LinkSegmentImpl;
import org.planit.utils.network.physical.Link;
import org.planit.utils.network.physical.Mode;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegment;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegmentType;

/**
 * Link segment for macroscopic transport networks.
 *
 * @author markr
 */
public class MacroscopicLinkSegmentImpl extends LinkSegmentImpl implements MacroscopicLinkSegment {

  // Private

  /** generated UID */
  private static final long serialVersionUID = 4574164258794764853L;

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(MacroscopicLinkSegmentImpl.class.getCanonicalName());

  // Protected

  /**
   * the link type of this link containing all macroscopic features by user class
   */
  protected MacroscopicLinkSegmentType linkSegmentType = null;

  // Public

  /**
   * Constructor
   *
   * @param parentLink  the parent link of this link segment
   * @param directionAB direction of travel
   */
  public MacroscopicLinkSegmentImpl(final Link parentLink, final boolean directionAB) {
    super(parentLink, directionAB);
  }

  /**
   * Return the total capacity
   *
   * Compute the total capacity by multiplying the capacity per lane and number of lanes
   *
   * @return linkSegmentCapacity in PCU
   */
  @Override
  public double computeCapacity() {
    return getLinkSegmentType().getCapacityPerLane() * getNumberOfLanes();
  }

  /**
   * Compute the free flow travel time by mode, i.e. when the link's maximum speed might be capped by the mode's maximum
   * speed
   *
   * If the input data are invalid, this method logs the problem and returns a negative value.
   *
   * @param mode mode of travel
   * @return freeFlowTravelTime for this mode
   * @throws PlanItException when mode is not allowed on the link
   */
  @Override
  public double computeFreeFlowTravelTime(final Mode mode) throws PlanItException {
    PlanItException.throwIf(!isModeAllowedThroughLink(mode), "mode not allowed on link segment, no free flow time can be computed");

    final double linkLength = getParentLink().getLength();
    final double maximumSpeed = getMaximumSpeed(mode);
    final double segmentTypeMaximumSpeed = getLinkSegmentType().getModeProperties(mode).getMaxSpeed();
    double computedMaximumSpeed = Math.min(maximumSpeed, segmentTypeMaximumSpeed);
    return linkLength / computedMaximumSpeed;
  }

  /**
   * Returns whether vehicles of a specified mode are allowed through this link
   * 
   * @param mode the specified mode
   * @return true if vehicles of this mode can drive along this link, false otherwise
   */
  @Override
  public boolean isModeAllowedThroughLink(Mode mode) {
    return linkSegmentType.getModeProperties(mode) != null;
  }

  // getters - setters

  @Override
  public void setLinkSegmentType(final MacroscopicLinkSegmentType linkSegmentType) {
    this.linkSegmentType = linkSegmentType;
  }

  @Override
  public MacroscopicLinkSegmentType getLinkSegmentType() {
    return linkSegmentType;
  }

}
