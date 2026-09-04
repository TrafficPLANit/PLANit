package org.goplanit.cost.virtual;

import org.goplanit.cost.SteadyStateHyperCriticalTravelTimeCalculator;
import org.goplanit.cost.physical.AbstractPhysicalCost;
import org.goplanit.cost.physical.SteadyStateTravelTimeCost;
import org.goplanit.interactor.LinkInflowOutflowAccessee;
import org.goplanit.interactor.LinkInflowOutflowAccessor;
import org.goplanit.network.LayeredNetwork;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.supply.fundamentaldiagram.FundamentalDiagram;
import org.goplanit.supply.fundamentaldiagram.FundamentalDiagramComponent;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegments;
import org.goplanit.utils.network.layer.physical.LinkSegment;
import org.goplanit.utils.network.layer.physical.UntypedPhysicalLayer;
import org.goplanit.utils.network.virtual.VirtualNetwork;
import org.goplanit.utils.network.virtual.physical.ConnectoidSegment;
import org.goplanit.utils.pcu.PcuCapacitated;
import org.goplanit.utils.time.TimePeriod;
import org.goplanit.utils.unit.Unit;

import java.util.Arrays;
import java.util.Map;

/**
 * Connectoid cost computation for travel times based on the work of Raadsen and Bliemer (2019), Steady-state link travel time
 * methods: Formulation, derivation, classification, and unification.
 * <p>
 * Suitable for static and semi-dynamic traffic assignment methods that are steady-state with respect to their
 * inflow and outflow rates which necessarily are able to take on different values, where the difference between in
 * and outflow results in a queue on the link. Most notable assignment method that adopts this method is sLTM.
 * </p>
 * <p>
 * Raadsen and Bliemer (2019) highlight three main computation methods to compute the same steady-state travel time
 * on the link level. In this implementation we adopt the exact same approach as for its physical cost equivalent albeit
 * that we opt for a fixed hypo critical cost, whereas the hyper critical delay is based on any queue that exists.
 * </p>
 * <p>
 *   the hypo critical aspect of this cost is currently always a fixed cost.
 * </p>
 *
 * @author markr
 */
public class SteadyStateConnectoidTravelTimeCost extends AbstractVirtualCost implements LinkInflowOutflowAccessor {

  private FixedConnectoidTravelTimeCost hypoCriticalCost;

  /** accessee to use to obtain inflow and outflows to derive costs for */
  private LinkInflowOutflowAccessee accessee;

  /** the time period in hours for which we are computing costs. In case of steady state costs it is the duration
   * of the period that is of interest */
  private double currentTimePeriodHours;

  /**
   * Compute travel time based on hypo connectoid fixed cost and hyper steady state cost
   *
   * @param linkSegment        to use
   * @param inflowRatePcuHour  to use
   * @param outflowRatePcuHour to use
   * @return travel time computed, when outflow is zero and inflow is positive an infinite travel time is returned
   */
  private double computeTravelTime(ConnectoidSegment linkSegment, double inflowRatePcuHour, double outflowRatePcuHour) {
    double hypoTravelTime = hypoCriticalCost.getFixedConnectoidCost();

    if(inflowRatePcuHour > ((PcuCapacitated)linkSegment).getCapacityOrDefaultPcuH()){
      if((inflowRatePcuHour - Precision.EPSILON_1) > ((PcuCapacitated)linkSegment).getCapacityOrDefaultPcuH()) {
        LOGGER.warning(String.format("Inflow rate (%.2f) exceed capacity for virtual link (%s), truncate to capacity (%.2f): ",
            inflowRatePcuHour, linkSegment.getIdsAsString(), ((PcuCapacitated) linkSegment).getCapacityOrDefaultPcuH()));
      }
      inflowRatePcuHour = ((PcuCapacitated)linkSegment).getCapacityOrDefaultPcuH();
    }

    double hyperCriticalDelay = SteadyStateHyperCriticalTravelTimeCalculator.computeHyperCriticalDelay(
        inflowRatePcuHour, outflowRatePcuHour, linkSegment.getNumberOfLanes(), currentTimePeriodHours);

    return hypoTravelTime + hyperCriticalDelay;
  }

