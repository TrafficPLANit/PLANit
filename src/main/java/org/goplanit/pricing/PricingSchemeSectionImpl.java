package org.goplanit.pricing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import org.goplanit.utils.containers.ContainerUtils;
import org.goplanit.utils.containers.ListUtils;
import org.goplanit.utils.id.ExternalIdAbleImpl;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;
import org.goplanit.utils.pricing.PricingSchemeSection;

/**
 * A section of a scheme: link segments, link segment types, or both, that the operator prices apart
 * <p>
 * For example a bridge charging a start each time it is crossed, or each gantry of a road charged per gantry. Sections
 * belong to the pricing, not the network, as they are the operator's way of pricing. The link segments and types are
 * kept as given, so a writer writes back what was read; the pricing keeps them in step when the network's links are
 * broken or removed.
 * </p>
 *
 * @author markr
 */
public class PricingSchemeSectionImpl extends ExternalIdAbleImpl implements PricingSchemeSection {

  /** generated UID */
  private static final long serialVersionUID = 3381609547201338461L;

  /** the link segments, held in a list since entities hash by id and recreating ids changes ids in place */
  private final List<MacroscopicLinkSegment> linkSegments;

  /** the link segment types */
  private final List<MacroscopicLinkSegmentType> linkSegmentTypes;

  /** the name, null when it has none */
  private String name;

  /**
   * Constructor
   *
   * @param groupId contiguous id generation within this group for instances of this class
   * @param linkSegments of the section, may be empty when it is given by type
   * @param linkSegmentTypes of the section, may be empty when it is given by link segment
   */
  protected PricingSchemeSectionImpl(
      final IdGroupingToken groupId,
      final Collection<? extends MacroscopicLinkSegment> linkSegments,
      final Collection<? extends MacroscopicLinkSegmentType> linkSegmentTypes) {
    super(IdGenerator.generateId(groupId, SCHEME_SECTION_ID_CLASS));
    this.linkSegments = new ArrayList<>(linkSegments);
    this.linkSegmentTypes = new ArrayList<>(linkSegmentTypes);
  }

  /**
   * Copy constructor. Shallow and deep copies alike hold their own lists, referring to the same link segments and types
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected PricingSchemeSectionImpl(final PricingSchemeSectionImpl other, boolean deepCopy) {
    super(other);
    this.linkSegments = new ArrayList<>(other.linkSegments);
    this.linkSegmentTypes = new ArrayList<>(other.linkSegmentTypes);
    this.name = other.name;
  }

  /**
   * Remove the link segments that meet a condition, such as those its scheme does not charge on
   *
   * @param condition to meet
   * @return the link segments removed
   */
  public List<MacroscopicLinkSegment> removeLinkSegmentsMatching(Predicate<? super MacroscopicLinkSegment> condition) {
    return ContainerUtils.removeMatching(linkSegments, condition);
  }

  /**
   * Remove a link segment type the network removes
   *
   * @param linkSegmentType removed from the network
   * @return true when the section held it
   */
  public boolean removeLinkSegmentType(MacroscopicLinkSegmentType linkSegmentType) {
    return linkSegmentTypes.removeIf(held -> held == linkSegmentType);
  }

  /**
   * Add a link segment right after one the section already holds, such as the new link segment of a link broken at a
   * node, so the section keeps both parts
   *
   * @param inSection link segment the section holds
   * @param linkSegment to add, nothing happens when the section holds it already
   */
  public void addLinkSegmentAfter(MacroscopicLinkSegment inSection, MacroscopicLinkSegment linkSegment) {
    ListUtils.addAfter(linkSegments, inSection, linkSegment);
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
  public List<MacroscopicLinkSegment> getLinkSegments() {
    return Collections.unmodifiableList(linkSegments);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<MacroscopicLinkSegmentType> getLinkSegmentTypes() {
    return Collections.unmodifiableList(linkSegmentTypes);
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
  public PricingSchemeSectionImpl shallowClone() {
    return new PricingSchemeSectionImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PricingSchemeSectionImpl deepClone() {
    return new PricingSchemeSectionImpl(this, true);
  }
}
