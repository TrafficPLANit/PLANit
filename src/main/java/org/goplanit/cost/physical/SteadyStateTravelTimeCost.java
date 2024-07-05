package org.goplanit.cost.physical;

import java.util.Arrays;
import java.util.Map;

import org.goplanit.interactor.LinkInflowOutflowAccessee;
import org.goplanit.interactor.LinkInflowOutflowAccessor;
import org.goplanit.network.LayeredNetwork;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.supply.fundamentaldiagram.FundamentalDiagram;
import org.goplanit.supply.fundamentaldiagram.FundamentalDiagramComponent;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegments;
import org.goplanit.utils.network.layer.physical.LinkSegment;
import org.goplanit.utils.network.layer.physical.UntypedPhysicalLayer;
import org.goplanit.utils.time.TimePeriod;
import org.goplanit.utils.unit.Unit;

/**
 * Cost computation for travel times based on the work of Raadsen and Bliemer (2019), Steady-state link travel time methods: Formulation, derivation, classification, and
 * unification.
 * <p>
 * Suitable for static and semi-dynamic traffic assignment methods that are steady-state with respect to their inflow and outflow rates which necessarily are able to take on
 * different values, where the difference between in and outflow results in a queue on the link. Most notable assignment method that adopts this method is sLTM.
 * <p>
 * Raadsen and Bliemer (2019) highlight three main computation methods to compute the same steady-state travel time on the link level. In this implementation we utilise the so
 * called static-functional - longitudinal method where we compute the travel time via three components: 1) free flow travel time + 2) hypocritical delay + 3) hypercritical delay
 *
 * @author markr
 */
public class SteadyStateTravelTimeCost extends AbstractPhysicalCost implements LinkInflowOutflowAccessor {

  /** use generated UID */
  private static final long serialVersionUID = 1270540193146782352L;

  /** accessee to use to obtain inflow and outflows to derive costs for */
  private LinkInflowOutflowAccessee accessee;

  /** the time period in hours for which we are computing costs. In case of steady state costs it is the duration of the period that is of interest */
  private double currentTimePeriodHours;

  /**
   * 2d Array to store (fixed) free flow travel times for each link segment to be used in calculateSegmentCost()
   */
  private double[] freeFlowTravelTimePerLinkSegment = null;

  /** tracking fundamental diagrams per link segment for performance reasons */
  private FundamentalDiagram[] linkSegmentFundamentalDiagrams = null;

  /**
   * Collect the fundamental diagram component from the accessee
   * 
   * @return fundamental diagram component
   */
  private FundamentalDiagramComponent getFundamentalDiagramComponent() {
    return accessee.getTrafficAssignmentComponent(FundamentalDiagramComponent.class);
  }

  /**
   * To speed up the computations we create the mapping between link segment and free flow travel times once and reuse it throughout the lifespan of this cost component. This
   * avoids repeating the same computations albeit at the cost of increased memory usage
   * 
   * @param linkSegments to create free flow travel time mapping for
   */
  private void initialiseFreeFlowTravelTimesPerLinkSegment(Mode mode, MacroscopicLinkSegments linkSegments) {
    this.freeFlowTravelTimePerLinkSegment = linkSegments.getFreeFlowTravelTimeHourPerLinkSegment(mode);
  }

  /**
   * To speed up the computations we create the mapping between link segment and fundamental diagram once and reuse it throughout the lifespan of this cost component. This avoids
   * repeating the same costly lookups via the component albeit at the cost of increased memory usage
   * 
   * @param linkSegments to create FD mapping for
   */
  private void initialiseFundamentalDiagramsPerLinkSegment(MacroscopicLinkSegments linkSegments) {
    linkSegmentFundamentalDiagrams = getFundamentalDiagramComponent().asLinkSegmentIndexedArray(linkSegments);
  }

