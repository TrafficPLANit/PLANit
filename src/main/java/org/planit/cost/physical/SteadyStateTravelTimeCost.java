package org.planit.cost.physical;

import org.planit.interactor.LinkInflowOutflowAccessee;
import org.planit.interactor.LinkInflowOutflowAccessor;
import org.planit.network.MacroscopicNetwork;
import org.planit.network.TransportLayerNetwork;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.mode.Mode;
import org.planit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.planit.utils.network.layer.physical.LinkSegment;
import org.planit.utils.network.layer.physical.UntypedPhysicalLayer;

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
   */
  public SteadyStateTravelTimeCost(SteadyStateTravelTimeCost other) {
    super(other);
    this.accessee = other.accessee;
  }

  /**
   * Return the average travel time for the current link segment for a given mode
   *
   * @param mode        the current Mode of travel
   * @param linkSegment the current link segment
   * @return the travel time for the current link (in hours)
   * @throws PlanItException when cost cannot be computed
   *
   */
  @Override
  public double getSegmentCost(final Mode mode, final MacroscopicLinkSegment linkSegment) throws PlanItException {
    double inflow = accessee.getLinkSegmentInflowPcuHour(linkSegment);
    double outflow = accessee.getLinkSegmentOutflowPcuHour(linkSegment);

    double hypoCriticalDelay = 0;
    double hyperCriticalDelay = 0;

    /* min travel time + hypo critical delay + hypercritical delay */
    return linkSegment.computeFreeFlowTravelTimeHour(mode) + hypoCriticalDelay + hyperCriticalDelay;
  }

  /**
   * Populate the cost array with the free flow link travel times for all link segments for the specified mode
   * 
   * @param physicalLayer to use
   * @param mode          the mode to use
   * @param costToFill    the cost to populate (in hours)
   */
  @Override
  public void populateWithCost(UntypedPhysicalLayer<?, ?, ?, ?, ?, ?> physicalLayer, Mode mode, double[] costToFill) throws PlanItException {
    double[] inflows = accessee.getLinkSegmentInflowsPcuHour();
    double[] outflows = accessee.getLinkSegmentOutflowsPcuHour();
    for (LinkSegment linkSegment : physicalLayer.getLinkSegments()) {
      double hypoCriticalDelay = 0;
      double hyperCriticalDelay = 0;

      /* min travel time + hypo critical delay + hypercritical delay */
      costToFill[(int) linkSegment.getId()] = ((MacroscopicLinkSegment) linkSegment).computeFreeFlowTravelTimeHour(mode) + hypoCriticalDelay + hyperCriticalDelay;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SteadyStateTravelTimeCost clone() {
    return new SteadyStateTravelTimeCost(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void initialiseBeforeSimulation(TransportLayerNetwork<?, ?> network) throws PlanItException {
    PlanItException.throwIf(!(network instanceof MacroscopicNetwork), "Steady state travel time cost is only compatible with macroscopic networks");
    MacroscopicNetwork macroscopicNetwork = (MacroscopicNetwork) network;
    PlanItException.throwIf(macroscopicNetwork.getTransportLayers().size() != 1,
        "Steady state travel time cost is currently only compatible with networks using a single infrastructure layer");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setAccessee(final LinkInflowOutflowAccessee accessee) {
    this.accessee = accessee;
  }

}
