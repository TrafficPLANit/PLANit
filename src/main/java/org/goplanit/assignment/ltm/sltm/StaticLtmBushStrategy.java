package org.goplanit.assignment.ltm.sltm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.logging.Logger;

import org.goplanit.algorithms.shortestpath.DijkstraShortestPathAlgorithm;
import org.goplanit.algorithms.shortestpath.OneToAllShortestPathAlgorithm;
import org.goplanit.algorithms.shortestpath.ShortestPathResult;
import org.goplanit.assignment.ltm.sltm.consumer.InitialiseBushEdgeSegmentDemandConsumer;
import org.goplanit.assignment.ltm.sltm.loading.StaticLtmLoadingBush;
import org.goplanit.assignment.ltm.sltm.loading.StaticLtmLoadingScheme;
import org.goplanit.cost.physical.AbstractPhysicalCost;
import org.goplanit.cost.physical.PhysicalCost;
import org.goplanit.cost.virtual.AbstractVirtualCost;
import org.goplanit.cost.virtual.VirtualCost;
import org.goplanit.gap.GapFunction;
import org.goplanit.gap.LinkBasedRelativeDualityGapFunction;
import org.goplanit.interactor.TrafficAssignmentComponentAccessee;
import org.goplanit.network.transport.TransportModelNetwork;
import org.goplanit.od.demand.OdDemands;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.graph.EdgeSegment;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.virtual.ConnectoidSegment;
import org.goplanit.zoning.Zoning;
import org.ojalgo.array.Array1D;

/**
 * Implementation to support a bush absed solution for sLTM
 * 
 * @author markr
 *
 */
public class StaticLtmBushStrategy extends StaticLtmAssignmentStrategy {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(StaticLtmBushStrategy.class.getCanonicalName());

  /** track bushes per origin (with non-zero demand) */
  private final Bush[] originBushes;

  /** track all unique PASs */
  private final PasManager pasManager;

  /**
   * Update gap. Unconventional gap function where we update the GAP based on PAS cost discrepancy. This is due to the impossibility of efficiently determining the network and
   * minimum path costs in a capacity constrained bush based setting. Instead we:
   * <p>
   * minimumCost PAS : s1 cost * SUM(s1 sending flow, s2 sending flow) measuredCost PAS: s1 sending flow * s1 cost + s2 sending flow * s2 cost
   * <p>
   * Sum the above over all PASs. Note that PASs can (partially) overlap, so the measured cost does likely not add up to the network cost
   * 
   * @param gapFunction   to use
   * @param pas           to compute for
   * @param s1SendingFlow of the PAS s1 segment
   * @param s2SendingFlow of the PAS s2 segment
   */
  private void updateGap(final LinkBasedRelativeDualityGapFunction gapFunction, final Pas pas, double s1SendingFlow, double s2SendingFlow) {
    gapFunction.increaseConvexityBound(pas.getAlternativeLowCost() * (s1SendingFlow + s2SendingFlow));
    gapFunction.increaseMeasuredCost(s1SendingFlow * pas.getAlternativeLowCost());
    gapFunction.increaseMeasuredCost(s2SendingFlow * pas.getAlternativeHighCost());
  }

  /**
   * Determine the adjusted flow shift by taking the proposed flow shift and and it by a designated amount based on the difference between the PAS alternative costs and the assumed
   * slack flow until capacity is reached on the PAS's S1 (low cost) segment
   * 
   * @param pas               at hand
   * @param proposedFlowShift to apply
   * @param slackFlow         that is expected
   * @return adjusted proposed flow shift (if any)
   */
  private double adjustFlowShiftBasedOnAvailableSlackFlow(final Pas pas, double proposedFlowShift, double slackFlow) {
    if (proposedFlowShift <= slackFlow) {
      return proposedFlowShift;
    }

    /*
     * We cannot shift all flow without having to assume part of it triggers congestion somewhere. We only partly shift the presumed congested portion of flow we would like to
     * shift. Since we do not know the derivative of travel time since the congestion does not yet exists, we instead use the ratio of the low and high cost segments such that the
     * closer the costs are to equilibrium, the less we shift, whereas if there is a big difference we shift more aggressively.
     */
    double assumedUncongestedShift = slackFlow;
    double assumedCongestedShift = proposedFlowShift - slackFlow;
    // TODO: replace by virtual derivative where we assume outflow rate of (s2Shiftableflow+slackFlow)
    double appliedCongestedShiftPortion = 0.01;// (1 - pas.getAlternativeLowCost() / pas.getAlternativeHighCost());
    return assumedUncongestedShift + assumedCongestedShift * appliedCongestedShiftPortion;
  }

