package org.planit.cost.physical;

import java.util.HashMap;
import java.util.Map;
import org.planit.interactor.LinkVolumeAccessee;
import org.planit.interactor.LinkVolumeAccessor;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.network.physical.macroscopic.MacroscopicNetwork;
import org.planit.utils.arrays.ArrayUtils;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.misc.Pair;
import org.planit.utils.network.physical.Link;
import org.planit.utils.network.physical.LinkSegment;
import org.planit.utils.network.physical.Mode;
import org.planit.utils.network.physical.Node;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegment;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegmentType;

/**
 * Well known BPR link performance function to compute travel time cost on link segment based on flow and configuration parameters.
 *
 * @author markr
 */
public class BPRLinkTravelTimeCost extends AbstractPhysicalCost implements LinkVolumeAccessor {

  /** generated UID */
  private static final long serialVersionUID = -1529475107840907959L;

  /**
   * Inner class to store Map of alpha and beta parameters used in BPR function for each mode
   */
  public class BPRParameters {

    /**
     * Alpha and Beta parameters in BPR function
     */
    private final Map<Mode, Pair<Double, Double>> parametersMap;

    /**
     * Constructor
     */
    public BPRParameters() {
      parametersMap = new HashMap<Mode, Pair<Double, Double>>();
    }

    /**
     * Store BPR parameters for a specified mode
     *
     * @param mode  mode of travel
     * @param alpha BPR alpha value
     * @param beta  BPR beta value
     */
    private void registerParameters(final Mode mode, final double alpha, final double beta) {
      parametersMap.put(mode, new Pair<Double, Double>(alpha, beta));
    }

    /**
     * Store BPR parameters for a specified mode as a Pair
     *
     * @param mode mode of travel
     * @param pair Pair containing BPR alpha and beta values
     */
    private void registerParameters(final Mode mode, final Pair<Double, Double> pair) {
      parametersMap.put(mode, pair);
    }

    /**
     * Retrieve Pair containing alpha and beta values for a specified mode
     *
     * @param mode mode of travel
     * @return Pair containing BPR alpha and beta values
     */
    public Pair<Double, Double> getAlphaBetaParameters(final Mode mode) {
      return parametersMap.get(mode);
    }
  }
 
  /** the network */
  protected MacroscopicNetwork network;

  /**
   * Link volume accessee object for this cost function
   */
  protected LinkVolumeAccessee linkVolumeAccessee = null;

  /**
   * Default alpha and beta values for all links
   */
  protected Pair<Double, Double> defaultParameters;

  /**
   * Map to store default alpha and beta values for each mode
   */
  protected BPRParameters defaultParametersPerMode;

  /**
   * Map to store default alpha and beta values for each link type and mode
   */
  protected Map<MacroscopicLinkSegmentType, BPRParameters> defaultParametersPerLinkSegmentTypeAndMode;

  /**
   * Map to store default alpha and beta values for a specific link segment
   */
  protected Map<MacroscopicLinkSegment, BPRParameters> parametersPerLinkSegmentAndMode;

  /**
   * Array to store BPRParameters objects for each link segment to be used in calculateSegmentCost()
   */
  protected BPRParameters[] bprParametersPerLinkSegment;
  
  /**
   * 2d Array to store free flow travel time for each [mode][link segment] to be used in calculateSegmentCost()
   */
  protected double[][] freeFlowTravelTimePerLinkSegment; 
  
  /**
   * BPR function computation for
   * 
   * @param linkSegment the link segment
   * @param mode given mode
   * @param flowPcuPerHour available flow
   * @return travel time in hours
   */
  protected double computeCostInHours(MacroscopicLinkSegment linkSegment, Mode mode, double flowPcuPerHour) {
    final int id = (int) linkSegment.getId();
    
    final double freeFlowTravelTime = freeFlowTravelTimePerLinkSegment[(int) mode.getId()][id];
    final double capacity = linkSegment.computeCapacityPcuH();
    
    final Pair<Double, Double> alphaBetaParameters = bprParametersPerLinkSegment[id].getAlphaBetaParameters(mode);
    final double alpha = alphaBetaParameters.getFirst();
    final double beta = alphaBetaParameters.getSecond();

    // Travel Time * (1 + alpha*(v/c)^beta)
    return freeFlowTravelTime * (1.0 + alpha * Math.pow(flowPcuPerHour / capacity, beta));                                                                                                      
  }  
  
  
  /**
   * Default alpha BPR parameter if not other information is available
   */
  public static final double DEFAULT_ALPHA = 0.5;

  /**
   * Default beta BPR parameter if not other information is available
   */
  public static final double DEFAULT_BETA = 4.0;  

  /**
   * Constructor
   * 
   * @param groupId, contiguous id generation within this group for instances of this class
   */
  public BPRLinkTravelTimeCost(IdGroupingToken groupId) {
    super(groupId);
    parametersPerLinkSegmentAndMode = new HashMap<MacroscopicLinkSegment, BPRParameters>();
    defaultParametersPerMode = new BPRParameters();
    defaultParametersPerLinkSegmentTypeAndMode = new HashMap<MacroscopicLinkSegmentType, BPRParameters>();
    defaultParameters = new Pair<Double, Double>(DEFAULT_ALPHA, DEFAULT_BETA);
  }
  
  /**
   * Set the alpha and beta values for a given link segment and mode
   *
   * @param linkSegment the specified link segment
   * @param mode        specified mode type
   * @param alpha       alpha value
   * @param beta        beta value
   */
  public void setParameters(final MacroscopicLinkSegment linkSegment, final Mode mode, final double alpha, final double beta) {
    if (parametersPerLinkSegmentAndMode.get(linkSegment) == null) {
      parametersPerLinkSegmentAndMode.put(linkSegment, new BPRParameters());
    }
    parametersPerLinkSegmentAndMode.get(linkSegment).registerParameters(mode, alpha, beta);
  }

