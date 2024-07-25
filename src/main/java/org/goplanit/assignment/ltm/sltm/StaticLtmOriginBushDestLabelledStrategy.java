package org.goplanit.assignment.ltm.sltm;

import java.util.logging.Logger;

import org.goplanit.algorithms.shortest.ShortestBushGeneralised;
import org.goplanit.algorithms.shortest.ShortestBushResult;
import org.goplanit.interactor.TrafficAssignmentComponentAccessee;
import org.goplanit.network.transport.TransportModelNetwork;
import org.goplanit.od.demand.OdDemands;
import org.goplanit.utils.graph.directed.acyclic.ACyclicSubGraph;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.virtual.CentroidVertex;
import org.goplanit.utils.zoning.Centroid;
import org.goplanit.utils.zoning.OdZone;
import org.goplanit.zoning.Zoning;

/**
 * Implementation to support an orgin-based bush (destination labelled) solution for sLTM.
 * Memory and computationally inferior to destination based implementation.
 * 
 * @author markr
 *
 */
public class StaticLtmOriginBushDestLabelledStrategy extends StaticLtmBushStrategyRootLabelled {

  /** Logger to use */
  private static final Logger LOGGER = Logger.getLogger(StaticLtmOriginBushDestLabelledStrategy.class.getCanonicalName());

  private final BushFlowLabel[] destinationLabels;

  /**
   * Populate with initial demand for given OD and shortest bush DAG
   * 
   * @param originBush   to populate
   * @param destination  to use
   * @param oDDemandPcuH to use
   * @param odDag        to use
   * 
   */
  private void initialiseBushForDestination(final OriginBush originBush, final OdZone destination, final Double oDDemandPcuH, final ACyclicSubGraph odDag) {

    /* destination label to use (can be reused across bushes) */
    final BushFlowLabel currentLabel = destinationLabels[(int) destination.getOdZoneId()];

    /* get topological sorted vertices to process */
    var vertexIter = odDag.getTopologicalIterator(true /* update */);
    var currVertex = vertexIter.next();
    if (!(currVertex instanceof CentroidVertex)) {
      LOGGER.warning("Root vertex is not a centroid vertex, should not happen");
      return;
    }

    var helper = BushInitialiserHelper.create(originBush, odDag, pasManager, getSettings().isDetailedLogging());
    helper.executeOdBushInitialisation(currVertex, oDDemandPcuH, vertexIter, currentLabel);
  }

  /**
   * Loop over all destinations for bush' origin and apply demand proportionally across available shortest path(s) based on given demand and shortest path algorithm used
   * 
   * @param bush                  to use
   * @param zoning                to use
   * @param odDemands             to use
   * @param shortestBushAlgorithm to use
   */
  @Override
  protected void initialiseBush(RootedLabelledBush bush, Zoning zoning, OdDemands odDemands, ShortestBushGeneralised shortestBushAlgorithm) {
    var originVertex = ((OriginBush) bush).getOrigin();
    var origin = (OdZone) originVertex.getParent().getParentZone();
    ShortestBushResult shortestBushResult = null;

    for (var destination : zoning.getOdZones()) {
      if (destination.idEquals(origin)) {
        continue;
      }

      Double currOdDemand = odDemands.getValue(origin, destination);
      if (currOdDemand != null && currOdDemand > 0) {

        /* find one-to-all shortest paths */
        if (shortestBushResult == null) {
          shortestBushResult = shortestBushAlgorithm.executeOneToAll(originVertex);
        }

        /* initialise bush with this destination shortest path */
        var destinationDag = shortestBushResult.createDirectedAcyclicSubGraph(getIdGroupingToken(), originVertex, findCentroidVertex(destination));

        ((OriginBush) bush).addOriginDemandPcuH(currOdDemand);
        initialiseBushForDestination((OriginBush) bush, destination, currOdDemand, destinationDag);
      }
    }
  }

  /**
   * Create origin bushes for all origins with non-zero flow to any destination, remaining entries in raw array will be null and theya re placed in the array by zone id
   *
   * @param mode to use
   * @return created origin bushes
   */
  @Override
  protected OriginBush[] createEmptyBushes(Mode mode) {
    Zoning zoning = getTransportNetwork().getZoning();
    OriginBush[] originBushes = new OriginBush[(int) zoning.getNumberOfCentroids()];

    OdDemands odDemands = getOdDemands(mode);
    for (var origin : zoning.getOdZones()) {
      var originVertex = findCentroidVertex(origin);
      for (var destination : zoning.getOdZones()) {
        if (destination.idEquals(origin)) {
          continue;
        }

        Double currOdDemand = odDemands.getValue(origin, destination);
        if (currOdDemand != null && currOdDemand > 0) {
          /* register new bush */
          var bush = new OriginBush(getIdGroupingToken(), originVertex, getTransportNetwork().getNumberOfEdgeSegmentsAllLayers());
          originBushes[(int) origin.getOdZoneId()] = bush;
          break;
        }
      }
    }
    return originBushes;
  }

  /**
   * {@inheritDoc}
   * 
   */
  @Override
  protected PasFlowShiftExecutor createPasFlowShiftExecutor(final Pas pas, final StaticLtmSettings settings) {
    return new PasFlowShiftOriginBasedDestLabelledExecutor(pas, settings);
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
  public StaticLtmOriginBushDestLabelledStrategy(final IdGroupingToken idGroupingToken, long assignmentId, final TransportModelNetwork transportModelNetwork,
      final StaticLtmSettings settings, final TrafficAssignmentComponentAccessee taComponents) {
    super(idGroupingToken, assignmentId, transportModelNetwork, settings, taComponents);

    this.destinationLabels = new BushFlowLabel[(int) transportModelNetwork.getZoning().getOdZones().size()];
    for (var odZone : transportModelNetwork.getZoning().getOdZones()) {
      destinationLabels[(int) odZone.getOdZoneId()] = BushFlowLabel.create(this.getIdGroupingToken(),
          odZone.getName() != null ? odZone.getName() : (odZone.getXmlId() != null ? odZone.getXmlId() : String.valueOf(odZone.getId())));
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getDescription() {
    return "Origin-based bush (destination-labelled)";
  }

}
