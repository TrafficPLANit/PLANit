package org.goplanit.assignment.ltm.sltm;

import java.util.logging.Logger;

import org.goplanit.algorithms.shortest.ShortestPathAcyclicMinMax;
import org.goplanit.algorithms.shortest.MinMaxPathResult;
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
public class OriginBush extends Bush {

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
    super(idToken, maxSubGraphEdgeSegments);
    addOriginDemandPcuH(origin, 0);
    this.dag.addRootVertex(origin.getCentroid());
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
    super(idToken, maxSubGraphEdgeSegments);
    addOriginDemandPcuH(origin, originDemandPcuH);
    this.dag.addRootVertex(origin.getCentroid());
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
   * {@inheritDoc}
   */
  @Override
  public OriginBush clone() {
    return new OriginBush(this);
  }

  /**
   * Compute the min-max path tree rooted at the origin and given the provided (network wide) costs. The provided costs are at the network level so should contain all the segments
   * active in the bush
   * 
   * @param linkSegmentCosts              to use
   * @param totalTransportNetworkVertices needed to be able to create primitive array recording the (partial) subgraph backward link segment results (efficiently)
   * @return minMaxPathResult, null if unable to complete
   */
  public MinMaxPathResult computeMinMaxShortestPaths(final double[] linkSegmentCosts, final int totalTransportNetworkVertices) {
    /* update topological ordering if needed - Always done for now, should be optimised */
    var topologicalOrder = getTopologicallySortedVertices();
    requireTopologicalSortUpdate = false;
    var dagOriginRootVertex = dag.getRootVertices().iterator().next();

    /* build min/max path tree */
    var minMaxBushPaths = new ShortestPathAcyclicMinMax(dag, topologicalOrder, linkSegmentCosts, totalTransportNetworkVertices);
    try {
      return minMaxBushPaths.executeOneToAll(dagOriginRootVertex);
    } catch (Exception e) {
      LOGGER.severe(String.format("Unable to complete minmax path three for bush rooted at origin %s", dagOriginRootVertex.getXmlId()));
    }
    return null;
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
