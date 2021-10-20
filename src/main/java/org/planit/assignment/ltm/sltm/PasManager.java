package org.planit.assignment.ltm.sltm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.planit.algorithms.shortestpath.ShortestPathResult;
import org.planit.assignment.ltm.sltm.loading.StaticLtmLoadingBush;
import org.planit.sdinteraction.smoothing.Smoothing;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.graph.directed.DirectedVertex;
import org.planit.utils.math.Precision;

/**
 * Container class for tracking all unique PASs indexed by their last (merge) vertex
 * 
 * @author markr
 *
 */
public class PasManager {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(PasManager.class.getCanonicalName());

  /**
   * reduced cost multiplier, empirical calibrated value to use as threshold to consider shifting flow on an origin matching with a PAS, such that reducedCost_max_bush_PAS_path -
   * reducedCost_min_bush_PAS_path > mu * reducedCost_min_network_PAS_path. 0.5 based on Bar-Gera (2010)
   */
  private final double MU = 0.5;

  /**
   * Flow Effective factor nu, empirically calibrated value to use as threshold to consider shifting flow on an origin matching with a PAS, such that max_cost_PAS_path_flow -
   * max_cost_PAS_path_flow > nu * min_network_PAS_path_flow. 0.25 based on Xie and Xie (2015)
   */
  private final double NU = 0.25;

  /**
   * Map storing all PASs by their merge vertex
   */
  private Map<DirectedVertex, Collection<Pas>> passByMergeVertex;

  /**
   * Verify if extending a bush with the given PAS given the reduced cost found, it would be effective in improving the bush. This is verified by
   * <p>
   * reducedCost = bush_min_path_cost - PAS_min_path_cost, then <br\> it is considered effective if <br\> (PAS_max_path_cost - PAS_min_path_cost) > mu*bushReducedCost.
   * <p>
   * Formulation based on Bar-Gera (2010). IDea is that if the PAS has little difference between high and low cost, we can't shift much flow to improve and it is less attractive.
   * This is ok if the reduced cost, i.e., the maximum improvement given the current state of the network, is also low, but when the best option (which might not exactly follow
   * this PAS) is much better than what this PAS offers, we regard this PAS as not cost-effective and ignore it as a viable option.
   * 
   * @return true when considered effective, false otherwise
   */
  private boolean isCostEffective(Pas pas, double reducedCost) {
    return Precision.isGreater(pas.getAlternativeHighCost() - pas.getAlternativeLowCost(), MU * reducedCost);
  }

  /**
   * Use the accepted flow on the bush from start-to-end of PAS high cost segment and make sure it exceeds NU * total accepted flow (on the bush at hand) on the final edge segment
   * of the PAS high cost segment. This is an adaptation from Bar-Gera who uses the minimum across all high cost segments on the bush, but since we are capacitated this won't be
   * representative. So instead we use the portion of the total flow on the final segment that belongs to the high-cost sub-path present on the bush instead.
   * <p>
   * the rationale here is that we should only consider the PAS as effective for this bush, i.e., consider it for inclusion - if a decent amount of flow leading to the end point of
   * this PAS comes from the high cost segment of this PAS which would allow for a decent chunk of the flow to be shifted to the low cost segment. If not, it would not improve this
   * bush much if we would consider it.
   * 
   * @param pas                   under consideration for a bush
   * @param originBush            the accepted flow found on the bush traversing the high cost PAS and reaching the end vertex (including final alpha) of the PAS
   * @param flowAcceptanceFactors the accepted flow found passing through the final vertex of the PAS from the origin of the bush, i.e., all sub-paths to this vertex
   * @return true when considered effective, false otherwise
   */
  private boolean isFlowEffective(Pas pas, Bush originBush, double[] flowAcceptanceFactors) {
    boolean lowCostPath = true;
    /* usage of high cost segment on bush */
    double s2SubPathAcceptedFlowOnBush = pas.computeOverlappingAcceptedFlow(originBush, !lowCostPath, flowAcceptanceFactors);
    /* usage of segment arriving at merge vertex in bush */
    EdgeSegment s2LastEdgeSegment = pas.getLastEdgeSegment(!lowCostPath);
    double s2LastSegmentSendingFlowOnBush = originBush.getSendingFlowPcuH(s2LastEdgeSegment);
    double s2LastSegmentAcceptedFlowOnBush = s2LastSegmentSendingFlowOnBush * flowAcceptanceFactors[(int) s2LastEdgeSegment.getId()];

    return Precision.isGreater(s2SubPathAcceptedFlowOnBush, NU * s2LastSegmentAcceptedFlowOnBush);
  }

