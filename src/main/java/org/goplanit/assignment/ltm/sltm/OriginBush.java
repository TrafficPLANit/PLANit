package org.goplanit.assignment.ltm.sltm;

import java.util.Iterator;
import java.util.logging.Logger;

import org.goplanit.algorithms.shortest.MinMaxPathResult;
import org.goplanit.algorithms.shortest.ShortestPathAcyclicMinMaxGeneralised;
import org.goplanit.algorithms.shortest.ShortestSearchType;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.zoning.OdZone;

/**
 * An origin bush is an acyclic directed graph comprising of implicit paths along a network rooted at a single origin to all destination with non-zero flow. Demand on the bush is
 * placed along its root node which is then split across the graph by (bush specific) splitting rates that reside on each edge. The sum of the edge splitting rates originating from
 * a vertex must always sum to 1.
 * 
 * @author markr
 *
 */
public class OriginBush extends RootedBush {

  /** Logger to use */
  private static final Logger LOGGER = Logger.getLogger(OriginBush.class.getCanonicalName());

  /**
   * Constructor
   * 
   * @param idToken                 the token to base the id generation on
   * @param origin                  origin of the bush
   * @param maxSubGraphEdgeSegments The maximum number of edge segments the bush can at most register given the parent network it is a subset of
   */
  public OriginBush(final IdGroupingToken idToken, OdZone origin, long maxSubGraphEdgeSegments) {
    this(idToken, origin, 0, maxSubGraphEdgeSegments);
  }

  /**
   * Constructor
   * 
   * @param idToken                 the token to base the id generation on
   * @param origin                  origin of the bush
   * @param originDemandPcuH        demand
   * @param maxSubGraphEdgeSegments The maximum number of edge segments the bush can at most register given the parent network it is a subset of
   */
  public OriginBush(final IdGroupingToken idToken, OdZone origin, double originDemandPcuH, long maxSubGraphEdgeSegments) {
    super(idToken, origin.getCentroid(), false /* not inverted */, maxSubGraphEdgeSegments);
    addOriginDemandPcuH(origin, originDemandPcuH);
  }

  /**
   * Copy constructor
   * 
   * @param bush to (shallow) copy
   */
  public OriginBush(OriginBush bush) {
    super(bush);
  }

  /**
   * Compute the min-max path tree rooted at the origin and given the provided (network wide) costs. The provided costs are at the network level so should contain all the segments
   * active in the bush
   * 
   * @param linkSegmentCosts              to use
   * @param totalTransportNetworkVertices needed to be able to create primitive array recording the (partial) subgraph backward link segment results (efficiently)
   * @return minMaxPathResult, null if unable to complete
   */
  @Override
  public MinMaxPathResult computeMinMaxShortestPaths(final double[] linkSegmentCosts, final int totalTransportNetworkVertices) {

    /* build min/max path tree */
    var minMaxBushPaths = new ShortestPathAcyclicMinMaxGeneralised(dag, requireTopologicalSortUpdate, linkSegmentCosts, totalTransportNetworkVertices);
    try {
      return minMaxBushPaths.executeOneToAll(dag.getRootVertex());
    } catch (Exception e) {
      LOGGER.severe(String.format("Unable to complete minmax path three for origin-bush rooted at origin %s", getOrigin().getXmlId()));
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ShortestSearchType getShortestSearchType() {
    return ShortestSearchType.ONE_TO_ALL;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Iterator<DirectedVertex> getTopologicalIterator(boolean originDestinationDirection) {
    return this.dag.getTopologicalIterator(requireTopologicalSortUpdate, !originDestinationDirection /* do not invert direction, dag is in od direction */ );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OriginBush clone() {
    return new OriginBush(this);
  }

  /**
   * collect origin of this bush
   * 
   * @return origin zone
   */
  public OdZone getOrigin() {
    return this.originDemandsPcuH.keySet().iterator().next();
  }

  /**
   * add origin demand for this origin bush
   * 
   * @param demandPcuH demand to add
   */
  public void addOriginDemandPcuH(double demandPcuH) {
    addOriginDemandPcuH(getOrigin(), demandPcuH);
  }

  /**
   * Get the origin demand
   * 
   * @return demand (if any)
   */
  public Double getOriginDemandPcuH() {
    return getOriginDemandPcuH(getOrigin());
  }
}