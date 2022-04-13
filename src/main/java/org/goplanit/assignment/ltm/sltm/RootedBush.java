package org.goplanit.assignment.ltm.sltm;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.goplanit.algorithms.shortest.MinMaxPathResult;
import org.goplanit.algorithms.shortest.ShortestPathSearchUtils;
import org.goplanit.algorithms.shortest.ShortestSearchType;
import org.goplanit.graph.directed.acyclic.ACyclicSubGraph;
import org.goplanit.graph.directed.acyclic.ACyclicSubGraphImpl;
import org.goplanit.utils.graph.directed.DirectedEdge;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.id.IdAble;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.zoning.OdZone;

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
public abstract class RootedBush implements IdAble {

  /** Logger to use */
  private static final Logger LOGGER = Logger.getLogger(RootedBush.class.getCanonicalName());

  /**
   * Determine the sending flow between origin,destination vertex using the subpath given by the subPathArray in order from start to finish. We utilise the initial sending flow on
   * the indexed segment and label as the base flow which is then followed along the subpath through the bush splitting rates up to the final link segment
   * 
   * @param subPathSendingFlow to start with
   * @param index              offset to start in array with
   * @param subPathArray       to extract path from
   * @return sendingFlowPcuH between index and end vertex following the sub-path
   */
  private double determineSubPathSendingFlow(double subPathSendingFlow, BushFlowLabel label, int index, final EdgeSegment[] subPathArray) {
    var currEdgeSegment = subPathArray[index++];
    if (index < subPathArray.length && Precision.positive(subPathSendingFlow)) {
      var nextEdgeSegment = subPathArray[index];

      var exitLabels = getFlowCompositionLabels(nextEdgeSegment);
      if (exitLabels == null) {
        return 0;
      }

      var exitSegmentExitLabelSplittingRates = bushData.getSplittingRates(currEdgeSegment, label);
      double remainingSubPathSendingFlow = 0;
      for (var exitLabel : exitLabels) {
        Double currSplittingRate = exitSegmentExitLabelSplittingRates.get(nextEdgeSegment, exitLabel);
        if (currSplittingRate == null || currSplittingRate <= 0) {
          continue;
        }
        remainingSubPathSendingFlow += subPathSendingFlow * currSplittingRate;
      }

      return determineSubPathSendingFlow(remainingSubPathSendingFlow, label, index, subPathArray);
    }
    return subPathSendingFlow;
  }

  /** the origin demands (PCU/h) of the bush all representing a root (starting point) within the DAG */
  protected Map<OdZone, Double> originDemandsPcuH;

  /** the directed acyclic subgraph representation of the bush, pertaining solely to the topology */
  protected final ACyclicSubGraph dag;