  /**
   * Verify if PAS is considered effective (enough) to improve the provided bush. This is verified by being both {@link #isCostEffective(Pas, double)} and
   * {@link #isFlowEffective(Pas, Bush, double[])}
   * 
   * @param pas                   to use
   * @param originBush            to use
   * @param flowAcceptanceFactors to use
   * @param reducedCost           to use
   * @return true when considered effective, false otherwise
   */
  private boolean isPasEffectiveForBush(Pas pas, Bush originBush, double[] flowAcceptanceFactors, double reducedCost) {
    /* Verify if low-cost PAS alternative is effective (enough) in improving the bush within the identified upper bound of the reduced cost */
    return isCostEffective(pas, reducedCost) && isFlowEffective(pas, originBush, flowAcceptanceFactors);
  }

  /**
   * Remove the PAS from the manager
   * 
   * @param pas to remove
   */
  private void removePas(final Pas pas) {
    passByMergeVertex.get(pas.getMergeVertex()).remove(pas);
  }

  /**
   * Extract a subpath in the form of a raw edge segment array from start to end vertex based on the shortest path result provided. Since the path tree is in reverse direction, the
   * array is filled from the back, i.e.,if there is spare cpacity the front of the array would be empty.
   * 
   * @param start       start vertex upstream
   * @param end         end vertex downstream
   * @param pathTree    to extract path from, tree is in upstream direction
   * @param arrayLength to use for the to be created array which should be at least as long as the path that is to be extracted
   * @return created array, null if no path could be found
   */
  public static EdgeSegment[] createSubpathArrayFrom(final DirectedVertex start, final DirectedVertex end, final ShortestPathResult pathTree, int arrayLength) {
    EdgeSegment[] edgeSegmentArray = new EdgeSegment[arrayLength];
    DirectedVertex currVertex = end;
    EdgeSegment currEdgeSegment = null;
    int index = edgeSegmentArray.length - 1;
    do {
      currEdgeSegment = pathTree.getIncomingEdgeSegmentForVertex(currVertex);
      edgeSegmentArray[index--] = currEdgeSegment;
      if (currEdgeSegment == null) {
        LOGGER.warning(String.format("Unable to extract subpath from start vertex %s to end vertex %s, no incoming edge segment available at intermediate vertex %s",
            start.getXmlId(), end.getXmlId(), currVertex.getXmlId()));
        return null;
      }
      currVertex = currEdgeSegment.getUpstreamVertex();
    } while (!currVertex.idEquals(start));
    return edgeSegmentArray;
  }

  /**
   * Extract a subpath in the form of a raw edge segment array from start to end vertex based on a map representing a tree with succeeding edge segments for each vertex
   * 
   * @param start       start vertex upstream
   * @param end         end vertex downstream
   * @param pathTree    to extract path from, tree is in downstream direction
   * @param arrayLength to use for the to be created array which should be at least as long as the path that is to be extracted
   * @return created array, null if no path could be found
   */
  public static EdgeSegment[] createSubpathArrayFrom(DirectedVertex start, DirectedVertex end, Map<DirectedVertex, EdgeSegment> pathTree, int arrayLength) {
    EdgeSegment[] edgeSegmentArray = new EdgeSegment[arrayLength];
    DirectedVertex currVertex = start;
    EdgeSegment currEdgeSegment = null;
    int index = 0;
    do {
      currEdgeSegment = pathTree.get(currVertex);
      edgeSegmentArray[index++] = currEdgeSegment;
      if (currEdgeSegment == null) {
        LOGGER.warning(String.format("Unable to extract subpath from start vertex %s to end vertex %s, no outgoing edge segment available at intermediate vertex %s",
            start.getXmlId(), end.getXmlId(), currVertex.getXmlId()));
        return null;
      }
      currVertex = currEdgeSegment.getDownstreamVertex();
    } while (!currVertex.idEquals(end));
    return edgeSegmentArray;
  }

  /**
   * Constructor
   */
  public PasManager() {
    this.passByMergeVertex = new HashMap<DirectedVertex, Collection<Pas>>();
  }

