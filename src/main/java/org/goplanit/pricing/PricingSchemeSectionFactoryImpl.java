package org.goplanit.pricing;

import java.util.Collection;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntityFactoryImpl;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;
import org.goplanit.utils.pricing.PricingSchemeSection;
import org.goplanit.utils.pricing.PricingSchemeSectionFactory;
import org.goplanit.utils.pricing.PricingSchemeSections;

/**
 * Factory for the sections of a network pricing, registering each on its container
 * <p>
 * Sections are registered before the schemes using them, as a pricing file lists them; whether a section's link
 * segments lie on its scheme's priced link segments is checked when the scheme is registered.
 * </p>
 *
 * @author markr
 */
public class PricingSchemeSectionFactoryImpl extends ManagedIdEntityFactoryImpl<PricingSchemeSection>
    implements PricingSchemeSectionFactory {

  /** container to register sections on */
  private final PricingSchemeSections sections;

  /**
   * Constructor
   *
   * @param groupId to use for creating ids for instances
   * @param sections to register on
   */
  protected PricingSchemeSectionFactoryImpl(final IdGroupingToken groupId, final PricingSchemeSections sections) {
    super(groupId);
    this.sections = sections;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PricingSchemeSection registerNew(
      Collection<MacroscopicLinkSegment> linkSegments,
      Collection<MacroscopicLinkSegmentType> linkSegmentTypes) {
    var section = new PricingSchemeSectionImpl(getIdGroupingToken(), linkSegments, linkSegmentTypes);
    sections.register(section);
    return section;
  }
}
