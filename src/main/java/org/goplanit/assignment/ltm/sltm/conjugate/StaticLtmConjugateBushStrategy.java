package org.goplanit.assignment.ltm.sltm.conjugate;

import java.util.Collection;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.goplanit.algorithms.shortest.ShortestBushGeneralised;
import org.goplanit.algorithms.shortest.ShortestBushResult;
import org.goplanit.assignment.ltm.sltm.*;
import org.goplanit.assignment.ltm.sltm.loading.StaticLtmLoadingBushConjugate;
import org.goplanit.interactor.TrafficAssignmentComponentAccessee;
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
import org.goplanit.utils.network.layer.physical.Movement;
import org.goplanit.utils.network.virtual.VirtualNetworkUtils;
import org.goplanit.utils.network.virtual.graph.CentroidVertex;
import org.goplanit.utils.network.virtual.physical.conjugate.ConjugateConnectoidNode;
import org.goplanit.utils.zoning.OdZone;
import org.goplanit.zoning.Zoning;

/**
 * Base implementation to support a bush based solution for sLTM
 * 
 * @author markr
 *
 */
public class StaticLtmConjugateBushStrategy extends StaticLtmBushStrategyBase<ConjugateDestinationBush> {

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

    /* populate initial demand on link sof shortest path */
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
  protected PasFlowShiftExecutor createPasFlowShiftExecutor(final Pas pas, final StaticLtmSettings settings) {
    // TODO: not implemented yet
    return null;
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
   * @param linkSegmentCosts to use
   * @param updateGap        flag
   * @param logAll           flag
   * @return newly created PASs
   */
  @Override
  protected Pair<Collection<Pas>, Collection<Pas>> updateBushPass(
          Mode mode, double[] linkSegmentCosts, boolean updateGap, boolean logAll){
    // TODO: not yet implemented for conjugate, take inspiration from "normal" implementation
    return null;
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
          final TransportModelNetwork<?,?> transportModelNetwork,
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
