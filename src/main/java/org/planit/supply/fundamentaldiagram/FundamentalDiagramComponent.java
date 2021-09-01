package org.planit.supply.fundamentaldiagram;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.planit.component.PlanitComponent;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.layer.macroscopic.MacroscopicLinkSegment;

/**
 * Fundamental diagram traffic component. Here we track the relation between a macroscopic link segment and its fundamental diagram. To minimise memory usage only unique
 * fundamental diagrams are retained based on their relaxed hash codes. IF a duplicate it registered it is simply discarded and the related link segment is attached to the already
 * present fundamental diagram in the pool
 *
 * @author markr
 *
 */
public abstract class FundamentalDiagramComponent extends PlanitComponent<FundamentalDiagramComponent> implements Serializable {

  /** generated UID */
  private static final long serialVersionUID = 5815100111048623093L;

  /** all unique fundamental diagrams found so far based on their relaxed hash code */
  private final Map<Integer, FundamentalDiagram> uniqueFundamentalDiagrams;

  // TODO: use same hierarchy as BPR -> default -> default per type -> default per link segment
  // make register method protected such that derived classes must implement their own specialised register method
  // with appropriate parameters which then delegates to the protected method, made a start below

  /** track fundamental diagram registered per link segment */
  private final Map<MacroscopicLinkSegment, FundamentalDiagram> linkSegmentFundamentalDiagrams;

  /**
   * precision scale to apply for the relaxed hash code to compare fundamental diagrams on being identical regarding their floating point variables, where 6 equates to 6 decimals
   * of precision
   */
  public static final int RELAXED_HASH_CODE_SCALE = 6;

  /**
   * Base constructor
   * 
   * @param groupId token
   */
  public FundamentalDiagramComponent(final IdGroupingToken groupId) {
    super(groupId, FundamentalDiagramComponent.class);
    this.uniqueFundamentalDiagrams = new HashMap<Integer, FundamentalDiagram>();
    this.linkSegmentFundamentalDiagrams = new HashMap<MacroscopicLinkSegment, FundamentalDiagram>();
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   */
  public FundamentalDiagramComponent(final FundamentalDiagramComponent other) {
    super(other);
    this.uniqueFundamentalDiagrams = new HashMap<Integer, FundamentalDiagram>(other.uniqueFundamentalDiagrams);
    this.linkSegmentFundamentalDiagrams = new HashMap<MacroscopicLinkSegment, FundamentalDiagram>(other.linkSegmentFundamentalDiagrams);
  }

  /**
   * Register the givejn fundamental diagram for the link segment. In case there already exists an identical fundamental diagram (based on relaxed hashcode comparison), the link
   * segment is assigned the already present fundamental diagram. The fundamental diagram used for the link segment is returned, which is either the passed in one, or an already
   * present functionally identical version
   * 
   * @param linkSegment        to use
   * @param fundamentalDiagram to register
   * @return used Fd for the link segment, can be different (but functionally equivalent) if registered Fd was already present for another link segment
   */
  public FundamentalDiagram register(final MacroscopicLinkSegment linkSegment, final FundamentalDiagram fundamentalDiagram) {
    int relaxedHash = fundamentalDiagram.relaxedHashCode(RELAXED_HASH_CODE_SCALE);
    FundamentalDiagram usedFd = fundamentalDiagram;
    if (uniqueFundamentalDiagrams.containsKey(relaxedHash)) {
      usedFd = uniqueFundamentalDiagrams.get(relaxedHash);
    }
    linkSegmentFundamentalDiagrams.put(linkSegment, usedFd);
    return usedFd;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract FundamentalDiagramComponent clone();

}
