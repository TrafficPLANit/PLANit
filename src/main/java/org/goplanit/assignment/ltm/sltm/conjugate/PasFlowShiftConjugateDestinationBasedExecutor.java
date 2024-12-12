package org.goplanit.assignment.ltm.sltm.conjugate;

import org.goplanit.assignment.ltm.sltm.*;
import org.goplanit.assignment.ltm.sltm.loading.StaticLtmLoadingBushBase;
import org.goplanit.cost.physical.AbstractPhysicalCost;
import org.goplanit.cost.virtual.AbstractVirtualCost;
import org.goplanit.sdinteraction.smoothing.Smoothing;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.graph.directed.ConjugateDirectedVertex;
import org.goplanit.utils.graph.directed.ConjugateEdgeSegment;
import org.goplanit.utils.mode.Mode;

import java.util.Map;
import java.util.logging.Logger;

/**
 * Functionality to conduct a PAS flow shift based on underlying destination based conjugate bush approach.
 *
 * @author markr
 *
 */
public class PasFlowShiftConjugateDestinationBasedExecutor
        extends PasFlowShiftExecutor<ConjugateDirectedVertex, ConjugateEdgeSegment> {

  /**
   * Logger to use
   */
  private final static Logger LOGGER = Logger.getLogger(
          PasFlowShiftConjugateDestinationBasedExecutor.class.getCanonicalName());

  /**
   * {@inheritDoc}
   */
  @Override
  protected double[] executeBushS2FlowShift(
          RootedBush<ConjugateDirectedVertex, ConjugateEdgeSegment> bush, ConjugateEdgeSegment entrySegment, double bushEntrySegmentFlowShift, double[] flowAcceptanceFactors) {
    throw new PlanItRunTimeException("not yet implemented");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void executeBushS1FlowShift(RootedBush<ConjugateDirectedVertex, ConjugateEdgeSegment> bush, ConjugateEdgeSegment entrySegment, double bushEntrySegmentFlowShift, double[] flowAcceptanceFactors, double[] endMergeSplittingRates) {
    throw new PlanItRunTimeException("not yet implemented");
  }

  @Override
  public void stepOneDetermineNetworkLoadingConsistentS1S2EntrySendingFlows(double[] flowAcceptanceFactors) {
    throw new PlanItRunTimeException("not yet implemented");
  }

  @Override
  public Map<ConjugateEdgeSegment, Double> determineProposedFlowShiftByEntrySegment(
          Mode theMode,
          AbstractPhysicalCost physicalCost,
          AbstractVirtualCost virtualCost,
          StaticLtmLoadingBushBase<?> networkLoading,
          double discontinuityDampeningFactor) {
    throw new PlanItRunTimeException("not yet implemented");
  }

  @Override
  public boolean performS2FlowShift(
          Map<ConjugateEdgeSegment, Double> proposedFlowShifts,
          Mode theMode,
          StaticLtmLoadingBushBase<?> networkLoading,
          Smoothing smoothing,
          boolean logAll) {
    throw new PlanItRunTimeException("not yet implemented");
  }

  @Override
  public void performS1FlowShift(
          Mode theMode,
          StaticLtmLoadingBushBase<?> networkLoading) {
    throw new PlanItRunTimeException("not yet implemented");
  }

  @Override
  public double getS2SendingFlow() {
    throw new PlanItRunTimeException("not yet implemented");
  }

  @Override
  public double getS1SendingFlow() {
    throw new PlanItRunTimeException("not yet implemented");
  }

  /**
   * Constructor
   *
   * @param pas      to use
   * @param settings to use
   */
  protected PasFlowShiftConjugateDestinationBasedExecutor(
          final Pas<ConjugateDirectedVertex, ConjugateEdgeSegment> pas, final StaticLtmSettings settings) {
    super(pas, settings);
  }

}
