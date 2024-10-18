package org.goplanit.assignment.ltm.sltm;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.goplanit.algorithms.shortest.MinMaxPathResult;
import org.goplanit.algorithms.shortest.ShortestPathAcyclicMinMaxGeneralised;
import org.goplanit.algorithms.shortest.ShortestSearchType;
import org.goplanit.utils.arrays.ArrayUtils;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.misc.IterableUtils;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.network.virtual.CentroidVertex;
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
  protected final CentroidVertex destination;

  /**
   * Constructor
   * 
   * @param idToken                 the token to base the id generation on
   * @param destination             destination of the bush
   * @param maxSubGraphEdgeSegments The maximum number of edge segments the bush can at most register given the parent network it is a subset of
   */
  public DestinationBush(final IdGroupingToken idToken, CentroidVertex destination, long maxSubGraphEdgeSegments) {
    super(idToken, destination, true /* inverted */, maxSubGraphEdgeSegments);
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
    var minMaxBushPaths = new ShortestPathAcyclicMinMaxGeneralised(
            getDag(), requireTopologicalSortUpdate, linkSegmentCosts, totalTransportNetworkVertices);
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
  public Iterator<DirectedVertex> getTopologicalIterator() {
    boolean invertDirection = false; /* do not invert direction, dag is in d-o direction */
    return getDag().getTopologicalIterator(requireTopologicalSortUpdate, invertDirection);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Iterator<DirectedVertex> getInvertedTopologicalIterator() {
    boolean invertDirection = true; /* do invert direction, dag is in o-d direction */
    return getDag().getTopologicalIterator(requireTopologicalSortUpdate, invertDirection);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CentroidVertex getRootZoneVertex() {
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
  public void performLowFlowBranchShifts(double flowThreshold, double[] flowAcceptanceFactors, boolean detailedLogging){

    // entry exit segment
    final MultiKeyMap<Object, Double> removedTurnFlows = new MultiKeyMap<>();

    /* traverse form origin->destination */
    forEachTopologicalSortedVertex(isInverted(), currVertex -> {
      for (var exitSegment : currVertex.getExitEdgeSegments()) {
        if(!containsEdgeSegment(exitSegment)){
          continue; // next vertex
        }

        // check if any preceding link flow was removed as a result of a threshold violation (see below).
        // if so, propagate this removal of flow before assessing if link is eligible for removal
        boolean initiateShift = true;
        double removedExitSegmentIncomingFlow = 0;
        if(!removedTurnFlows.isEmpty()){
          for (var entrySegment : currVertex.getEntryEdgeSegments()) {
            if(removedTurnFlows.keySet().stream().noneMatch(e -> e.getKey(1).equals(entrySegment)) ||
                    !containsTurnSendingFlow(entrySegment, exitSegment)){
              continue;
            }
            // a) collect the removed entry link flow
            double removedTurnSendingFlow = removedTurnFlows.entrySet().stream().filter(
                    e -> e.getKey().getKey(1).equals(entrySegment)).mapToDouble(
                            Map.Entry::getValue).sum();
            if(removedTurnSendingFlow<=0){
              continue;
            }
            initiateShift = false;
          }
        }

        // the remaining total sending flow on the exit segment (adjusted with any removed upstream flow)
        double totalInflowPcuH = bushData.getTotalAcceptedFlowToPcuH(exitSegment, flowAcceptanceFactors);
        if(totalInflowPcuH >= flowThreshold || exitSegment.getDownstreamVertex() instanceof CentroidVertex) {
          continue;
        }

        if(!initiateShift){
          //already some flow removed from an entry link, check no new flow has been merged into this link from other
          // incoming links, because if so, we must treat this like a new branch shift and flip the flag back
          initiateShift = IterableUtils.asStream(currVertex.getEntryEdgeSegments()).filter(
                  es -> removedTurnFlows.keySet().stream().noneMatch(k -> k.getKey(1).equals(es))).anyMatch(
                          es -> containsTurnSendingFlow(es, exitSegment));
        }

        // special case, if no flow exits the link, we are at a destination, in which case sending flow is always zero
        // so sending flow is not a good indicator, instead we utilise the more costly accepted flow into the link instead
        // and redo the check
        if (ArrayUtils.sumOf(bushData.getSplittingRates(exitSegment)) <= 0) {
          totalInflowPcuH = bushData.getTotalAcceptedFlowToPcuH(exitSegment, flowAcceptanceFactors);
          if (totalInflowPcuH >= flowThreshold) {
            continue;
          }
        }

        // safety --> we can only initiate an implicit shift if there is an alternative flow into another exit segment available
        // if not then we cannot remove this flow for an implicit shift to another branch, so check this availability
        var alternativeUsedExitSegmentFlows = IterableUtils.asStream(exitSegment.getUpstreamVertex().getExitEdgeSegments()).filter(
                es -> es != exitSegment).map(es -> Pair.of(es, bushData.getTotalAcceptedFlowToPcuH(es, flowAcceptanceFactors))).collect(Collectors.toList());
        if(initiateShift && alternativeUsedExitSegmentFlows.stream().mapToDouble(Pair::second).sum()<=0){
          // no other branch available to reallocate flow to, so we must maintain this flow despite it being low
          // this can happen 1) halfway along a corridor with alphas < 1 such that flow reduces below threshold halfway but without an
          // option to divert.
          continue;
        }

        // candidate with "too low" flow found --> perform implicit branch shift

        //1. remove all turn sending flows into the link
        for (var entrySegment : currVertex.getEntryEdgeSegments()) {
          double turnFlow = getTurnSendingFlow(entrySegment, exitSegment);
          if(turnFlow <= 0){
            continue;
          }

          // remove turn flow.
          removeTurn(entrySegment,exitSegment);
          //remove edge segment explicitly, because otherwise it may not be removed if it still
          // has sending flow, but we can only deal with that later, so do it explicitly
          getDag().removeEdgeSegment(exitSegment);

          // redistribute based on used exit segment flow distribution, only needed when initiating a branch shift
          if(initiateShift) {
            // It may be that some entry segments have no current other used exit turns, while others do.
            // We therefore use the general distribution across exit segments as a proxy and shift
            // the removed turn flow to all these used exit segments (across all entries) to forcibly create
            // a used turn for such entries in case it does not exist (but already exists for other entries)
            double totalAcceptedExitFlow = alternativeUsedExitSegmentFlows.stream().mapToDouble(
                    Pair::second).reduce(0.0, Double::sum);
            Map<EdgeSegment, Double> altExitSegmentFlowSplittingRates = new TreeMap<>();
            alternativeUsedExitSegmentFlows.forEach(
                    e -> altExitSegmentFlowSplittingRates.put(e.first(), e.second() / totalAcceptedExitFlow));
            for (var altExitSegment : currVertex.getExitEdgeSegments()) {
              var splittingRate = altExitSegmentFlowSplittingRates.getOrDefault(altExitSegment, 0.0);
              if (splittingRate > 0) {
                double removedTurnFlow = turnFlow * splittingRate;
                addTurnSendingFlow(entrySegment, altExitSegment, turnFlow * splittingRate);

                //2. propagate removal of the link's flow in case it should lead to downstream removal of more link segments which
                //   may be required to avoid dangling links within the bush
                //   Note: since we are removing turns on-the-fly which affects the topological order, we should not create another
                //         topological iterator at this point as the bush's state is in flux and may be invalid temporarily. therefore
                //         we will track the to be removed flow as we go and deal with it while traversing the bush instead
                removedTurnFlows.put(entrySegment, exitSegment, removedTurnFlow);

                if (detailedLogging) {
                  LOGGER.info(String.format(
                          "Implicit branch shift for too low flows: shifted edge segment (%s) flow: %.10f) from exit link (%s) to other exit link (%s) from bush (%s)",
                          entrySegment.getIdsAsString(),
                          turnFlow * splittingRate,
                          exitSegment.getIdsAsString(),
                          altExitSegment.getIdsAsString(),
                          getRootZoneVertex().getParent().getParentZone().getIdsAsString()));
                }
              }
            }
          }
        }
      }
    });

//    if(detailedLogging && !removedTurnFlows.isEmpty()){
//      LOGGER.info(String.format(
//              "Applied implicit branch shift for too low flows: Removed link segments (%s) from bush (%s)",
//              removedTurnFlows.keySet().stream().map(es -> "["+es.getIdsAsString()+"]").collect(Collectors.joining(",")),
//              getRootZoneVertex().getParent().getParentZone().getIdsAsString()));
//
//    }
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    String result = super.toString();
    return "Bush: destination zone: " + getDestination().getParent().getParentZone().getXmlId() + "\n" + result;
  }

  /**
   * collect destination of this bush
   * 
   * @return destination zone
   */
  public CentroidVertex getDestination() {
    return this.destination;
  }

}
