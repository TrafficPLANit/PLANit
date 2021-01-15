package org.planit.network.macroscopic.physical;

import java.util.Set;
import java.util.logging.Logger;

import org.planit.network.physical.LinkSegmentImpl;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.mode.Mode;
import org.planit.utils.network.physical.Link;
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
  protected MacroscopicLinkSegmentImpl(final IdGroupingToken groupId, final Link parentLink, final boolean directionAB) throws PlanItException {
    super(groupId, parentLink, directionAB);
  }

  /**
   * Copy constructor
   * 
   * @param macroscopicLinkSegmentImpl to copy
   */
  protected MacroscopicLinkSegmentImpl(MacroscopicLinkSegmentImpl macroscopicLinkSegmentImpl) {
    super(macroscopicLinkSegmentImpl);
    setLinkSegmentType(macroscopicLinkSegmentImpl.getLinkSegmentType());
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
   * {@inheritDoc}
   */
  @Override
  public double computeFreeFlowTravelTime(final Mode mode) {
    if (!isModeAllowed(mode)) {
      return Double.POSITIVE_INFINITY;
    }

    return getParentLink().getLengthKm() / getModelledSpeedLimitKmH(mode);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getModelledSpeedLimitKmH(Mode mode) {
    if (!isModeAllowed(mode)) {
      return 0.0;
    }
    final double modeSpeedLimit = mode.getMaximumSpeedKmH();
    final double segmentTypeMaximumSpeed = getLinkSegmentType().getModeProperties(mode).getMaximumSpeedKmH();
    return Math.min(getPhysicalSpeedLimitKmH(), Math.min(modeSpeedLimit, segmentTypeMaximumSpeed));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isModeAllowed(Mode mode) {
    return linkSegmentType.isModeAvailable(mode);
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

  /**
   * {@inheritDoc}
   */
  public MacroscopicLinkSegmentImpl clone() {
    return new MacroscopicLinkSegmentImpl(this);
  }

}
