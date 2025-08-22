package org.goplanit.assignment.ltm.sltm;

import java.util.*;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Predicate;
import java.util.logging.Logger;

import org.goplanit.algorithms.shortest.ShortestBushGeneralised;
import org.goplanit.algorithms.shortest.ShortestBushResult;
import org.goplanit.algorithms.shortest.ShortestPathGeneralised;
import org.goplanit.interactor.TrafficAssignmentComponentAccessee;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.transport.TransportModelNetwork;
import org.goplanit.od.demand.OdDemands;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.graph.directed.acyclic.ACyclicSubGraph;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.misc.Triple;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.virtual.VirtualNetwork;
import org.goplanit.utils.network.virtual.graph.CentroidVertex;
import org.goplanit.utils.zoning.OdZone;
import org.goplanit.zoning.Zoning;

/**
 * Implementation to support a destination-based bush solution for sLTM.
 * 
 * @author markr
 *
 */
public class StaticLtmDestinationBushStrategy extends StaticLtmBushStrategyRootLabelled<DestinationBush> {

  /** Logger to use */
  private static final Logger LOGGER = Logger.getLogger(StaticLtmDestinationBushStrategy.class.getCanonicalName());

  /**
   * Populate with initial demand for given OD and shortest bush DAG
   * 
   * @param destinationBush  to populate
   * @param  originCentroidVertex     to use
   * @param oDDemandPcuH     to use
   * @param destinationOriginInvertedDag            to use
   * 
   */
  private void initialiseBushForOrigin(
          final DestinationBush destinationBush,
          final CentroidVertex originCentroidVertex,
          final Double oDDemandPcuH,
          final ACyclicSubGraph destinationOriginInvertedDag) {

    /* get topological sorted vertices to process from origin-to-destination in direction of odDag, so invert iterator since it runs
       from destination to origin currently */
    var vertexIter = destinationOriginInvertedDag.getTopologicalIterator(true, true);

    /* proceed until we arrive at our origin */
    DirectedVertex currVertex = null;
    while (vertexIter.hasNext() && !originCentroidVertex.equals(currVertex)) {
      currVertex = vertexIter.next();
    }

    /* re-use the general approach which populates the bush from origin-to-destination direction, hence the fiddling to
     * to reorganise the dag to traverse it this way rather than the inverted setup (d-to-o) it has by default)
     * todo: when we remove the origin-based implementation revisit this perhaps
     */
    var helper = BushInitialiserHelper.create(
            destinationBush, destinationOriginInvertedDag, pasManager, getSettings().isDetailedLogging());
    helper.executeOdBushInitialisation(currVertex, oDDemandPcuH, vertexIter);
  }

