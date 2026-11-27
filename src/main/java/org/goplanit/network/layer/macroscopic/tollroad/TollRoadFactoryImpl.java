package org.goplanit.network.layer.macroscopic.tollroad;

import java.util.List;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntityFactoryImpl;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.tollroad.PricedModeSelection;
import org.goplanit.utils.network.layer.macroscopic.tollroad.TollRoad;
import org.goplanit.utils.network.layer.macroscopic.tollroad.TollRoadFactory;
import org.goplanit.utils.network.layer.macroscopic.tollroad.TollRoads;
import org.goplanit.utils.network.layer.physical.LinkSegmentUtils;

/**
 * Factory for the toll roads of a layer, registering each on its container
 * <p>
 * It makes and registers what it is given; the caller checks first, through the toll roads (a link segment is on at
 * most one toll road of its layer) and {@link LinkSegmentUtils#findModesAllowedByNone} (a priced mode no link segment
 * allows), so any caller can ask the same questions.
 * </p>
 *
 * @author markr
 */
public class TollRoadFactoryImpl extends ManagedIdEntityFactoryImpl<TollRoad> implements TollRoadFactory {

  /** container to register toll roads on */
  private final TollRoads tollRoads;

  /**
   * Constructor
   *
   * @param groupId to use for creating ids for instances
   * @param tollRoads to register on
   */
  protected TollRoadFactoryImpl(final IdGroupingToken groupId, final TollRoads tollRoads) {
    super(groupId);
    this.tollRoads = tollRoads;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TollRoad registerNew(List<MacroscopicLinkSegment> linkSegments, PricedModeSelection pricedModes) {
    var tollRoad = new TollRoadImpl(getIdGroupingToken(), linkSegments, pricedModes);
    tollRoads.register(tollRoad);
    return tollRoad;
  }
}
