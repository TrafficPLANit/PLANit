package org.goplanit.supply.fundamentaldiagram;

import java.util.Map;
import java.util.logging.Logger;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;

/**
 * Newell fundamental diagram traffic component
 *
 * @author markr
 *
 */
public class NewellFundamentalDiagramComponent extends FundamentalDiagramComponent {

  /** generated UID */
  private static final long serialVersionUID = -3166623064510413929L;

  /**
   * Logger to use
   */
  private static final Logger LOGGER = Logger.getLogger(NewellFundamentalDiagramComponent.class.getCanonicalName());

  /**
   * Factory method based on link segment type
   *
   * @param lsType to use
   * @param mode mode to use
   * @return created FD
   */
  @Override
  protected NewellFundamentalDiagram createFundamentalDiagramByLinkSegmentType(
          MacroscopicLinkSegmentType lsType, Mode mode) {

    NewellFundamentalDiagram newellFd;
    double modeSpeed = lsType.getMaximumSpeedKmH(mode);
    if (!lsType.isExplicitMaximumDensityPerLaneSet() && !lsType.isExplicitCapacityPerLaneSet()) {
      /* use free speed to create Newell FD with inferred capacity */
      newellFd = new NewellFundamentalDiagram(modeSpeed);
    } else if (!lsType.isExplicitMaximumDensityPerLaneSet()) {
      /* capacity is explicitly overwritten, so use that as well, use default for jam density,
      * we do not use default capacity since it is expected that inferring capacity from free speed and jam density is
      * more accurate */
      newellFd = new NewellFundamentalDiagram(
              modeSpeed, lsType.getExplicitCapacityPerLane(), lsType.getExplicitMaximumDensityPerLaneOrDefault());
    } else if(!lsType.isExplicitCapacityPerLaneSet()){
      /* only jam density set, use that */
      newellFd = new NewellFundamentalDiagram(modeSpeed, lsType.getExplicitMaximumDensityPerLane());
    }else{
      newellFd = new NewellFundamentalDiagram(
              modeSpeed, lsType.getExplicitCapacityPerLane(), lsType.getExplicitMaximumDensityPerLane());
    }
    return newellFd;
  }

  /**
   * Constructor
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   */
  public NewellFundamentalDiagramComponent(final IdGroupingToken groupId) {
    super(groupId);
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  public NewellFundamentalDiagramComponent(final NewellFundamentalDiagramComponent other, boolean deepCopy) {
    super(other, deepCopy);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public NewellFundamentalDiagramComponent shallowClone() {
    return new NewellFundamentalDiagramComponent(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public NewellFundamentalDiagramComponent deepClone() {
    return new NewellFundamentalDiagramComponent(this, true);
  }

  /**
   * Register the given Newell fundamental diagram for the link segment. This overrules the fundamental diagram that
   * would be used based on the link segment's type. In case there already exists an identical fundamental diagram
   * (based on relaxed hashcode comparison), the link segment is assigned the already present fundamental diagram.
   * The fundamental diagram used for the link segment is returned, which is either the passed in one, or an
   * already present functionally identical version
   * 
   * @param linkSegment        to use
   * @param fundamentalDiagram to register
   * @return used Fd for the link segment, can be different (but functionally equivalent) if registered Fd was
   *  already present for another link segment
   */
  public FundamentalDiagram register(
          final MacroscopicLinkSegment linkSegment, final NewellFundamentalDiagram fundamentalDiagram) {
    return super.register(linkSegment, fundamentalDiagram);
  }

  /**
   * Register the given Newell fundamental diagram for the link segment type. In case there already exists an
   * identical fundamental diagram (based on relaxed hashcode comparison), the link segment type is assigned
   * the already present fundamental diagram. The fundamental diagram used for the link segment type is returned,
   * which is either the passed in one, or an already present functionally identical version
   * 
   * @param linkSegmentType    to use
   * @param fundamentalDiagram to register
   * @return used Fd for the link segment type, can be different (but functionally equivalent) if registered
   *  Fd was already present for another link segment
   */
  public FundamentalDiagram register(
          final MacroscopicLinkSegmentType linkSegmentType, final NewellFundamentalDiagram fundamentalDiagram) {
    return super.register(linkSegmentType, fundamentalDiagram);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<String, String> collectSettingsAsKeyValueMap() {
    return null;
  }

}
