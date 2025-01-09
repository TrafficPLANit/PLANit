package org.goplanit.assignment.ltm.sltm.conjugate;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.goplanit.algorithms.shortest.ShortestBushGeneralised;
import org.goplanit.algorithms.shortest.ShortestBushResult;
import org.goplanit.algorithms.shortest.ShortestPathDijkstra;
import org.goplanit.algorithms.shortest.ShortestPathResult;
import org.goplanit.assignment.ltm.sltm.*;
import org.goplanit.assignment.ltm.sltm.loading.StaticLtmLoadingBushConjugate;
import org.goplanit.gap.GapFunction;
import org.goplanit.gap.PathBasedGapFunction;
import org.goplanit.interactor.TrafficAssignmentComponentAccessee;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.transport.ConjugateTransportModelNetwork;
import org.goplanit.network.transport.ConjugateTransportModelNetworkUtils;
import org.goplanit.network.transport.TransportModelNetwork;
import org.goplanit.network.transport.TransportModelNetworkUtils;
import org.goplanit.od.demand.OdDemands;
import org.goplanit.utils.graph.directed.ConjugateDirectedVertex;
import org.goplanit.utils.graph.directed.ConjugateEdgeSegment;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.acyclic.ACyclicSubGraph;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.virtual.VirtualNetwork;
import org.goplanit.utils.network.virtual.VirtualNetworkUtils;
import org.goplanit.utils.network.virtual.graph.CentroidVertex;
import org.goplanit.utils.network.virtual.physical.conjugate.ConjugateConnectoidNode;
import org.goplanit.utils.zoning.OdZone;
import org.goplanit.zoning.Zoning;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Base implementation to support a bush based solution for sLTM
 * 
 * @author markr
 *
 */
