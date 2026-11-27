package org.goplanit.pricing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import org.goplanit.utils.containers.ContainerUtils;
import org.goplanit.utils.id.ExternalIdAbleImpl;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.pricing.PricingSchemeGate;

/**
 * A gate of an entry-exit scheme: where its priced link segments are entered, left, or both, grouped as the operator's
 * table prices them
 * <p>
 * For example an interchange whose two on-ramps are one entry and whose two off-ramps are one exit. Gates belong to
 * the pricing, not the network, as the data a network is built from holds the physical points but not how an operator
 * groups them. A gate is an entry, an exit or both, so a caller registers none with neither.
 * </p>
 *
 * @author markr
 */
public class PricingSchemeGateImpl extends ExternalIdAbleImpl implements PricingSchemeGate {

  /** generated UID */
  private static final long serialVersionUID = -7259033181240186317L;

  /** the link segments entered at the gate */
  private final List<MacroscopicLinkSegment> entryLinkSegments;

  /** the link segments left at the gate */
  private final List<MacroscopicLinkSegment> exitLinkSegments;

  /** the name, null when it has none */
  private String name;

  /**
   * Constructor
   *
   * @param groupId contiguous id generation within this group for instances of this class
   * @param entryLinkSegments entered at the gate, may be empty
   * @param exitLinkSegments left at the gate, may be empty
   */
  protected PricingSchemeGateImpl(
      final IdGroupingToken groupId,
      final Collection<? extends MacroscopicLinkSegment> entryLinkSegments,
      final Collection<? extends MacroscopicLinkSegment> exitLinkSegments) {
    super(IdGenerator.generateId(groupId, SCHEME_GATE_ID_CLASS));
    this.entryLinkSegments = new ArrayList<>(entryLinkSegments);
    this.exitLinkSegments = new ArrayList<>(exitLinkSegments);
  }

  /**
   * Copy constructor. Shallow and deep copies alike hold their own lists, referring to the same link segments
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected PricingSchemeGateImpl(final PricingSchemeGateImpl other, boolean deepCopy) {
    super(other);
    this.entryLinkSegments = new ArrayList<>(other.entryLinkSegments);
    this.exitLinkSegments = new ArrayList<>(other.exitLinkSegments);
    this.name = other.name;
  }

  /**
   * Remove the entries and the exits that meet a condition, such as those not where its scheme's priced link segments
   * are entered or left
   *
   * @param notAnEntry true for an entry link segment to remove
   * @param notAnExit true for an exit link segment to remove
   * @return the entry and exit link segments removed
   */
  public List<MacroscopicLinkSegment> removeEntriesAndExitsMatching(
      Predicate<? super MacroscopicLinkSegment> notAnEntry,
      Predicate<? super MacroscopicLinkSegment> notAnExit) {
    var removed = ContainerUtils.removeMatching(entryLinkSegments, notAnEntry);
    removed.addAll(ContainerUtils.removeMatching(exitLinkSegments, notAnExit));
    return removed;
  }

  /**
   * Replace a link segment among the entries by one, and among the exits by another, such as after a link is broken at
   * a node: the entry keeps the part starting where the original started, the exit the part ending where it ended
   *
   * @param original link segment to replace
   * @param asEntry its replacement among the entries
   * @param asExit its replacement among the exits
   */
  public void replaceLinkSegment(
      MacroscopicLinkSegment original,
      MacroscopicLinkSegment asEntry,
      MacroscopicLinkSegment asExit) {
    entryLinkSegments.replaceAll(linkSegment -> linkSegment == original ? asEntry : linkSegment);
    exitLinkSegments.replaceAll(linkSegment -> linkSegment == original ? asExit : linkSegment);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setName(String name) {
    this.name = name;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<MacroscopicLinkSegment> getEntryLinkSegments() {
    return Collections.unmodifiableList(entryLinkSegments);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<MacroscopicLinkSegment> getExitLinkSegments() {
    return Collections.unmodifiableList(exitLinkSegments);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long recreateManagedIds(IdGroupingToken tokenId) {
    setId(IdGenerator.generateId(tokenId, getIdClass()));
    return getId();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PricingSchemeGateImpl shallowClone() {
    return new PricingSchemeGateImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PricingSchemeGateImpl deepClone() {
    return new PricingSchemeGateImpl(this, true);
  }
}
