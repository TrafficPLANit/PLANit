package org.planit.assignment.ltm.sltm;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

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
   * this pas comes from the high cost segment of this PAS which would allow for a decent chunk of the flow to be shifted to the low cost segment. If not, it would not improve this
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
   * Verify if PAS is considered effective (anough) to improve the provided bush. This is verified by being both {@link #isCostEffective(Pas, double)} and
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
   * Constructor
   */
  public PasManager() {
    this.passByMergeVertex = new HashMap<DirectedVertex, Collection<Pas>>();
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

}
