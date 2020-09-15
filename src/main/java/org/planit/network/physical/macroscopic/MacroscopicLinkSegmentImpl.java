package org.planit.network.physical.macroscopic;

import java.util.Set;
import java.util.logging.Logger;

import org.planit.network.physical.LinkSegmentImpl;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;
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
  @SuppressWarnings("unused")
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
   * @param groupId     contiguous id generation within this group for instances of this class
   * @param parentLink  the parent link of this link segment
   * @param directionAB direction of travel
   * @throws PlanItException thrown when error
   */
  public MacroscopicLinkSegmentImpl(final IdGroupingToken groupId, final Link parentLink, final boolean directionAB) throws PlanItException {
    super(groupId, parentLink, directionAB);
  }

  /**
   * Return the total capacity
   *
   * Compute the total capacity by multiplying the capacity per lane and number of lanes
   *
   * @return linkSegmentCapacity in PCU/h
   */
  @Override
  public double computeCapacityPcuH() {
    return getLinkSegmentType().getCapacityPerLane() * getNumberOfLanes();
  }

  /**
   * Compute the free flow travel time by mode, i.e. when the link's maximum speed might be capped by the mode's maximum speed
   *
   * If the input data are invalid, this method logs the problem and returns a negative value. If the mode is not allowed on the link segment and infinite free flow travel time is
   * returned.
   *
   * @param mode mode of travel
   * @return freeFlowTravelTime for this mode (when feasible)
   * @throws PlanItException when mode is not allowed on the link
   */
  @Override
  public double computeFreeFlowTravelTime(final Mode mode) {
    if (!isModeAllowed(mode)) {
      return Double.POSITIVE_INFINITY;
    }

    final double segmentTypeMaximumSpeed = getLinkSegmentType().getModeProperties(mode).getMaxSpeed();
    return getParentLink().getLengthKm() / Math.min(getMaximumSpeedKmH(), segmentTypeMaximumSpeed);
  }

  /**
   * Returns whether vehicles of a specified mode are allowed through this link
   * 
   * @param mode the specified mode
   * @return true if vehicles of this mode can drive along this link, false otherwise
   */
  @Override
  public boolean isModeAllowed(Mode mode) {
    return linkSegmentType.getModeProperties(mode) != null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Mode> getAllowedModes() {
    return linkSegmentType.getAvailableModes();
  }

  // getters - setters

  /**
   * {@inheritDoc}
   */
  @Override
  public void setLinkSegmentType(final MacroscopicLinkSegmentType linkSegmentType) {
    this.linkSegmentType = linkSegmentType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicLinkSegmentType getLinkSegmentType() {
    return linkSegmentType;
  }

}