  /**
   * For the given PAS determine the amount of slack flow on its cheaper alternative, i.e., the minimum difference between the link outflow rate and the capacity across all its
   * link segments, including the link segments beyond its alternative it is directing the flows to. It is assumed the cheap cost alternative of the PAS has already been found to
   * be uncongested and as such should have a zero or higher slack flow.
   * <p>
   * In the special case that it passes through (or directs to) a segment that is at capacity (due to for example one or more of its other in-links being congested), then we return
   * a slack capacity of zero. In that case the caller of this method should likely still move some flow, but must assume that all shifted flow immediately causes congestion
   * 
   * 
   * @param pas            PAS to determine slack for
   * @param networkLoading to collect outflow rates from
   * @return pair of slack flow and slack capacity ratio
   */
  private double determineS1SlackFlow(Pas pas, StaticLtmLoadingBush networkLoading) {
    var lastS2Segment = pas.getLastEdgeSegment(false);
    double slackFlow = Double.POSITIVE_INFINITY;

    Array1D<Double> splittingRates = networkLoading.getSplittingRateData().getSplittingRates(lastS2Segment);

    int index = 0;
    int linkSegmentId = -1;

    for (var exitSegment : lastS2Segment.getDownstreamVertex().getExitEdgeSegments()) {
      double splittingRate = splittingRates.get(index);
      if (splittingRate > 0) {
        linkSegmentId = (int) exitSegment.getId();
        /* do not use outflows directly because they are only available on potentially blocking nodes in point queue basic solution scheme */
        double outflow = networkLoading.getCurrentInflowsPcuH()[linkSegmentId] * networkLoading.getCurrentFlowAcceptanceFactors()[linkSegmentId];
        /* since only a portion is directed to this out link, we can multiply the slack with the reciprocal of the splitting rate */
        double scaledSlackFlow = (1 / splittingRate) * ((MacroscopicLinkSegment) exitSegment).getCapacityOrDefaultPcuH() - outflow;
        slackFlow = Math.min(slackFlow, scaledSlackFlow);
      }
      ++index;
    }

    MacroscopicLinkSegment linkSegment = null;
    EdgeSegment[] s1 = pas.getAlternative(true);
    for (index = 0; index < s1.length; ++index) {
      linkSegment = (MacroscopicLinkSegment) s1[index];
      linkSegmentId = (int) linkSegment.getId();
      /* do not use outflows directly because they are only available on potentially blocking nodes in point queue basic solution scheme */
      double outflow = networkLoading.getCurrentInflowsPcuH()[linkSegmentId] * networkLoading.getCurrentFlowAcceptanceFactors()[linkSegmentId];
      double currSlackflow = linkSegment.getCapacityOrDefaultPcuH() - outflow;
      if (Precision.smaller(currSlackflow, slackFlow)) {
        slackFlow = currSlackflow;
      }
    }

    return slackFlow;
  }

