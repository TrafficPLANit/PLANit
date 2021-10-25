package org.planit.gap.consumer;

import java.util.function.BiConsumer;

import org.planit.algorithms.shortestpath.ShortestPathResult;
import org.planit.gap.LinkBasedRelativeDualityGapFunction;
import org.planit.utils.zoning.OdZone;

/**
 * A consumer that takes destination demand (for a given origin) and based on information of the shortest paths min-cost tree (which is expected to be kept consistent for the
 * origin at hand) update the minimum cost gap, i.e., convexity bound on the link based relative duality gap function.
 * <p>
 * to be used in conjunction with the OdDemands consumer oriented method(s)
 * 
 * @author markr
 *
 */
public class LinkBasedRelativeGapMinCostUpdate implements BiConsumer<OdZone, Double> {

  /** gap function to use */
  private final LinkBasedRelativeDualityGapFunction gapFunction;

  /** current origin at hand */
  @SuppressWarnings("unused")
  private OdZone currentOrigin;

  /** current shortest path tree for origin at hand */
  private ShortestPathResult originMinCostTree;

  /**
   * Constructor
   * 
   * @param gapFunction to use
   */
  public LinkBasedRelativeGapMinCostUpdate(final LinkBasedRelativeDualityGapFunction gapFunction) {
    this.gapFunction = gapFunction;
    this.currentOrigin = null;
    this.originMinCostTree = null;
  }

  /**
   * Increase the minimum cost gap for the OD based on the min cost tree cost and provided demand
   */
  @Override
  public void accept(OdZone destination, Double travelDemandPcuH) {
    double costH = originMinCostTree.getCostToReach(destination.getCentroid());
    gapFunction.increaseConvexityBound(travelDemandPcuH * costH);
  }

  /**
   * Update to the given origin and accompanying min cost tree
   * 
   * @param origin            to set
   * @param originMinCostTree to set, to be consistent with the origin
   */
  public void updateOrigin(final OdZone origin, final ShortestPathResult originMinCostTree) {
    this.originMinCostTree = originMinCostTree;
    this.currentOrigin = origin;
  }

}
