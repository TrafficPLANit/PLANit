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
}