  /** track bush specific data */
  protected final BushTurnData bushData;

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
    this.originDemandsPcuH.put(originZone, demandPcuH);
  }

  /**
   * Constructor
   * 
   * @param idToken                 the token to base the id generation on
   * @param rootVertex              the root vertex of the bush which can be the end or starting point depending whether or not direction is inverted
   * @param inverted                when true bush ends at root vertex and all other vertices precede it, when false the root is the starting point and all other vertices succeed
   *                                it
   * @param maxSubGraphEdgeSegments The maximum number of edge segments the bush can at most register given the parent network it is a subset of
   */
  public RootedBush(final IdGroupingToken idToken, DirectedVertex rootVertex, boolean inverted, long maxSubGraphEdgeSegments) {
    this.dag = new ACyclicSubGraphImpl(idToken, rootVertex, inverted, (int) maxSubGraphEdgeSegments);
    this.bushData = new BushTurnData();
    this.bushGroupingToken = IdGenerator.createIdGroupingToken(this, dag.getId());
    this.originDemandsPcuH = new HashMap<>();
  }

  /**
   * Copy constructor
   * 
   * @param bush to (shallow) copy
   */
  public RootedBush(RootedBush bush) {
    this.originDemandsPcuH = new HashMap<>(bush.originDemandsPcuH);
    this.dag = bush.dag.clone();
    this.bushData = bush.bushData.clone();
    this.requireTopologicalSortUpdate = bush.requireTopologicalSortUpdate;
    this.bushGroupingToken = bush.bushGroupingToken;
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
   * Collect an iterator over topologically sorted bush in origin-destination or destination-origin direction. Depending on the derived bush implementation this might require
   * inverting the iteration direction. Hence it is an abstract method here
   * 
   * @param originDestinationDirection when true, iterator runs topological order from origin towards destinatino, when false, they other way around
   * @return iterator over topologically ordered bush vertices
   */
  public abstract Iterator<DirectedVertex> getTopologicalIterator(boolean originDestinationDirection);

  /**
   * determine the search type supported by the bush based on the underlying dag's construction, i.e., a destination-based dag results in ALL-To-One, whereas an origin based dag
   * results in One-To-All searches.
   * 
   * @return shortest searcht ype compatible with this bush implementation
   */
  public abstract ShortestSearchType getShortestSearchType();

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract RootedBush clone();

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
  public DirectedVertex getRootVertex() {
    return dag.getRootVertex();
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
   * Verify if adding the sub-path edge segments would introduce a cycle in this bush
   * 
   * @param alternative to verify
   * @return edge segment that would introduces a cycle, null otherwise
   */
  public EdgeSegment determineIntroduceCycle(EdgeSegment[] alternative) {
    if (alternative == null) {
      LOGGER.severe("Cannot verify if edge segments introduce cycle when parameters are null");
      return null;
    }
    EdgeSegment currSegment = null;
    for (int index = 0; index < alternative.length; ++index) {
      currSegment = alternative[index].getOppositeDirectionSegment();
      if (currSegment != null && containsEdgeSegment(currSegment)) {
        return alternative[index];
      }
    }
    return null;
  }

  /**
   * Add turn sending flow to the bush. In case the turn does not yet exist on the bush it is newly registered. If it does exist and there is already flow present, the provided
   * flow is added to it. If by adding the flow (can be negative) the turn no longer has any flow, nor labels nor the turn itself is removed
   * 
   * @param from        from segment of the turn
   * @param fromLabel   to use
   * @param to          to segment of the turn
   * @param toLabel     to use
   * @param addFlowPcuH to add
   * @return new labelled turn sending flow after adding given flow
   */
  public double addTurnSendingFlow(final EdgeSegment from, final BushFlowLabel fromLabel, final EdgeSegment to, final BushFlowLabel toLabel, double addFlowPcuH) {
    return addTurnSendingFlow(from, fromLabel, to, toLabel, addFlowPcuH, false);
  }

  /**
   * Add turn sending flow to the bush. In case the turn does not yet exist on the bush it is newly registered. If it does exist and there is already flow present, the provided
   * flow is added to it. If by adding the flow (can be negative) the turn no longer has any flow, the labels are removed
   * 
   * @param from             from segment of the turn
   * @param fromLabel        to use
   * @param to               to segment of the turn
   * @param toLabel          to use
   * @param addFlowPcuH      to add
   * @param allowTurnRemoval when true we remove turn when no flow remains after adding (negative) flow, when false, we only change the flow to zero but the bush is not adjusted
   * @return new labelled turn sending flow after adding given flow
   */
  public double addTurnSendingFlow(final EdgeSegment from, final BushFlowLabel fromLabel, final EdgeSegment to, final BushFlowLabel toLabel, double addFlowPcuH,
      boolean allowTurnRemoval) {

    if (addFlowPcuH > 0) {
      if (!containsEdgeSegment(from)) {
        if (containsEdgeSegment(from.getOppositeDirectionSegment())) {
          LOGGER.warning(String.format("Trying to add turn flow (%s,%s) on bush where the opposite direction (of segment %s) already is part of the bush, this break acyclicity",
              from.getXmlId(), to.getXmlId(), from.getXmlId()));
        }
        dag.addEdgeSegment(from);
        requireTopologicalSortUpdate = true;
      }
      if (!containsEdgeSegment(to)) {
        if (containsEdgeSegment(to.getOppositeDirectionSegment())) {
          LOGGER.warning(String.format("Trying to add turn flow (%s,%s) on bush where the opposite direction (of segment %s) already is part of the bush, this break acyclicity",
              from.getXmlId(), to.getXmlId(), to.getXmlId()));
        }
        dag.addEdgeSegment(to);
        requireTopologicalSortUpdate = true;
      }
    }
    return bushData.addTurnSendingFlow(from, fromLabel, to, toLabel, addFlowPcuH, allowTurnRemoval);
  }

  /**
   * Collect bush turn sending flow (if any)
   * 
   * @param from to use
   * @param to   to use
   * @return sending flow, zero if unknown
   */
  public double getTurnSendingFlow(final EdgeSegment from, final EdgeSegment to) {
    return bushData.getTurnSendingFlowPcuH(from, to);
  }

  /**
   * Collect bush turn sending flow (if any)
   * 
   * @param from      to use
   * @param fromLabel to filter by
   * @param to        to use
   * @param toLabel   to filter by
   * @return sending flow, zero if unknown
   */
  public double getTurnSendingFlow(final EdgeSegment from, final BushFlowLabel fromLabel, final EdgeSegment to, final BushFlowLabel toLabel) {
    return bushData.getTurnSendingFlowPcuH(from, fromLabel, to, toLabel);
  }

  /**
   * Collect the sending flow of an edge segment in the bush, if not present, zero flow is returned
   * 
   * @param edgeSegment to collect sending flow for
   * @return bush sending flow on edge segment
   */
  public double getSendingFlowPcuH(final EdgeSegment edgeSegment) {
    return bushData.getTotalSendingFlowFromPcuH(edgeSegment);
  }

  /**
   * Collect the sending flow of an edge segment in the bush but only for the specified label, if not present, zero flow is returned
   * 
   * @param edgeSegment      to collect sending flow for
   * @param compositionLabel to filter by
   * @return bush sending flow on edge segment
   */
  public double getSendingFlowPcuH(EdgeSegment edgeSegment, BushFlowLabel compositionLabel) {
    return bushData.getTotalSendingFlowFromPcuH(edgeSegment, compositionLabel);
  }

  /**
   * Verify if the provided turn has any registered sending flow
   * 
   * @param from to use
   * @param to   to use
   * @return true when turn sending flow is present, false otherwise
   */
  public boolean containsTurnSendingFlow(final EdgeSegment from, final EdgeSegment to) {
    return bushData.getTurnSendingFlowPcuH(from, to) > 0;
  }

  /**
   * Verify if the provided turn has any registered sending flow for the given label combination
   * 
   * @param from      to use
   * @param fromLabel to use
   * @param to        to use
   * @param toLabel   to use
   * @return true when turn sending flow is present, false otherwise
   */
  public boolean containsTurnSendingFlow(EdgeSegment from, BushFlowLabel fromLabel, EdgeSegment to, BushFlowLabel toLabel) {
    return bushData.getTurnSendingFlowPcuH(from, fromLabel, to, toLabel) > 0;
  }

  /**
   * Collect the bush splitting rate on the given turn
   * 
   * @param from to use
   * @param to   to use
   * @return found splitting rate, in case the turn is not used, 0 is returned
   */
  public double getSplittingRate(final EdgeSegment from, final EdgeSegment to) {
    return bushData.getSplittingRate(from, to);
  }

  /**
   * Collect the bush splitting rate on the given turn for a given label. This might be 0, or 1, but cna also be something in between in case the label splits off in multiple
   * directions
   * 
   * @param entrySegment   to use
   * @param exitSegment    to use
   * @param entryExitLabel label to be used for both entry and exit of the turn
   * @return found splitting rate, in case the turn is not used, 0 is returned
   */
  public double getSplittingRate(EdgeSegment entrySegment, EdgeSegment exitSegment, BushFlowLabel entryExitLabel) {
    return bushData.getSplittingRate(entrySegment, exitSegment, entryExitLabel);
  }

  /**
   * Collect the bush splitting rates for a given incoming edge segment. If entry segment has no flow, zero splitting rates are returned for all turns
   * 
   * @param entrySegment to use
   * @return splitting rates in primitive array in order of which one iterates over the outgoing edge segments of the downstream from segment vertex
   */
  public double[] getSplittingRates(final EdgeSegment entrySegment) {
    return bushData.getSplittingRates(entrySegment);
  }

  /**
   * Collect the bush splitting rates for a given incoming edge segment and entry label. If no flow exits, no splitting rate is provided in the returned map
   * 
   * @param entrySegment to use
   * @param entryLabel   to use
   * @return splitting rates in multikeymap where the key is the combination of exit segment and exit label and the value is the portion of the entry segment entry label flow
   *         directed to it
   */
  public MultiKeyMap<Object, Double> getSplittingRates(final EdgeSegment entrySegment, final BushFlowLabel entryLabel) {
    return bushData.getSplittingRates(entrySegment, entryLabel);
  }

  /**
   * Remove a turn from the bush by removing it from the acyclic graph and removing any data associated with it. Edge segments are also removed in case the no longer carry any flow
   * 
   * @param fromEdgeSegment of the turn
   * @param toEdgeSegment   of the turn
   */
  public void removeTurn(final EdgeSegment fromEdgeSegment, final EdgeSegment toEdgeSegment) {
    bushData.removeTurn(fromEdgeSegment, toEdgeSegment);
    LOGGER.info(String.format("Removing turn (%s,%s) from bush", fromEdgeSegment.getXmlId(), toEdgeSegment.getXmlId()));

    if (!Precision.positive(getSendingFlowPcuH(fromEdgeSegment))) {
      removeEdgeSegment(fromEdgeSegment);
    }
    if (!Precision.positive(getSendingFlowPcuH(toEdgeSegment))) {
      removeEdgeSegment(toEdgeSegment);
    }
    requireTopologicalSortUpdate = true;
  }

  /**
   * Remove edge segment from bush, if it no longer has flow
   * 
   * @param edgeSegment to remove
   * @return true when removed, false otherwise
   */
  public boolean removeEdgeSegment(EdgeSegment edgeSegment) {
    /* update graph if edge segment is unused */
    if (!Precision.positive(getSendingFlowPcuH(edgeSegment))) {
      LOGGER.info(String.format("Removing edge segment (%s) from bush", edgeSegment.getXmlId()));
      dag.removeEdgeSegment(edgeSegment);
      return true;
    }

    LOGGER.warning(String.format("Unable to remove edge segment %s from bush (origin %s) unless it has no flow", edgeSegment.getXmlId()));
    return false;
  }

  /**
   * Verify if the bush contains the given edge segment
   * 
   * @param edgeSegment to verify
   * @return true when present, false otherwise
   */
  public boolean containsEdgeSegment(EdgeSegment edgeSegment) {
    return dag.containsEdgeSegment(edgeSegment);
  }

  /**
   * Verify if the bush contains any edge segment of the edge in either direction
   * 
   * @param edge to verify
   * @return true when an edge segment of the edge is present, false otherwise
   */
  public boolean containsAnyEdgeSegmentOf(DirectedEdge edge) {
    for (var edgeSegment : edge.getEdgeSegments()) {
      if (dag.containsEdgeSegment(edgeSegment)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Collect iterator for all unique directed vertices in the bush
   * 
   * @return iterator
   */
  public Iterator<DirectedVertex> getDirectedVertexIterator() {
    return dag.iterator();
  }

  /**
   * The alternative subpath is provided through link segment labels of value -1. The point at which they coincide with the bush is indicated with label 1 at the given reference
   * vertex (passed in). Here we do a breadth-first search on the bush in the direction towards its root to find a location the alternative path reconnects to the bush, which, at
   * the latest, should be at the root and at the earliest directly at the next vertex compared to the reference vertex.
   * <p>
   * Note that the breadth-first approach is a choice not a necessity but the underlying idea is that a shorter PAS (which is likely to be found) is used by more origins and
   * therefore more useful to explore than a really long PAS. This is preferred - in the original TAPAS - over simply backtracking along either the shortest or longest path of the
   * min-max tree which would also be viable options,a s would a depth-first search.
   * <p>
   * Consider implementing various strategies here in order to explore what works best but for now we adopt a breadth-first search
   * <p>
   * The returned map contains the next edge segment for each vertex, from the vertex closer to the bush root to the reference vertex where for the reference vertex the edge
   * segment remains null
   * 
   * @param referenceVertex                to start breadth first search from as it is the point of coincidence of the alternative path (via labelled vertices) and bush
   * @param alternativeSubpathVertexLabels indicating the shortest (network) path at the reference vertex but not part of the bush at that point (different edge segment used)
   * @return vertex at which the two paths coincided again and the map to extract the path from the this vertex to the reference vertex that was found using the breadth-first
   *         method
   */
  public Pair<DirectedVertex, Map<DirectedVertex, EdgeSegment>> findBushAlternativeSubpath(DirectedVertex referenceVertex, final short[] alternativeSubpathVertexLabels) {
    Deque<Pair<DirectedVertex, EdgeSegment>> openVertexQueue = new ArrayDeque<>(30);
    Map<DirectedVertex, EdgeSegment> processedVertices = new TreeMap<>();

    /*
     * Construct results in same direction as shortest path search, so So, for one-to-all regular search, we construct results where we have for each vertex its upstream segment,
     * while for all-to-one we have the downstream segment for each vertex
     */
    final boolean invertNextDirection = true;
    final var getNextEdgeSegments = ShortestPathSearchUtils.getEdgeSegmentsInDirectionLambda(this, invertNextDirection);
    final var getNextVertex = ShortestPathSearchUtils.getVertexFromEdgeSegmentLambda(this, invertNextDirection);
    
    /* start with eligible edge segments of reference vertex except alternative labelled segment */
    processedVertices.put(referenceVertex, null);
    var nextEdgeSegments = getNextEdgeSegments.apply(referenceVertex);
    for (var nextSegment : nextEdgeSegments) {
      if (containsEdgeSegment(nextSegment) && alternativeSubpathVertexLabels[(int) referenceVertex.getId()] != -1) {
        openVertexQueue.add(Pair.of(getNextVertex.apply(nextSegment), nextSegment));
      }
    }

    while (!openVertexQueue.isEmpty()) {
      Pair<DirectedVertex, EdgeSegment> current = openVertexQueue.pop();
      var currentVertex = current.first();
      if (processedVertices.containsKey(currentVertex)) {
        continue;
      }

      if (alternativeSubpathVertexLabels[(int) currentVertex.getId()] == -1) {
        /* first point of coincidence with alternative labelled path */
        processedVertices.put(currentVertex, current.second());
        return Pair.of(current.first(), processedVertices);
      }

      /* breadth-first loop for used turns that not yet have been processed */
      nextEdgeSegments = getNextEdgeSegments.apply(currentVertex);
      for (var nextSegment : nextEdgeSegments) {
        if (containsEdgeSegment(nextSegment) && bushData.containsTurnSendingFlow(nextSegment, current.second())) {
          var nextVertex = getNextVertex.apply(nextSegment);
          if (!processedVertices.containsKey(nextVertex)) {
            openVertexQueue.add(Pair.of(nextVertex, nextSegment));
          }
        }
      }

      processedVertices.put(currentVertex, current.second());
    }

    /*
     * no result could be found, only possible when cycle is detected before reaching origin Not sure this will actually happen, so created warning to check, when it does happen
     * investigate and see if this expected behaviour (if so remove statement). this would equate to finding a vertex marked with a '1' in Xie & Xie, which I do not do because I
     * don't think it is needed, but I might be wrong.
     */
    LOGGER.warning(String.format("Cycle found when finding alternative subpath on bush merging at vertex %s", referenceVertex.getXmlId()));
    return null;
  }

  /**
   * Determine the sending flow between origin,destination vertex using the subpath given by the subPathMap, where each vertex provides its exit segment. This is used to traverse
   * the subpath and extract the portion of the sending flow currently known at the bushes startVertex provided to the end vertex
   * 
   * @param startVertex to use
   * @param endVertex   to use
   * @param subPathMap  to extract path from
   * @return sendingFlowPcuH between start and end vertex following the found sub-path
   */
  public double computeSubPathSendingFlow(final DirectedVertex startVertex, final DirectedVertex endVertex, final Map<DirectedVertex, EdgeSegment> subPathMap) {
    EdgeSegment nextEdgeSegment = subPathMap.get(startVertex);
    double subPathSendingFlow = bushData.getTotalSendingFlowFromPcuH(nextEdgeSegment);

    if (Precision.positive(subPathSendingFlow)) {
      var currEdgeSegment = nextEdgeSegment;
      nextEdgeSegment = subPathMap.get(currEdgeSegment.getDownstreamVertex());
      do {
        subPathSendingFlow *= bushData.getSplittingRate(currEdgeSegment, nextEdgeSegment);
        currEdgeSegment = nextEdgeSegment;
        nextEdgeSegment = subPathMap.get(currEdgeSegment.getDownstreamVertex());
      } while (nextEdgeSegment != null && Precision.positive(subPathSendingFlow));
    }

    return subPathSendingFlow;
  }

  /**
   * Determine the accepted flow between origin,destination vertex using the subpath given by the subPathArray in order from start to finish. We utilise the initial sending flow on
   * the first segment as the base flow which is then reduced by the splitting rates and acceptance factor up to and including the final link segment
   * 
   * @param startVertex                  to use
   * @param endVertex                    to use
   * @param subPathArray                 to extract path from
   * @param linkSegmentAcceptanceFactors the acceptance factor to apply along the path, indexed by link segment id
   * @return acceptedFlowPcuH between start and end vertex following the sub-path
   */
  public double computeSubPathAcceptedFlow(final DirectedVertex startVertex, final DirectedVertex endVertex, final EdgeSegment[] subPathArray,
      final double[] linkSegmentAcceptanceFactors) {

    int index = 0;
    EdgeSegment currEdgeSegment = subPathArray[index++];
    double subPathAcceptedFlowPcuH = bushData.getTotalSendingFlowFromPcuH(currEdgeSegment);

    var nextEdgeSegment = currEdgeSegment;
    while (index < subPathArray.length && Precision.positive(subPathAcceptedFlowPcuH)) {
      currEdgeSegment = nextEdgeSegment;
      nextEdgeSegment = subPathArray[index++];
      subPathAcceptedFlowPcuH *= bushData.getSplittingRate(currEdgeSegment, nextEdgeSegment) * linkSegmentAcceptanceFactors[(int) currEdgeSegment.getId()];
    }
    subPathAcceptedFlowPcuH *= linkSegmentAcceptanceFactors[(int) nextEdgeSegment.getId()];

    return subPathAcceptedFlowPcuH;
  }

  /**
   * Determine the sending flow between origin,destination vertex using the subpath given by the segment + subPathArray in order from start to finish. We utilise the initial
   * sending flow on the entry segment as the base flow which is then followed along the subpath through the bush splitting rates up to the final link segment
   *
   * @param entrySegment to start subpath from
   * @param subPathArray to append to entry segment to extract path from
   * @return sendingFlowPcuH between start and end vertex following the sub-path
   */
  public double determineSubPathSendingFlow(EdgeSegment entrySegment, EdgeSegment[] subPathArray) {

    int index = 0;
    var usedEntryLabels = getFlowCompositionLabels(entrySegment);
    double subPathSendingFlow = 0;
    for (var entryLabel : usedEntryLabels) {
      double labelSendingFlow = bushData.getTotalSendingFlowFromPcuH(entrySegment, entryLabel);

      /* determine flow from entry segment into initial segment, from there on recursively traverse sub-path */
      var initialSubPathEdgeSegment = subPathArray[index];
      var exitLabels = getFlowCompositionLabels(initialSubPathEdgeSegment);
      if (exitLabels == null) {
        return 0;
      }

      var exitSegmentExitLabelSplittingRates = bushData.getSplittingRates(entrySegment, entryLabel);
      double remainingSubPathSendingFlow = 0;
      for (var exitLabel : exitLabels) {
        Double currSplittingRate = exitSegmentExitLabelSplittingRates.get(initialSubPathEdgeSegment, exitLabel);
        if (currSplittingRate == null || currSplittingRate <= 0) {
          continue;
        }
        remainingSubPathSendingFlow += labelSendingFlow * currSplittingRate;
      }

      labelSendingFlow = determineSubPathSendingFlow(remainingSubPathSendingFlow, entryLabel, index, subPathArray);
      subPathSendingFlow += labelSendingFlow;
    }

    return subPathSendingFlow;
  }

  /**
   * Find out the portion of the origin attributed flow on the segment that belongs to each available flow composition label proportional to the total flow across all provided
   * labels on this same segment
   * 
   * @param edgeSegment              to determine the label rates for
   * @param pasFlowCompositionLabels to determine relative proportions for based on total flow across provided labels on the link segment
   * @return the rates at hand for each found composition label
   */
  public TreeMap<BushFlowLabel, Double> determineProportionalFlowCompositionRates(final EdgeSegment edgeSegment, final Set<BushFlowLabel> pasFlowCompositionLabels) {
    double totalSendingFlow = 0;
    var rateMap = new TreeMap<BushFlowLabel, Double>();
    for (var label : pasFlowCompositionLabels) {
      double labelFlow = bushData.getTotalSendingFlowFromPcuH(edgeSegment, label);
      rateMap.put(label, labelFlow);
      totalSendingFlow += labelFlow;
    }

    for (var entry : rateMap.entrySet()) {
      entry.setValue(entry.getValue() / totalSendingFlow);
    }

    return rateMap;
  }

  /**
   * The labels present for the given segment
   * 
   * @param edgeSegment to collect composition labels for
   * @return the flow composition labels found
   */
  public TreeSet<BushFlowLabel> getFlowCompositionLabels(EdgeSegment edgeSegment) {
    return bushData.getFlowCompositionLabels(edgeSegment);
  }

  /**
   * The first of the flow composition labels present on the given segment. If no lables are present null is returned
   * 
   * @param edgeSegment to collect composition labels for
   * @return the flow composition labels found
   */
  public BushFlowLabel getFirstFlowCompositionLabel(EdgeSegment edgeSegment) {
    return hasFlowCompositionLabel(edgeSegment) ? bushData.getFlowCompositionLabels(edgeSegment).first() : null;
  }

  /**
   * Verify if the edge segment has any flow composition labels registered on it
   * 
   * @param edgeSegment to verify
   * @return true when present, false otherwise
   */
  public boolean hasFlowCompositionLabel(final EdgeSegment edgeSegment) {
    return bushData.hasFlowCompositionLabel(edgeSegment);
  }

  /**
   * Verify if the edge segment has the flow composition label provided
   * 
   * @param edgeSegment      to verify
   * @param compositionLabel to verify
   * @return true when present, false otherwise
   */
  public boolean hasFlowCompositionLabel(final EdgeSegment edgeSegment, final BushFlowLabel compositionLabel) {
    return bushData.hasFlowCompositionLabel(edgeSegment, compositionLabel);
  }

  /**
   * Relabel existing flow from one composition from-to combination to a new from-to label
   * 
   * @param fromSegment    from segment of turn
   * @param oldFromLabel   from composition label to replace
   * @param toSegment      to segment of turn
   * @param oldToLabel     to composition label to replace
   * @param newFromToLabel label to replace flow with
   * @return the amount of flow that was relabelled
   */
  public double relabel(EdgeSegment fromSegment, BushFlowLabel oldFromLabel, EdgeSegment toSegment, BushFlowLabel oldToLabel, BushFlowLabel newFromToLabel) {
    return bushData.relabel(fromSegment, oldFromLabel, toSegment, oldToLabel, newFromToLabel);
  }

  /**
   * Relabel the from label of existing flow from one composition from-to combination to a new from-to label
   * 
   * @param fromSegment  from segment of turn
   * @param oldFromLabel from composition label to replace
   * @param toSegment    to segment of turn
   * @param toLabel      to composition label
   * @param newFromLabel label to replace flow with
   * @return the amount of flow that was relabelled
   */
  public double relabelFrom(EdgeSegment fromSegment, BushFlowLabel oldFromLabel, EdgeSegment toSegment, BushFlowLabel toLabel, BushFlowLabel newFromLabel) {
    return bushData.relabelFrom(fromSegment, oldFromLabel, toSegment, toLabel, newFromLabel);
  }

  /**
   * Relabel the to label of existing flow from one composition from-to combination to a new from-to label
   * 
   * @param fromSegment from segment of turn
   * @param fromLabel   from composition label
   * @param toSegment   to segment of turn
   * @param oldToLabel  to composition label to replace
   * @param newToLabel  label to replace flow with
   * @return the amount of flow that was relabelled
   */
  public double relabelTo(EdgeSegment fromSegment, BushFlowLabel fromLabel, EdgeSegment toSegment, BushFlowLabel oldToLabel, BushFlowLabel newToLabel) {
    return bushData.relabelTo(fromSegment, fromLabel, toSegment, oldToLabel, newToLabel);
  }

  /**
   * Conduct an update of the bush turn flows based on the network flow acceptance factors by conducting a bush DAG loading and updating the turn sending flows from the root, i.e.,
   * scale them back with the flow acceptance factor whenever one is encountered.
   * 
   * @param flowAcceptanceFactors to use
   */
  public void updateTurnFlows(double[] flowAcceptanceFactors) {

    /* get topological sorted vertices to process */
    var vertexIter = getTopologicalIterator(true /* od-direction */);
    if (vertexIter == null) {
      LOGGER.severe(String.format("Topologically sorted vertices on bush not available, this shouldn't happen, skip turn flow update"));
      return;
    }
    var currVertex = vertexIter.next();

    /* pass over bush in topological order updating turn sending flows based on flow acceptance factors */
    final boolean AllowTurnRemoval = false;
    while (vertexIter.hasNext()) {
      currVertex = vertexIter.next();
      for (var entrySegment : currVertex.getEntryEdgeSegments()) {
        if (!containsEdgeSegment(entrySegment)) {
          continue;
        }

        /* if flow has fallen below threshold due to queues, remove from bush */
        var usedLabels = getFlowCompositionLabels(entrySegment);
        if (usedLabels == null) {
          continue;
        }

        for (var entrylabel : usedLabels) {
          double entryLabelAcceptedFlow = bushData.getTotalAcceptedFlowToPcuH(entrySegment, entrylabel, flowAcceptanceFactors);

          /*
           * bush splitting rates by [exit segment, exit label] as key - splitting rates are computed based on turn flows but placed in new map. so once we have the splitting rates
           * in this map, we can safely update the turn flows without affecting these splitting rates
           */
          MultiKeyMap<Object, Double> splittingRates = getSplittingRates(entrySegment, entrylabel);

          for (var exitSegment : currVertex.getExitEdgeSegments()) {
            if (!containsEdgeSegment(exitSegment)) {
              continue;
            }

            var exitLabels = getFlowCompositionLabels(exitSegment);
            if (exitLabels == null) {
              continue;
            }

            for (var exitLabel : exitLabels) {
              Double bushExitSegmentLabelSplittingRate = splittingRates.get(exitSegment, exitLabel);
              if (bushExitSegmentLabelSplittingRate != null && Precision.positive(bushExitSegmentLabelSplittingRate)) {
                double bushTurnLabeledAcceptedFlow = entryLabelAcceptedFlow * bushExitSegmentLabelSplittingRate;
                bushData.setTurnSendingFlow(entrySegment, entrylabel, exitSegment, exitLabel, bushTurnLabeledAcceptedFlow, AllowTurnRemoval);
              }
            }
          }
        }
      }
    }
  }

  /**
   * Verify if empty
   * 
   * @return true when empty, false otherwise
   */
  public boolean isEmpty() {
    return bushData.hasTurnFlows();
  }

  /**
   * Origins (witrh non-zero flow) registered on this bush
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
}
