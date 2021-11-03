package org.goplanit.network.layer.macroscopic;

import java.util.Set;
import java.util.logging.Logger;

import org.goplanit.network.layer.physical.LinkSegmentImpl;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;
import org.goplanit.utils.network.layer.physical.Link;

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
   * {@inheritDoc}
   */
  @Override
  public double computeFreeFlowTravelTimeHour(final Mode mode) {
    if (!isModeAllowed(mode)) {
      return Double.MAX_VALUE;
    }

    return getParentLink().getLengthKm() / getModelledSpeedLimitKmH(mode);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isModeAllowed(Mode mode) {
    return linkSegmentType.isModeAllowed(mode);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Mode> getAllowedModes() {
    return linkSegmentType.getAllowedModes();
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
