package org.goplanit.assignment.ltm.sltm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.graph.directed.acyclic.ACyclicSubGraph;
import org.goplanit.utils.math.Precision;

public class BushInitialiserHelper {

  /** Logger to use */
  private static final Logger LOGGER = Logger.getLogger(BushInitialiserHelper.class.getCanonicalName());

  /** to initialise */
  private final RootedBush bush;

  /** to use to initialise bush */
  private final ACyclicSubGraph odDag;

  /** pas manager to use */
  private final PasManager pasManager;

  /** flag indicating if new pass are logged or not */
  private final boolean logNewPass;

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
   * Helper to populate container with provided parameters.
   * 
   * @param edgeSegmentToAdd     edge segment to add to inner most array of targeted vertex and alternative (by index)
   * @param unfinishedPasDiverge start of unfinishedPas (key)
   * @param alternativeIndex     index in array which is the value of the container entry obtained by the key, when -1 we create a new entry always
   * @param containerToAddTo     container to extend with edge segment
   * @return used alternative index, which in case of newly added entry differs from passed in value
   */
  private static int extendUnfinishedPasAlternativeWithEdgeSegment(EdgeSegment edgeSegmentToAdd, DirectedVertex unfinishedPasDiverge, int alternativeIndex,
      Map<DirectedVertex, List<List<EdgeSegment>>> containerToAddTo) {

    var alternatives = containerToAddTo.get(unfinishedPasDiverge);
    if (alternatives == null) {
      alternatives = containerToAddTo.put(unfinishedPasDiverge, new ArrayList<List<EdgeSegment>>());
    }
    ArrayList<EdgeSegment> alternative = null;
    if (alternativeIndex < 0 || alternatives.size() < alternativeIndex) {
      alternative = new ArrayList<EdgeSegment>(5);
      if (alternativeIndex < 0) {
        alternatives.add(alternative);
      } else {
        ((ArrayList<List<EdgeSegment>>) alternatives).ensureCapacity(alternativeIndex + 1);
        alternatives.set(alternativeIndex, alternative);
      }
    } else {
      alternative = (ArrayList<EdgeSegment>) alternatives.get(alternativeIndex);
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
  private static void removeFinishedPasAlternativeTracking(List<EdgeSegment> pasAlternative, List<List<EdgeSegment>> allOriginVertexAlternatives,
      Map<EdgeSegment, Map<DirectedVertex, Integer>> edgeSegmentPasOriginVertexAlternativeIndex, List<EdgeSegment> entrySegmentsWithUnfinishedPas) {

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
   * Helper method to register tracking information on passed in containers to start tracking new unfinished PASs starting at the provided (diverge) vertex. Create alternatives for
   * each available exit segment of the vertex on the DAG
   * 
   * @param pasDivergeVertex                           starting vertex of new unfinished PAS(s)
   * @param currentDestinationDag                      to identify used exit edge segments
   * @param edgeSegmentPasOriginVertexAlternativeIndex to be populated with exit segments registered to approprate alternative for unfinished PAS start vertex
   * @param originVertexAlternatives                   to be populated with origin vertex and alternative segments based on passed in vertex and used exit segments
   */
  private static void addNewUnfinishedPass(final DirectedVertex pasDivergeVertex, final ACyclicSubGraph currentDestinationDag,
      Map<EdgeSegment, Map<DirectedVertex, Integer>> edgeSegmentPasOriginVertexAlternativeIndex, Map<DirectedVertex, List<List<EdgeSegment>>> originVertexAlternatives) {
    /* new PAS(s) start here, flow splits, create and register exit edge segments as start of alternatives for new diverge leading to PAS(s) when merging later */
    var pasAlternatives = new ArrayList<List<EdgeSegment>>();
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
   * Helper method, extend all unfinished PASs identified by means of the entrySegmentsWithUnfinishedPas with the provided edge segment by updating the provided tracking containers
   * 
   * @param exitSegment                                to extend unfinished PAS alternatives with
   * @param entrySegmentsWithUnfinishedPas             eligible unfinished PASs to consider
   * @param edgeSegmentUnfinishedPasAlternativeIndices all the known unfinished PASs registered on all edge segments (to be updated)
   * @param unfinishedPasAlternatives                  all the known unfinished PASs and its alternatives so far (to be updated)
   */
  private static void extendUnfinishedPass(EdgeSegment exitSegment, List<EdgeSegment> entrySegmentsWithUnfinishedPas,
      Map<EdgeSegment, Map<DirectedVertex, Integer>> edgeSegmentUnfinishedPasAlternativeIndices, Map<DirectedVertex, List<List<EdgeSegment>>> unfinishedPasAlternatives) {

    for (var entrySegmentWithUnfinishedPas : entrySegmentsWithUnfinishedPas) {
      var unfinishedPassForEntrySegment = edgeSegmentUnfinishedPasAlternativeIndices.get(entrySegmentWithUnfinishedPas);
      for (var unfinishedPasEntry : unfinishedPassForEntrySegment.entrySet()) {
        extendUnfinishedPasAlternativeWithEdgeSegment(exitSegment, unfinishedPasEntry.getKey(), unfinishedPasEntry.getValue(), unfinishedPasAlternatives);
        registerUnfinishedPasOnEdgeSegment(exitSegment, unfinishedPasEntry.getKey(), unfinishedPasEntry.getValue(), edgeSegmentUnfinishedPasAlternativeIndices);
      }
    }
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
   * @param entrySegmentsWithUnfinishedPas             entry segments of merge with unfinished PAS(s)
   * @param originVertexAlternatives                   information regarding origin diverge vertices and alternatives of unfinished PAS(s)
   * @param edgeSegmentPasOriginVertexAlternativeIndex information regarding what unfinished PAS(s) pass through what edge segments
   */
  private void finishInitialBushPassAtMerge(List<EdgeSegment> entrySegmentsWithUnfinishedPas, Map<DirectedVertex, List<List<EdgeSegment>>> originVertexAlternatives,
      Map<EdgeSegment, Map<DirectedVertex, Integer>> edgeSegmentPasOriginVertexAlternativeIndex) {

    /*
     * Collect all unfinished PASs by origin vertex that pass through this vertex. Create local version of overall container just containing the subselection of unfinished PAS
     * alternatives that merge at this vertex
     */
    Map<DirectedVertex, List<List<EdgeSegment>>> localPasAlternatives = new HashMap<>();
    for (var entryEdgeSegment : entrySegmentsWithUnfinishedPas) {
      var entrySegmentUnfinishedPass = edgeSegmentPasOriginVertexAlternativeIndex.get(entryEdgeSegment);
      List<List<EdgeSegment>> currEntryPasAlternatives = null;
      for (var entry : entrySegmentUnfinishedPass.entrySet()) {
        currEntryPasAlternatives = localPasAlternatives.get(entry.getKey());
        if (currEntryPasAlternatives == null) {
          currEntryPasAlternatives = new ArrayList<List<EdgeSegment>>();
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
            pas = pasManager.createAndRegisterNewPas(bush, referenceAlternative, pasAlternative);
            if (logNewPass) {
              LOGGER.info(String.format("Created new PAS: %s", pas.toString()));
            }
          } else {
            pas.registerBush(bush);
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
   * Constructor
   * 
   * @param bush       to (further) initialise
   * @param odDag      to add to bush as initial supported DAG
   * @param pasManager to use
   * @param logNewPass when true log new PASs, otherwise not
   */
  protected BushInitialiserHelper(final RootedBush bush, final ACyclicSubGraph odDag, final PasManager pasManager, boolean logNewPass) {
    this.bush = bush;
    this.odDag = odDag;
    this.pasManager = pasManager;
    this.logNewPass = logNewPass;
  }

  /**
   * Factory method for bush initialiser
   * 
   * @param bush  to use
   * @param odDag to use
   * @return created helper
   */
  public static BushInitialiserHelper create(final RootedBush bush, final ACyclicSubGraph odDag, final PasManager pasManager, boolean logNewPass) {
    return new BushInitialiserHelper(bush, odDag, pasManager, logNewPass);
  }

  /**
   * Execute the initialisation by ensuring the correct flow is added to the bush for the given od dag and it related demand.
   * 
   * @param currVertex     to start with, expected to be the centroid of the od's origin. It is expected the iterator proceeds in downstream direction until reaching the
   *                       destination
   * @param oDDemandPcuH   to use for the origin vertex
   * @param vertexIter     flag indicating if new pass are to be logged
   * @param entryExitLabel to use
   */
  public void executeOdBushInitialisation(DirectedVertex originVertex, final Double oDDemandPcuH, final Iterator<DirectedVertex> vertexIter, BushFlowLabel entryExitLabel) {

    /* initialise starting flows on initial vertex */
    Map<EdgeSegment, Double> odDagFlows = new HashMap<>();
    int numUsedOdExitSegments = odDag.getNumberOfEdgeSegments(originVertex, true /* exit segments */);
    for (var exitEdgeSegment : originVertex.getExitEdgeSegments()) {
      odDagFlows.put(exitEdgeSegment, oDDemandPcuH / numUsedOdExitSegments);
    }

    /* track unfinished initial PASs while initialising bush */
    Map<DirectedVertex, List<List<EdgeSegment>>> originVertexAlternatives = new HashMap<>();
    Map<EdgeSegment, Map<DirectedVertex, Integer>> edgeSegmentPasOriginVertexAlternativeIndex = new HashMap<>();

    DirectedVertex currVertex;
    /* pass over destination DAG in (reverse) topological order propagating o-d flow and initialising labels from origin */
    List<EdgeSegment> entrySegmentsWithUnfinishedPas = new ArrayList<EdgeSegment>(5);
    while (vertexIter.hasNext()) {
      currVertex = vertexIter.next();

      /* aggregate incoming vertex flows */
      double vertexOdSendingFlow = 0;
      entrySegmentsWithUnfinishedPas.clear();
      for (var entryEdgeSegment : currVertex.getEntryEdgeSegments()) {
        if (odDag.containsEdgeSegment(entryEdgeSegment)) {
          Double entrySegmentSendingFlow = odDagFlows.get(entryEdgeSegment);
          if (entrySegmentSendingFlow == null) {
            continue;
          }

          if (Precision.positive(entrySegmentSendingFlow) && edgeSegmentPasOriginVertexAlternativeIndex.get(entryEdgeSegment) != null) {
            entrySegmentsWithUnfinishedPas.add(entryEdgeSegment);
          }
          vertexOdSendingFlow += entrySegmentSendingFlow;
        }
      }

      if (entrySegmentsWithUnfinishedPas.size() > 1) {
        /* create PASs at this merge and update passed in containers to reflect changes */
        finishInitialBushPassAtMerge(entrySegmentsWithUnfinishedPas, originVertexAlternatives, edgeSegmentPasOriginVertexAlternativeIndex);
      }

      numUsedOdExitSegments = odDag.getNumberOfEdgeSegments(currVertex, true /* exit segments */);
      double proportionalOdExitFlow = vertexOdSendingFlow / numUsedOdExitSegments;

      for (var entrySegment : currVertex.getEntryEdgeSegments()) {
        if (!odDag.containsEdgeSegment(entrySegment)) {
          continue;
        }

        int numUsedExits = 0;
        for (var exitSegment : currVertex.getExitEdgeSegments()) {
          if (odDag.containsEdgeSegment(exitSegment)) {
            /* update bush flow */
            bush.addTurnSendingFlow(entrySegment, entryExitLabel, exitSegment, entryExitLabel, proportionalOdExitFlow);
            odDagFlows.put(exitSegment, proportionalOdExitFlow);
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
          addNewUnfinishedPass(currVertex, odDag, edgeSegmentPasOriginVertexAlternativeIndex, originVertexAlternatives);
        }
      }
    }
  }

}
