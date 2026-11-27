package org.goplanit.network.layer.macroscopic.tollroad;

import java.util.function.BiConsumer;
import java.util.function.Function;

import org.goplanit.utils.containers.EntityIndex;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntitiesWithForeignKeyIndicesImpl;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.tollroad.TollRoad;
import org.goplanit.utils.network.layer.macroscopic.tollroad.TollRoadFactory;
import org.goplanit.utils.network.layer.macroscopic.tollroad.TollRoads;
import org.goplanit.utils.network.layer.physical.LinkSegment;

/**
 * Container for the toll roads of a macroscopic network layer, with a lookup of the toll road a link segment is on.
 * Registering a toll road indexes its link segments at that moment; a later change of them goes through
 * {@link #update}, which reindexes it
 * <p>
 * A link segment is on at most one toll road of its layer. The factory checks that through this lookup before it adds
 * a link segment, and the layer modifier finds the toll road of a link segment it breaks or removes through it, without
 * going over every toll road of the layer.
 * </p>
 *
 * @author markr
 */
public class TollRoadsImpl extends ManagedIdEntitiesWithForeignKeyIndicesImpl<TollRoad> implements TollRoads {

  /** factory to create toll roads with */
  private final TollRoadFactory tollRoadFactory;

  /** link segment to the toll road it is on, by identity */
  private final EntityIndex<LinkSegment, TollRoad> byLinkSegment = addForeignKeyIndex(TollRoad::getLinkSegments);

  /**
   * Constructor
   *
   * @param groupId to use for creating ids for instances
   */
  public TollRoadsImpl(final IdGroupingToken groupId) {
    super(TollRoad::getId, TollRoad.TOLL_ROAD_ID_CLASS);
    this.tollRoadFactory = new TollRoadFactoryImpl(groupId, this);
  }

  /**
   * Copy constructor
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   * @param mapper apply to each mapping from original to copy
   */
  public TollRoadsImpl(final TollRoadsImpl other, boolean deepCopy, BiConsumer<TollRoad, TollRoad> mapper) {
    super(other, deepCopy, mapper);
    this.tollRoadFactory = new TollRoadFactoryImpl(other.tollRoadFactory.getIdGroupingToken(), this);
  }

  /**
   * Update the link segments of every toll road based on the mapping provided, e.g. to their copies after a deep copy
   * of the layer; the index is rebuilt to match
   *
   * @param linkSegmentMapping should contain the link segment as currently used and then the value is the new link
   *                           segment to replace it
   * @param removeMissingMappings when true a link segment without a mapping is removed, otherwise it is left in-tact
   */
  public void updateLinkSegmentMapping(
      Function<MacroscopicLinkSegment, MacroscopicLinkSegment> linkSegmentMapping,
      boolean removeMissingMappings) {
    forEach(tollRoad -> ((TollRoadImpl) tollRoad).updateLinkSegmentMapping(
        linkSegmentMapping, removeMissingMappings));
    reindex();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TollRoadFactory getFactory() {
    return tollRoadFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TollRoad getByLinkSegment(LinkSegment linkSegment) {
    return byLinkSegment.getFirst(linkSegment);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TollRoadsImpl shallowClone() {
    return new TollRoadsImpl(this, false, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TollRoadsImpl deepClone() {
    return new TollRoadsImpl(this, true, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TollRoadsImpl deepCloneWithMapping(BiConsumer<TollRoad, TollRoad> mapper) {
    return new TollRoadsImpl(this, true, mapper);
  }
}
