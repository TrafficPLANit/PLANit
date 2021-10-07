package org.planit.assignment.ltm.sltm;

import java.util.logging.Logger;

import org.planit.graph.directed.acyclic.ACyclicSubGraph;
import org.planit.graph.directed.acyclic.ACyclicSubGraphImpl;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.id.IdAble;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.zoning.OdZone;

/**
 * A bush is an acyclic directed graph comprising of all implicit paths used by an origin to reach all its destinations. This is achieved by having the total origin demand at its
 * root vertex which is then split across the graph by (bush specific) splitting rates that reside on each edge. The sum of the edge splitting rates originating from a vertex must
 * always sum to 1.
 * <p>
 * The vertices in the bush represent link segments in the physical network, whereas each edge represents a turn from one link to another. This way each splitting rate uniquely
 * relates to a single turn and all outgoing edges of a vertex represent all turns of a node's incoming link
 * 
 * @author markr
 *
 */
public class Bush implements IdAble {

  /** Logger to use */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(Bush.class.getCanonicalName());

  /** the origin of the bush */
  protected final OdZone origin;

  /** the total demand of the bush */
  protected double originDemandPcuH;

  /** the directed acyclic subgraph representation of the bush, pertaining solely to the topology */
  protected final ACyclicSubGraph dag;

  /** track bush specific data */
  protected final BushTurnData bushData;

  /**
   * Constructor
   * 
   * @param idToken              the token to base the id generation on
   * @param origin               of the bush
   * @param numberOfEdgeSegments The maximum number of edge segments the bush can at most register given the parent network it is a subset of
   */
  public Bush(final IdGroupingToken idToken, final OdZone origin, long numberOfEdgeSegments) {
    this.origin = origin;
    this.dag = new ACyclicSubGraphImpl(idToken, (int) numberOfEdgeSegments, origin.getCentroid());
    this.bushData = new BushTurnData();
  }

  /**
   * Copy constructor
   * 
   * @param bush to copy
   */
  public Bush(Bush bush) {
    this.origin = bush.getOrigin();
    this.dag = bush.dag.clone();
    this.bushData = bush.bushData.clone();
  }

  /**
   * Add turn sending flow to the bush. In case the turn does not yet exist on the bush it is newly registered. If it does exist and there is already flow present, the provided
   * flow is added to it.
   * 
   * @param fromEdgeSegment     from segment of the turn
   * @param toEdgeSegment       to segment of the turn
   * @param turnSendingflowPcuH to add
   */
  public void addTurnSendingFlow(final EdgeSegment fromEdgeSegment, final EdgeSegment toEdgeSegment, double turnSendingflowPcuH) {
    if (!dag.containsEdgeSegment(fromEdgeSegment)) {
      dag.addEdgeSegment(fromEdgeSegment);
    }
    if (!dag.containsEdgeSegment(toEdgeSegment)) {
      dag.addEdgeSegment(toEdgeSegment);
    }
    bushData.addTurnSendingFlow(fromEdgeSegment, toEdgeSegment, turnSendingflowPcuH);
  }

  /**
   * Remove a turn from the bush by removing it from the acyclic graph and removing any data associated with it
   * 
   * @param fromEdgeSegment of the turn
   * @param toEdgeSegment   of the turn
   */
  public void removeTurn(final EdgeSegment fromEdgeSegment, final EdgeSegment toEdgeSegment) {
    dag.removeEdgeSegment(fromEdgeSegment);
    dag.removeEdgeSegment(toEdgeSegment);
    bushData.removeTurn(fromEdgeSegment, toEdgeSegment);
  }

  /**
   * Get the origin, the root of this bush
   * 
   * @return origin
   */
  public OdZone getOrigin() {
    return origin;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getId() {
    return dag.getId();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Bush clone() {
    return new Bush(this);
  }

}
