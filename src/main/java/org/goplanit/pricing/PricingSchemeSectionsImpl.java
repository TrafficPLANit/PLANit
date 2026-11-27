package org.goplanit.pricing;

import java.util.List;
import java.util.function.BiConsumer;

import org.goplanit.utils.containers.EntityIndex;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntitiesWithForeignKeyIndicesImpl;
import org.goplanit.utils.network.layer.physical.LinkSegment;
import org.goplanit.utils.pricing.PricingSchemeSection;
import org.goplanit.utils.pricing.PricingSchemeSectionFactory;
import org.goplanit.utils.pricing.PricingSchemeSections;

/**
 * Container for the sections of a network pricing, of all its schemes, with a lookup of the sections listing a link
 * segment
 * <p>
 * Sections of different schemes may list the same link segment, such as a bridge priced by a toll road and by a truck
 * charge. When a link is broken or removed the pricing finds the sections listing its link segments through this
 * lookup, and a scheme finds two of its own sections sharing a link segment the same way.
 * </p>
 *
 * @author markr
 */
public class PricingSchemeSectionsImpl extends ManagedIdEntitiesWithForeignKeyIndicesImpl<PricingSchemeSection>
    implements PricingSchemeSections {

  /** factory to create sections with */
  private final PricingSchemeSectionFactory sectionFactory;

  /** link segment to the sections listing it, by identity */
  private final EntityIndex<LinkSegment, PricingSchemeSection> byLinkSegment =
      addForeignKeyIndex(PricingSchemeSection::getLinkSegments);

  /**
   * Constructor
   *
   * @param groupId to use for creating ids for instances
   */
  public PricingSchemeSectionsImpl(final IdGroupingToken groupId) {
    super(PricingSchemeSection::getId, PricingSchemeSection.SCHEME_SECTION_ID_CLASS);
    this.sectionFactory = new PricingSchemeSectionFactoryImpl(groupId, this);
  }

  /**
   * Copy constructor
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   * @param mapper apply to each mapping from original to copy
   */
  public PricingSchemeSectionsImpl(
      final PricingSchemeSectionsImpl other,
      boolean deepCopy,
      BiConsumer<PricingSchemeSection, PricingSchemeSection> mapper) {
    super(other, deepCopy, mapper);
    this.sectionFactory = new PricingSchemeSectionFactoryImpl(other.sectionFactory.getIdGroupingToken(), this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PricingSchemeSectionFactory getFactory() {
    return sectionFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<PricingSchemeSection> getByLinkSegment(LinkSegment linkSegment) {
    return byLinkSegment.get(linkSegment);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PricingSchemeSectionsImpl shallowClone() {
    return new PricingSchemeSectionsImpl(this, false, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PricingSchemeSectionsImpl deepClone() {
    return new PricingSchemeSectionsImpl(this, true, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PricingSchemeSectionsImpl deepCloneWithMapping(BiConsumer<PricingSchemeSection, PricingSchemeSection> mapper) {
    return new PricingSchemeSectionsImpl(this, true, mapper);
  }
}
