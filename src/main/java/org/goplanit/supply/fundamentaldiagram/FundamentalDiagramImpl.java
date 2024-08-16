package org.goplanit.supply.fundamentaldiagram;

import org.goplanit.utils.misc.HashUtils;

/**
 * Base class for fundamental diagram instances
 * 
 * @author markr
 *
 */
public abstract class FundamentalDiagramImpl implements FundamentalDiagram {

  /**
   * the free flow branch to use
   */
  private final FundamentalDiagramBranch freeFlowBranch;

  /**
   * the free flow branch to use
   */
  private final FundamentalDiagramBranch congestedBranch;

  /**
   * Compute the backward wave speed that goes with a given capacity keeping all other variables the same
   *
   * @param capacityPcuHour to compute backward wave speed for ceteris paribus
   * @return proposed backward wave speed
   */
  protected double computeBackwardWaveSpeedForCapacity(double capacityPcuHour) {
    return FundamentalDiagram.computeBackwardWaveSpeedFor(
            capacityPcuHour,
            getFreeFlowBranch().getDensityPcuKm(capacityPcuHour),
            getCongestedBranch().getDensityPcuKm(0));
  }

  /**
   * Constructor
   * 
   * @param freeFlowBranch  to use
   * @param congestedBranch to use
   */
  public FundamentalDiagramImpl(final FundamentalDiagramBranch freeFlowBranch, final FundamentalDiagramBranch congestedBranch) {
    this.freeFlowBranch = freeFlowBranch;
    this.congestedBranch = congestedBranch;
  }

  /**
   * Copy Constructor
   * 
   * @param fundamentalDiagramImpl to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  public FundamentalDiagramImpl(final FundamentalDiagramImpl fundamentalDiagramImpl, boolean deepCopy) {
    super();
    /* deep copy makes an actual copy, otherwise we just reuse existing references */
    this.freeFlowBranch = deepCopy ? fundamentalDiagramImpl.freeFlowBranch.deepClone() : fundamentalDiagramImpl.freeFlowBranch;
    this.congestedBranch = deepCopy ? fundamentalDiagramImpl.congestedBranch.deepClone() : fundamentalDiagramImpl.congestedBranch;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public FundamentalDiagramBranch getFreeFlowBranch() {
    return freeFlowBranch;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public FundamentalDiagramBranch getCongestedBranch() {
    return congestedBranch;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int relaxedHashCode(int scale) {
    return HashUtils.createCombinedHashCode(freeFlowBranch.relaxedHashCode(scale), congestedBranch.relaxedHashCode(scale));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract FundamentalDiagram shallowClone();

  /**
   * {@inheritDoc}
   */
  public abstract FundamentalDiagram deepClone();
}
