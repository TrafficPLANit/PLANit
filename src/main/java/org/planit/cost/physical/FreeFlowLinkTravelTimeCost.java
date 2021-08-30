package org.planit.cost.physical;

import org.planit.network.MacroscopicNetwork;
import org.planit.network.TransportLayerNetwork;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.mode.Mode;
import org.planit.utils.network.layer.MacroscopicNetworkLayer;
import org.planit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.planit.utils.network.layer.physical.LinkSegment;
import org.planit.utils.network.layer.physical.UntypedPhysicalLayer;

/**
 * Simplest possible travel time cost, namely fixed to free flow travel time regardless of the flows measured.
 *
 * @author markr
 */
public class FreeFlowLinkTravelTimeCost extends AbstractPhysicalCost {

  /** use generated UID */
  private static final long serialVersionUID = 4465724624295866542L;

  /** the network layer the cost is applied to */
  protected MacroscopicNetworkLayer networkLayer;

  /**
   * Constructor
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   */
  public FreeFlowLinkTravelTimeCost(IdGroupingToken groupId) {
    super(groupId);
  }

  /**
   * Copy Constructor
   * 
   * @param other to copy
   */
  public FreeFlowLinkTravelTimeCost(FreeFlowLinkTravelTimeCost other) {
    super(other);
    this.networkLayer = other.networkLayer;
  }

  /**
   * Return the free flow travel time for the current link for a given mode
   *
   * If the input data are invalid, this method returns a negative value.
   *
   * @param mode        the current Mode of travel
   * @param linkSegment the current link segment
   * @return the travel time for the current link (in hours)
   * @throws PlanItException when cost cannot be computed
   *
   */
  @Override
  public double getSegmentCost(final Mode mode, final MacroscopicLinkSegment linkSegment) throws PlanItException {
    return linkSegment.computeFreeFlowTravelTime(mode);
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
    for (LinkSegment linkSegment : physicalLayer.getLinkSegments()) {
      final int id = (int) linkSegment.getId();
      costToFill[id] = MacroscopicLinkSegment.class.cast(linkSegment).computeFreeFlowTravelTime(mode);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public FreeFlowLinkTravelTimeCost clone() {
    return new FreeFlowLinkTravelTimeCost(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void initialiseBeforeSimulation(TransportLayerNetwork<?, ?> network) throws PlanItException {
    PlanItException.throwIf(!(network instanceof MacroscopicNetwork), "Free flow  travel time cost is only compatible with macroscopic networks");
    MacroscopicNetwork macroscopicNetwork = (MacroscopicNetwork) network;
    PlanItException.throwIf(macroscopicNetwork.getTransportLayers().size() != 1,
        "Free flow travel time cost is currently only compatible with networks using a single infrastructure layer");
  }

}
