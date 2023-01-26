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
    this.freeFlowBranch = deepCopy ? fundamentalDiagramImpl.freeFlowBranch.clone() : fundamentalDiagramImpl.freeFlowBranch;
    this.congestedBranch = deepCopy ? fundamentalDiagramImpl.congestedBranch.clone() : fundamentalDiagramImpl.congestedBranch;
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

  /**
   * {@inheritDoc}
   */
  public abstract FundamentalDiagram deepClone();
}
