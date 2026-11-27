package org.goplanit.pricing;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.goplanit.utils.containers.EntityIndex;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntitiesWithForeignKeyIndicesImpl;
import org.goplanit.utils.network.layer.physical.LinkSegment;
import org.goplanit.utils.pricing.PricingSchemeGate;
import org.goplanit.utils.pricing.PricingSchemeGateFactory;
import org.goplanit.utils.pricing.PricingSchemeGates;

/**
 * Container for the gates of a network pricing, of all its entry-exit schemes, with a lookup of the gates entered or
 * left along a link segment
 * <p>
 * When a link is broken or removed the pricing finds the gates using its link segments through this lookup, and a
 * scheme finds two of its own gates sharing an entry or an exit the same way.
 * </p>
 *
 * @author markr
 */
public class PricingSchemeGatesImpl extends ManagedIdEntitiesWithForeignKeyIndicesImpl<PricingSchemeGate>
    implements PricingSchemeGates {

  /** factory to create gates with */
  private final PricingSchemeGateFactory gateFactory;

  /** link segment to the gates entered or left along it, by identity */
  private final EntityIndex<LinkSegment, PricingSchemeGate> byLinkSegment = addForeignKeyIndex(gate ->
      Stream.concat(gate.getEntryLinkSegments().stream(), gate.getExitLinkSegments().stream())
          .collect(Collectors.toList()));

  /** link segment to the gates entered along it, by identity */
  private final EntityIndex<LinkSegment, PricingSchemeGate> byEntryLinkSegment =
      addForeignKeyIndex(PricingSchemeGate::getEntryLinkSegments);

  /** link segment to the gates left along it, by identity */
  private final EntityIndex<LinkSegment, PricingSchemeGate> byExitLinkSegment =
      addForeignKeyIndex(PricingSchemeGate::getExitLinkSegments);

  /**
   * Constructor
   *
   * @param groupId to use for creating ids for instances
   */
  public PricingSchemeGatesImpl(final IdGroupingToken groupId) {
    super(PricingSchemeGate::getId, PricingSchemeGate.SCHEME_GATE_ID_CLASS);
    this.gateFactory = new PricingSchemeGateFactoryImpl(groupId, this);
  }

  /**
   * Copy constructor
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   * @param mapper apply to each mapping from original to copy
   */
  public PricingSchemeGatesImpl(
      final PricingSchemeGatesImpl other,
      boolean deepCopy,
      BiConsumer<PricingSchemeGate, PricingSchemeGate> mapper) {
    super(other, deepCopy, mapper);
    this.gateFactory = new PricingSchemeGateFactoryImpl(other.gateFactory.getIdGroupingToken(), this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PricingSchemeGateFactory getFactory() {
    return gateFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<PricingSchemeGate> getByLinkSegment(LinkSegment linkSegment) {
    return byLinkSegment.get(linkSegment);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<PricingSchemeGate> getByEntryLinkSegment(LinkSegment linkSegment) {
    return byEntryLinkSegment.get(linkSegment);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<PricingSchemeGate> getByExitLinkSegment(LinkSegment linkSegment) {
    return byExitLinkSegment.get(linkSegment);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PricingSchemeGatesImpl shallowClone() {
    return new PricingSchemeGatesImpl(this, false, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PricingSchemeGatesImpl deepClone() {
    return new PricingSchemeGatesImpl(this, true, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PricingSchemeGatesImpl deepCloneWithMapping(BiConsumer<PricingSchemeGate, PricingSchemeGate> mapper) {
    return new PricingSchemeGatesImpl(this, true, mapper);
  }
}
