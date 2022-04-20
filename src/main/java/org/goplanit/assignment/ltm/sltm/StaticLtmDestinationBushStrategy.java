package org.goplanit.assignment.ltm.sltm;

import java.util.logging.Logger;

import org.goplanit.algorithms.shortest.ShortestBushGeneralised;
import org.goplanit.algorithms.shortest.ShortestBushResult;
import org.goplanit.interactor.TrafficAssignmentComponentAccessee;
import org.goplanit.network.transport.TransportModelNetwork;
import org.goplanit.od.demand.OdDemands;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.acyclic.ACyclicSubGraph;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.zoning.OdZone;
import org.goplanit.zoning.Zoning;

/**
 * Implementation to support a destination-based bush solution for sLTM.
 * 
 * @author markr
 *
 */
public class StaticLtmDestinationBushStrategy extends StaticLtmBushStrategyRooted {

  /** Logger to use */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(StaticLtmDestinationBushStrategy.class.getCanonicalName());

  /** single dummy label used throughout for destination bushes */
  private final BushFlowLabel dummyLabel;

  /**
   * Populate with initial demand for given OD and shortest bush DAG
   * 
   * @param destinationBush  to populate
   * @param destination      to use
   * @param oDDemandPcuH     to use
   * @param odDag            to use
   * @param destinationLabel dummy destination label to use
   * 
   */
  private void initialiseBushForOrigin(final DestinationBush destinationBush, final OdZone origin, final Double oDDemandPcuH, final ACyclicSubGraph odDag,
      BushFlowLabel destinationLabel) {

    /* get topological sorted vertices to process (starting at destination) */
    boolean descendingIterator = true;
    /* reverse iterate so we go from origin(s) to single destination */
    var vertexIter = odDag.getTopologicalIterator(true /* update */, descendingIterator);

    /* proceed until we arrive at our origin */
    DirectedVertex currVertex = null;
    while (vertexIter.hasNext() && !origin.getCentroid().equals(currVertex)) {
      currVertex = vertexIter.next();
    }

    var helper = BushInitialiserHelper.create(destinationBush, odDag, pasManager, getSettings().isDetailedLogging());
    helper.executeOdBushInitialisation(currVertex, oDDemandPcuH, vertexIter, destinationLabel);
  }

  /**
   * Loop over all origins for bush' destination and apply demand proportionally across available shortest path(s) based on given demand and shortest path algorithm used
   * 
   * @param bush                  to use
   * @param zoning                to use
   * @param odDemands             to use
   * @param shortestBushAlgorithm to use
   */
  @Override
  protected void initialiseBush(RootedBush bush, Zoning zoning, OdDemands odDemands, ShortestBushGeneralised shortestBushAlgorithm) {
    var destination = ((DestinationBush) bush).getDestination();
    ShortestBushResult allToOneResult = null;

    for (var origin : zoning.getOdZones()) {
      if (origin.idEquals(destination)) {
        continue;
      }

      Double currOdDemand = odDemands.getValue(origin, destination);
      if (currOdDemand != null && currOdDemand > 0) {

        /* find all-to-one shortest paths */
        if (allToOneResult == null) {
          allToOneResult = shortestBushAlgorithm.executeAllToOne(destination.getCentroid());
        }

        /* initialise bush with this origin shortest path(s) */
        var originDag = allToOneResult.createDirectedAcyclicSubGraph(getIdGroupingToken(), origin.getCentroid(), destination.getCentroid());
        if (originDag.isEmpty()) {
          LOGGER.severe(String.format("Unable to create bush connection(s) from origin (%s) to destination %s", origin.getXmlId(), destination.getXmlId()));
          continue;
        }

        bush.addOriginDemandPcuH(origin, currOdDemand);
        initialiseBushForOrigin((DestinationBush) bush, origin, currOdDemand, originDag, dummyLabel);
      }
    }
  }

  /**
   * Create destination bushes for all destination with non-zero flow from any origin, remaining entries in raw array will be null and they are placed in the array by zone id
   * 
   * @return created destination bushes
   */
  @Override
  protected DestinationBush[] createEmptyBushes() {
    Zoning zoning = getTransportNetwork().getZoning();
    DestinationBush[] destinationBushes = new DestinationBush[(int) zoning.getNumberOfCentroids()];

    OdDemands odDemands = getOdDemands();
    for (var destination : zoning.getOdZones()) {
      for (var origin : zoning.getOdZones()) {
        if (destination.idEquals(origin)) {
          continue;
        }

        Double currOdDemand = odDemands.getValue(origin, destination);
        if (currOdDemand != null && currOdDemand > 0) {
          /* register new bush */
          var bush = new DestinationBush(getIdGroupingToken(), destination, getTransportNetwork().getNumberOfEdgeSegmentsAllLayers());
          destinationBushes[(int) destination.getOdZoneId()] = bush;
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
  protected PasFlowShiftExecutor createPasFlowShiftExecutor(final Pas pas, final StaticLtmSettings settings) {
    return new PasFlowShiftDestinationBasedExecutor(pas, settings, dummyLabel);
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
  public StaticLtmDestinationBushStrategy(final IdGroupingToken idGroupingToken, long assignmentId, final TransportModelNetwork transportModelNetwork,
      final StaticLtmSettings settings, final TrafficAssignmentComponentAccessee taComponents) {
    super(idGroupingToken, assignmentId, transportModelNetwork, settings, taComponents);

    /* no labels needed for destination bush, but for now, we use a single label for all flow to be able to keep using current implementation that relies on it */
    /* TODO: remove labelling once it works */
    this.dummyLabel = BushFlowLabel.create(getIdGroupingToken(), "dummy");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getDescription() {
    return "Destination-based Bush (unlabelled)";
  }

}
