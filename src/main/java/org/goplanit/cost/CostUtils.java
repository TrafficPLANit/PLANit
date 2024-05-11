package org.goplanit.cost;

import org.goplanit.cost.physical.AbstractPhysicalCost;
import org.goplanit.cost.physical.PhysicalCost;
import org.goplanit.cost.physical.initial.InitialModesLinkSegmentCost;
import org.goplanit.cost.virtual.VirtualCost;
import org.goplanit.network.UntypedPhysicalNetwork;
import org.goplanit.network.transport.TransportModelNetwork;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.graph.GraphEntities;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegments;
import org.goplanit.utils.network.layer.physical.UntypedPhysicalLayer;
import org.goplanit.utils.network.virtual.VirtualNetwork;
import org.goplanit.zoning.Zoning;

import java.util.logging.Logger;

/**
 * Utilities to assist in prepping or filling low level array based costs for assignment or otherwise assuming concrete cost
 * instances and network (virtual or otherwise) are available.
 *
 * @author markr
 */
public class CostUtils {

  /** Logger to use */
  private static final Logger LOGGER = Logger.getLogger(CostUtils.class.getCanonicalName());

  /**
   * Create an empty cost array for all link segments in both virtual and physical part of the network
   *
   * @param network to use
   * @param zoning   to apply to virtual part of network
   * @return generalised cost array by link segment id
   */
  public static double[] createEmptyLinkSegmentCostArray(UntypedPhysicalNetwork network, Zoning zoning){
    if(network.getTransportLayers().size()>1){
      //todo: eventually the costs should be tracked per layer as id numbering of link segments is only unique per layer, for now we crash in case someone tries
      throw new PlanItRunTimeException("Link segment cost array can only be created if physical network only has a single layer, multi-layer support has not yet been implemented");
    }
    return new double[TransportModelNetwork.getNumberOfEdgeSegmentsAllLayers(network, zoning)];
  }

  /**
   * Create an empty cost array for all physical link segments assuming ONLY physical link segments exist
   *
   * @param network to use
   * @return generalised cost array by link segment id
   */
  public static double[] createEmptyLinkSegmentCostArray(UntypedPhysicalNetwork network){
    if(network.getTransportLayers().size()>1){
      //todo: eventually the costs should be tracked per layer as id numbering of link segments is only unique per layer, for now we crash in case someone tries
      throw new PlanItRunTimeException("Link segment cost array can only be created if physical network only has a single layer, multi-layer support has not yet been implemented");
    }
    return new double[TransportModelNetwork.getNumberOfPhysicalLinkSegmentsAllLayers(network)];
  }

  /**
   * Populate part of cost array for virtual link segment costs based on the concrete cost class, for a given mode
   *
   * @param mode to use
   * @param virtualCost   to apply to virtual part of network
   * @param virtualNetwork virtualNetwork containing virtual part of the network
   * @param costArray to fill
   */
  public static void populateModalVirtualLinkSegmentCosts(Mode mode, VirtualCost virtualCost, VirtualNetwork virtualNetwork, double[] costArray){
    if(virtualNetwork.getConnectoidSegments().isEmpty()){
      LOGGER.warning("No connectoid segments found in provided virtual network, unable to populate connectoid segment costs");
    }
    for (var currentSegment : virtualNetwork.getConnectoidSegments()) {
      costArray[(int) currentSegment.getId()] = virtualCost.getGeneralisedCost(mode, currentSegment);
    }
  }

  /**
   * Populate part of cost array with physical link segment costs based on the concrete cost class, for a given mode
   *
   * @param mode to use
   * @param physicalCost to apply to physical part of network
   * @param network physical network
   * @param costArray to fill
   */
  public static void populateModalPhysicalLinkSegmentCosts(
          Mode mode, AbstractPhysicalCost physicalCost, UntypedPhysicalNetwork<?,?> network, double[] costArray) {
    physicalCost.populateWithCost((UntypedPhysicalLayer<?, ?, MacroscopicLinkSegment>) network.getLayerByMode(mode), mode, costArray);
  }

  /**
   * Populate part of cost array with physical link segment costs based on the concrete cost class, for a given mode
   *
   * @param <T> concrete type of link segments
   * @param mode to use
   * @param physicalCost to apply to physical part of network
   * @param linkSegments physical link segments to consider
   * @param costArray to fill
   */
  @SuppressWarnings("unchecked")
  public static <T extends GraphEntities<? extends MacroscopicLinkSegment>> void populateModalPhysicalLinkSegmentCosts(
          Mode mode, PhysicalCost<MacroscopicLinkSegment> physicalCost, T linkSegments, double[] costArray) {
    physicalCost.populateWithCost((GraphEntities<MacroscopicLinkSegment>)linkSegments, mode, costArray);
  }

  /**
   * Populate cost array with only physical link segment costs based on the concrete cost classes, for given mode. Note that this requires no edge segments of any other type
   * than physical link segments to be present, otherwise the indexing in the raw cost array may be inconsistent
   *
   * @param mode to use
   * @param physicalCost to apply to physical part of network
   * @param network physical network
   * @return generalised cost array by link segment id
   */
  public static double[] createAndPopulateModalSegmentCost(Mode mode, AbstractPhysicalCost physicalCost, UntypedPhysicalNetwork<?,?> network){
    double[] segmentCosts =createEmptyLinkSegmentCostArray(network);
    populateModalPhysicalLinkSegmentCosts(mode, physicalCost, network, segmentCosts);
    return segmentCosts;
  }

  /**
   * Populate entire cost array with both virtual and non-virtual link segment costs based on the concrete cost classes, for given mode
   *
   * @param mode to use
   * @param virtualCost   to apply to virtual part of network
   * @param physicalCost to apply to physical part of network
   * @param network physical network
   * @param zoning zoning containing virtual part of the network
   * @return generalised cost array by link segment id
   */
  public static double[] createAndPopulateModalSegmentCost(Mode mode, VirtualCost virtualCost, AbstractPhysicalCost physicalCost, UntypedPhysicalNetwork network, Zoning zoning){
    double[] segmentCosts =createEmptyLinkSegmentCostArray(network, zoning);
    populateModalVirtualLinkSegmentCosts(mode, virtualCost, zoning.getVirtualNetwork(), segmentCosts);
    populateModalPhysicalLinkSegmentCosts(mode, physicalCost, network, segmentCosts);
    return segmentCosts;
  }

  /**
   * Simple convenience method to populate a raw cost array with free flow link segment costs for a given model
   * @param mode to use
   * @param linkSegments to apply to
   * @param costArray to fill
   */
  public static void populateModalFreeFlowPhysicalLinkSegmentCosts(Mode mode, MacroscopicLinkSegments linkSegments, double[] costArray) {
    linkSegments.populateFreeFlowTravelTimeHourPerLinkSegment(mode, costArray);
  }
}
