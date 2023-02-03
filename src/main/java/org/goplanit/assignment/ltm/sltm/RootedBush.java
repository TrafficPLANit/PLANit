package org.goplanit.assignment.ltm.sltm;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.goplanit.algorithms.shortest.MinMaxPathResult;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.graph.directed.acyclic.UntypedACyclicSubGraph;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.zoning.OdZone;
import org.goplanit.utils.zoning.Zone;

/**
 * A rooted bush is an acyclic directed graph comprising of implicit paths along a network. It has a single root which can be any vertex with only outgoing edge segments. while
 * acyclic its direction can be either be in up or downstream direction compared to the super network it is situated on.
 * <p>
 * The vertices in the bush represent link segments in the physical network, whereas each edge represents a turn from one link to another. This way each splitting rate uniquely
 * relates to a single turn and all outgoing edges of a vertex represent all turns of a node's incoming link
 * 
 * @author markr
 *
 */
public abstract class RootedBush<V extends DirectedVertex, ES extends EdgeSegment> implements Bush {

  /** Logger to use */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(RootedBush.class.getCanonicalName());

  /** the directed acyclic subgraph representation of the bush, pertaining solely to the topology */
  private final UntypedACyclicSubGraph<V, ES> dag;

  /** the origin demands (PCU/h) of the bush all representing a root (starting point) within the DAG */
  protected Map<OdZone, Double> originDemandsPcuH;

  /** token for id generation unique within this bush */
  protected final IdGroupingToken bushGroupingToken;

  /** track if underlying acyclic graph is modified, if so, an update of the topological sort is required flagged by this member */
  protected boolean requireTopologicalSortUpdate = false;

  /**
   * Track origin demands for bush
   *
   * @param originZone to set
   * @param demandPcuH demand to set
   */
  protected void addOriginDemandPcuH(OdZone originZone, double demandPcuH) {
    double currentDemandPcuH = this.originDemandsPcuH.getOrDefault(originZone, 0.0);
    this.originDemandsPcuH.put(originZone, currentDemandPcuH + demandPcuH);
  }

  /**
   * Access to the underlying dag
   *
   * @return dag of the bush
   */
  protected UntypedACyclicSubGraph<V, ES> getDag() {
    return this.dag;
  }

  /**
   * Conduct an update of the bush turn flows based on the network flow acceptance factors by conducting a bush DAG loading and updating the turn sending flows from the root, i.e.,
   * scale them back with the flow acceptance factor whenever one is encountered.
   *
   * @param flowAcceptanceFactors to use
   */
  public abstract void syncToNetworkFlows(double[] flowAcceptanceFactors);

  /**
   * Constructor
   *
   * @param idToken    the token to base the id generation on
   * @param rootVertex the root vertex of the bush which can be the end or starting point depending whether or not direction is inverted
   * @param inverted   when true bush ends at root vertex and all other vertices precede it, when false the root is the starting point and all other vertices succeed it
   * @param dag        to use for the subgraph representation
   */
  public RootedBush(final IdGroupingToken idToken, DirectedVertex rootVertex, boolean inverted, UntypedACyclicSubGraph<V, ES> dag) {
    this.dag = dag;
    this.bushGroupingToken = IdGenerator.createIdGroupingToken(this, dag.getId());
    this.originDemandsPcuH = new HashMap<>();
  }

  /**
   * Copy constructor
   *
   * @param other to copy
   * @param deepCopy when true, create a eep copy, shallow copy otherwise
   */
  public RootedBush(RootedBush<V, ES> other, boolean deepCopy) {
    this.originDemandsPcuH = new HashMap<>(other.originDemandsPcuH);
    this.requireTopologicalSortUpdate = other.requireTopologicalSortUpdate;
    this.bushGroupingToken = other.bushGroupingToken;

    this.dag = deepCopy ? other.getDag().deepClone() : other.dag.shallowClone();
  }

  /**
   * Compute the min-max path tree rooted in location depending on underlying dag configuration of derived implementation and given the provided (network wide) costs. The provided
   * costs are at the network level so should contain all the segments active in the bush
   * 
   * @param linkSegmentCosts              to use
   * @param totalTransportNetworkVertices needed to be able to create primitive array recording the (partial) subgraph backward link segment results (efficiently)
   * @return minMaxPathResult, null if unable to complete
   */
  public abstract MinMaxPathResult computeMinMaxShortestPaths(final double[] linkSegmentCosts, final int totalTransportNetworkVertices);

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract RootedBush<V, ES> shallowClone();

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract RootedBush<V, ES> deepClone();

  /**
   * {@inheritDoc}
   */
  @Override
  public long getId() {
    return dag.getId();
  }

  /**
   * root vertex of the bush
   * 
   * @return root vertex of the bush
   */
  public V getRootVertex() {
    return dag.getRootVertices().iterator().next();
  }

  /**
   * Indicates if bush has inverted direction w.r.t. its root
   * 
   * @return true when inverted, false otherwise
   */
  public boolean isInverted() {
    return dag.isDirectionInverted();
  }

  /**
   * Origins (with non-zero flow) registered on this bush
   * 
   * @return origins on this bush
   */
  public Set<OdZone> getOrigins() {
    return this.originDemandsPcuH.keySet();
  }

  /**
   * Get the origin demand for a given origin
   * 
   * @param originZone to collect demand for
   * @return demand (if any)
   */
  public Double getOriginDemandPcuH(OdZone originZone) {
    return this.originDemandsPcuH.get(originZone);
  }

  /**
   * Collect iterator for all unique directed vertices in the bush
   *
   * @return iterator
   */
  public Iterator<V> getDirectedVertexIterator() {
    return getDag().iterator();
  }

  /**
   * Each rooted bush is expected to have a zone attached to its root vertex, which is to be returned here
   * 
   * @return root zone
   */
  protected abstract Zone getRootZone();
}
