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
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.virtual.CentroidVertex;
import org.goplanit.utils.zoning.OdZone;
import org.goplanit.zoning.Zoning;

/**
 * Implementation to support a destination-based bush solution for sLTM.
 * 
 * @author markr
 *
 */
public class StaticLtmDestinationBushStrategy extends StaticLtmBushStrategyRootLabelled {

  /** Logger to use */
  private static final Logger LOGGER = Logger.getLogger(StaticLtmDestinationBushStrategy.class.getCanonicalName());

  /** single dummy label used throughout for destination bushes */
  private final BushFlowLabel dummyLabel;

  /**
   * Populate with initial demand for given OD and shortest bush DAG
   * 
   * @param destinationBush  to populate
   * @param  originCentroidVertex     to use
   * @param oDDemandPcuH     to use
   * @param destinationOriginInvertedDag            to use
   * @param destinationLabel dummy destination label to use
   * 
   */
  private void initialiseBushForOrigin(
          final DestinationBush destinationBush,
          final CentroidVertex originCentroidVertex,
          final Double oDDemandPcuH,
          final ACyclicSubGraph destinationOriginInvertedDag,
          BushFlowLabel destinationLabel) {

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
    var helper = BushInitialiserHelper.create(destinationBush, destinationOriginInvertedDag, pasManager, getSettings().isDetailedLogging());
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
  protected void initialiseBush(
          RootedLabelledBush bush, Zoning zoning, OdDemands odDemands, ShortestBushGeneralised shortestBushAlgorithm) {
    var destinationVertex = ((DestinationBush) bush).getDestination();
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
        var originCentroidVertex = findCentroidVertex(origin);
        var destinationOriginInvertedDag =
                allToOneResult.createDirectedAcyclicSubGraph(getIdGroupingToken(), originCentroidVertex, destinationVertex);
        if (destinationOriginInvertedDag.isEmpty()) {
          LOGGER.severe(String.format("Unable to create bush connection(s) from origin (%s) to destination %s", origin.getXmlId(), destination.getXmlId()));
          continue;
        }

        // destination bush has root in destination, but still tracks origin demands that it uses
        bush.addOriginDemandPcuH(originCentroidVertex, currOdDemand);
        initialiseBushForOrigin((DestinationBush) bush, originCentroidVertex, currOdDemand, destinationOriginInvertedDag, dummyLabel);
      }
    }
  }

  /**
   * Create destination bushes for all destination with non-zero flow from any origin, remaining entries in raw array will be null and they are placed in the array by zone id
   *
   * @param mode to use
   * @return created destination bushes
   */
  @Override
  protected DestinationBush[] createEmptyBushes(Mode mode) {
    Zoning zoning = getTransportNetwork().getZoning();
    DestinationBush[] destinationBushes = new DestinationBush[(int) zoning.getNumberOfCentroids()];

    OdDemands odDemands = getOdDemands(mode);
    for (var destination : zoning.getOdZones()) {
      var destinationVertex = findCentroidVertex(destination);
      for (var origin : zoning.getOdZones()) {
        if (destination.idEquals(origin)) {
          continue;
        }

        Double currOdDemand = odDemands.getValue(origin, destination);
        if (currOdDemand != null && currOdDemand > 0) {
          /* register new bush */
          var bush = new DestinationBush(getIdGroupingToken(), destinationVertex, getTransportNetwork().getNumberOfEdgeSegmentsAllLayers());
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
    return "Destination-based Bush (single dummy label)";
  }

}