  /**
   * create a new PAS for the given cheap and expensive paired alternative segments (subpaths) and register the origin bush on it that was responsible for creating it
   * 
   * @param originBush responsible for triggering the creation of this PAS
   * @param s1         cheap alternative segment
   * @param s2         expensive alternative segment
   * @return createdPas
   */
  public Pas createNewPas(final Bush originBush, final EdgeSegment[] s1, final EdgeSegment[] s2) {
    Pas newPas = Pas.create(s1, s2);
    newPas.registerOrigin(originBush);
    passByMergeVertex.putIfAbsent(newPas.getMergeVertex(), new ArrayList<Pas>());
    passByMergeVertex.get(newPas.getMergeVertex()).add(newPas);
    return newPas;
  }

  /**
   * Collect all PASs that share the same merge (end) vertex
   * 
   * @param mergeVertex to collect for
   * @return found PAS matches
   */
  public Collection<Pas> getPassByMergeVertex(final DirectedVertex mergeVertex) {
    return passByMergeVertex.get(mergeVertex);
  }

  /**
   * find the first PAS which has the given merge vertex as end vertex and which if we would extend the bush with its least cost alternative would improve to the point it is
   * considered effective enough compared to the upper bound (reduced cost) improvement provided as well as that the bush has sufficient flow on the high cost alternative of the
   * PAS such that it can improve sufficiently by shifting flow towards the new low cost segment. If this all holds the PAS is selected and returned. We select the first PAS we can
   * find that matches this criteria.
   * 
   * @param originBush            to find suitable PAS for
   * @param mergeVertex           to use
   * @param flowAcceptanceFactors to use (required to assess flow effectiveness in capacitated context)
   * @param reducedCost           the upper bound on the improvement that is known for this merge vertex
   * @return pas found, null if no suitable candidates exist
   */
  public Pas findFirstSuitableExistingPas(final Bush originBush, final DirectedVertex mergeVertex, double[] flowAcceptanceFactors, double reducedCost) {

    /* verify potential PASs */
    Pas matchedPas = null;
    for (Pas pas : getPassByMergeVertex(mergeVertex)) {
      boolean pasPotentialMatch = false;
      for (EdgeSegment pasFirstExitSegment : pas.getDivergeVertex().getExitEdgeSegments()) {
        if (originBush.containsEdgeSegment(pasFirstExitSegment)) {
          pasPotentialMatch = true;
        }
      }

      /* PAS start/end node are on bush, now check PAS carefully if its high cost segment is present on the bush fully */
      if (!pasPotentialMatch) {
        continue;
      }

      if (isPasEffectiveForBush(pas, originBush, flowAcceptanceFactors, reducedCost)) {
        matchedPas = pas;
        break;
      }
    }
    return matchedPas;
  }

  /**
   * Shift flows based on the registered PASs, their origins, and the currently known link segment costs
   * 
   * @param linkSegmentCosts to use
   * @return true when at least some flows were shifted, false otherwise
   */
  public void updateCosts(final double[] linkSegmentCosts) {
    for (Collection<Pas> pass : passByMergeVertex.values()) {
      for (Pas pas : pass) {
        pas.updateCost(linkSegmentCosts);
      }
    }
  }

  /**
   * Shift flows based on the registered PASs and their origins
   * 
   * @param staticLtmNetworkLoading to obtain current flows on high cost PAS segments from
   * @param smoothing               to use when determining the desired flow shift to apply
   * @return true when at least some flows were shifted, false otherwise
   */
  public boolean shiftFlows(final StaticLtmLoadingBush networkLoading, final Smoothing smoothing) {
    boolean flowShifted = false;
    List<Pas> passWithoutOrigins = new ArrayList<Pas>();
    for (Collection<Pas> pass : passByMergeVertex.values()) {
      for (Pas pas : pass) {
        /* determine the network flow on the high cost subpath */
        double subPathTotalShiftableFlow = networkLoading.computeSubPathSendingFlow(pas.getDivergeVertex(), pas.getMergeVertex(), pas.getAlternative(true /* highCost */));
        /* DUMB approach -> shift all of it smoothed by MSA */
        double flowshift = smoothing.execute(subPathTotalShiftableFlow, 0);
        flowShifted |= pas.executeFlowShift(subPathTotalShiftableFlow, flowshift, networkLoading.getCurrentFlowAcceptanceFactors());
        if (!pas.hasOrigins()) {
          passWithoutOrigins.add(pas);
        }
      }
    }

    if (!passWithoutOrigins.isEmpty()) {
      passWithoutOrigins.forEach((pas) -> removePas(pas));
    }
    return flowShifted;
  }

}