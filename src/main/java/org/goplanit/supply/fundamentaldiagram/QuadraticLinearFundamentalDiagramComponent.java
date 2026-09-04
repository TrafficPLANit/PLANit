package org.goplanit.supply.fundamentaldiagram;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.macroscopic.MacroscopicConstants;
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

  /** flag to indicate of we force a concave free flow in case critical speed is defined or inferred to be
   * greater or equal than the free speed */
  private boolean forceConcaveFreeFlowBranch = DEFAULT_FORCE_CONCAVE_FREE_FLOW_BRANCH;

  /** factor to apply to the free speed to obtain critical speed in case {@link #forceConcaveFreeFlowBranch} is set to
   * and the critical speed is not explicitly set to a lower value than the free speed */
  private double criticalSpeedFactor = DEFAULT_CRITICAL_SPEED_FACTOR;

  /**
   * Logger to use
   */
  private static final Logger LOGGER =
          Logger.getLogger(QuadraticLinearFundamentalDiagramComponent.class.getCanonicalName());

  /**
   * {@inheritDoc}
   */
  @Override
  protected FundamentalDiagram createFundamentalDiagramByLinkSegmentType(
          MacroscopicLinkSegmentType lsType, Mode mode) {

    if(!lsType.isModeAllowed(mode)){
      LOGGER.warning("Unable to create fundamental diagram for mode (%s) on link type since the" +
          " mode is not allowed");
      return null;
    }
    // free speed:      use explicitly set or mode speed limit
    double modeFreeSpeedForType = lsType.getMaximumSpeedKmH(mode);

    // critical speed:  use explicitly set or minimum of mode speed limit and default critical speed
    var modeCriticalSpeed = lsType.getCriticalSpeedKmH(mode);
    if(isForceConcaveFreeFlowBranch() && Precision.greaterEqual(modeCriticalSpeed, modeFreeSpeedForType)){
      modeCriticalSpeed = modeCriticalSpeed * getCriticalSpeedFactor();
    }

    double maxDensity = lsType.getExplicitMaximumDensityPerLaneOrDefault();
    double capacity = lsType.isExplicitCapacityPerLaneSet() ? lsType.getExplicitCapacityPerLane() :
            FundamentalDiagramUtils.computeCapacityPcuHLaneFrom(
                    modeCriticalSpeed, MacroscopicConstants.DEFAULT_BACKWARD_WAVE_SPEED_KM_HOUR, maxDensity);

    // when free flow branch is in fact not quadratic but linear revert to triangular FD
    if(Precision.greaterEqual(modeCriticalSpeed, modeFreeSpeedForType, Precision.EPSILON_3)){
      return new NewellFundamentalDiagram(
              modeFreeSpeedForType,
              capacity,
              maxDensity);
    }

    return new QuadraticLinearFundamentalDiagram(
              modeFreeSpeedForType,
              modeCriticalSpeed,
              capacity,
              maxDensity);
  }

  /** default for forcing a concave free flow branch flag */
  public static final boolean DEFAULT_FORCE_CONCAVE_FREE_FLOW_BRANCH = true;

  /** default for critical speed factor setting */
  public static final double DEFAULT_CRITICAL_SPEED_FACTOR = 0.8;

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
  public QuadraticLinearFundamentalDiagramComponent(
      final QuadraticLinearFundamentalDiagramComponent other, boolean deepCopy) {
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

  // getters / setters

  /** flag to indicate of we force a concave free flow in case critical speed is defined or inferred to be
   * greater or equal than the free speed
   *
   * @return flag
   */
  public boolean isForceConcaveFreeFlowBranch() {
    return forceConcaveFreeFlowBranch;
  }

  public void setForceConcaveFreeFlowBranch(boolean forceConcaveFreeFlowBranch) {
    this.forceConcaveFreeFlowBranch = forceConcaveFreeFlowBranch;
  }

  /** factor to apply to the free speed to obtain critical speed in case {@link #forceConcaveFreeFlowBranch} is set to
   * and the critical speed is not explicitly set to a lower value than the free speed
   *
   * @return flag
   */
  public double getCriticalSpeedFactor() {
    return criticalSpeedFactor;
  }

  public void setCriticalSpeedFactor(double criticalSpeedFactor) {
    this.criticalSpeedFactor = criticalSpeedFactor;
  }
}