public class StaticLtmConjugateBushStrategy
        extends StaticLtmBushStrategyBase<ConjugateDirectedVertex, ConjugateEdgeSegment,ConjugateDestinationBush> {

  /** logger to use */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(StaticLtmConjugateBushStrategy.class.getCanonicalName());

  /**
   * Given non conjugate costs for link segments, expand to concjugate segments (turns)
   *
   * @param nonConjugateLinkSegmentCosts original costs
   * @return conjugate projected costs
   */
  private double[] expandNonConjugateLinkSegmentCostToConjugateSegmentCost(double[] nonConjugateLinkSegmentCosts){
    double[] conjugateSegmentCosts = new double[conjugateTransportModelNetwork.getNumberOfEdgeSegmentsAllLayers()];
    var conjugatePhysicalLayer = conjugateTransportModelNetwork.getInfrastructureNetwork().getTransportLayers().getFirst();
    conjugatePhysicalLayer.getLinkSegments().forEach(cs ->
            // [conj_segment_cost] = [link_segment_cost_of_original_turn_entry]
            conjugateSegmentCosts[(int)cs.getId()] =
                    nonConjugateLinkSegmentCosts[(int)cs.getOriginalAdjacentEdgeSegments().first().getId()]);
    return conjugateSegmentCosts;
  }

  /**
   * Populate with initial demand for given OD and shortest conjugate bush DAG
   *
   * @param conjugateDestinationBush  to populate
   * @param  originConjugateVertex     to use
   * @param odDemandPcuH     to use
   * @param destinationOriginInvertedDag            to use
   *
   */
  private void initialiseConjugateBushForOrigin(
          final ConjugateDestinationBush conjugateDestinationBush,
          final ConjugateDirectedVertex originConjugateVertex,
          final Double odDemandPcuH,
          final ACyclicSubGraph destinationOriginInvertedDag) {

    /* get topological sorted vertices to process from origin-to-destination in direction of odDag, so invert iterator since it runs
       from destination to origin currently */
    var vertexIter = destinationOriginInvertedDag.getTopologicalIterator(true, true);

    /* proceed until we arrive at our origin */
    DirectedVertex currVertex = null;
    while (vertexIter.hasNext() && !originConjugateVertex.equals(currVertex)) {
      currVertex = vertexIter.next();
    }

    /* populate initial demand on links of shortest path */
    var helper = ConjugateBushSimpleInitialiserHelper.create(
            conjugateDestinationBush, destinationOriginInvertedDag);
    helper.executeOdBushInitialisation(currVertex, odDemandPcuH, vertexIter);
  }

  /** because the bushes will be created and tracked in conjugate network form, we create a conjugate version of the
   * entire network from which the bushes draw */
  protected final ConjugateTransportModelNetwork conjugateTransportModelNetwork;

  /** inverse mapping from centroid vertices to their conjugate node */
  protected final Map<CentroidVertex, ConjugateConnectoidNode> centroid2ConjugateNodeMapping;

  /** inverse mapping from turn edge segments (double key) to conjugate edge segment */
  protected final  MultiKeyMap<Object, ConjugateEdgeSegment> turn2ConjugateSegmentMapping;

  /**
   * Create a shortest bush search algorithm for the conjugate bushes based on conjugate edge segments and costs
   *
   * @param nonConjugateLinkSegmentCosts to use
   * @return create shortest busg algorithm
   */
  @Override
  protected ShortestBushGeneralised createNetworkShortestBushAlgo(double[] nonConjugateLinkSegmentCosts) {
    //todo: once base implementation works, replace nonConjugateLinkSegment costs with turn based costs throughout
    // implementation. For now project non conjugate link segment costs to conjugate segments by using the entry segment
    // as the point of reference
    double[] conjugateSegmentCosts =
            expandNonConjugateLinkSegmentCostToConjugateSegmentCost(nonConjugateLinkSegmentCosts);
    final int numberOfVertices = this.conjugateTransportModelNetwork.getNumberOfVerticesAllLayers();
    return new ShortestBushGeneralised(conjugateSegmentCosts, numberOfVertices);
  }

  /**
   * {@inheritDoc
   */
  @Override
  protected ShortestPathDijkstra createNetworkShortestPathAlgo(final double[] conjugateLinkSegmentCosts) {
    final int numberOfVertices = this.conjugateTransportModelNetwork.getNumberOfVerticesAllLayers();
    return new ShortestPathDijkstra(conjugateLinkSegmentCosts, numberOfVertices);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void updatePasCosts(double[] originalNetworkLinkSegmentCosts) {
    var conjSegmentCosts = expandNonConjugateLinkSegmentCostToConjugateSegmentCost(originalNetworkLinkSegmentCosts);
    pasManager.updateCosts(conjSegmentCosts);
  }

  /**
   * Check if an existing PAS exists that terminates/starts (depending on bush config) at the given bush vertex. If so,
   * it is considered a match when:
   * <ul>
   * <li>The cheap alternative ends with a link segment that is not part of the bush
   * (Assumed true, to be checked beforehand)</li>
   * <li>The expensive alternative overlaps with the bush (has non-zero flow)</li>
   * <li>It is considered an improvement, i.e., effective based on the settings in terms of cost and flow</li>
   * </ul>
   * When this holds, accept this PAS as a decent enough alternative to the true shortest path (which its cheaper
   * segment might or might not overlap with, as long as it is close enough to the potential reduced cost we'll
   * take it to avoid exponential growth of PASs)
   *
   * @param bush        to consider
   * @param reducedCostVertex where we identified a potential reduced cost compared to current bush
   * @param reducedCost between the shorter path and current shortest path in the bush
   *
   * @return PAS when a match is found and null otherwise (PAS is already registered as part of this call)
   */
  protected Pas<ConjugateDirectedVertex,ConjugateEdgeSegment> extendConjugateBushWithSuitableExistingPas(
          final ConjugateDestinationBush bush,
          final ConjugateDirectedVertex reducedCostVertex,
          final double reducedCost) {

    double[] alphas = getLoading().getCurrentFlowAcceptanceFactors();
    var effectivePas = pasManager.findFirstSuitableExistingPas(bush, reducedCostVertex, alphas, reducedCost);
    if (effectivePas == null) {
      return null;
    }

    /*
     * found -> register origin, shifting of flow occurs when updating pas, extending bush with low cost segment
     * occurs automatically when shifting flow later (flow is added to low cost link segments which will be created
     * if non-existent on bush)
     */
    boolean newlyRegistered = effectivePas.registerBush(bush);
    if (newlyRegistered && getSettings().isDetailedLogging()) {
      LOGGER.info(String.format("Destination %s added to PAS %s",
              bush.getRootZoneVertex().getParent().getParentZone().getXmlId(), effectivePas));
    }
    return effectivePas;
  }

  /**
   * Try to create a new PAS for the given bush and the provided merge vertex. If a new PAS can be created given that
   * it is considered sufficiently effective the bush is registered on it.
   *
   * @param bush              to identify new PAS for
   * @param reducedCostVertex to use for creating the PAS as a cheaper path to the root exists at this vertex
   * @param conjugateNetworkMinPaths   the current conjugate network shortest path tree
   * @param reducedCost       to check if new PAS is considered effective
   * @param conjugateLinkSegmentCosts  to check if new PAS is considered effective
   * @return new created PAS if successfully created, null otherwise, the boolean indicates if it indeed is a brand
   * new PAS or for some reason we still reused an existing one
   */
  protected Pas<ConjugateDirectedVertex,ConjugateEdgeSegment> extendConjugateBushWithNewPas(
          final ConjugateDestinationBush bush,
          final ConjugateDirectedVertex reducedCostVertex,
          final ShortestPathResult conjugateNetworkMinPaths,
          double reducedCost,
          double[] conjugateLinkSegmentCosts) {

    /* Label all vertices on shortest path root-reducedCostVertex as -1, and PAS reference vertex itself as 1 */
    final short[] conjAlternativeSegmentVertexLabels =
            new short[conjugateTransportModelNetwork.getNumberOfVerticesAllLayers()];
    conjAlternativeSegmentVertexLabels[(int) reducedCostVertex.getId()] = 1;
    int numShortestPathEdgeSegments = conjugateNetworkMinPaths.forEachNextEdgeSegment(
            bush.getRootVertex(),
            reducedCostVertex,
            (edgeSegment) -> conjAlternativeSegmentVertexLabels[(int)
                    conjugateNetworkMinPaths.getNextVertexForEdgeSegment(edgeSegment).getId()] = -1);

    /* Identify when it coincides again with bush (closer to root) using back link tree BF search */
    var highCostSubPathResultPair =
            bush.findBushAlternativeSubpathByBackLinkTree(
                    reducedCostVertex,
                    (ConjugateEdgeSegment) conjugateNetworkMinPaths.getNextEdgeSegmentForVertex(reducedCostVertex),
                    conjAlternativeSegmentVertexLabels);
    if (highCostSubPathResultPair == null || highCostSubPathResultPair.first() == null) {
      /* likely cycle detected on bush for merge vertex, unable to identify higher cost segment for NEW PAS */
      LOGGER.info(String.format(
              "Unable to create new PAS for conjugate bush rooted at vertex (%s), despite shorter path found on " +
                      "network to reduced cost vertex (%s)",
              bush.getRootVertex().getIdsAsString(), reducedCostVertex.getIdsAsString()));
      return null;
    }

    /* create the PAS and register bush on it */
    boolean truncateSpareArrayCapacity = true;
    var coincideCloserToRootVertex = highCostSubPathResultPair.first();
    Map<ConjugateDirectedVertex, ConjugateEdgeSegment> backLinkTreeAsMap = highCostSubPathResultPair.second();

    /* S1 */
    ConjugateEdgeSegment[] s1 = PasManager.createSubPathArrayFrom(
            coincideCloserToRootVertex,
            reducedCostVertex,
            conjugateNetworkMinPaths,
            new ConjugateEdgeSegment[numShortestPathEdgeSegments],
            truncateSpareArrayCapacity);
    var cycleInducingSegment = bush.determineIntroduceCycle(s1);
    if (cycleInducingSegment != null) {
      /*
       * this can happen if the merge vertex can only be reached by traversing the bush in opposite direction of
       * existing edge segment on the bush. In which case, an alternative PAS further upstream should be considered,
       * now identify this and ignore PAS as it is sub-optimal and cycle inducing
       */
//      LOGGER.info(String.format("Newly identified PAS alternative for bush rooted at vertex (%s) would introduce cycle on low cost alternative (edge segment [%s]), ignore",
//          bush.getRootVertex().getIdsAsString(), cycleInducingSegment.getIdsAsString()));
      return null;
    }

    /* S2 */
    ConjugateEdgeSegment[] s2 = PasManager.createSubPathArrayFrom(
            coincideCloserToRootVertex,
            reducedCostVertex,
            bush.getShortestSearchType(),
            backLinkTreeAsMap,
            new ConjugateEdgeSegment[highCostSubPathResultPair.second().size()],
            truncateSpareArrayCapacity);

    /* register on existing PAS (if available) otherwise create new PAS */
    var existingPas = pasManager.findExistingPas(s1, s2);
    if (existingPas != null) {
      // exists already but was discarded as option for this bush (otherwise we would not ask for a new PAS to be
      // created not able to create new PAS using this S1/S2 alternative
      //todo: consider alternative S1/S2 creation using DFS for example to see if an alternative partially overlapping PAS
      // could help. For now we just accept no new PAS is to be created for this bush at this vertex
      return null;
    }

    /* New pas */
    var pas = pasManager.createAndRegisterNewPas(bush, s1, s2);
    pas.updateCost(conjugateLinkSegmentCosts);

    if (!PasManager.isPasEffectiveForBush(
            pas, bush, getLoading().getCurrentFlowAcceptanceFactors(), reducedCost)) {
      pasManager.removePas(pas, false);
      return null;
    }else if(getSettings().isDetailedLogging()){
      LOGGER.info(String.format("Created new PAS: %s", pas));
    }

    /* make sure all nodes along the PAS are tracked on the network level, for splitting rate/sending flow/acceptance
     * factor information */
    getLoading().activateNodeTrackingFor(pas);
    return pas;
  }

  /**
   * Create initial conjugate (destination based) empty bushes
   *
   * @param mode to use
   * @return created empty bushes suitable for this strategy
   */
  protected ConjugateDestinationBush[] createEmptyBushes(Mode mode) {

    var conjugateNetworkLayer =
        conjugateTransportModelNetwork.getInfrastructureNetwork().getTransportLayers().getFirst();
    Zoning zoning = getTransportNetwork().getZoning();
    ConjugateDestinationBush[] conjugateBushes = new ConjugateDestinationBush[(int) zoning.getNumberOfCentroids()];

    OdDemands odDemands = getOdDemands(mode);
    for (var destination : zoning.getOdZones()) {
      ConjugateDestinationBush bush = null;
      for (var origin : zoning.getOdZones()) {
        if (destination.idEquals(origin)) {
          continue;
        }

        Double currOdDemand = odDemands.getValue(origin, destination);
        if (currOdDemand != null && currOdDemand > 0) {

          /* centroid vertex to which demand will be mapped */
          var destinationCentroidVertex = findCentroidVertex(destination);
          if(destinationCentroidVertex == null){
            LOGGER.severe(String.format("Destination zone (%s) without centroid vertex to connect to network, " +
                "this shouldn't happen", destination.getIdsAsString()));
            continue;
          }

          /* collect conjugate root node for this conjugate destination bush */
          var rootConjugateConnectoidNode =
              centroid2ConjugateNodeMapping.get(destinationCentroidVertex);

          /* register new bush */
          bush = new ConjugateDestinationBush(
              conjugateNetworkLayer.getLayerIdGroupingToken(),
              destinationCentroidVertex,
              rootConjugateConnectoidNode,
              /* all "real" turns as conjugate segment is a turn */
              conjugateTransportModelNetwork.getNumberOfEdgeSegmentsAllLayers(),
              turn2ConjugateSegmentMapping);
          conjugateBushes[(int) destination.getOdZoneId()] = bush;
          break;
        }
      }
    }
    return conjugateBushes;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void initialiseBush(
          ConjugateDestinationBush bush,
          Zoning zoning,
          OdDemands odDemands,
          ShortestBushGeneralised shortestBushAlgorithm) {

    final var destinationCentroidVertex = bush.getRootZoneVertex();
    final OdZone destination = (OdZone) destinationCentroidVertex.getParent().getParentZone();
    final var destinationConjugateReferenceVertex = centroid2ConjugateNodeMapping.get(destinationCentroidVertex);
    ShortestBushResult allToOneResult = null;

    for (var origin : zoning.getOdZones()) {
      if (origin.idEquals(destination)) {
        continue;
      }

      Double currOdDemand = odDemands.getValue(origin, destination);
      if (currOdDemand != null && currOdDemand > 0) {

        /* find all-to-one shortest paths */
        if (allToOneResult == null) {
          allToOneResult = shortestBushAlgorithm.executeAllToOne(destinationConjugateReferenceVertex);
        }

        /* initialise conjugate bush with this origin shortest path(s) */
        var originConjugateReferenceVertex = centroid2ConjugateNodeMapping.get(findCentroidVertex(origin));
        var destinationOriginInvertedDag =
                allToOneResult.createDirectedAcyclicSubGraph(
                        getIdGroupingToken(), originConjugateReferenceVertex, destinationConjugateReferenceVertex);
        if (destinationOriginInvertedDag.isEmpty()) {
          LOGGER.severe(String.format("Unable to create conjugate bush connection(s) from origin (%s) to destination %s", origin.getXmlId(), destination.getXmlId()));
          continue;
        }

        bush.addOriginDemandPcuH(originConjugateReferenceVertex, currOdDemand);
        initialiseConjugateBushForOrigin(
                bush, originConjugateReferenceVertex, currOdDemand, destinationOriginInvertedDag);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected PasFlowShiftExecutor<ConjugateDirectedVertex, ConjugateEdgeSegment> createPasFlowShiftExecutor(
          final Pas<ConjugateDirectedVertex, ConjugateEdgeSegment> pas, final StaticLtmSettings settings) {
    return new PasFlowShiftConjugateDestinationBasedExecutor(pas, settings);
  }

  /**
   * Create conjugate bush based network loading implementation
   *
   * @return created loading implementation supporting conjugate bush-based approach
   */
  @Override
  protected StaticLtmLoadingBushConjugate createNetworkLoading() {
    return new StaticLtmLoadingBushConjugate(
            getIdGroupingToken(),
            getAssignmentId(),
            turn2ConjugateSegmentMapping,
            this.conjugateTransportModelNetwork,
            getSettings());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected StaticLtmLoadingBushConjugate getLoading() {
    return (StaticLtmLoadingBushConjugate) super.getLoading();
  }

  /**
   * Based on provided original network link segment costs see if we can update the existing collection of PASs
   *
   * @param mode             to use
   * @param nonConjugateLinkSegmentCosts to use
   * @param updateGap        flag
   * @param logAll           flag
   * @return newly created PASs
   */
  @Override
  protected Pair<Collection<Pas<ConjugateDirectedVertex, ConjugateEdgeSegment>>, Collection<Pas<ConjugateDirectedVertex, ConjugateEdgeSegment>>>
  updateBushPass(Mode mode, double[] nonConjugateLinkSegmentCosts, boolean updateGap, boolean logAll){

    double totalMinCostForGap = 0; // track during bush traversal to get min OD costs based on shortest paths
    double totalRealisedCostForGap = 0;
    if(updateGap) {
      // costs as they currently are utilising the unconstrained demand as a point of reference
      for (var linkSegment : getTransportNetwork().getInfrastructureNetwork().getLayerByMode(mode).getLinkSegments()) {
        double linkDemand = this.getLoading().getUnconstrainedFlowsPcuHour()[(int) linkSegment.getId()];
        double linkCost = nonConjugateLinkSegmentCosts[(int) linkSegment.getId()];
        totalRealisedCostForGap += linkCost * linkDemand;
      }
      for (var linkSegment : getTransportNetwork().getVirtualNetwork().getLayer().getConnectoidSegments()) {
        double linkDemand = this.getLoading().getUnconstrainedFlowsPcuHour()[(int) linkSegment.getId()];
        double linkCost = nonConjugateLinkSegmentCosts[(int) linkSegment.getId()];
        totalRealisedCostForGap += linkCost * linkDemand;
      }
    }

    //todo --> should be sets
    List<Pas<ConjugateDirectedVertex, ConjugateEdgeSegment>> newPass = new ArrayList<>();
    List<Pas<ConjugateDirectedVertex, ConjugateEdgeSegment>> existingPassWithNewBushes = new ArrayList<>();

    // method overridden for conjugate implementation resulting in conjugate compatible shortest path search using
    // conjugate link segment costs. For maintainability/readability expansion to conjugate costs occurs within method for now...
    final var conjLinkSegmentCosts = expandNonConjugateLinkSegmentCostToConjugateSegmentCost(nonConjugateLinkSegmentCosts);
    final var conjNetworkShortestPathAlgo = createNetworkShortestPathAlgo(conjLinkSegmentCosts);
    for (var conjBush : bushes) {
      if (conjBush == null) {
        continue;
      }

      /* within-bush min/max-paths - searched from root in designated direction (inverted if ALL-TO-ONE, i.e., root
       * is destination) */
      //todo: we do not yet account for if the path is used --> we should because we will likely get unused max cost paths now!
      var conjBushMinMaxPaths = conjBush.computeMinMaxShortestPaths(
              conjLinkSegmentCosts, conjugateTransportModelNetwork.getNumberOfVerticesAllLayers());
      if (conjBushMinMaxPaths == null) {
        LOGGER.severe(String.format(
                "Unable to obtain conjugate min-max paths for bush, this shouldn't happen, skip updateBushPass"));
        continue;
      }
      conjBushMinMaxPaths.setMinPathState(false);

      /* network min-paths - searched in designated direction (inverted if ALL-TO-ONE, so it is compatible with bush
       * where destination is root) */
      // todo below rewritten but not yet tested, so continue here
      var conjNetworkMinPaths =
              conjNetworkShortestPathAlgo.execute(conjBush.getShortestSearchType(), conjBush.getRootVertex());
      if (conjNetworkMinPaths == null) {
        LOGGER.severe(String.format(
                "Unable to obtain conjugate network min paths for conjugate bush, " +
                        "this shouldn't happen, skip updateBushPass"));
        continue;
      }

      if(updateGap) {
        // update/track total min cost across bushes(Ods) for gap calculation
        var odDemands = getOdDemands(mode);
        var destination = conjBush.getDestination().getParent().getParentZone();
        for (var originVertex : conjBush.getOriginVertices()) {
          var origin = ((ConjugateConnectoidNode)originVertex).getCentroidVertex().getParent().getParentZone();
          double odDemand = odDemands.getValue(origin, destination);
          double minOdCost = conjNetworkMinPaths.getCostToReach(originVertex);
          totalMinCostForGap += minOdCost * odDemand;
        }
      }

      /* find (new) matching PASs - start with new PAS close to origin exploration first
       *  todo: this is a choice, could choose differently and going with close to destination seems safer */
      var bushVertexIter = conjBush.isInverted() ?
              conjBush.getInvertedTopologicalIterator() : conjBush.getTopologicalIterator();
      while(bushVertexIter.hasNext()) {
        ConjugateDirectedVertex conjBushVertex = bushVertexIter.next();
        ConjugateEdgeSegment reducedCostSegment =
                (ConjugateEdgeSegment) conjNetworkMinPaths.getNextEdgeSegmentForVertex(conjBushVertex);
        if(reducedCostSegment == null) {
          continue;
        }

        double reducedCost =
                conjBushMinMaxPaths.getCostToReach(conjBushVertex) - conjNetworkMinPaths.getCostToReach(conjBushVertex);
        if(reducedCost <= 0){
          continue;
        }

        if(conjBushMinMaxPaths.getNextEdgeSegmentForVertex(conjBushVertex).equals(
                conjNetworkMinPaths.getNextEdgeSegmentForVertex(conjBushVertex))){
          // not the location that min path and bush min path split, so do not create the start point of PAS here yet
          continue;
        }

        /* when bush does not contain the opposite direction which would cause a cycle it is worth checking */
        boolean viableSearch =
                reducedCostSegment.getOppositeDirectionSegment()==null ||
                        !conjBush.contains(reducedCostSegment.getOppositeDirectionSegment());
        if (!viableSearch) {
          // preferred alternative cannot be added due to bush triggering a cycle if we would
          continue;
        }

        var existingRegisteredPas = extendConjugateBushWithSuitableExistingPas(conjBush, conjBushVertex, reducedCost);
        if (existingRegisteredPas != null) {
          if(isDestinationTrackedForLogging(conjBush) || logAll){
            LOGGER.info(String.format("Registered suitable existing PAS (%s) on bush (%s)",
                    existingRegisteredPas, conjBush));
          }
          existingPassWithNewBushes.add(existingRegisteredPas);
          continue;
        }

        /* no suitable match, attempt creating an entirely new PAS */
        var newPas = extendConjugateBushWithNewPas(
                conjBush, conjBushVertex, conjNetworkMinPaths, reducedCost, conjLinkSegmentCosts);
        if (newPas == null) {
          continue;
        }

        // truly new PAS
        newPass.add(newPas);
        newPas.updateCost(conjLinkSegmentCosts);
        if(isDestinationTrackedForLogging(conjBush) || logAll){
          LOGGER.info(String.format("Registered new PAS (%s) on conjugate bush (%s)",
                  newPas, conjBush.getRootZoneVertex().getParent().getParentZone().getIdsAsString()));
        }

        // BRANCH SHIFT
        {
          // NOTE: since we will perform an update on all PASs it seems illogical to also explicitly register the
          // required branch shifts since they will be carried out regardless. Hence we do not log a warning nor
          // implement the branch shift until it appears necessary

          /* no suitable new or existing PAS could be found given the conditions applied, do a branch shift instead */
          // LOGGER.info("No existing/new PAS found that satisfies flow/cost effective conditions for origin bush %s, consider branch shift - not yet implemented");
          // TODO: currently not implemented yet -> requires shifting flow on existing bush with the given vertex as the end point
        }

      }
    }

    if(updateGap){
      var gapFunction = (PathBasedGapFunction) getTrafficAssignmentComponent(GapFunction.class);
      // both costs have already been normalised to demand so use unity to transfer as is
      // ideally we'd use a link based gap but this is not ideal with the path based implementation we also support
      // for sLTM
      gapFunction.increaseMinimumPathCosts(totalMinCostForGap,1);
      gapFunction.increaseAbsolutePathGap(totalRealisedCostForGap, 1, totalMinCostForGap);
    }

    return Pair.of(newPass,existingPassWithNewBushes);
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
  public StaticLtmConjugateBushStrategy(
          final IdGroupingToken idGroupingToken,
          long assignmentId,
          final TransportModelNetwork<MacroscopicNetwork, VirtualNetwork> transportModelNetwork,
          final StaticLtmSettings settings,
          final TrafficAssignmentComponentAccessee taComponents) {
    super(idGroupingToken, assignmentId, transportModelNetwork, settings, taComponents);

    // construct conjugate version of original transport model network, to be used by all conjugate bushes
    this.conjugateTransportModelNetwork = transportModelNetwork.createConjugate(
            TransportModelNetworkUtils.generateDerivedConjugateIdGoupingToken(transportModelNetwork));
    conjugateTransportModelNetwork.logInfo("");

    centroid2ConjugateNodeMapping =
            VirtualNetworkUtils.createCentroidVertexToConjugateNodeMapping(
                    conjugateTransportModelNetwork.getVirtualNetwork().getLayer());
    turn2ConjugateSegmentMapping =
            ConjugateTransportModelNetworkUtils.createOriginalSegmentsToConjugateSegmentsMapping(
                    conjugateTransportModelNetwork);

    // todo: remove at some point as for large networks this will mean a lot of logging!
    boolean logMapping = true;
    if(logMapping) {
      conjugateTransportModelNetwork.logConjugateToOriginalMapping();
    }
  }

  /**
   *
   * @return description of this strategy for sLTM
   */
  @Override
  public String getDescription() {
    return "Conjugate destination-based Bush";
  }

}