  /**
   * Compute travel time based on static functional - longitudinal perspective as per Raadsen and Bliemer (2019)
   * 
   * @param linkSegment        to use
   * @param fd                 to use
   * @param inflowRatePcuHour  to use
   * @param outflowRatePcuHour to use
   * @return travel time computed, when outflow is zero and inflow is positive an infinite travel time is returned
   */
  private double computeTravelTime(LinkSegment linkSegment, FundamentalDiagram fd, double inflowRatePcuHour, double outflowRatePcuHour) {
    /* minimum travel time */
    double freeFlowTravelTime = freeFlowTravelTimePerLinkSegment[(int) linkSegment.getLinkSegmentId()];

    double hypoCriticalDelay = 0;
    double hyperCriticalDelay = 0;

    if (Precision.positive(inflowRatePcuHour)) {
      /* hypo critical delay */
      if (!fd.getFreeFlowBranch().isLinear()) {
        // hypocritical delay = hypocritical travel time - minimum travel time
        hypoCriticalDelay = (linkSegment.getParentLink().getLengthKm() / fd.getFreeFlowBranch().getSpeedKmHourByFlow(inflowRatePcuHour)) - freeFlowTravelTime;
      }

      /* average hyper critical delay */
      if (Precision.smaller(outflowRatePcuHour, inflowRatePcuHour)) {

        if (!Precision.positive(outflowRatePcuHour)) {
          LOGGER.warning(String.format("Link segment (%s) appears to have no outflow while positive inflow (%.2f) -> infinite travel time, this is unlikely",
              linkSegment.getIdsAsString(), inflowRatePcuHour));
          return Double.POSITIVE_INFINITY;
        }

        // hyperCriticalDelay = (excess inflow rate * 1/2* duration)/outflow rate)
        hyperCriticalDelay = ((inflowRatePcuHour - outflowRatePcuHour) * 0.5 * currentTimePeriodHours / outflowRatePcuHour);
      }
    }

    /* min travel time + hypo critical delay + hypercritical delay */
    return freeFlowTravelTime + hypoCriticalDelay + hyperCriticalDelay;
  }

  /**
   * Constructor
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   */
  public SteadyStateTravelTimeCost(IdGroupingToken groupId) {
    super(groupId);
    this.accessee = null;
  }