  /**
   * For the given PAS determine the flow shift to apply from the high cost to the low cost segment. Depending on the state of the segments we utilise their derivatives of travel
   * time towards flow to determine the optimal shift. In case one or both segments are uncongested, we shift as much as possible conditional on the available slack for when we
   * would expect the segment to transition to congestion.
   * 
   * @param pas                       to determine shift for
   * @param s2ShiftableFlow           maximum shiftable flow
   * @param theMode                   we're considering
   * @param physicalCost              to use (for performance)
   * @param virtualCost               to use (for performance)
   * @param networkLoading            to use (for performance)
   * @param firstCongestedLinkSegment to use (for performance)
   * @return amount of flow to shift
   */
  private double determineFlowShift(final Pas pas, double s2ShiftableFlow, final Mode theMode, final PhysicalCost physicalCost, final VirtualCost virtualCost,
      final StaticLtmLoadingBush networkLoading, Predicate<EdgeSegment> firstCongestedLinkSegment) {

    /* obtain derivatives of traveltime towards flow for PAS segments. */
    // TODO: Currently requires instanceof, so benchmark if not too slow
    double denominatorS2 = 0;
    double denominatorS1 = 0;

    var firstS2CongestedLinkSegment = pas.matchFirst(false /* high cost */, firstCongestedLinkSegment);
    var firstS1CongestedLinkSegment = pas.matchFirst(true, /* low cost */ firstCongestedLinkSegment);

    if (firstS1CongestedLinkSegment == null) {
      // cheap option not congested, derivative of zero
      denominatorS1 = 0;
    } else {
      if (firstS1CongestedLinkSegment instanceof MacroscopicLinkSegment) {
        denominatorS1 = physicalCost.getDTravelTimeDFlow(false, theMode, (MacroscopicLinkSegment) firstS1CongestedLinkSegment);
      } else if (firstS1CongestedLinkSegment instanceof ConnectoidSegment) {
        denominatorS1 = virtualCost.getDTravelTimeDFlow(false, theMode, (ConnectoidSegment) firstS1CongestedLinkSegment);
      }
    }

    /* cheap option congested, determine flow shift based on its derivative towards travel time: We are equating the travel times between segments: */
    // tauw_s1 + dtauw_s1/ds_1 * (-flowShift) = tauw_s2 + dtauw_s2/ds_2 * (flowShift) we find:
    // flowShift = (tauw_s2-tauw_s1)/(1/v_s1_first_bottleneck + 1/v_s2_first_bottleneck))

    if (firstS2CongestedLinkSegment == null) {
      /* expensive option not congested, derivative of zero */
      denominatorS2 = 0;
    } else {
      if (firstS2CongestedLinkSegment instanceof MacroscopicLinkSegment) {
        denominatorS2 = physicalCost.getDTravelTimeDFlow(false, theMode, (MacroscopicLinkSegment) firstS2CongestedLinkSegment);
      } else if (firstS2CongestedLinkSegment instanceof ConnectoidSegment) {
        denominatorS2 = virtualCost.getDTravelTimeDFlow(false, theMode, (ConnectoidSegment) firstS2CongestedLinkSegment);
      }
    }

    double denominator = denominatorS2 + denominatorS1;

    Double slackFlow = null;
    if (!Precision.positive(denominatorS1) || !Precision.positive(denominatorS2)) {
      slackFlow = determineS1SlackFlow(pas, networkLoading);
    }

    if (!Precision.positive(denominator)) {
      /* neither alternative is congested, determine flow shift via slack */
      return adjustFlowShiftBasedOnAvailableSlackFlow(pas, s2ShiftableFlow, slackFlow);
    }

    double numerator = pas.getAlternativeHighCost() - pas.getAlternativeLowCost();
    double flowShift = Math.min(s2ShiftableFlow, numerator / denominator);

    if (slackFlow != null) {
      /*
       * when one of the alternatives has no derivative, we also adjust the flow shift (if deemed needed) based on the slack flow. this only occurs when we must assume the shift
       * would trigger congestion. Note this severely reduces the risk of flip-flopping due to the discontinuous nature of the travel time function
       */
      flowShift = adjustFlowShiftBasedOnAvailableSlackFlow(pas, flowShift, slackFlow);
    } else {

      /* debug only, test if shift solves travel time discrepancy, to be removed when it works */
      double diff = (pas.getAlternativeLowCost() + denominatorS1 * flowShift) - (pas.getAlternativeHighCost() + denominatorS2 * -flowShift);
      if (Precision.notEqual(diff, 0.0)) {
        LOGGER.severe("Computation of using derivatives to shift flows between PAS segments does not result in equal travel time after shift, this should not happen");
      }
    }

    return flowShift;
  }

