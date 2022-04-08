package org.goplanit.assignment.ltm.sltm;

import java.util.logging.Logger;

import org.goplanit.algorithms.shortest.MinMaxPathResult;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.zoning.OdZone;

/**
 * A destination bush is an (inverted) acyclic directed graph rooted at many origins going to a single destination representing all implicit paths along a network to the given
 * destination. Demand on the bush is placed along its root node(s) which is then split across the graph by (bush specific) splitting rates that reside on each edge. The sum of the
 * edge splitting rates originating from a vertex must always sum to 1.
 * 
 * @author markr
 *
 */
public class DestinationBush extends Bush {

  /** Logger to use */
  private static final Logger LOGGER = Logger.getLogger(DestinationBush.class.getCanonicalName());

  /** Destination of this bush */
  protected final OdZone destination;

  /**
   * Constructor
   * 
   * @param idToken                 the token to base the id generation on
   * @param destination             destination of the bush
   * @param maxSubGraphEdgeSegments The maximum number of edge segments the bush can at most register given the parent network it is a subset of
   */
  public DestinationBush(final IdGroupingToken idToken, OdZone destination, long maxSubGraphEdgeSegments) {
    super(idToken, maxSubGraphEdgeSegments);
    this.destination = destination;
  }

  /**
   * Copy constructor
   * 
   * @param bush to (shallow) copy
   */
  public DestinationBush(DestinationBush bush) {
    super(bush);
    this.destination = bush.destination;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DestinationBush clone() {
    return new DestinationBush(this);
  }

  /**
   * Compute the min-max path tree rooted at the destination towards all origins given the provided (network wide) costs. The provided costs are at the network level so should
   * contain all the segments active in the bush
   * 
   * @param linkSegmentCosts              to use
   * @param totalTransportNetworkVertices number of vertices in overall network needed to be able to construct result per vertex based on id
   * @return minMaxPathResult, null if unable to complete
   */
  public MinMaxPathResult computeMinMaxShortestPaths(final double[] linkSegmentCosts, final int totalTransportNetworkVertices) {
    // TODO:
//    /* update topological ordering if needed - Always done for now, should be optimised */
//    var topologicalOrder = getTopologicallySortedVertices();
//    requireTopologicalSortUpdate = false;
//    var dagOriginRootVertex = dag.getRootVertices().iterator().next();
//
//    /* build min/max path tree */
//    var minMaxBushPaths = new AcyclicMinMaxShortestPathAlgorithm(dag, topologicalOrder, linkSegmentCosts, totalTransportNetworkVertices);
//    try {
//      return minMaxBushPaths.executeOneToAll(dagOriginRootVertex);
//    } catch (Exception e) {
//      LOGGER.severe(String.format("Unable to complete minmax path three for bush rooted at origin %s", dagOriginRootVertex.getXmlId()));
//    }
    return null;
  }

  /**
   * collect destination of this bush
   * 
   * @return destination zone
   */
  public OdZone getDestination() {
    return this.destination;
  }

  /**
   * add origin demand for this origin bush
   * 
   * @param demandPcuH demand to add
   */
  public void addOriginDemandPcuH(OdZone originZone, double demandPcuH) {
    super.addOriginDemandPcuH(originZone, demandPcuH);
  }

  /**
   * Get the origin demand
   * 
   * @return demand (if any)
   */
  public Double getOriginDemandPcuH(OdZone originZone) {
    return super.getOriginDemandPcuH(originZone);
  }
}
