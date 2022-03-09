package org.goplanit.assignment.ltm.sltm;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.goplanit.algorithms.shortest.AcyclicMinMaxShortestPathAlgorithm;
import org.goplanit.algorithms.shortest.MinMaxPathResult;
import org.goplanit.graph.directed.acyclic.ACyclicSubGraph;
import org.goplanit.graph.directed.acyclic.ACyclicSubGraphImpl;
import org.goplanit.utils.graph.EdgeSegment;
import org.goplanit.utils.graph.directed.DirectedEdge;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.id.IdAble;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.zoning.OdZone;

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
  private static final Logger LOGGER = Logger.getLogger(Bush.class.getCanonicalName());

  /**
   * Determine the sending flow between origin,destination vertex using the subpath given by the subPathArray in order from start to finish. We utilise the initial sending flow on
   * the first segment as the base flow which is then followed along the subpath through the bush splitting rates up to the final link segment
   * 
   * @param subPathSendingFlow to start with
   * @param index              offset to start in array with
   * @param subPathArray       to extract path from
   * @return sendingFlowPcuH between index and end vertex following the sub-path
   */
  private double determineSubPathSendingFlow(double subPathSendingFlow, int index, final EdgeSegment[] subPathArray) {
    var currEdgeSegment = subPathArray[index];
    var nextEdgeSegment = currEdgeSegment;
    while (index < subPathArray.length && Precision.positive(subPathSendingFlow)) {
      currEdgeSegment = nextEdgeSegment;
      nextEdgeSegment = subPathArray[index++];
      subPathSendingFlow *= bushData.getSplittingRate(currEdgeSegment, nextEdgeSegment);
    }
  
    return subPathSendingFlow;
  }

  /** the origin of the bush */
  protected final OdZone origin;

  /** the total demand of the bush */
  protected double originDemandPcuH;

  /** the directed acyclic subgraph representation of the bush, pertaining solely to the topology */
  protected final ACyclicSubGraph dag;

  /** track bush specific data */
  protected final BushTurnData bushData;

  /** token for id generation unique within this bush */
  protected final IdGroupingToken bushGroupingToken;

  /** track if underlying acyclic graph is modified, if so, an update of the topological sort is required flagged by this member */
  boolean requireTopologicalSortUpdate = false;

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
    this.bushGroupingToken = IdGenerator.createIdGroupingToken(this, origin.getId());
  }

  /**
   * Copy constructor
   * 
   * @param bush to (shallow) copy
   */
  public Bush(Bush bush) {
    this.origin = bush.getOrigin();
    this.dag = bush.dag.clone();
    this.bushData = bush.bushData.clone();
    this.requireTopologicalSortUpdate = bush.requireTopologicalSortUpdate;
    this.bushGroupingToken = bush.bushGroupingToken;
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

    /* build min/max path tree */
    var minMaxBushPaths = new AcyclicMinMaxShortestPathAlgorithm(dag, topologicalOrder, linkSegmentCosts, totalTransportNetworkVertices);
    try {
      return minMaxBushPaths.executeOneToAll(dag.getRootVertex());
    } catch (Exception e) {
      LOGGER.severe(String.format("Unable to complete minmax path three for bush rooted at origin %s", dag.getRootVertex().getXmlId()));
    }
    return null;
  }

  /**
   * Add additional demand to the bush's root
   * 
   * @param originDemandPcuH to add
   */
  public void addOriginDemandPcuH(double originDemandPcuH) {
    this.originDemandPcuH += originDemandPcuH;
  }

  /**
   * Add turn sending flow to the bush. In case the turn does not yet exist on the bush it is newly registered. If it does exist and there is already flow present, the provided
   * flow is added to it.
   * 
   * @param fromEdgeSegment     from segment of the turn
   * @param fromLabel           to use
   * @param toEdgeSegment       to segment of the turn
   * @param toLabel             to use
   * @param turnSendingflowPcuH to add
   */
  public void addTurnSendingFlow(final EdgeSegment fromEdgeSegment, final BushFlowLabel fromLabel, final EdgeSegment toEdgeSegment, final BushFlowLabel toLabel,
      double turnSendingflowPcuH) {
    if (!containsEdgeSegment(fromEdgeSegment)) {
      if (containsAnyEdgeSegmentOf(fromEdgeSegment.getParentEdge())) {
        LOGGER.warning(String.format("Trying to add turn flow (%s,%s) on bush where the opposite direction (of segment %s) already is part of the bush, this break acyclicity",
            fromEdgeSegment.getXmlId(), toEdgeSegment.getXmlId(), fromEdgeSegment.getXmlId()));
      }
      dag.addEdgeSegment(fromEdgeSegment);
      requireTopologicalSortUpdate = true;
    }
    if (!containsEdgeSegment(toEdgeSegment)) {
      if (containsAnyEdgeSegmentOf(toEdgeSegment.getParentEdge())) {
        LOGGER.warning(String.format("Trying to add turn flow (%s,%s) on bush where the opposite direction (of segment %s) already is part of the bush, this break acyclicity",
            fromEdgeSegment.getXmlId(), toEdgeSegment.getXmlId(), toEdgeSegment.getXmlId()));
      }
      dag.addEdgeSegment(toEdgeSegment);
      requireTopologicalSortUpdate = true;
    }
    bushData.addTurnSendingFlow(fromEdgeSegment, fromLabel, toEdgeSegment, toLabel, turnSendingflowPcuH);
  }

  /**
   * Collect bush turn sending flow (if any)
   * 
   * @param fromEdgeSegment to use
   * @param toEdgeSegment   to use
   * @return sending flow, zero if unknown
   */
  public double getTurnSendingFlow(final EdgeSegment fromEdgeSegment, final EdgeSegment toEdgeSegment) {
    return bushData.getTurnSendingFlowPcuH(fromEdgeSegment, toEdgeSegment);
  }

  /**
   * Collect bush turn sending flow (if any)
   * 
   * @param fromEdgeSegment to use
   * @param fromLabel       to filter by
   * @param toEdgeSegment   to use
   * @param toLabel         to filter by
   * @return sending flow, zero if unknown
   */
  public double getTurnSendingFlow(final EdgeSegment fromEdgeSegment, final BushFlowLabel fromLabel, final EdgeSegment toEdgeSegment, final BushFlowLabel toLabel) {
    return bushData.getTurnSendingFlowPcuH(fromEdgeSegment, fromLabel, toEdgeSegment, toLabel);
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
   * @param entrySegment to use
   * @param exitSegment  to use
   * @return true when turn sending flow is present, false otherwise
   */
  public boolean containsTurnSendingFlow(final EdgeSegment entrySegment, final EdgeSegment exitSegment) {
    return Precision.positive(bushData.getTurnSendingFlowPcuH(entrySegment, exitSegment));
  }

  /**
   * Verify if the provided turn has any registered sending flow for the given label combination
   * 
   * @param entrySegment          to use
   * @param entryCompositionLabel to use
   * @param exitSegment           to use
   * @param exitCompositionLabel  to use
   * @return true when turn sending flow is present, false otherwise
   */
  public boolean containsTurnSendingFlow(EdgeSegment entrySegment, BushFlowLabel entryCompositionLabel, EdgeSegment exitSegment, BushFlowLabel exitCompositionLabel) {
    return Precision.positive(bushData.getTurnSendingFlowPcuH(entrySegment, entryCompositionLabel, exitSegment, exitCompositionLabel));
  }

  /**
   * Multiply all turn sending flows with the given entry label that have the given segment as entry segment, with the given factor
   * 
   * @param entrySegment          to use as turn entry segment
   * @param entryCompositionLabel to filter turn flow by
   * @param factor                to multiply with
   */
  public void multiplyTurnSendingFlows(final EdgeSegment entrySegment, final BushFlowLabel entryCompositionLabel, double factor) {
    for (EdgeSegment exitSegment : entrySegment.getDownstreamVertex().getExitEdgeSegments()) {
      var exitLabels = bushData.getFlowCompositionLabels(exitSegment);
      if (exitLabels == null) {
        continue;
      }
      for (var exitLabel : exitLabels) {
        double currentTurnLabeledSendingFlow = bushData.getTurnSendingFlowPcuH(entrySegment, entryCompositionLabel, exitSegment, exitLabel);
        if (Precision.positive(currentTurnLabeledSendingFlow)) {
          bushData.setTurnSendingFlow(entrySegment, entryCompositionLabel, exitSegment, exitLabel,
              bushData.getTurnSendingFlowPcuH(entrySegment, entryCompositionLabel, exitSegment, exitLabel) * factor);
        }
      }
    }
  }

  /**
   * Collect the bush splitting rate on the given turn
   * 
   * @param entrySegment to use
   * @param exitSegment  to use
   * @return found splitting rate, in case the turn is not used, 0 is returned
   */
  public double getSplittingRate(final EdgeSegment entrySegment, final EdgeSegment exitSegment) {
    return bushData.getSplittingRate(entrySegment, exitSegment);
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
   * Collect the bush splitting rates for a given incoming edge segment and entry label. If no flow exits, zero splitting rates are returned
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
   * Collect the splitting rates for the root vertex (which do not have an entry segment). Result has an entry for each outgoing segment regardless if it is part of the bush in the
   * order of looping through the outgoing edge segments on the root vertex
   * 
   * @return splitting rates for the root vertex exit segments.
   */
  public double[] getRootVertexSplittingRates() {
    double[] splittingRates = new double[this.dag.getRootVertex().sizeOfExitEdgeSegments()];
    int index = 0;
    double foundRootDemandPcuH = 0;
    for (var exitSegment : this.dag.getRootVertex().getExitEdgeSegments()) {
      if (containsEdgeSegment(exitSegment)) {
        double rootExitDemandPcuH = bushData.getTotalSendingFlowFromPcuH(exitSegment);
        splittingRates[index] = rootExitDemandPcuH / originDemandPcuH;
        foundRootDemandPcuH += rootExitDemandPcuH;
      }
      ++index;
    }

    /* make sure the total of demand found exiting matches the originally registered total root demand */
    if (!Precision.equal(foundRootDemandPcuH, originDemandPcuH)) {
      LOGGER.severe(String.format("Combined flows (%.2f) on bush root for origin %s do not add up to the origin's travel demand (%.2f)", foundRootDemandPcuH,
          getOrigin().getXmlId(), originDemandPcuH));
    }
    return splittingRates;
  }

  /**
   * Remove a turn from the bush by removing it from the acyclic graph and removing any data associated with it
   * 
   * @param fromEdgeSegment of the turn
   * @param toEdgeSegment   of the turn
   */
  public void removeTurn(final EdgeSegment fromEdgeSegment, final EdgeSegment toEdgeSegment) {
    bushData.removeTurn(fromEdgeSegment, toEdgeSegment);

    /* update graph if entry/exit segment is now unused as well */
    if (!Precision.positive(getSendingFlowPcuH(toEdgeSegment))) {
      dag.removeEdgeSegment(toEdgeSegment);
    }
    if (!Precision.positive(getSendingFlowPcuH(fromEdgeSegment))) {
      dag.removeEdgeSegment(fromEdgeSegment);
    }
    requireTopologicalSortUpdate = true;
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
   * The alternative subpath (not in this bush) is provided as link segment labels of value -1. The end point at which they merge with the bush is indicated with label 1 at the
   * downstream bush vertex (passed in). Here we do a breadth-first search on the bush in the upstream direction to find a location the alternative path diverged from the bush,
   * which, at the latest, should be at the origin and at the earliest directly upstream of the provided vertex.
   * <p>
   * Note that the breadth-first approach is a choice not a necessity but the underlying idea is that a shorter PAS (which is likely to be found) is used by more origins and
   * therefore more useful to explore than a really long PAS. This is preferred - in the original TAPAS - over simply backtracking along either the shortest or longest path of the
   * min-max tree which would also be viable options,a s would a depth-first search.
   * <p>
   * Consider implementing various strategies here in order to explore what works best but for now we adopt a breadth-first search
   * <p>
   * The returned map contains the outgoing edge segment for each vertex, from the diverge to the merge node where for the merge node the edge segment remains null
   * 
   * @param mergeVertex                    to start backward breadth first search from as it is the point of merging between alternative path (via labelled vertices) and bush
   * @param alternativeSubpathVertexLabels indicating the shortest (network) path merging at the bush vertex but not part of the bush's path to the merge vertex for some part of
   *                                       its path prior to merging
   * @return vertex at which the two paths diverged upstream and the map to extract the path from the diverge vertex to the merge vertex that was found using the breadth-first
   *         method
   */
  public Pair<DirectedVertex, Map<DirectedVertex, EdgeSegment>> findBushAlternativeSubpath(DirectedVertex mergeVertex, final short[] alternativeSubpathVertexLabels) {
    ArrayDeque<Pair<DirectedVertex, EdgeSegment>> openVertexQueue = new ArrayDeque<Pair<DirectedVertex, EdgeSegment>>(30);
    Map<DirectedVertex, EdgeSegment> processedVertices = new TreeMap<DirectedVertex, EdgeSegment>();

    /* start with incoming edge segments of merge vertex except alternative labelled segment */
    processedVertices.put(mergeVertex, null);
    for (var entrySegment : mergeVertex.getEntryEdgeSegments()) {
      if (containsEdgeSegment(entrySegment) && alternativeSubpathVertexLabels[(int) mergeVertex.getId()] != -1) {
        openVertexQueue.add(Pair.of(entrySegment.getUpstreamVertex(), entrySegment));
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
      for (var entrySegment : currentVertex.getEntryEdgeSegments()) {
        if (containsEdgeSegment(entrySegment) && bushData.containsTurnSendingFlow(entrySegment, current.second())) {
          if (!processedVertices.containsKey(entrySegment.getUpstreamVertex())) {
            openVertexQueue.add(Pair.of(entrySegment.getUpstreamVertex(), entrySegment));
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
    LOGGER.warning(String.format("Cycle found when finding alternative subpath on bush for origin zone %s merging at vertex %s", getOrigin().getXmlId(), mergeVertex.getXmlId()));
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
   * Determine the sending flow between origin,destination vertex using the subpath given by the subPathArray in order from start to finish. We utilise the initial sending flow on
   * the first segment as the base flow which is then followed along the subpath through the bush splitting rates up to the final link segment
   * 
   * @param subPathArray to extract path from
   * @return sendingFlowPcuH between start and end vertex following the sub-path
   */
  public double determineSubPathSendingFlow(final EdgeSegment[] subPathArray) {

    int index = 0;
    EdgeSegment currEdgeSegment = subPathArray[index++];
    double subPathSendingFlow = bushData.getTotalSendingFlowFromPcuH(currEdgeSegment);

    return determineSubPathSendingFlow(subPathSendingFlow, index, subPathArray);
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

    double subPathSendingFlow = bushData.getTotalSendingFlowFromPcuH(entrySegment);

    int index = 0;
    EdgeSegment currEdgeSegment = subPathArray[index++];
    subPathSendingFlow *= bushData.getSplittingRate(entrySegment, currEdgeSegment);

    return determineSubPathSendingFlow(subPathSendingFlow, index, subPathArray);
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
    Collection<DirectedVertex> topSortedVertices = getTopologicallySortedVertices();
    var vertexIter = topSortedVertices.iterator();
    var currVertex = vertexIter.next();

    /* pass over bush in topological order updating turn sending flows based on flow acceptance factors */
    while (vertexIter.hasNext()) {
      currVertex = vertexIter.next();
      for (var entrySegment : currVertex.getEntryEdgeSegments()) {
        if (!containsEdgeSegment(entrySegment)) {
          continue;
        }

        var usedLabels = getFlowCompositionLabels(entrySegment);
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
            for (var exitLabel : exitLabels) {
              Double bushExitSegmentLabelSplittingRate = splittingRates.get(exitSegment, exitLabel);
              if (bushExitSegmentLabelSplittingRate != null && Precision.positive(bushExitSegmentLabelSplittingRate)) {
                double bushTurnLabeledAcceptedFlow = entryLabelAcceptedFlow * bushExitSegmentLabelSplittingRate;
                bushData.setTurnSendingFlow(entrySegment, entrylabel, exitSegment, exitLabel, bushTurnLabeledAcceptedFlow);
              }
            }
          }
        }
      }
    }
  }

  /**
   * Topologically sorted vertices of the bush
   * 
   * @return vertices
   */
  public Collection<DirectedVertex> getTopologicallySortedVertices() {
    return dag.topologicalSort(requireTopologicalSortUpdate);
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
   * Collect the total travel demand for this origin bush
   * 
   * @return travel demand
   */
  public double getTravelDemandPcuH() {
    return originDemandPcuH;
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
