package org.goplanit.supply.fundamentaldiagram;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;

import java.util.Map;
import java.util.logging.Logger;

/**
 * Quadratic-Linear fundamental diagram traffic component
 *
 * @author markr
 *
 */
public class QuadraticLinearFundamentalDiagramComponent extends FundamentalDiagramComponent {

  /** generated UID */
  private static final long serialVersionUID = -3166623064510413929L;

  /**
   * Logger to use
   */
  private static final Logger LOGGER = Logger.getLogger(QuadraticLinearFundamentalDiagramComponent.class.getCanonicalName());

  /**
   * {@inheritDoc}
   */
  @Override
  protected QuadraticLinearFundamentalDiagram createFundamentalDiagramByLinkSegmentType(
          MacroscopicLinkSegmentType lsType, Mode mode) {

    // free speed:      use explicitly set or mode speed limit
    double modeFreeSpeedForType = lsType.getMaximumSpeedKmH(mode);
    // critical speed:  use explicitly set or minimum of mode speed limit and default critical speed
    var modeCriticalSpeed = lsType.getCriticalSpeedKmH(mode);

    QuadraticLinearFundamentalDiagram qlFd;
    if (!lsType.isExplicitMaximumDensityPerLaneSet() && !lsType.isExplicitCapacityPerLaneSet()) {
      /* use free speed/critical speed to create FD with inferred capacity */
      qlFd = new QuadraticLinearFundamentalDiagram(modeFreeSpeedForType, modeCriticalSpeed);
    } else if (!lsType.isExplicitMaximumDensityPerLaneSet()) {
      /* capacity is explicitly overwritten, so use that as well, use default for jam density */
      qlFd = new QuadraticLinearFundamentalDiagram(
              modeFreeSpeedForType,
              modeCriticalSpeed,
              lsType.getExplicitCapacityPerLane(),
              lsType.getExplicitMaximumDensityPerLaneOrDefault());
    } else {
      /* only jam density set, use that */
      qlFd = new QuadraticLinearFundamentalDiagram(
              modeFreeSpeedForType, modeCriticalSpeed, lsType.getExplicitMaximumDensityPerLane());
    }
    return qlFd;
  }

  /**
   * Constructor
   *
   * @param groupId contiguous id generation within this group for instances of this class
   */
  public QuadraticLinearFundamentalDiagramComponent(final IdGroupingToken groupId) {
    super(groupId);
  }

  /**
   * Copy constructor
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  public QuadraticLinearFundamentalDiagramComponent(final QuadraticLinearFundamentalDiagramComponent other, boolean deepCopy) {
    super(other, deepCopy);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public QuadraticLinearFundamentalDiagramComponent shallowClone() {
    return new QuadraticLinearFundamentalDiagramComponent(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public QuadraticLinearFundamentalDiagramComponent deepClone() {
    return new QuadraticLinearFundamentalDiagramComponent(this, true);
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
