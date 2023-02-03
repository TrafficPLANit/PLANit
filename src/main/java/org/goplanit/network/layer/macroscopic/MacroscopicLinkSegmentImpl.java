package org.goplanit.network.layer.macroscopic;

import java.util.Set;
import java.util.logging.Logger;

import org.goplanit.network.layer.physical.LinkSegmentBase;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLink;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;
import org.goplanit.utils.network.layer.physical.Link;

/**
 * Link segment for macroscopic transport networks.
 *
 * @author markr
 */
public class MacroscopicLinkSegmentImpl extends LinkSegmentBase<MacroscopicLink> implements MacroscopicLinkSegment {

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
   */
  protected MacroscopicLinkSegmentImpl(final IdGroupingToken groupId, final MacroscopicLink parentLink, final boolean directionAB) {
    super(groupId, parentLink, directionAB);
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected MacroscopicLinkSegmentImpl(MacroscopicLinkSegmentImpl other, boolean deepCopy) {
    super(other, deepCopy);
    setLinkSegmentType(other.getLinkSegmentType());
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
  public MacroscopicLinkSegmentImpl shallowClone() {
    return new MacroscopicLinkSegmentImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  public MacroscopicLinkSegmentImpl deepClone() {
    return new MacroscopicLinkSegmentImpl(this, true);
  }

}