  /**
   * Constructor
   *
   * @param groupId contiguous id generation within this group for instances of this class
   */
  public SteadyStateConnectoidTravelTimeCost(IdGroupingToken groupId) {
    super(groupId);
    this.hypoCriticalCost = new FixedConnectoidTravelTimeCost(groupId);
  }

  /**
   * Copy Constructor
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  public SteadyStateConnectoidTravelTimeCost(SteadyStateConnectoidTravelTimeCost other, boolean deepCopy /*no impact at present */) {
    super(other, deepCopy);
    if (deepCopy){
      this.hypoCriticalCost = other.hypoCriticalCost.deepClone();
    }else {
      this.hypoCriticalCost = other.hypoCriticalCost.shallowClone();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void initialiseBeforeSimulation(VirtualNetwork virtualNetwork) throws PlanItException {
    // hyper critical cost not needed to initialise as we do not rely on anything but flow information currently
    this.hypoCriticalCost.initialiseBeforeSimulation(virtualNetwork);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void updateTimePeriod(final TimePeriod timePeriod) {
    this.currentTimePeriodHours = timePeriod.getDurationHours();
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public void populateWithCost(final VirtualNetwork virtualNetwork, Mode mode, double[] costToFill) {
    double[] inflows = accessee.getLinkSegmentInflowsPcuHour();
    double[] outflows = accessee.getLinkSegmentOutflowsPcuHour();
    for (var linkSegment : virtualNetwork.getLayer().getConnectoidSegments()) {
      int linkSegmentId = (int) linkSegment.getLinkSegmentId();
      costToFill[linkSegmentId] = computeTravelTime(linkSegment, inflows[linkSegmentId], outflows[linkSegmentId]);
    }
  }

  @Override
  public String getName() {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SteadyStateConnectoidTravelTimeCost shallowClone() {
    return new SteadyStateConnectoidTravelTimeCost(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SteadyStateConnectoidTravelTimeCost deepClone() {
    return new SteadyStateConnectoidTravelTimeCost(this, true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setAccessee(final LinkInflowOutflowAccessee accessee) {
    this.accessee = accessee;
  }

  /**
   * Full reset returns to pre-{@link #initialiseBeforeSimulation(VirtualNetwork)}state.
   */
  @Override
  public void reset() {
    hypoCriticalCost.reset();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getTravelTimeCost(final Mode mode, final ConnectoidSegment linkSegment) {
    double[] inflows = accessee.getLinkSegmentInflowsPcuHour();
    double[] outflows = accessee.getLinkSegmentOutflowsPcuHour();

    int linkSegmentId = (int) linkSegment.getLinkSegmentId();
    return computeTravelTime(linkSegment, inflows[linkSegmentId], outflows[linkSegmentId]);
  }

  @Override
  public double getGeneralisedCost(Mode mode, ConnectoidSegment edgeSegment) {
    return getTravelTimeCost(mode, edgeSegment);
  }

  /**
   * First Derivative towards inflowRate.
   * <p>HypocriticalDelay is based on fixed cost so zero </p>
   * <p>HyperCritical delay equates to (timePeriod duration/2) * (1/outflowRate)</p>
   * 
   * {@inheritDoc}
   */
  @Override
  public double getDTravelTimeDFlow(boolean uncongested, Mode mode, ConnectoidSegment linkSegment) {
    double hypoDerivative = hypoCriticalCost.getDTravelTimeDFlow(uncongested, mode, linkSegment);

    /* hyperCriticalDelay derivative */
    double hyperDerivative = SteadyStateHyperCriticalTravelTimeCalculator.computeDTravelTimeDFlow(
        uncongested,
        accessee.getLinkSegmentInflowPcuHour(linkSegment),
        accessee.getLinkSegmentOutflowPcuHour(linkSegment),
        currentTimePeriodHours);
    return hypoDerivative + hyperDerivative;

  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<String, String> collectSettingsAsKeyValueMap() {
    // no settings
    return null;
  }

  /**
   * Access to the current time period in hours considered
   * @return time period in hours
   */
  public double getCurrentTimePeriodH() {
    return currentTimePeriodHours;
  }

}