  private void updatePasStatusBeforeFlowShift(
      Pas<DirectedVertex, EdgeSegment> pas, double[] flowAcceptanceFactors) {

    // test if conj segment is congested by considering original entry segment acceptance factor
    Predicate<EdgeSegment> congestedPred = es -> Precision.smaller(
        flowAcceptanceFactors[(int)es.getId()],1, Precision.EPSILON_9);

    for( var entrySegment : pas.getDivergeVertex().getEntryEdgeSegments()){
      if(congestedPred.test(entrySegment) &&
          pas.getRegisteredBushes().stream().anyMatch( b -> b.contains(entrySegment))){
        pas.updateStatus(PasStatus.CONGESTED);
        return;
      }
    }

    if( pas.anyMatch(congestedPred,false) || pas.anyMatch( congestedPred,true)){
      pas.updateStatus(PasStatus.CONGESTED);
    }else{
      pas.updateStatus(PasStatus.UNCONGESTED_WITHOUT_SHIFT);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected boolean initialiseBush(
          DestinationBush bush, Zoning zoning, OdDemands odDemands, ShortestPathGeneralised shortestTreeAlgorithm) {
    //todo: not yet updated to new bush implementation
    var shortestBushAlgorithm = (ShortestBushGeneralised) shortestTreeAlgorithm;
    
    var destinationVertex = bush.getDestination();
    var destination = (OdZone) destinationVertex.getParent().getParentZone();
    ShortestBushResult allToOneResult = null;

    for (var origin : zoning.getOdZones()) {
      if (origin.idEquals(destination)) {
        continue;
      }

      Double currOdDemand = odDemands.getValue(origin, destination);
      if (currOdDemand != null && currOdDemand > 0) {

        /* find all-to-one shortest paths */
        if (allToOneResult == null) {
          allToOneResult = shortestBushAlgorithm.executeAllToOne(destinationVertex);
        }

        /* initialise bush with this origin shortest path(s) */
        var originCentroidVertex = findOriginCentroidVertex(origin);
        var destinationOriginInvertedDag =
                allToOneResult.createDirectedAcyclicSubGraph(
                        getIdGroupingToken(), originCentroidVertex, destinationVertex);
        if (destinationOriginInvertedDag.isEmpty()) {
          LOGGER.severe(String.format("Unable to create bush connection(s) from origin (%s) to destination %s",
                  origin.getXmlId(), destination.getXmlId()));
          continue;
        }

        // destination bush has root in destination, but still tracks origin demands that it uses
        bush.addOriginDemandPcuH(originCentroidVertex, currOdDemand);
        initialiseBushForOrigin(bush, originCentroidVertex, currOdDemand, destinationOriginInvertedDag);
      }
    }

    return !bush.getDag().isEmpty();
  }


  /**
   * Create destination bushes for all destination with non-zero flow from any origin, remaining entries in raw array will be null and they are placed in the array by zone id
   *
   * @param mode to use
   * @return created destination bushes
   */
  @Override
  protected TreeSet<DestinationBush> createEmptyBushes(Mode mode) {
    Zoning zoning = getTransportNetwork().getZoning();
    TreeSet<DestinationBush> destinationBushes = new TreeSet<>();

    OdDemands odDemands = getOdDemands(mode);
    for (var destination : zoning.getOdZones()) {
      var destinationVertex = findDestinationCentroidVertex(destination);
      for (var origin : zoning.getOdZones()) {
        if (destination.idEquals(origin)) {
          continue;
        }

        Double currOdDemand = odDemands.getValue(origin, destination);
        if (currOdDemand != null && currOdDemand > 0) {
          /* register new bush */
          var bush = new DestinationBush(
                  getIdGroupingToken(), destinationVertex, getTransportNetwork().getNumberOfEdgeSegmentsAllLayers());
          destinationBushes.add(bush);
          break;
        }
      }
    }
    return destinationBushes;
  }

  /**
   * {@inheritDoc}
   * 
   */
  @Override
  protected PasFlowShiftExecutor<DirectedVertex, EdgeSegment> createPasFlowShiftExecutor(
      final Pas<DirectedVertex, EdgeSegment> pas, final StaticLtmSettings settings) {
    return new PasFlowShiftDestinationBasedExecutor(pas, settings);
  }

  @Override
  protected Pair<Set<Pas<DirectedVertex, EdgeSegment>>, Set<Pas<DirectedVertex, EdgeSegment>>>
  performLocalisedPasNetworkLoading(
      Mode theMode,
      Map<Pas<DirectedVertex, EdgeSegment>, Pair<EdgeSegment, Double>> pasDesiredFlowShifts,
      Map<Pas<DirectedVertex, EdgeSegment>, PasFlowShiftExecutor<DirectedVertex, EdgeSegment>> pasExecutors,
      double[] originalNetworkCosts,
      Set<DestinationBush> bushes,
      boolean logAll,
      TreeSet<DirectedVertex> onPasTouchedNodes,
      TreeSet<EdgeSegment> onPasTouchedSegments,
      TreeSet<DirectedVertex> pasMergeExitDownstreamNodesForOutFlowUpdate) {
    throw new PlanItRunTimeException("performLocalisedPasNetworkLoading not supported yet");
  }

  @Override
  protected Map<Pas<DirectedVertex, EdgeSegment>, Pair<EdgeSegment, Double>> applyOverlapSmoothingToProposedPasShifts(
      Mode theMode, Map<Pas<DirectedVertex, EdgeSegment>, Pair<EdgeSegment, Double>> pasDesiredFlowShifts){
    throw new PlanItRunTimeException("applyOverlapSmoothingToProposedPasShifts not supported yet");
  }

  @Override
  protected void hookBeforePasUpdate(
      Collection<PasFlowShiftExecutor<DirectedVertex, EdgeSegment>> pasExecutors) {
    throw new PlanItRunTimeException("hookBeforeCongestedPasUpdate not implemented in non-conjugate destination based");
  }

  @Override
  protected Triple<TreeSet<DirectedVertex>, TreeSet<EdgeSegment>, TreeSet<DirectedVertex>>
  constructPasTouchedNetworkEntities(Set<Pas<DirectedVertex, EdgeSegment>> passToConsider) {
    throw new PlanItRunTimeException("constructPasTouchedNetworkEntities not implemented in non-conjugate destination based");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void updatePasCosts(Mode theMode, double[] originalNetworkLinkSegmentCosts) {
    pasManager.updateActivePassCosts(originalNetworkLinkSegmentCosts);
    pasManager.updateInactivePassCosts(originalNetworkLinkSegmentCosts);
  }

  @Override
  protected void updatePasStatusBeforeFlowShifts(Mode theMode, double[] networkLinkSegmentFlowAcceptanceFactors) {
    // execute status update without considering any flow shift information
    pasManager.forEachActivePas( p -> updatePasStatusBeforeFlowShift(p, networkLinkSegmentFlowAcceptanceFactors));
    pasManager.forEachInactivePas(p -> updatePasStatusBeforeFlowShift(p, networkLinkSegmentFlowAcceptanceFactors));
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
  public StaticLtmDestinationBushStrategy(
          final IdGroupingToken idGroupingToken,
          long assignmentId,
          final TransportModelNetwork<MacroscopicNetwork, VirtualNetwork> transportModelNetwork,
      final StaticLtmSettings settings, final TrafficAssignmentComponentAccessee taComponents) {
    /* destination based bushes are inverted, so PASs are to be registered based on vertex farthest from root,
     * i.e, farthest from destination, so at the upstream point of the PAS at its diverge (hence true at end of super)*/
    super(idGroupingToken, assignmentId, transportModelNetwork, settings, taComponents, true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getDescription() {
    return "Destination-based Bush";
  }

}
