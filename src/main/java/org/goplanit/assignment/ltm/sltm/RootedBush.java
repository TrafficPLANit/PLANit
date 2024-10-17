package org.goplanit.assignment.ltm.sltm;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Logger;

import org.goplanit.algorithms.shortest.MinMaxPathResult;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.graph.directed.acyclic.UntypedACyclicSubGraph;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.virtual.CentroidVertex;

/**
 * A rooted bush is an acyclic directed graph comprising implicit paths along a network. It has a root which can be any
 * vertex with only outgoing edge segments (or ingoing ones if it is an inverted bushed), so
 * while acyclic its direction can be either be in up or downstream direction compared to the super network it is situated on.
 * <p>
 * The vertices in the bush represent link segments in the physical network, whereas each edge represents a turn from
 * one link to another. This way each splitting rate uniquely relates to a single turn and all outgoing edges of a
 * vertex represent all turns of a node's incoming link
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

  /** the origin demands (PCU/h) of the bush this may or may not be at the root (depending on whether we root in origin or destination) */
  protected Map<CentroidVertex, Double> originDemandsPcuH;

  /** token for id generation unique within this bush */
  protected final IdGroupingToken bushGroupingToken;

  /** track if underlying acyclic graph is modified, if so, an update of the topological sort is required flagged by this member */
  protected boolean requireTopologicalSortUpdate = true;

  /**
   * Track origin demands for bush
   *
   * @param originDemandCentroidVertex to set
   * @param demandPcuH demand to set
   */
  protected void addOriginDemandPcuH(CentroidVertex originDemandCentroidVertex, double demandPcuH) {
    double currentDemandPcuH = this.originDemandsPcuH.getOrDefault(originDemandCentroidVertex, 0.0);
    this.originDemandsPcuH.put(originDemandCentroidVertex, currentDemandPcuH + demandPcuH);
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
   * Traverse a bush in topological order, invert traversal of root is inverted
   *
   * @param invertIterator when true invert iterator direction
   * @param vertexConsumer to apply to each vertex
   */
  @Override
  public void forEachTopologicalSortedVertex(boolean invertIterator, Consumer<DirectedVertex> vertexConsumer) {

    /* get topological sorted vertices to process in indicated direction */
    var vertexIter = invertIterator ? getInvertedTopologicalIterator() : getTopologicalIterator();
    if (vertexIter == null) {
      LOGGER.severe(String.format("Topologically sorted vertices on bush not available, this shouldn't happen, skip vertex traversal"));
      LOGGER.info(String.format("Bush at risk: %s", this));
      return;
    }
    var currVertex = vertexIter.next();

    /* pass over bush in topological order updating turn sending flows based on flow acceptance factors */
    while (vertexIter.hasNext()) {
      currVertex = vertexIter.next();
      vertexConsumer.accept(currVertex);
    }
  }

  /**
   * Conduct an update of the bush turn flows based on the network flow acceptance factors by conducting a bush DAG loading and updating the turn sending flows from the root, i.e.,
   * scale them back with the flow acceptance factor whenever one is encountered.
   *
   * @param flowAcceptanceFactors to use
   */
  public abstract void syncToNetworkFlows(double[] flowAcceptanceFactors);

  /**
   * To avoid bushes keeping low flow links occupied and limiting options to use links or opposite links
   * more efficiently, we will remove very low flow links from each bush, implicitly shifting this flow to
   * higher usage branches.
   *
   * @param flowThreshold any links with flow below this threshold will be implicitly branch shifted
   * @param flowAcceptanceFactors edge segment flow acceptance factors indexed by internal id
   * @param detailedLogging when true log what branch shifted links are affected
   */
  public abstract void performLowFlowBranchShifts(
          double flowThreshold, double[] flowAcceptanceFactors, boolean detailedLogging);

  /**
   * Constructor
   *
   * @param dag        to use for the subgraph representation
   */
  public RootedBush(UntypedACyclicSubGraph<V, ES> dag) {
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
  public abstract MinMaxPathResult computeMinMaxShortestPaths(
      final double[] linkSegmentCosts, final int totalTransportNetworkVertices);

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
  public Set<CentroidVertex> getOriginVertices() {
    return this.originDemandsPcuH.keySet();
  }

  /**
   * Get the origin demand for a given origin
   * 
   * @param originVertex to collect demand for
   * @return demand (if any)
   */
  public Double getOriginDemandPcuH(CentroidVertex originVertex) {
    return this.originDemandsPcuH.get(originVertex);
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
  protected abstract CentroidVertex getRootZoneVertex();

}