  /**
   * Check if an existing PAS exists that terminates at the given (bush) merge vertex. If so, it is considered a match when:
   * <ul>
   * <li>The cheap alternative ends with a link segment that is not part of the bush (Assumed true, to be checked beforehand)</li>
   * <li>The expensive alternative overlaps with the bush (has non-zero flow)</li>
   * <li>It is considered an improvement, i.e., effective based on the settings in terms of cost and flow</li>
   * 
   * When this holds, accept this PAS as a decent enough alternative to the true shortest path (which its cheaper segment might or might not overlap with, as long as it is close
   * enough to the potential reduced cost we'll take it to avoid exponential growth of PASs)
   * 
   * @param originBush  to consider
   * @param mergeVertex where we identified a potential reduced cost compared to current bush
   * @param reducedCost between the shorter path and current shortest path in the bush
   * 
   * @return true when a match is found and bush is newly registered on a PAS, false otherwise
   */
  private boolean extendBushWithSuitableExistingPas(final Bush originBush, final DirectedVertex mergeVertex, final double reducedCost) {

    boolean bushFlowThroughMergeVertex = false;
    for (var entrySegment : mergeVertex.getEntryEdgeSegments()) {
      for (var exitSegment : mergeVertex.getExitEdgeSegments()) {
        if (originBush.containsTurnSendingFlow(entrySegment, exitSegment)) {
          bushFlowThroughMergeVertex = true;
          break;
        }
      }
      if (bushFlowThroughMergeVertex) {
        break;
      }
    }

    if (!bushFlowThroughMergeVertex) {
      // TODO: when we find this condition never occurs (and it shouldn't, remove the above checks as they are costly)
      LOGGER.warning(String.format("Explored vertex %s for existing PAS match even though bush has not flow passing through it. This should not happen", mergeVertex.getXmlId()));
      return false;
    }

    double[] alphas = getLoading().getCurrentFlowAcceptanceFactors();
    Pas effectivePas = pasManager.findFirstSuitableExistingPas(originBush, mergeVertex, alphas, reducedCost);
    if (effectivePas == null) {
      return false;
    }

    /*
     * found -> register origin, shifting of flow occurs when updating pas, extending bush with low cost segment occurs automatically when shifting flow later (flow is added to low
     * cost link segments which will be created if non-existent on bush)
     */
    effectivePas.registerOrigin(originBush);
    return true;
  }

