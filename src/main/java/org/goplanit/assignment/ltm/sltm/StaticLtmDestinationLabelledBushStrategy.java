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
import org.goplanit.utils.math.Precision;
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
   * Helper method to register tracking information on passed in containers to start tracking new unfinished PASs starting at the provided (diverge) vertex. Create alternatives for
   * each available exit segment of the vertex on the DAG
   * 
   * @param pasDivergeVertex                           starting vertex of new unfinished PAS(s)
   * @param currentDestinationDag                      to identify used exit edge segments
   * @param edgeSegmentPasOriginVertexAlternativeIndex to be populated with exit segments registered to approprate alternative for unfinished PAS start vertex
   * @param originVertexAlternatives                   to be populated with origin vertex and alternative segments based on passed in vertex and used exit segments
   */
  private static void addNewUnfinishedPass(final DirectedVertex pasDivergeVertex, final ACyclicSubGraph currentDestinationDag,
      Map<EdgeSegment, Map<DirectedVertex, Integer>> edgeSegmentPasOriginVertexAlternativeIndex, Map<DirectedVertex, ArrayList<ArrayList<EdgeSegment>>> originVertexAlternatives) {
    /* new PAS(s) start here, flow splits, create and register exit edge segments as start of alternatives for new diverge leading to PAS(s) when merging later */
    var pasAlternatives = new ArrayList<ArrayList<EdgeSegment>>();
    originVertexAlternatives.put(pasDivergeVertex, pasAlternatives); // track from vertex
    for (var exitSegment : pasDivergeVertex.getExitEdgeSegments()) {
      if (currentDestinationDag.containsEdgeSegment(exitSegment)) {
        int pasAlternativeIndex = extendUnfinishedPasAlternativeWithEdgeSegment(exitSegment, pasDivergeVertex, -1, originVertexAlternatives);
        /* register how link relates to unfinished pas by means of diverge vertex and index in list */
        registerUnfinishedPasOnEdgeSegment(exitSegment, pasDivergeVertex, pasAlternativeIndex, edgeSegmentPasOriginVertexAlternativeIndex);
      }
    }
  }

  /**
   * Helper to populate container with provided parameters
   * 
   * @param edgeSegmentToAdd           key in container
   * @param unfinishedPasDivergeVertex entrykey to add to value map
   * @param alternativeIndex           entry value to add to value map
   * @param containerToRegisterOn      container to use
   */
  private static void registerUnfinishedPasOnEdgeSegment(EdgeSegment edgeSegmentToAdd, DirectedVertex unfinishedPasDivergeVertex, int alternativeIndex,
      Map<EdgeSegment, Map<DirectedVertex, Integer>> containerToRegisterOn) {

    var unfinishedPassThroughSegment = containerToRegisterOn.get(edgeSegmentToAdd);
    if (unfinishedPassThroughSegment == null) {
      unfinishedPassThroughSegment = new HashMap<DirectedVertex, Integer>();
      containerToRegisterOn.put(edgeSegmentToAdd, unfinishedPassThroughSegment);
    }
    unfinishedPassThroughSegment.put(unfinishedPasDivergeVertex, alternativeIndex);
  }

  /**
   * Helper method, extend all unfinished PASs identified by means of the entrySegmentsWithUnfinishedPas with the provided edge segment by updating the provided tracking containers
   * 
   * @param exitSegment                                to extend unfinished PAS alternatives with
   * @param entrySegmentsWithUnfinishedPas             eligible unfinished PASs to consider
   * @param edgeSegmentUnfinishedPasAlternativeIndices all the known unfinished PASs registered on all edge segments (to be updated)
   * @param unfinishedPasAlternatives                  all the known unfinished PASs and its alternatives so far (to be updated)
   */
  private static void extendUnfinishedPass(EdgeSegment exitSegment, ArrayList<EdgeSegment> entrySegmentsWithUnfinishedPas,
      Map<EdgeSegment, Map<DirectedVertex, Integer>> edgeSegmentUnfinishedPasAlternativeIndices, Map<DirectedVertex, ArrayList<ArrayList<EdgeSegment>>> unfinishedPasAlternatives) {

    for (var entrySegmentWithUnfinishedPas : entrySegmentsWithUnfinishedPas) {
      var unfinishedPassForEntrySegment = edgeSegmentUnfinishedPasAlternativeIndices.get(entrySegmentWithUnfinishedPas);
      for (var unfinishedPasEntry : unfinishedPassForEntrySegment.entrySet()) {
        extendUnfinishedPasAlternativeWithEdgeSegment(exitSegment, unfinishedPasEntry.getKey(), unfinishedPasEntry.getValue(), unfinishedPasAlternatives);
        registerUnfinishedPasOnEdgeSegment(exitSegment, unfinishedPasEntry.getKey(), unfinishedPasEntry.getValue(), edgeSegmentUnfinishedPasAlternativeIndices);
      }
    }
  }

  /**
   * Helper to populate container with provided parameters.
   * 
   * @param edgeSegmentToAdd     edge segment to add to inner most array of targeted vertex and alternative (by index)
   * @param unfinishedPasDiverge start of unfinishedPas (key)
   * @param alternativeIndex     index in array which is the value of the container entry obtained by the key, when -1 we create a new entry always
   * @param containerToAddTo     container to extend with edge segment
   * @return used alternative index, which in case of newly added entry differs from passed in value
   */
  private static int extendUnfinishedPasAlternativeWithEdgeSegment(EdgeSegment edgeSegmentToAdd, DirectedVertex unfinishedPasDiverge, int alternativeIndex,
      Map<DirectedVertex, ArrayList<ArrayList<EdgeSegment>>> containerToAddTo) {

    var alternatives = containerToAddTo.get(unfinishedPasDiverge);
    if (alternatives == null) {
      alternatives = containerToAddTo.put(unfinishedPasDiverge, new ArrayList<ArrayList<EdgeSegment>>());
    }
    ArrayList<EdgeSegment> alternative = null;
    if (alternativeIndex < 0 || alternatives.size() < alternativeIndex) {
      alternative = new ArrayList<EdgeSegment>(5);
      if (alternativeIndex < 0) {
        alternatives.add(alternative);
      } else {
        alternatives.ensureCapacity(alternativeIndex + 1);
        alternatives.set(alternativeIndex, alternative);
      }
    } else {
      alternative = alternatives.get(alternativeIndex);
    }
    alternative.add(edgeSegmentToAdd);

    return alternatives.size() - 1;
  }

  /**
   * Helper method, remove any tracking regarding unfinished PAS regarding the alternative segment passed in as it is presumed finished and part of a PAS now
   * 
   * @param pasAlternative                             to purge from unfinished tracking containers
   * @param allOriginVertexAlternatives                container to update
   * @param edgeSegmentPasOriginVertexAlternativeIndex container to update
   * @param entrySegmentsWithUnfinishedPas             container to update
   */
  private static void removeFinishedPasAlternativeTracking(ArrayList<EdgeSegment> pasAlternative, ArrayList<ArrayList<EdgeSegment>> allOriginVertexAlternatives,
      Map<EdgeSegment, Map<DirectedVertex, Integer>> edgeSegmentPasOriginVertexAlternativeIndex, ArrayList<EdgeSegment> entrySegmentsWithUnfinishedPas) {

    var originVertex = pasAlternative.get(0).getUpstreamVertex();
    allOriginVertexAlternatives.remove(pasAlternative);
    for (var segment : pasAlternative) {
      var edgeSegmentUnfinishedPass = edgeSegmentPasOriginVertexAlternativeIndex.get(segment);
      edgeSegmentUnfinishedPass.remove(originVertex);
      if (edgeSegmentUnfinishedPass.isEmpty()) {
        edgeSegmentPasOriginVertexAlternativeIndex.remove(segment);
      }
    }
    entrySegmentsWithUnfinishedPas.remove(pasAlternative.get(pasAlternative.size() - 1));
  }

  /**
   * Based on the provided entry segments that carry an unfinished PAS to this merge, we create PASs where possible. The construction is based on the following algorithm:
   * <ul>
   * <li>select unfinished PAS alternatives by origin vertex that merge at this given vertex</li>
   * <li>when more than one alternative exists for a given origin diverge merging at this vertex, create one or more PAS(s) such that all alternatives are covered</li>
   * <li>remove now finished alternatives from relevant containers that were passed in
   * </ul>
   * 
   * Any created PASs result in unfinishedPas container to be updated by removing entry segments when no longer carrying an infinished PAS, and updating the other two containers
   * accordingly by removing entries that are now considered finished.
   * 
   * @param originBush                                 to register PASs on
   * @param entrySegmentsWithUnfinishedPas             entry segments of merge with unfinished PAS(s)
   * @param originVertexAlternatives                   information regarding origin diverge vertices and alternatives of unfinished PAS(s)
   * @param edgeSegmentPasOriginVertexAlternativeIndex information regarding what unfinished PAS(s) pass through what edge segments
   */
  private void finishEligiblePassAtMerge(final Bush originBush, ArrayList<EdgeSegment> entrySegmentsWithUnfinishedPas,
      Map<DirectedVertex, ArrayList<ArrayList<EdgeSegment>>> originVertexAlternatives, Map<EdgeSegment, Map<DirectedVertex, Integer>> edgeSegmentPasOriginVertexAlternativeIndex) {

    /*
     * Collect all unfinished PASs by origin vertex that pass through this vertex. Create local version of overall container just containing the subselection of unfinished PAS
     * alternatives that merge at this vertex
     */
    Map<DirectedVertex, ArrayList<ArrayList<EdgeSegment>>> localPasAlternatives = new HashMap<>();
    for (var entryEdgeSegment : entrySegmentsWithUnfinishedPas) {
      var entrySegmentUnfinishedPass = edgeSegmentPasOriginVertexAlternativeIndex.get(entryEdgeSegment);
      ArrayList<ArrayList<EdgeSegment>> currEntryPasAlternatives = null;
      for (var entry : entrySegmentUnfinishedPass.entrySet()) {
        currEntryPasAlternatives = localPasAlternatives.get(entry.getKey());
        if (currEntryPasAlternatives == null) {
          currEntryPasAlternatives = new ArrayList<ArrayList<EdgeSegment>>();
          localPasAlternatives.put(entry.getKey(), currEntryPasAlternatives);
        }
        /* unfinished Pas alternative (value) along entry segment originating from a diverge (key) */
        currEntryPasAlternatives.add(originVertexAlternatives.get(entry.getKey()).get(entry.getValue()));
      }
    }

    /* per possible origin vertex that has merging flow here -> create PASs where possible */
    for (var entry : localPasAlternatives.entrySet()) {
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

          var pas = pasManager.findExistingPas(referenceAlternative, pasAlternative);
          if (pas == null) {
            pas = pasManager.createAndRegisterNewPas(originBush, referenceAlternative, pasAlternative);
          } else {
            pas.registerOrigin(originBush);
          }

          // remove tracking info from alternative - finished
          removeFinishedPasAlternativeTracking(pasAlternative, allOriginVertexAlternatives, edgeSegmentPasOriginVertexAlternativeIndex, entrySegmentsWithUnfinishedPas);
        }
        // remove tracking info from reference alternative - finished
        removeFinishedPasAlternativeTracking(referenceAlternative, allOriginVertexAlternatives, edgeSegmentPasOriginVertexAlternativeIndex, entrySegmentsWithUnfinishedPas);
        if (allOriginVertexAlternatives.isEmpty()) {
          originVertexAlternatives.remove(originVertex);
        }
      }
    }

  }

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

    /* track unfinished initial PASs while initialising bush */
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

          if (Precision.positive(entrySegmentSendingFlow) && edgeSegmentPasOriginVertexAlternativeIndex.get(entryEdgeSegment) != null) {
            entrySegmentsWithUnfinishedPas.add(entryEdgeSegment);
          }
        }
      }

      if (entrySegmentsWithUnfinishedPas.size() > 1) {
        /* create PASs at this merge and update passed in containers to reflect changes */
        finishEligiblePassAtMerge(originBush, entrySegmentsWithUnfinishedPas, originVertexAlternatives, edgeSegmentPasOriginVertexAlternativeIndex);
      }

      numUsedOdExitSegments = currentDestinationDag.getNumberOfEdgeSegments(currVertex, true /* exit segments */);
      double proportionalOdExitFlow = vertexOdSendingFlow / numUsedOdExitSegments;

      for (var entrySegment : currVertex.getEntryEdgeSegments()) {
        if (!currentDestinationDag.containsEdgeSegment(entrySegment)) {
          continue;
        }

        int numUsedExits = 0;
        for (var exitSegment : currVertex.getExitEdgeSegments()) {
          if (currentDestinationDag.containsEdgeSegment(exitSegment)) {
            /* update bush flow */
            originBush.addTurnSendingFlow(entrySegment, currentLabel, exitSegment, currentLabel, proportionalOdExitFlow);
            destinationDagInitialFlows.put(exitSegment, proportionalOdExitFlow);
            ++numUsedExits;

            /* for all continuing PASs - we select the first exit segment as their continuation path and add it */
            if (!entrySegmentsWithUnfinishedPas.isEmpty()) {
              extendUnfinishedPass(exitSegment, entrySegmentsWithUnfinishedPas, edgeSegmentPasOriginVertexAlternativeIndex, originVertexAlternatives);
              entrySegmentsWithUnfinishedPas.clear();
            }
          }
        }

        if (numUsedExits > 1 && !originVertexAlternatives.containsKey(currVertex)) {
          /* new PAS(s) start here, flow splits, create and register exit edge segments as start of alternatives for new diverge leading to PAS(s) when merging later */
          addNewUnfinishedPass(currVertex, currentDestinationDag, edgeSegmentPasOriginVertexAlternativeIndex, originVertexAlternatives);
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
