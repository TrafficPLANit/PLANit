package org.planit.supply.fundamentaldiagram;

import org.planit.utils.misc.HashUtils;

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
   */
  public FundamentalDiagramImpl(final FundamentalDiagramImpl fundamentalDiagramImpl) {
    this.freeFlowBranch = fundamentalDiagramImpl.freeFlowBranch.clone();
    this.congestedBranch = fundamentalDiagramImpl.congestedBranch.clone();
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
  public abstract FundamentalDiagram clone();
}