  /**
   * Set the default alpha and beta values for a mode
   *
   * @param mode  the specified mode type
   * @param alpha alpha value
   * @param beta  beta value
   */
  public void setDefaultParameters(final Mode mode, final double alpha, final double beta) {
    defaultParametersPerMode.registerParameters(mode, alpha, beta);
  }

  /**
   * Set the default alpha and beta values for a given link type and mode
   *
   * @param macroscopicLinkSegmentType the specified link type
   * @param mode                       the specified mode type
   * @param alpha                      alpha value
   * @param beta                       beta value
   */
  public void setDefaultParameters(final MacroscopicLinkSegmentType macroscopicLinkSegmentType, final Mode mode, final double alpha, final double beta) {
    if (defaultParametersPerLinkSegmentTypeAndMode.get(macroscopicLinkSegmentType) == null) {
      defaultParametersPerLinkSegmentTypeAndMode.put(macroscopicLinkSegmentType, new BPRParameters());
    }
    defaultParametersPerLinkSegmentTypeAndMode.get(macroscopicLinkSegmentType).registerParameters(mode, alpha, beta);
  }

  /**
   * Set the default alpha and beta values
   *
   * @param alpha alpha value
   * @param beta  beta value
   */
  public void setDefaultParameters(final double alpha, final double beta) {
    defaultParameters = new Pair<Double, Double>(alpha, beta);
  }  

  /**
   * Return the travel time for the current link for a given mode
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
  public double getSegmentCost(final Mode mode, final LinkSegment linkSegment) throws PlanItException {
    return computeCostInHours((MacroscopicLinkSegment)linkSegment, mode, linkVolumeAccessee.getLinkSegmentFlow(linkSegment));
  }
  
  /**
   * populate the cost array with the BPR link travel times for all link segments for the specified mode
   * 
   * @param mode the mode to use
   * @param costToFill the cost to populate (in hours)
   */
  @Override
  public void populateWithCost(Mode mode, double[] costToFill) throws PlanItException {
    double[] linkSegmentFlows = linkVolumeAccessee.getLinkSegmentFlows();
    
    for(MacroscopicLinkSegment linkSegment : network.linkSegments) {
      final int id = (int) linkSegment.getId();
      costToFill[id] = computeCostInHours(linkSegment, mode, linkSegmentFlows[id]);
    }
  }  

  /**
   * Register the BPR cost parameter values on the PhysicalNetwork
   *
   * Call this method after all the calls to set the cost parameters have been made
   *
   * @param physicalNetwork PhysicalNetwork object containing the updated parameter values
   * @throws PlanItException thrown if error
   */
  @Override
  public void initialiseBeforeSimulation(final PhysicalNetwork<? extends Node, ? extends Link, ? extends LinkSegment> physicalNetwork) throws PlanItException {
    PlanItException.throwIf(!(physicalNetwork instanceof MacroscopicNetwork), "BPR cost is oncly compatible with macroscopic networks");
    this.network = (MacroscopicNetwork)physicalNetwork;
    
    /* pre-compute the free flow travel times */
    freeFlowTravelTimePerLinkSegment = new double[network.modes.size()][ network.linkSegments.size()];
    ArrayUtils.loopAll(freeFlowTravelTimePerLinkSegment, (modeId, linkSegmentId) -> freeFlowTravelTimePerLinkSegment[modeId][linkSegmentId] = network.linkSegments.get(linkSegmentId).computeFreeFlowTravelTime(network.modes.get(modeId)));
    
    /* explicitly set bpr parameters for each mode/segment combination */
    bprParametersPerLinkSegment = new BPRParameters[network.linkSegments.size()];
    for (final MacroscopicLinkSegment macroscopicLinkSegment : network.linkSegments) {
      final int id = (int) macroscopicLinkSegment.getId();
      bprParametersPerLinkSegment[id] = new BPRParameters();
      final MacroscopicLinkSegmentType macroscopicLinkSegmentType = macroscopicLinkSegment.getLinkSegmentType();
      for (final Mode mode : network.modes) {
        Pair<Double, Double> alphaBetaPair;
        if ((parametersPerLinkSegmentAndMode.get(macroscopicLinkSegment) != null)
            && (parametersPerLinkSegmentAndMode.get(macroscopicLinkSegment).getAlphaBetaParameters(mode) != null)) {
          alphaBetaPair = parametersPerLinkSegmentAndMode.get(macroscopicLinkSegment).getAlphaBetaParameters(mode);
        } else if ((defaultParametersPerLinkSegmentTypeAndMode.get(macroscopicLinkSegmentType) != null)
            && (defaultParametersPerLinkSegmentTypeAndMode.get(macroscopicLinkSegmentType).getAlphaBetaParameters(mode) != null)) {
          alphaBetaPair = defaultParametersPerLinkSegmentTypeAndMode.get(macroscopicLinkSegmentType).getAlphaBetaParameters(mode);
        } else if (defaultParametersPerMode.getAlphaBetaParameters(mode) != null) {
          alphaBetaPair = defaultParametersPerMode.getAlphaBetaParameters(mode);
        } else {
          alphaBetaPair = defaultParameters;
        }
        bprParametersPerLinkSegment[id].registerParameters(mode, alphaBetaPair);
      }
    }
  }

  /**
   * we expect a link volume accessee to be provided by the environment. This is our point of access
   * 
   * @param linkVolumeAccessee the accessee to extract link volumes from
   */
  @Override
  public void setLinkVolumeAccessee(LinkVolumeAccessee linkVolumeAccessee) {
    this.linkVolumeAccessee = linkVolumeAccessee;
  }

}