  /**
   * Try to create a new PAS for the given bush and the provided merge vertex. If a new PAS can be created given that it is considered sufficiently effective the origin is
   * registered on it.
   * 
   * @param originBush      to identify new PAS for
   * @param mergeVertex     at which the PAS is supposed to terminate
   * @param networkMinPaths the current network shortest path tree
   * @return new created PAS if successfully created, null otherwise
   */
  private Pas extendBushWithNewPas(final Bush originBush, final DirectedVertex mergeVertex, final ShortestPathResult networkMinPaths) {

    Pas newPas = null;

    /* Label all vertices on shortest path origin-bushVertex as -1, and PAS merge Vertex itself as 1 */
    final short[] alternativeSegmentVertexLabels = new short[getTransportNetwork().getNumberOfVerticesAllLayers()];
    alternativeSegmentVertexLabels[(int) mergeVertex.getId()] = 1;
    int shortestPathLength = networkMinPaths.forEachBackwardEdgeSegment(originBush.getOrigin().getCentroid(), mergeVertex,
        (edgeSegment) -> alternativeSegmentVertexLabels[(int) edgeSegment.getUpstreamVertex().getId()] = -1);

    /* Use labels to identify when it merges again with bush (at upstream diverge point) */
    Pair<DirectedVertex, Map<DirectedVertex, EdgeSegment>> highCostSegment = originBush.findBushAlternativeSubpath(mergeVertex, alternativeSegmentVertexLabels);
    if (highCostSegment == null) {
      /* likely cycle detected on bush for merge vertex, unable to identify higher cost segment for NEW PAS, log issue */
      LOGGER.info(String.format("Unable to create new PAS for origin zone %s, despite shorter path found on network to vertex %s", originBush.getOrigin().getXmlId(),
          mergeVertex.getXmlId()));
      return null;
    }

    /* create the PAS and register origin bush on it */
    boolean truncateSpareArrayCapacity = true;
    EdgeSegment[] s1 = PasManager.createSubpathArrayFrom(highCostSegment.first(), mergeVertex, networkMinPaths, shortestPathLength, truncateSpareArrayCapacity);
    EdgeSegment[] s2 = PasManager.createSubpathArrayFrom(highCostSegment.first(), mergeVertex, highCostSegment.second(), shortestPathLength, truncateSpareArrayCapacity);
    newPas = pasManager.createNewPas(originBush, s1, s2);

    /* make sure all nodes along the PAS are tracked on the network level, for splitting rate/sending flow/acceptance factor information */
    getLoading().activateNodeTrackingFor(newPas);

    return newPas;
  }

  /**
   * Create a network wide shortest path algorithm based on provided costs
   * 
   * @param linkSegmentCosts to use
   * @return one-to-all shortest path algorithm
   */
  private OneToAllShortestPathAlgorithm createNetworkShortestPathAlgo(final double[] linkSegmentCosts) {
    final int numberOfEdgeSegments = getTransportNetwork().getNumberOfEdgeSegmentsAllLayers();
    final int numberOfVertices = getTransportNetwork().getNumberOfVerticesAllLayers();
    return new DijkstraShortestPathAlgorithm(linkSegmentCosts, numberOfEdgeSegments, numberOfVertices);
  }

  /**
   * Initialise bushes. Find shortest path for each origin and add the links to the bush
   * 
   * @param linkSegmentCosts costs to use
   * @throws PlanItException thrown when error
   */
  private void initialiseBushes(final double[] linkSegmentCosts) throws PlanItException {
    final OneToAllShortestPathAlgorithm shortestPathAlgorithm = createNetworkShortestPathAlgo(linkSegmentCosts);

    Zoning zoning = getTransportNetwork().getZoning();
    OdDemands odDemands = getOdDemands();
    for (var origin : zoning.getOdZones()) {
      ShortestPathResult oneToAllResult = null;
      InitialiseBushEdgeSegmentDemandConsumer initialiseBushConsumer = null;
      Bush originBush = null;
      for (var destination : zoning.getOdZones()) {
        if (destination.idEquals(origin)) {
          continue;
        }

        Double currOdDemand = odDemands.getValue(origin, destination);
        if (currOdDemand != null && currOdDemand > 0) {

          if (originBush == null) {
            /* register new bush */
            originBush = new Bush(getIdGroupingToken(), origin, getTransportNetwork().getNumberOfEdgeSegmentsAllLayers());
            originBushes[(int) origin.getOdZoneId()] = originBush;
            initialiseBushConsumer = new InitialiseBushEdgeSegmentDemandConsumer(originBush);
          }
          /* ensure bush initialisation is applied to the right destination/demand */
          initialiseBushConsumer.setDestination(destination.getCentroid(), currOdDemand);

          /* find one-to-all shortest paths */
          if (oneToAllResult == null) {
            oneToAllResult = shortestPathAlgorithm.executeOneToAll(origin.getCentroid());
          }

          /* initialise bush with this destination shortest path */
          oneToAllResult.forEachBackwardEdgeSegment(origin.getCentroid(), destination.getCentroid(), initialiseBushConsumer);
        }

      }
    }
  }

