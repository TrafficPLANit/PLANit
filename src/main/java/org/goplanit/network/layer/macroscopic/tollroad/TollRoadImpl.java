package org.goplanit.network.layer.macroscopic.tollroad;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.goplanit.utils.containers.ListUtils;
import org.goplanit.utils.id.ExternalIdAbleImpl;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.tollroad.PricedModeSelection;
import org.goplanit.utils.network.layer.macroscopic.tollroad.TollRoad;
import org.goplanit.utils.network.layer.physical.LinkSegment;

/**
 * A toll road of a macroscopic network layer: its link segments, the modes that pay on them as given, and a name
 * <p>
 * Its link segments change only through the layer modifier, which keeps them in step with the graph: a link broken at
 * a node leaves both new link segments on the toll road, and a removed link segment leaves it. The modes that pay are
 * kept as given, so a writer writes back what was read, and are resolved against the layer's modes when asked.
 * </p>
 *
 * @author markr
 */
public class TollRoadImpl extends ExternalIdAbleImpl implements TollRoad {

  /** generated UID */
  private static final long serialVersionUID = -2361780352087411915L;

  /** the link segments, held in a list since entities hash by id and recreating ids changes ids in place */
  private final List<MacroscopicLinkSegment> linkSegments;

  /** the modes that pay, as given */
  private final PricedModeSelection pricedModeSelection;

  /** the name, null when it has none */
  private String name;

  /**
   * Constructor
   *
   * @param groupId contiguous id generation within this group for instances of this class
   * @param linkSegments of the toll road
   * @param pricedModeSelection the modes that pay on it, as given
   */
  protected TollRoadImpl(
      final IdGroupingToken groupId,
      final Collection<? extends MacroscopicLinkSegment> linkSegments,
      final PricedModeSelection pricedModeSelection) {
    super(IdGenerator.generateId(groupId, TOLL_ROAD_ID_CLASS));
    this.linkSegments = new ArrayList<>(linkSegments);
    this.pricedModeSelection = pricedModeSelection;
  }

  /**
   * Copy constructor. Shallow and deep copies alike hold their own list of link segments, referring to the same link
   * segments; pointing those to copied link segments is up to the copy of the layer. The priced modes, which cannot be
   * changed, are shared
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected TollRoadImpl(final TollRoadImpl other, boolean deepCopy) {
    super(other);
    this.linkSegments = new ArrayList<>(other.linkSegments);
    this.pricedModeSelection = other.pricedModeSelection;
    this.name = other.name;
  }

  /**
   * Update the link segments based on the mapping provided, e.g. to their copies after a deep copy of the layer
   *
   * @param linkSegmentMapping should contain the link segment as currently used and then the value is the new link
   *                           segment to replace it
   * @param removeMissingMappings when true a link segment without a mapping is removed, otherwise it is left in-tact
   */
  protected void updateLinkSegmentMapping(
      Function<MacroscopicLinkSegment, MacroscopicLinkSegment> linkSegmentMapping,
      boolean removeMissingMappings) {
    ListUtils.updateMapping(linkSegments, linkSegmentMapping, removeMissingMappings);
  }

  /**
   * Add a link segment right after one the toll road already holds, such as the new link segment of a link broken at a
   * node, so the link segments stay in the order of the road
   *
   * @param onTheRoad link segment the toll road holds
   * @param linkSegment to add, nothing happens when the toll road holds it already
   */
  public void addLinkSegmentAfter(LinkSegment onTheRoad, MacroscopicLinkSegment linkSegment) {
    ListUtils.addAfter(linkSegments, (MacroscopicLinkSegment) onTheRoad, linkSegment);
  }

  /**
   * Remove a link segment from the toll road, compared by identity
   *
   * @param linkSegment to remove
   * @return true when the toll road held it
   */
  public boolean removeLinkSegment(LinkSegment linkSegment) {
    return linkSegments.removeIf(held -> held == linkSegment);
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
  public boolean containsLinkSegment(LinkSegment linkSegment) {
    return ListUtils.containsByIdentity(linkSegments, linkSegment);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PricedModeSelection getPricedModeSelection() {
    return pricedModeSelection;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Mode> findPricedModes(Collection<? extends Mode> modes) {
    return pricedModeSelection.findPricedModesIn(modes, PricedModeSelection::isPricedByDefault);
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
  public TollRoadImpl shallowClone() {
    return new TollRoadImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TollRoadImpl deepClone() {
    return new TollRoadImpl(this, true);
  }
}
