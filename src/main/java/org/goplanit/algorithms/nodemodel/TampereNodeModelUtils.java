package org.goplanit.algorithms.nodemodel;

import org.goplanit.utils.functionalinterface.TriConsumer;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.pcu.PcuCapacitated;
import org.ojalgo.array.Array1D;
import org.ojalgo.array.Array2D;
import org.ojalgo.function.PrimitiveFunction;
import org.ojalgo.structure.Access1D;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Utils class for the Tampere node model. Aimed at simplifying to prepare inputs based on PLANit networks
 * and network loading methods.
 *
 * @author markr
 */
public class TampereNodeModelUtils {

  /**
   * Method to create incoming capacities based on node's entry segments. It is assumed those implement the
   * {@link PcuCapacitated} interface to be able to obtain the capacity. Capacity is capped at DEFAULT_MAX_IN_CAPACITY
   * in case we are dealing with connectoid with infinite capacity which would through the computation out of whack.
   *
   * @param node to create incoming capacities for
   * @return incomingCapacities in compatible array1d form
   */
  public static Array1D<Double> createIncomingCapacities(DirectedVertex node){
    int numEntrySegments = node.getNumberOfEntryEdgeSegments();

    /* C_a : in Array1D form, capped to maximum physical capacity in case we are dealing with connectoid with
     * infinite capacity */
    var inCapacities = Array1D.PRIMITIVE64.makeZero(numEntrySegments);
    int index = 0;
    for (var entryEdgeSegment : node.getEntryEdgeSegments()) {
      inCapacities.set(
          index++,
          Math.min(TampereNodeModelFixedInput.DEFAULT_MAX_IN_CAPACITY,
              ((PcuCapacitated) entryEdgeSegment).getCapacityOrDefaultPcuH()));
    }
    return inCapacities;
  }

  /**
   * Method to create outgoing receiving flows based on node's exit segment's capacities.
   * It is assumed those implement the {@link PcuCapacitated} interface to be able to obtain the capacity.
   *
   * @param node to create incoming capacities for
   * @return incomingCapacities in compatible array1d form
   */
  public static Array1D<Double> createOutgoingReceivingFlows(DirectedVertex node){
    int numExitSegments = node.getNumberOfExitEdgeSegments();
    /* r_a : in Array1D form */
    var outReceivingFlows = Array1D.PRIMITIVE64.makeZero(numExitSegments);
    int index = 0;
    for (var exitEdgeSegment : node.getExitEdgeSegments()) {
      outReceivingFlows.set(index++, ((PcuCapacitated) exitEdgeSegment).getCapacityOrDefaultPcuH());
    }
    return outReceivingFlows;
  }

  /**
   * Create turn sending flows based on given node, a sending flow array indexed by the segment id and a function
   * that takes an edge segment (entry link) and produces the splitting rates in the same order of the entry segments
   * when looped over by the node.
   *
   * @param node to use
   * @param sendingFlowsBySegmentId ensing flows to use
   * @param createSplittingRatesForSegment splitting rate creator, invoked for each node entry segment.
   *                                       Must be an array1d that may be modified as it will be modified into the final
   *                                       turn sending flows that are returned.
   * @return turn sending flows to feed into the node model
   */
  public static Array2D<Double> createTurnSendingFlowsUsingSplittingRates(
      DirectedVertex node,
      double[] sendingFlowsBySegmentId,
      Function<EdgeSegment, Array1D<Double>> createSplittingRatesForSegment){

    int numEntrySegments = node.getNumberOfEntryEdgeSegments();

    Access1D<Double>[] tunSendingFlowsByEntryLinkSegment = (Access1D<Double>[]) new Access1D<?>[numEntrySegments];
    int entryIndex = 0;
    for (var iter = node.getEntryEdgeSegments().iterator(); iter.hasNext(); ++entryIndex) {
      EdgeSegment entryEdgeSegment = iter.next();
      /* s_ab = s_a*phi_ab */
      double sendingFlow = sendingFlowsBySegmentId[(int) entryEdgeSegment.getId()];
      Array1D<Double> localTurnSendingFlows = createSplittingRatesForSegment.apply(entryEdgeSegment).copy();
      localTurnSendingFlows.modifyAll(PrimitiveFunction.MULTIPLY.by(sendingFlow));
      tunSendingFlowsByEntryLinkSegment[entryIndex] = localTurnSendingFlows;
    }
    return Array2D.PRIMITIVE64.rows(tunSendingFlowsByEntryLinkSegment);
  }

  /**
   * Relate result from node model back to entry exit segment combinations and apply provided function to it
   *
   * @param node to use
   * @param entryExitSegmentResults results (typically from running Tampere node model in turn based form)
   * @param entryExitSegmentResultConsumer consumer to apply to result
   */
  public static void forEachTurnBasedResult(
      DirectedVertex node,
      Array2D<Double> entryExitSegmentResults,
      TriConsumer<EdgeSegment, EdgeSegment, Double> entryExitSegmentResultConsumer){

    int entryIndex = 0;
    int exitIndex = 0;
    for (var iter = node.getEntryEdgeSegments().iterator(); iter.hasNext(); ++entryIndex) {
      EdgeSegment entryEdgeSegment = iter.next();
      exitIndex = 0;
      for (var exitIter = node.getExitEdgeSegments().iterator(); exitIter.hasNext(); ++exitIndex) {
        EdgeSegment exitEdgeSegment = exitIter.next();
        Double result = entryExitSegmentResults.get(entryIndex, exitIndex);
        entryExitSegmentResultConsumer.accept(entryEdgeSegment, exitEdgeSegment, result);
      }
    }
  }

  /**
   * Relate result from node model back to entry exit segment combinations and apply provided function to it
   *
   * @param node to use
   * @param entrySegmentResultConsumer consumer to apply to result
   * @param entrySegmentResults results (typically from running Tampere node model in turn based form)
   */
  public static void forEachLinkBasedResult(
      DirectedVertex node,
      BiConsumer<EdgeSegment, Double> entrySegmentResultConsumer,
      Array1D<Double> entrySegmentResults){

    int entryIndex = 0;
    for (var iter = node.getEntryEdgeSegments().iterator(); iter.hasNext(); ++entryIndex) {
      EdgeSegment entryEdgeSegment = iter.next();
      Double result = entrySegmentResults.get(entryIndex);
      entrySegmentResultConsumer.accept(entryEdgeSegment, result);
    }
  }

}