  /**
   * Match (new) PASs to improve existing bushes (origin) at hand.
   * <p>
   * Note that in order to extend the bushes we run a shortest path rooted at each bush's origin, since this is costly, we utilise the result also to update the min-cost gap for
   * each OD which requires the min-cost from each origin to each destination which is what the shortest path trees provide. The updating of the network's actual costs occurs
   * elsewhere
   * 
   * @param linkSegmentCosts to use to construct min-max path three rooted at each bush's origin
   * @return newly created PASs (empty if no new PASs were created)
   * @throws PlanItException thrown if error
   */
  private Collection<Pas> extendBushes(final double[] linkSegmentCosts) throws PlanItException {

    List<Pas> newPass = new ArrayList<>();

    final OneToAllShortestPathAlgorithm networkShortestPathAlgo = createNetworkShortestPathAlgo(linkSegmentCosts);

    for (int index = 0; index < originBushes.length; ++index) {
      Bush originBush = originBushes[index];
      if (originBush != null) {

        /* within-bush min/max-paths */
        var minMaxPaths = originBush.computeMinMaxShortestPaths(linkSegmentCosts, this.getTransportNetwork().getNumberOfVerticesAllLayers());

        /* network min-paths */
        var networkMinPaths = networkShortestPathAlgo.executeOneToAll(originBush.getOrigin().getCentroid());

        /* find (new) matching PASs */
        for (var bushVertexIter = originBush.getDirectedVertexIterator(); bushVertexIter.hasNext();) {
          DirectedVertex bushVertex = bushVertexIter.next();

          /* when bush does not contain the reduced cost edge segment (or the opposite direction which would cause a cycle) consider it */
          EdgeSegment reducedCostSegment = networkMinPaths.getIncomingEdgeSegmentForVertex(bushVertex);
          if (reducedCostSegment != null && !originBush.containsAnyEdgeSegmentOf(reducedCostSegment.getParentEdge())) {

            double reducedCost = minMaxPaths.getCostToReach(bushVertex) - networkMinPaths.getCostToReach(bushVertex);

            boolean matchFound = extendBushWithSuitableExistingPas(originBush, bushVertex, reducedCost);
            if (matchFound) {
              continue;
            }

            /* no suitable match, attempt creating an entirely new PAS */
            Pas newPas = extendBushWithNewPas(originBush, bushVertex, networkMinPaths);
            if (newPas != null) {
              newPass.add(newPas);
              continue;
            }

            // BRANCH SHIFT
            {
              // NOTE: since we will perform an update on all PASs it seems illogical to also explicitly register the required branch shifts
              // since they will be carried out regardless. Hence we do not log a warning nor implement the branch shift until it appears necessary

              /* no suitable new or existing PAS could be found given the conditions applied, do a branch shift instead */
              // LOGGER.info("No existing/new PAS found that satisfies flow/cost effective conditions for origin bush %s, consider branch shift - not yet implemented");
              // TODO: currently not implemented yet -> requires shifting flow on existing bush with the given vertex as the end point
            }

          }
        }
      }
    }
    return newPass;
  }