  /**
   * Copy Constructor
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  public SteadyStateTravelTimeCost(SteadyStateTravelTimeCost other, boolean deepCopy /*no impact at present */) {
    super(other, deepCopy);
    this.accessee = other.accessee;

    this.currentTimePeriodHours = other.currentTimePeriodHours;
    this.freeFlowTravelTimePerLinkSegment = Arrays.copyOf(other.freeFlowTravelTimePerLinkSegment, other.freeFlowTravelTimePerLinkSegment.length);
    this.linkSegmentFundamentalDiagrams = Arrays.copyOf(other.linkSegmentFundamentalDiagrams, other.linkSegmentFundamentalDiagrams.length);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void initialiseBeforeSimulation(LayeredNetwork<?, ?> network) throws PlanItException {
    PlanItException.throwIf(!(network instanceof MacroscopicNetwork), "Steady state travel time cost is only compatible with macroscopic networks");
    var macroscopicNetwork = (MacroscopicNetwork) network;
    PlanItException.throwIf(macroscopicNetwork.getTransportLayers().size() != 1,
        "Steady state travel time cost is currently only compatible with networks using a single infrastructure layer");
    PlanItException.throwIf(macroscopicNetwork.getTransportLayers().size() != 1, "Steady state travel time cost is currently only compatible with a single mode, found %d",
        network.getModes().size());

    var mode = network.getModes().getFirst();
    var linkSegments = ((MacroscopicNetwork) network).getLayerByMode(mode).getLinkSegments();
    initialiseFreeFlowTravelTimesPerLinkSegment(mode, linkSegments);
    initialiseFundamentalDiagramsPerLinkSegment(linkSegments);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void updateTimePeriod(final TimePeriod timePeriod) {
    try {
      this.currentTimePeriodHours = Unit.SECOND.convertTo(Unit.HOUR, timePeriod.getDurationSeconds());
    } catch (Exception e) {
      LOGGER.severe(String.format("Unable to convert seconds to hours for time period %s in steady-state travel time cost", timePeriod.getXmlId()));
    }
  }

  /**
   * Return the average travel time for the current link segment for a given mode
   *
   * @param mode        the current Mode of travel
   * @param linkSegment the current link segment
   * @return the travel time for the current link (in hours)
   *
   */
  @Override
  public double getGeneralisedCost(final Mode mode, final MacroscopicLinkSegment linkSegment) {
    double inflow = accessee.getLinkSegmentInflowPcuHour(linkSegment);
    double outflow = accessee.getLinkSegmentOutflowPcuHour(linkSegment);

    int linkSegmentId = (int) linkSegment.getLinkSegmentId();
    return computeTravelTime(linkSegment, linkSegmentFundamentalDiagrams[linkSegmentId], inflow, outflow);
  }

  /**
   * Populate the cost array with the free flow link travel times for all link segments for the specified mode
   * 
   * @param layer to use
   * @param mode          the mode to use
   * @param costToFill    the cost to populate (in hours)
   */
  @Override
  public void populateWithCost(UntypedPhysicalLayer<?, ?, MacroscopicLinkSegment> layer, Mode mode, double[] costToFill) {
    double[] inflows = accessee.getLinkSegmentInflowsPcuHour();
    double[] outflows = accessee.getLinkSegmentOutflowsPcuHour();
    for (var linkSegment : layer.getLinkSegments()) {
      int linkSegmentId = (int) linkSegment.getLinkSegmentId();
      costToFill[linkSegmentId] = computeTravelTime(
              linkSegment, linkSegmentFundamentalDiagrams[linkSegmentId], inflows[linkSegmentId], outflows[linkSegmentId]);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SteadyStateTravelTimeCost shallowClone() {
    return new SteadyStateTravelTimeCost(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SteadyStateTravelTimeCost deepClone() {
    return new SteadyStateTravelTimeCost(this, true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setAccessee(final LinkInflowOutflowAccessee accessee) {
    this.accessee = accessee;
  }

  /**
   * Full reset returns to pre-{@link #initialiseBeforeSimulation(LayeredNetwork)} state.
   */
  @Override
  public void reset() {
    this.freeFlowTravelTimePerLinkSegment = null;
    this.linkSegmentFundamentalDiagrams = null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getTravelTimeCost(final Mode mode, final MacroscopicLinkSegment linkSegment) {
    return getGeneralisedCost(mode, linkSegment);
  }

  /**
   * First Derivative towards inflowRate. HypocriticalDelay is FD dependent. HyperCritical delay equates to (timePeriod duration/2) * (1/outflowRate).
   * 
   * {@inheritDoc}
   */
  @Override
  public double getDTravelTimeDFlow(boolean uncongested, Mode mode, MacroscopicLinkSegment linkSegment) {
    double outflowRatePcuH = accessee.getLinkSegmentOutflowPcuHour(linkSegment);

    int linkSegmentId = (int) linkSegment.getLinkSegmentId();
    FundamentalDiagram fd = linkSegmentFundamentalDiagrams[linkSegmentId];

    /* hypo critical delay derivative */
    if (uncongested) {
      if (fd.getFreeFlowBranch().isLinear()) {
        // 0 -> linear free flow branch
        return 0.0;
      } else {
        LOGGER.severe("Steady state travel time implementation does not yet support derivative of hypocritical delay on non-linear uncongested FD branches");
        throw new RuntimeException("Unable to continue due to error in Steady State travel time cost computation");
      }
    }
    /* hyperCriticalDelay derivative */
    else {
      if (Precision.positive(outflowRatePcuH)) {
        /* congested derivative (T/2)*(1/v) */
        return 0.5 * currentTimePeriodHours / outflowRatePcuH;
      } else {
        /* avoid division by zero, if no outflow rate but congested, it is undesirable to use this link, we return infinity */
        return Double.POSITIVE_INFINITY;
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<String, String> collectSettingsAsKeyValueMap() {
    // no settings
    return null;
  }

}
