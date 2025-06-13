package org.goplanit.assignment.ltm.sltm;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.goplanit.algorithms.shortest.MinMaxPathResult;
import org.goplanit.algorithms.shortest.ShortestPathAcyclicMinMaxGeneralised;
import org.goplanit.algorithms.shortest.ShortestSearchType;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.misc.IterableUtils;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.network.virtual.graph.CentroidVertex;

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
    if(!destination.isSinkVertex() || destination.isSourceVertex()){
      throw new PlanItRunTimeException(
              "Destination bush does not have a sink centroid vertex as its root, this is not allowed");
    }
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
   * collect destination of this bush
   *
   * @return destination zone
   */
  public CentroidVertex getDestination() {
    return this.destination;
  }

  /**
   * Compute the min-max path tree rooted at the destination towards all origins given the provided (network wide)
   * costs. The provided costs are at the network level so should contain all the segments active in the bush
   *
   * @param excludeZeroFlowLinkSegmentsFromMaxPaths when true we do not consider link segments with zero flow when
   *                                                constructing max paths.
   * @param linkSegmentCosts              to use
   * @param totalTransportNetworkVertices number of vertices in overall network needed to be able to construct result
   *                                      per vertex based on id
   * @return minMaxPathResult, null if unable to complete
   */
  @Override
  public MinMaxPathResult computeMinMaxShortestPaths(boolean excludeZeroFlowLinkSegmentsFromMaxPaths,
          final double[] linkSegmentCosts, final int totalTransportNetworkVertices) {

    //todo: excludeZeroFlowLinkSegmentsFromMaxPaths ignored, see conjugate on how to use

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
  public void remove(EdgeSegment edgeSegment) {
    for(var exit : edgeSegment.getDownstreamVertex().getExitEdgeSegments()) {
      bushData.removeTurn(edgeSegment,exit);
    }
    if(getDag().containsEdgeSegment(edgeSegment)) {
      getDag().removeEdgeSegment(edgeSegment);
      requireTopologicalSortUpdate = true;
    }
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

  @Override
  public <T> double determineConstrainedSubPathSendingFlow(
      EdgeSegment[] subPathArray,
      double[] onTheFlyFlowAcceptanceFactors,
      double[] nlNonConjugateFlowAcceptanceFactors,
      T bushConstrainedFlowData) {
    throw new PlanItRunTimeException("determineConstrainedSubPathSendingFlow not yet implemented in DestinationBush");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TreeSet<EdgeSegment> performLowFlowBranchShifts(
          double flowThreshold, double[] flowAcceptanceFactors, boolean detailedLogging){

    // removed turn flows with multikey being entry and exit segment
    final MultiKeyMap<Object, Double> removedTurnFlows = new MultiKeyMap<>();
    // a removed turn flow does not mean a removed edge segment necessarily (as we avoid performing a full bush update
    // as a result, so track removed edge segments separately. We do so because removed edge segments allow us to deregister
    // bushes from PASs that have this edge segment (but after branch shift this bush is no longer eleigible for the PAS)
    final TreeSet<EdgeSegment> removedEdgeSegments = new TreeSet<>();

    /* traverse form origin->destination */
    forEachTopologicalSortedVertex(isInverted(), currVertex -> {

      Map<EdgeSegment, Double> exitSegmentsWithRemovedIncomingFlows = new TreeMap<>();
      for (var exitSegment : currVertex.getExitEdgeSegments()) {
        if (!contains(exitSegment)) {
          continue; // next vertex
        }
        if (exitSegment.getDownstreamVertex() instanceof CentroidVertex) {
          //todo not ideal in case we have a continuing shift that should remove this connector becuase it is no longer used
          // should gennerally not happen but it could... Conversely we don't want to remove ways to a destination
          continue;
        }

        // check if any preceding link flow was removed as a result of a threshold violation (see below).
        // if so, propagate this removal of flow before assessing if link is eligible for removal
        if (!removedTurnFlows.isEmpty()) {
          for (var entrySegment : currVertex.getEntryEdgeSegments()) {
            if (removedTurnFlows.keySet().stream().noneMatch(e -> e.getKey(1).equals(entrySegment)) ||
                    !containsTurnSendingFlow(entrySegment, exitSegment)) {
              continue;
            }
            double removedPortionIntoExit = bushData.getSplittingRate(entrySegment, exitSegment);
            double removedTotalOnEntry = removedTurnFlows.entrySet().stream().filter(
                    e -> e.getKey().getKey(1).equals(entrySegment)).mapToDouble(Map.Entry::getValue).sum();
            // incoming flow removed into this exit as a result of branch shift, track what was removed in total going
            // into current exit segment
            exitSegmentsWithRemovedIncomingFlows.put(exitSegment, exitSegmentsWithRemovedIncomingFlows.getOrDefault(exitSegment, 0.0) +
                    flowAcceptanceFactors[(int) exitSegment.getId()] * removedPortionIntoExit * removedTotalOnEntry);
          }
        }
      }

      // now determine which exit segments are eligible for removal (may be multiple) and if they are initiating a new
      // branch shift or not (when initiating a new shift, this may indicate merging with a continuing one, but this is dealt
      // with after)
      Map<EdgeSegment, Boolean> exitSegmentToRemove = new TreeMap<>();
      Set<EdgeSegment> exitSegmentsToTerminateTrackingButFinaliseUpstreamRemovals = new TreeSet<>();
      for (var exitSegment : currVertex.getExitEdgeSegments()) {
        double totalInflowPcuH = bushData.getTotalAcceptedFlowToPcuH(exitSegment, flowAcceptanceFactors);
        if(totalInflowPcuH<=0){
          continue;
        }

        // test for eligibility of removal based on the total inflow into the exit segment
        // (adjusted with any removed upstream flow)
        double removedExitSegmentIncomingFlow = exitSegmentsWithRemovedIncomingFlows.getOrDefault(exitSegment, 0.0);
        if ((totalInflowPcuH - removedExitSegmentIncomingFlow) < flowThreshold) {
          // below threshold hold, so initiate (or continue) a branch merge. Check if (new) flow has been merged into this link
          // from other incoming links, because if so, it is a new branch shift (possibly in addition to a continuing one)
          boolean initiateNewShift = IterableUtils.asStream(currVertex.getEntryEdgeSegments()).filter(
                  es -> removedTurnFlows.keySet().stream().noneMatch(k -> k.getKey(1).equals(es))).anyMatch(
                  es -> containsTurnSendingFlow(es, exitSegment));
          exitSegmentToRemove.put(exitSegment, initiateNewShift);
        } else if (removedExitSegmentIncomingFlow > 0 /* but above threshold for removal*/) {
            exitSegmentsToTerminateTrackingButFinaliseUpstreamRemovals.add(exitSegment);
        }
      }


      // perform continuation/new branch shift on nominated exit segments when eligible
      for (var candidate : exitSegmentToRemove.entrySet()) {
        EdgeSegment lowFlowSegment = candidate.getKey();
        boolean initiateNewShift = candidate.getValue();

        // safety --> we can only initiate an implicit shift if there is an alternative flow into another exit segment available
        // that is not a candidate for removal. if not then we cannot remove this flow for an implicit shift to another branch, so check this availability
        // note: In case we allow multiple branch shifts per bush we must exclude any removed segments from this selection as their turn sending flows may not have been removed yet
        // (this is triggered by the exit link, rather than the removed entry). As a result we should disallow multiple branch shifts per bush er iteration to avoid
        // such complexities (also because we not fully propagate the shift anyway potentially cuasing other problems).
        var alternativeUsedExitSegmentFlows = IterableUtils.asStream(lowFlowSegment.getUpstreamVertex().getExitEdgeSegments()).filter(
            es -> !exitSegmentToRemove.containsKey(es)).map(es ->
                Pair.of(es, bushData.getTotalAcceptedFlowToPcuH(es, flowAcceptanceFactors))).collect(Collectors.toList());
        if (initiateNewShift && alternativeUsedExitSegmentFlows.stream().mapToDouble(Pair::second).sum() <= 0) {
          // no other branch available to reallocate flow to, so we must maintain this flow despite it being low
          // this can happen 1) halfway along a corridor with alphas < 1 such that flow reduces below threshold halfway but without an
          // option to divert. Since, we may alos have conintuing removals at the same time, we flag the exit for temrination
          // in case this happens
          exitSegmentsToTerminateTrackingButFinaliseUpstreamRemovals.add(lowFlowSegment);
          continue;
        }

        // WHEN REACING THIS POINT WE ARE: continuing an existing or initiating a new branch shift on threshold compliant
        // segment that is to be removed...

        //remove edge segment explicitly, because otherwise it may not be removed if it still
        // has sending flow, but we can only deal with that later, so do it explicitly
        getDag().removeEdgeSegment(lowFlowSegment);
        removedEdgeSegments.add(lowFlowSegment);

        for (var entrySegment : currVertex.getEntryEdgeSegments()) {
          double turnFlow = getTurnSendingFlow(entrySegment, lowFlowSegment);
          if(turnFlow <= 0){
            continue;
          }

          // remove turn coming into this exit segment.
          boolean entryIsContinuingRemoval = !getDag().containsEdgeSegment(entrySegment);
          removeTurn(entrySegment,lowFlowSegment);
          if(!entryIsContinuingRemoval) {
            // when initiating a new shift, we only consider moving flow across from those incoming links that
            // were not already removed, i.e., the continuing portion of a branch shift cannot be redistributed because that
            // flow was already redistributed upstream

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
                double shiftedTurnFlow = turnFlow * splittingRate;
                addTurnSendingFlow(entrySegment, altExitSegment, shiftedTurnFlow);

                if (detailedLogging) {
                  LOGGER.info(String.format(
                      "%s branch shift for too low flows: shifted edge segment (%s) flow: %.10f) from exit link (%s) to other exit link (%s) from bush (%s)",
                          exitSegmentsWithRemovedIncomingFlows.getOrDefault(lowFlowSegment,0.0) > 0 ? "initiate additional" : "Initiate",
                      entrySegment.getIdsAsString(),
                      shiftedTurnFlow,
                          lowFlowSegment.getIdsAsString(),
                      altExitSegment.getIdsAsString(),
                      getRootZoneVertex().getParent().getParentZone().getIdsAsString()));
                }
              }
            }
          }else if(exitSegmentsWithRemovedIncomingFlows.getOrDefault(lowFlowSegment, 0.0) > 0){
            //  continuing existing branch shift, but not initiating a new one, which will continue tracking of the removed flows
            if (detailedLogging) {
              LOGGER.info(String.format(
                  "Continuing Implicit branch shift: shifted flow: %.10f, from edge segment (%s) to other exit segment (%s) from bush (%s)",
                  turnFlow,
                  entrySegment.getIdsAsString(),
                      lowFlowSegment.getIdsAsString(),
                  getRootZoneVertex().getParent().getParentZone().getIdsAsString()));
            }
          }

          // propagate removed link's flow in case it should lead to downstream removal of more link segments which
          //   may be required to avoid dangling links within the bush
          //   Note: since we are removing turns on-the-fly which affects the topological order, we should not create another
          //         topological iterator at this point as the bush's state is in flux and may be invalid temporarily. therefore
          //         we will track the to be removed flow as we go and deal with it here while traversing the bush instead
          removedTurnFlows.put(entrySegment, lowFlowSegment, turnFlow);
        }

      }

      // finalise by removal of preceding incoming turns from removed segments but without further tracking/removal required
      // because too much flow remains on exit segment despite removed incoming flow, so just remove the turns from removed
      // entry segments into this exit to finalise the shift but do not track removal propagation any further because
      // it is not required (no dangling links can occur)
      for (var nonCandidateWithPrecedingLowFlowremoval : exitSegmentsToTerminateTrackingButFinaliseUpstreamRemovals) {
        for (var entrySegment : currVertex.getEntryEdgeSegments()) {
          if (removedTurnFlows.keySet().stream().noneMatch(e -> e.getKey(1).equals(entrySegment)) ||
                  !containsTurnSendingFlow(entrySegment, nonCandidateWithPrecedingLowFlowremoval)) {
            continue;
          }
          removeTurn(entrySegment, nonCandidateWithPrecedingLowFlowremoval);
          if (detailedLogging) {
            LOGGER.info(String.format(
                    "Finalising branch shift; keep segment (%s) with above threshold flow : removed turn from edge segment (%s) into  (%s) from bush (%s)",
                    nonCandidateWithPrecedingLowFlowremoval.getIdsAsString(),
                    entrySegment.getIdsAsString(),
                    nonCandidateWithPrecedingLowFlowremoval.getIdsAsString(),
                    getRootZoneVertex().getParent().getParentZone().getIdsAsString()));
          }
        }
      }
    });

    if (detailedLogging && !removedEdgeSegments.isEmpty()) {
      removedEdgeSegments.forEach( es -> LOGGER.info(String.format(
              "Branch shift removed edge segment (%s)",es.getIdsAsString())));
    }
    return removedEdgeSegments;
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public String  toString() {
    String result = super.toString();
    return "Bush: destination zone: " + getDestination().getParent().getParentZone().getXmlId() + "\n" + result;
  }

}