  /**
   * Shift flows based on the registered PASs and their origins.
   * 
   * @param theMode to use
   * @return all PASs where non-zero flow was shifted on
   */
  private Collection<Pas> shiftFlows(final Mode theMode) {
    var flowShiftedPass = new ArrayList<Pas>((int) pasManager.getNumberOfPass());
    var passWithoutOrigins = new ArrayList<Pas>();

    var networkLoading = getLoading();
    var gapFunction = (LinkBasedRelativeDualityGapFunction) getTrafficAssignmentComponent(GapFunction.class);
    var physicalCost = getTrafficAssignmentComponent(AbstractPhysicalCost.class);
    var virtualCost = getTrafficAssignmentComponent(AbstractVirtualCost.class);

    /* reused predicate */
    Predicate<EdgeSegment> firstCongestedLinkSegment = es -> getLoading().getCurrentFlowAcceptanceFactors()[(int) es.getId()] < 1;

    /**
     * Sort all PAss by their reduced cost ensuring we shift flows from the most attractive shift towards the least where we exclude all link segments of a processed PAS such that
     * no other PASs are allowed to shift flows if they overlap to avoid using inconsistent costs after a flow shift to or from a link segment
     */
    BitSet linkSegmentsUsed = new BitSet(networkLoading.getCurrentInflowsPcuH().length);
    Collection<Pas> sortedPass = pasManager.getPassSortedByReducedCost();
    for (Pas pas : sortedPass) {

      /* determine the network flow on the high cost subpath */
      double s2ShiftableFlow = networkLoading.computeSubPathSendingFlow(pas.getDivergeVertex(), pas.getMergeVertex(), pas.getAlternative(false /* highCost */));

      // gap function update
      double s1SendingFlow = networkLoading.computeSubPathSendingFlow(pas.getDivergeVertex(), pas.getMergeVertex(), pas.getAlternative(true /* low cost */));
      updateGap(gapFunction, pas, s1SendingFlow, s2ShiftableFlow);

      if (pas.containsAny(linkSegmentsUsed)) {
        continue;
      }
      /* untouched PAS (no flows shifted yet) in this iteration */

      double flowShift = determineFlowShift(pas, s2ShiftableFlow, theMode, physicalCost, virtualCost, networkLoading, firstCongestedLinkSegment);
      boolean pasFlowShifted = pas.executeFlowShift(s2ShiftableFlow, flowShift, networkLoading.getCurrentFlowAcceptanceFactors());
      if (pasFlowShifted) {
        flowShiftedPass.add(pas);

        /* s1 */
        pas.forEachEdgeSegment(true /* low cost */, (es) -> linkSegmentsUsed.set((int) es.getId()));
        /* s2 */
        pas.forEachEdgeSegment(false /* high cost */, (es) -> linkSegmentsUsed.set((int) es.getId()));

        /* when s2 no longer used on any bush - mark PAS for overall removal */
        if (!pas.hasOrigins()) {
          passWithoutOrigins.add(pas);
        }
      }
    }

    if (!passWithoutOrigins.isEmpty()) {
      passWithoutOrigins.forEach((pas) -> pasManager.removePas(pas));
    }
    return flowShiftedPass;
  }

