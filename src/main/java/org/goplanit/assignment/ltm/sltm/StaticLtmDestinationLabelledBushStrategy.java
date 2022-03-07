package org.goplanit.assignment.ltm.sltm;

import java.util.ArrayList;
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
import org.goplanit.utils.misc.Pair;
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

    /* create initial PASs while initialising bush */
    Map<DirectedVertex, ArrayList<ArrayList<EdgeSegment>>> originVertexAlternatives = new HashMap<>();
    Map<EdgeSegment, Map<DirectedVertex, Integer>> edgeSegmentPasOriginVertexAlternativeIndex = new HashMap<>();

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
    ArrayList<EdgeSegment> entrySegmentsWithUnfinishedPas = new ArrayList<EdgeSegment>(5);
    while (vertexIter.hasNext()) {
      currVertex = vertexIter.next();

      /* aggregate incoming vertex flows */
      double vertexOdSendingFlow = 0;
      entrySegmentsWithUnfinishedPas.clear();
      for (var entryEdgeSegment : currVertex.getEntryEdgeSegments()) {
        if (currentDestinationDag.containsEdgeSegment(entryEdgeSegment)) {
          Double entrySegmentSendingFlow = destinationDagInitialFlows.get(entryEdgeSegment);
          vertexOdSendingFlow += entrySegmentSendingFlow != null ? entrySegmentSendingFlow : 0;

          if (edgeSegmentPasOriginVertexAlternativeIndex.get(entryEdgeSegment) != null) {
            entrySegmentsWithUnfinishedPas.add(entryEdgeSegment);
          }
        }
      }

      if (entrySegmentsWithUnfinishedPas.size() > 1) {

        /* Collect all unfinished PASs by origin vertex that pass through this vertex */
        Map<DirectedVertex, ArrayList<ArrayList<EdgeSegment>>> newPasAlternatives = new HashMap<>();
        for (var entryEdgeSegment : entrySegmentsWithUnfinishedPas) {
          var entrySegmentUnfinishedPass = edgeSegmentPasOriginVertexAlternativeIndex.get(entryEdgeSegment);
          ArrayList<ArrayList<EdgeSegment>> currEntryPasAlternatives = null;
          for (var entry : entrySegmentUnfinishedPass.entrySet()) {
            currEntryPasAlternatives = newPasAlternatives.get(entry.getKey());
            if (currEntryPasAlternatives == null) {
              currEntryPasAlternatives = new ArrayList<ArrayList<EdgeSegment>>();
              newPasAlternatives.put(entry.getKey(), currEntryPasAlternatives);
            }
            /* unfinished Pas alternative (value) along entry segment originating from a diverge (key) */
            currEntryPasAlternatives.add(originVertexAlternatives.get(entry.getKey()).get(entry.getValue()));
          }
        }

        // per possible origin vertex that has merging flow here -> create PASs where possible
        for (var entry : newPasAlternatives.entrySet()) {
          var originVertex = entry.getKey();
          var alternatives = entry.getValue();
          var allOriginVertexAlternatives = originVertexAlternatives.get(originVertex);
          if (alternatives.size() < 2) {
            /* single entry, so unfinished PAS does not merge here despite that flows merge here, do not create new PAS */
            // do nothing for now
          } else if (alternatives.size() >= 2) {
            /* for each combination of two, create a new PAS and remove now finished PAS information from tracking containers */
            var iter = alternatives.iterator();
            var referenceAlternative = iter.next();
            while (iter.hasNext()) {
              var pasAlternative = iter.next();
              find existing matching pas first, if exists, register originbush, otherwise create new PAS
              pasManager.createNewPas(originBush, (EdgeSegment[]) referenceAlternative.toArray(), (EdgeSegment[]) pasAlternative.toArray());

              // remove tracking info from alternative - finished
              allOriginVertexAlternatives.remove(pasAlternative);
              for (var segment : pasAlternative) {
                edgeSegmentPasOriginVertexAlternativeIndex.get(segment).remove(originVertex);
              }
              entrySegmentsWithUnfinishedPas.remove(pasAlternative.get(pasAlternative.size()-1));
            }
            // remove tracking info from alternative - finished
            allOriginVertexAlternatives.remove(referenceAlternative);
            for (var segment : referenceAlternative) {
              edgeSegmentPasOriginVertexAlternativeIndex.get(segment).remove(originVertex);
            }
            entrySegmentsWithUnfinishedPas.remove(referenceAlternative.get(referenceAlternative.size()-1));
          }
        }
      }

      continue with exit segments -> add to eligible unfinished origin diverge vertex entries (increase alternative segments with exit segment)
      use info from remaining entrySegmentsWithUnfinishedPas
      
      numUsedOdExitSegments = currentDestinationDag.getNumberOfEdgeSegments(currVertex, true /* exit segments */);
      double proportionalOdExitFlow = vertexOdSendingFlow / numUsedOdExitSegments;

      for (var entrySegment : currVertex.getEntryEdgeSegments()) {
        if (!currentDestinationDag.containsEdgeSegment(entrySegment)) {
          continue;
        }

        int numUsedExits = 0;
        for (var exitSegment : currVertex.getExitEdgeSegments()) {
          if (currentDestinationDag.containsEdgeSegment(exitSegment)) {
            originBush.addTurnSendingFlow(entrySegment, currentLabel, exitSegment, currentLabel, proportionalOdExitFlow);
            destinationDagInitialFlows.put(exitSegment, proportionalOdExitFlow);
            ++numUsedExits;
          }
        }

        if (numUsedExits > 1 && !originVertexAlternatives.containsKey(currVertex)) {
          /* new PAS(s) needed, flow splits, create an register and track relevant links until merge */
          var pasAlternatives = new ArrayList<ArrayList<EdgeSegment>>();
          originVertexAlternatives.put(currVertex, pasAlternatives);
          for (var exitSegment : currVertex.getExitEdgeSegments()) {
            if (currentDestinationDag.containsEdgeSegment(exitSegment)) {
              var pasAlternativeStart = new ArrayList<EdgeSegment>();
              pasAlternativeStart.add(exitSegment);
              pasAlternatives.add(pasAlternativeStart);
              /* register how link relates to unfinished pas by means of diverge vertex and index in list */
              edgeSegmentPasOriginVertexAlternativeIndex.put(exitSegment, Pair.of(currVertex, pasAlternatives.size() - 1));
            }
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
