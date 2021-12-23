package org.goplanit.assignment.ltm.sltm;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.goplanit.graph.directed.acyclic.ACyclicSubGraph;
import org.goplanit.interactor.TrafficAssignmentComponentAccessee;
import org.goplanit.network.transport.TransportModelNetwork;
import org.goplanit.utils.graph.EdgeSegment;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.zoning.Centroid;
import org.goplanit.utils.zoning.OdZone;

/**
 * Implementation to support a destination labelled bush based solution for sLTM
 * 
 * @author markr
 *
 */
public class StaticLtmDestinationLabelledBushStrategy extends StaticLtmBushStrategy {

  /** Logger to use */
  private static final Logger LOGGER = Logger.getLogger(StaticLtmDestinationLabelledBushStrategy.class.getCanonicalName());

  private final BushFlowLabel[] destinationLabels;

  /**
   * {@inheritDoc}
   * 
   * Label each destination separately
   */
  @Override
  public void initialiseBushForDestination(final Bush originBush, final OdZone currentDestination, final Double originDestinationDemandPcuH,
      final ACyclicSubGraph currentDestinationDag) {

    originBush.addOriginDemandPcuH(originDestinationDemandPcuH);

    Map<EdgeSegment, Double> destinationDagInitialFlows = new HashMap<>();
    /* destination label to to use (can be reused across bushes) */
    final BushFlowLabel currentLabel = destinationLabels[(int) currentDestination.getOdZoneId()];

    /* get topological sorted vertices to process */
    Collection<DirectedVertex> topSortedVertices = currentDestinationDag.topologicalSort(true);
    var vertexIter = topSortedVertices.iterator();
    var currVertex = vertexIter.next();
    if (!(currVertex instanceof Centroid)) {
      LOGGER.warning("root vertex is not centroid, should not happen");
      return;
    }

    /* initialise */
    int numUsedOdExitSegments = currentDestinationDag.getNumberOfEdgeSegments(currVertex, true /* exit segments */);
    for (var exitEdgeSegment : currVertex.getExitEdgeSegments()) {
      destinationDagInitialFlows.put(exitEdgeSegment, originDestinationDemandPcuH / numUsedOdExitSegments);
    }

    /* pass over destination DAG in topological order propagating o-d flow and initialising labels from origin */
    while (vertexIter.hasNext()) {
      currVertex = vertexIter.next();

      /* aggregate incoming vertex flows */
      boolean anyEntryInBush = false;
      double vertexOdSendingFlow = 0;
      for (var entryEdgeSegment : currVertex.getEntryEdgeSegments()) {
        if (currentDestinationDag.containsEdgeSegment(entryEdgeSegment)) {
          Double entrySegmentSendingFlow = destinationDagInitialFlows.get(entryEdgeSegment);
          vertexOdSendingFlow += entrySegmentSendingFlow != null ? entrySegmentSendingFlow : 0;
        }
        if (!anyEntryInBush && originBush.hasFlowCompositionLabel(entryEdgeSegment)) {
          anyEntryInBush = true;
        }
      }

      numUsedOdExitSegments = currentDestinationDag.getNumberOfEdgeSegments(currVertex, true /* exit segments */);
      double proportionalOdExitFlow = vertexOdSendingFlow / numUsedOdExitSegments;

      for (var entrySegment : currVertex.getEntryEdgeSegments()) {
        if (!currentDestinationDag.containsEdgeSegment(entrySegment)) {
          continue;
        }

        for (var exitSegment : currVertex.getExitEdgeSegments()) {
          if (currentDestinationDag.containsEdgeSegment(exitSegment)) {
            originBush.addTurnSendingFlow(entrySegment, currentLabel, exitSegment, currentLabel, proportionalOdExitFlow);
            destinationDagInitialFlows.put(exitSegment, proportionalOdExitFlow);
          }
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   * 
   */
  @Override
  protected PasFlowShiftExecutor createPasFlowShiftExecutor(final Pas pas) {
    return new PasFlowShiftDestinationLabelledExecutor(pas);
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
  public StaticLtmDestinationLabelledBushStrategy(final IdGroupingToken idGroupingToken, long assignmentId, final TransportModelNetwork transportModelNetwork,
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
    return "Bush-based (destination-labelled)";
  }

}