  /**
   * Create bush based network loading implementation
   * 
   * @return created loading implementation supporting bush-based approach
   */
  @Override
  protected StaticLtmLoadingBush createNetworkLoading() {
    return new StaticLtmLoadingBush(getIdGroupingToken(), getAssignmentId(), getSettings());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected StaticLtmLoadingBush getLoading() {
    return (StaticLtmLoadingBush) super.getLoading();
  }

  /**
   * Create initial bushes, where for each origin the bush is initialised with the shortest path only
   * 
   * @param initialLinkSegmentCosts costs to use
   */
  @Override
  public void createInitialSolution(double[] initialLinkSegmentCosts) {
    try {
      initialiseBushes(initialLinkSegmentCosts);
      getLoading().setBushes(originBushes);
      getLoading().setPasManager(this.pasManager);

    } catch (PlanItException e) {
      LOGGER.severe(String.format("Unable to create initial bushes for sLTM %d", getAssignmentId()));
    }
  }

  /**
   * Constructor
   * 
   * @param idGroupingToken       to use for internal managed ids
   * @param assignmentId          of parent assignment
   * @param transportModelNetwork to use
   * @param settings              to use
   * @param taComponents          to use for access to user configured assignment components
   */
  public StaticLtmBushStrategy(final IdGroupingToken idGroupingToken, long assignmentId, final TransportModelNetwork transportModelNetwork, final StaticLtmSettings settings,
      final TrafficAssignmentComponentAccessee taComponents) {
    super(idGroupingToken, assignmentId, transportModelNetwork, settings, taComponents);
    this.originBushes = new Bush[transportModelNetwork.getZoning().getOdZones().size()];
    this.pasManager = new PasManager();
  }

  //@formatter:off
  /**
   * Perform an iteration by:
   *  
   * 1. Identify new PASs and shift flow from affected bushes
   * 2. Conduct another loading update based on adjusted PASs and bushes
   * 3. Update Bushes by shifting flow between existing PASs 
   * 4. Conducting a loading to obtain network costs 
   * 
   * @param theMode to use
   * @param costsToUpdate to place updated costs in (output)
   * @param iterationIndex we're at
   * @return true when iteration could be successfully completed, false otherwise
   */
  @Override
  public boolean performIteration(final Mode theMode, double[] costsToUpdate, int iterationIndex) {
    try {
      
      /* 1 - NETWORK LOADING - UPDATE ALPHAS - USE BUSH SPLITTING RATES (i-1) -  MODE AGNOSTIC FOR NOW */
      {
        executeNetworkLoading();
      }
             
      /* 2 - NETWORK COST UPDATE + UPDATE NETWORK REALISED COST GAP */
      {
        boolean updateOnlyPotentiallyBlockingNodeCosts = getLoading().getActivatedSolutionScheme().equals(StaticLtmLoadingScheme.POINT_QUEUE_BASIC);
        this.executeNetworkCostsUpdate(theMode, updateOnlyPotentiallyBlockingNodeCosts, costsToUpdate);
        
        /* PAS COST UPDATE*/
        pasManager.updateCosts(costsToUpdate);      
                          
        LOGGER.severe(String.format("** ALPHA: %s", Arrays.toString(getLoading().getCurrentFlowAcceptanceFactors())));
        LOGGER.severe(String.format("** COSTS: %s", Arrays.toString(costsToUpdate)));
        LOGGER.severe(String.format("** INFLOW: %s", Arrays.toString(getLoading().getCurrentInflowsPcuH())));
        LOGGER.severe(String.format("** OUTFLOW: %s", Arrays.toString(getLoading().getCurrentOutflowsPcuH())));
      }
      
      /* 3 - BUSH LOADING - SYNC BUSH TURN FLOWS - USE NETWORK LOADING ALPHAS - MODE AGNOSTIC FOR NOW */
      {
        syncBushTurnFlows(); 
      }
      
      /* 4 - BUSH ROUTE CHOICE - UPDATE BUSH SPLITTING RATES - SHIFT BUSH TURN FLOWS - MODE AGNOSTIC FOR NOW */     
      {
        /* (NEW) PAS MATCHING FOR BUSHES */
        Collection<Pas> newPass = extendBushes(costsToUpdate);      
        pasManager.updateCosts(newPass, costsToUpdate);      
              
        /* PAS/BUSH FLOW SHIFTS + GAP UPDATE */
        Collection<Pas> updatedPass = shiftFlows(theMode);      
        
        /* Remove unused new PASs, in case no flow shift is applied due to overlap with PAS with higher reduced cost 
         * In this case, the new PAS is not used and is to be removed identical to how existing PASs are removed during flow shifts when they no longer carry flow*/
        newPass.removeAll(updatedPass);
        newPass.forEach( pas -> pasManager.removePas(pas));
      }
      
    }catch(Exception e) {
      LOGGER.severe(e.getMessage());
      LOGGER.severe("Unable to complete sLTM iteration");
      if(getSettings().isDetailedLogging()) {
        e.printStackTrace();
      }
      return false;
    }
    return true;
  }

  private void syncBushTurnFlows() {
    for (var originBush : originBushes) {
      if (originBush == null) {
        continue;
      }
      
      originBush.updateTurnFlows(getLoading().getCurrentFlowAcceptanceFactors());
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getDescription() {
    return "Bush-based";
  }   
  

}
