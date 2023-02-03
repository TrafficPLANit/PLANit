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
 * A destination bush is an (inverted) acyclic directed graph rooted at many origins going to a single destination representing all implicit paths along a network to the given
 * destination. Demand on the bush is placed along its root node(s) which is then split across the graph by (bush specific) splitting rates that reside on each edge. The sum of the
 * edge splitting rates originating from a vertex must always sum to 1.
 * 
 * @author markr
 *
 */
public class DestinationBush extends RootedLabelledBush {

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
    super(idToken, destination.getCentroid(), true /* inverted */, maxSubGraphEdgeSegments);
    this.destination = destination;
  }

  /**
   * Copy constructor
   * 
   * @param bush to copy
   * @param deepCopy when true, create a eep copy, shallow copy otherwise
   */
  public DestinationBush(DestinationBush bush, boolean deepCopy) {
    super(bush, deepCopy);
    this.destination = bush.destination;
  }

  /**
   * Compute the min-max path tree rooted at the destination towards all origins given the provided (network wide) costs. The provided costs are at the network level so should
   * contain all the segments active in the bush
   * 
   * @param linkSegmentCosts              to use
   * @param totalTransportNetworkVertices number of vertices in overall network needed to be able to construct result per vertex based on id
   * @return minMaxPathResult, null if unable to complete
   */
  @Override
  public MinMaxPathResult computeMinMaxShortestPaths(final double[] linkSegmentCosts, final int totalTransportNetworkVertices) {

    /* build min/max path tree */
    var minMaxBushPaths = new ShortestPathAcyclicMinMaxGeneralised(getDag(), requireTopologicalSortUpdate, linkSegmentCosts, totalTransportNetworkVertices);
    try {
      return minMaxBushPaths.executeAllToOne(getRootVertex());
    } catch (Exception e) {
      LOGGER.severe(String.format("Unable to complete minmax path three for destination-based bush ending at destination %s", getDestination().getXmlId()));
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ShortestSearchType getShortestSearchType() {
    return ShortestSearchType.ALL_TO_ONE;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Iterator<DirectedVertex> getTopologicalIterator(boolean originDestinationDirection) {
    return getDag().getTopologicalIterator(requireTopologicalSortUpdate, originDestinationDirection /* reverse for od direction since dag is destination to origin oriented */);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OdZone getRootZone() {
    return getDestination();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DestinationBush shallowClone() {
    return new DestinationBush(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DestinationBush deepClone() {
    return new DestinationBush(this, true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    String result = super.toString();
    return "Bush: destination zone: " + getDestination().getXmlId() + "\n" + result;
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

}
