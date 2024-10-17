package org.goplanit.assignment.ltm.sltm;

import java.util.Iterator;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.goplanit.algorithms.shortest.MinMaxPathResult;
import org.goplanit.algorithms.shortest.ShortestPathAcyclicMinMaxGeneralised;
import org.goplanit.algorithms.shortest.ShortestSearchType;
import org.goplanit.utils.arrays.ArrayUtils;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.misc.IterableUtils;
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

    final TreeMap<EdgeSegment, Double> removedLinkFlow = new TreeMap<>();

    /* traverse form origin->destination */
    forEachTopologicalSortedVertex(isInverted(), currVertex -> {
      for (var exitSegment : currVertex.getExitEdgeSegments()) {
        if(!containsEdgeSegment(exitSegment)){
          continue; // next vertex
        }

        // check if any preceding link flow was removed as a result of a threshold violation (see below).
        // if so, propagate this removal of flow before assessing if link is eligible for removal
        boolean initiateShift = true;
        if(!removedLinkFlow.isEmpty()){
          double removedExitSegmentIncomingFlow = 0;
          for (var entrySegment : currVertex.getEntryEdgeSegments()) {
            Double removedEntryLinkFlow = removedLinkFlow.get(entrySegment);
            if(removedEntryLinkFlow==null){
              continue;
            }
            // a) determine the removed turn flow that reaches the exit segment: as the below sending flow is based on outgoing flow,
            // not incoming flow, the turn removal is not sufficient to bring this number down and propagate the flow removal
            double turnSendingFlow = getTurnSendingFlow(entrySegment, exitSegment);
            double turnAcceptedFlow = turnSendingFlow * flowAcceptanceFactors[(int)entrySegment.getId()];
            removedExitSegmentIncomingFlow += turnAcceptedFlow;
            // b) remove turn into this link
            removeTurn(entrySegment, exitSegment);
            if(detailedLogging){
              LOGGER.info(String.format(
                      "Implicit branch shift for too low flows: Removed turn (flow: %.10f) from (%s) to (%s) from bush (%s)",
                      turnSendingFlow,
                      entrySegment.getIdsAsString(),
                      exitSegment.getIdsAsString(),
                      getRootZoneVertex().getParent().getParentZone().getIdsAsString()));
            }
          }

          if(removedExitSegmentIncomingFlow > 0){
            // now propagate the removed flow from the turn sending flows of the exit segment, which in turn affects the
            // total sending flow on the exit link so that we can use it to assess whether to remove it if it drops below the threshold.
            int exitExitIndex = 0;
            double[] exitExitSplittingRates = bushData.getSplittingRates(exitSegment);
            for (var exitSegmentExitSegment : exitSegment.getDownstreamVertex().getExitEdgeSegments()) {
              double exitExitSplittingRate = exitExitSplittingRates[exitExitIndex++];
              if(exitExitSplittingRate<=0){
                continue;
              }
              double exitSegmentTurnFlowToRemove = removedExitSegmentIncomingFlow * exitExitSplittingRate;
              // adjust turn flow based on upstream removed link flow
              bushData.addTurnSendingFlow(exitSegment, exitSegmentExitSegment, -exitSegmentTurnFlowToRemove);
            }
            // current exit segment is continuation of earlier shift, so not a point of initiation
            initiateShift = false;
          }
        }

        // the remaining total sending flow on the exit segment (adjusted with any removed upstream flow)
        double totalInflowPcuH = bushData.getTotalSendingFlowFromPcuH(exitSegment);
        if(totalInflowPcuH >= flowThreshold) {
          continue;
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
        double totalAlternativeBranchFlows = IterableUtils.asStream(exitSegment.getUpstreamVertex().getExitEdgeSegments()).filter(es -> es != exitSegment).mapToDouble(
                es -> bushData.getTotalAcceptedFlowToPcuH(es, flowAcceptanceFactors)).sum();
        if(initiateShift && totalAlternativeBranchFlows <= 0){
          // not other branch available to initiate a branch shift, so we must maintain this flow despite it being low
          // this can only happen on corridor with alphas < 1 such that flow reduces below threshold halfway but without an
          // option to divert. Any other situation should not occur.
          continue;
        }

        // candidate with "too low" flow found --> perform implicit branch shift

        //1. remove all turn sending flows into the link
        for (var entrySegment : currVertex.getEntryEdgeSegments()) {
          double turnFlow = getTurnSendingFlow(entrySegment, exitSegment);
          if(turnFlow <= 0){
            continue;
          }

          removeTurn(entrySegment, exitSegment);

          // do not only remove turn, but move the removed flow to other turns proportionally to avoid losing origin flow
          // only required at origins in al lother cases it gets fixed automatically with network loading in next iteration
          if(entrySegment.getUpstreamVertex() instanceof CentroidVertex &&
                  getOriginVertices().contains((CentroidVertex)entrySegment.getUpstreamVertex())) {
            var splittingRates = bushData.getSplittingRates(entrySegment);
            int index = 0;
            for(var connectoidExitSegment : currVertex.getExitEdgeSegments()){
              var splittingRate = splittingRates[index++];
              if(splittingRate > 0){
                bushData.addTurnSendingFlow(entrySegment, connectoidExitSegment, turnFlow * splittingRate);
                if(detailedLogging){
                  LOGGER.info(String.format(
                          "Implicit branch shift for too low flows: shifted connectoid (%s) flow: %.10f) from exit link (%s) to other exit link (%s) from bush (%s)",
                          entrySegment.getIdsAsString(),
                          turnFlow * splittingRate,
                          exitSegment.getIdsAsString(),
                          connectoidExitSegment.getIdsAsString(),
                          getRootZoneVertex().getParent().getParentZone().getIdsAsString()));
                }
              }
            }
          }else if(detailedLogging){
            LOGGER.info(String.format(
                    "Implicit branch shift for too low flows: Removed turn (flow: %.10f) from (%s) to (%s) from bush (%s)",
                    turnFlow,
                    entrySegment.getIdsAsString(),
                    exitSegment.getIdsAsString(),
                    getRootZoneVertex().getParent().getParentZone().getIdsAsString()));
          }
        }

        //2. propagate removal of the link's flow in case it should lead to downstream removal of more link segments which
        //   may be required to avoid dangling links within the bush
        //   Note: since we are removing turns on-the-fly which affects the topological order, we should not create another
        //         topological iterator at this point as the bush's state is in flux and may be invalid temporarily. therefore
        //         we will track the to be removed flow as we go and deal with it while traversing the bush instead
        removedLinkFlow.put(exitSegment, totalInflowPcuH);
      }
    });

    if(detailedLogging && !removedLinkFlow.isEmpty()){
      LOGGER.info(String.format(
              "Applied implicit branch shift for too low flows: Removed link segments (%s) from bush (%s)",
              removedLinkFlow.keySet().stream().map(es -> "["+es.getIdsAsString()+"]").collect(Collectors.joining(",")),
              getRootZoneVertex().getParent().getParentZone().getIdsAsString()));

    }
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
