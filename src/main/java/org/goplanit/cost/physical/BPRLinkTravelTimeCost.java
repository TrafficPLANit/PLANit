package org.goplanit.cost.physical;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.goplanit.interactor.LinkVolumeAccessee;
import org.goplanit.interactor.LinkVolumeAccessor;
import org.goplanit.network.LayeredNetwork;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;
import org.goplanit.utils.network.layer.physical.UntypedPhysicalLayer;
import org.goplanit.utils.time.TimePeriod;

/**
 * Well known BPR link performance function to compute travel time cost on link segment based on flow and configuration parameters. An instance of this class is compatible with a
 * single macroscopic physical network (layer)
 *
 * @author markr
 */
public class BPRLinkTravelTimeCost extends AbstractPhysicalCost implements LinkVolumeAccessor {

  /** generated UID */
  private static final long serialVersionUID = -1529475107840907959L;

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(BPRLinkTravelTimeCost.class.getCanonicalName());

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
      parametersMap = new HashMap<>();
    }

    /**
     * Store BPR parameters for a specified mode
     *
     * @param mode  mode of travel
     * @param alpha BPR alpha value
     * @param beta  BPR beta value
     */
    private void registerParameters(final Mode mode, final double alpha, final double beta) {
      if (beta < 1) {
        LOGGER.warning(String.format("BPR Beta parameter smaller than 1 (%.2f), unlikely choice", mode.getXmlId(), beta));
      }
      parametersMap.put(mode, Pair.of(alpha, beta));
    }

    /**
     * Store BPR parameters for a specified mode as a Pair
     *
     * @param mode mode of travel
     * @param pair Pair containing BPR alpha and beta values
     */
    private void registerParameters(final Mode mode, final Pair<Double, Double> pair) {
      if (pair.second() < 1) {
        LOGGER.warning(String.format("BPR Beta parameter smaller than one (%.2f), unlikely choice", mode.getXmlId(), pair.second()));
      }
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

    /**
     * the registered modes
     * 
     * @return modes
     */
    public Set<Mode> getModes() {
      return this.parametersMap.keySet();
    }
  }

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
  protected BPRParameters[] bprParametersPerLinkSegment = null;

  /**
   * 2d Array to store free flow travel time for each [mode][link segment] to be used in calculateSegmentCost()
   */
  protected double[][] freeFlowTravelTimePerLinkSegment = null;

  /**
   * BPR function computation for. In case mode is nto allowed Double.MAX_VALUE is returned
   * 
   * @param linkSegment    the link segment
   * @param mode           given mode
   * @param flowPcuPerHour available flow
   * @return travel time in hours
   */
  protected double computeCostInHours(MacroscopicLinkSegment linkSegment, Mode mode, double flowPcuPerHour) {
    if (!linkSegment.isModeAllowed(mode)) {
      return Double.MAX_VALUE;
    }

    final int id = (int) linkSegment.getId();

    final double freeFlowTravelTime = freeFlowTravelTimePerLinkSegment[(int) mode.getId()][id];
    final double capacity = linkSegment.getCapacityOrDefaultPcuH();

    final Pair<Double, Double> alphaBetaParameters = bprParametersPerLinkSegment[id].getAlphaBetaParameters(mode);
    final double alpha = alphaBetaParameters.first();
    final double beta = alphaBetaParameters.second();

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
   * @param groupId contiguous id generation within this group for instances of this class
   */
  public BPRLinkTravelTimeCost(IdGroupingToken groupId) {
    super(groupId);
    this.parametersPerLinkSegmentAndMode = new HashMap<MacroscopicLinkSegment, BPRParameters>();
    this.defaultParametersPerMode = new BPRParameters();
    this.defaultParametersPerLinkSegmentTypeAndMode = new HashMap<MacroscopicLinkSegmentType, BPRParameters>();
    this.defaultParameters = Pair.of(DEFAULT_ALPHA, DEFAULT_BETA);
  }

  /**
   * Copy Constructor
   * 
   * @param bprLinkTravelTimeCost to use
   */
  public BPRLinkTravelTimeCost(BPRLinkTravelTimeCost bprLinkTravelTimeCost) {
    super(bprLinkTravelTimeCost);
    this.linkVolumeAccessee = bprLinkTravelTimeCost.linkVolumeAccessee;

    this.parametersPerLinkSegmentAndMode = bprLinkTravelTimeCost.parametersPerLinkSegmentAndMode;
    this.defaultParametersPerMode = bprLinkTravelTimeCost.defaultParametersPerMode;
    this.defaultParametersPerLinkSegmentTypeAndMode = bprLinkTravelTimeCost.defaultParametersPerLinkSegmentTypeAndMode;
    this.defaultParameters = bprLinkTravelTimeCost.defaultParameters;

    this.bprParametersPerLinkSegment = bprLinkTravelTimeCost.bprParametersPerLinkSegment;
    this.freeFlowTravelTimePerLinkSegment = bprLinkTravelTimeCost.freeFlowTravelTimePerLinkSegment;
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
    defaultParameters = Pair.of(alpha, beta);
  }

  /**
   * Register the BPR cost parameter values on the PhysicalNetwork
   *
   * @param network network object containing the updated parameter values
   * @throws PlanItException thrown if error
   */
  @Override
  public void initialiseBeforeSimulation(final LayeredNetwork<?, ?> network) throws PlanItException {
    PlanItException.throwIf(!(network instanceof MacroscopicNetwork), "BPR cost is only compatible with macroscopic networks");
    MacroscopicNetwork macroscopicNetwork = (MacroscopicNetwork) network;
    PlanItException.throwIf(macroscopicNetwork.getTransportLayers().size() != 1, "BPR cost is currently only compatible with networks using a single infrastructure layer");
    MacroscopicNetworkLayer networkLayer = macroscopicNetwork.getTransportLayers().getFirst();
    if (network.getModes().size() != networkLayer.getSupportedModes().size()) {
      LOGGER.warning("network wide modes do not match modes supported by only layer, this makes the assignment less efficient, consider removing unused modes");
    }

    /* pre-compute the free flow travel times */
    freeFlowTravelTimePerLinkSegment = new double[network.getModes().size()][(int) networkLayer.getLinkSegments().size()];
    for (var mode : network.getModes()) {
      freeFlowTravelTimePerLinkSegment[(int) mode.getId()] = networkLayer.getLinkSegments().getFreeFlowTravelTimeHourPerLinkSegment(mode);
    }

    /* explicitly set BPR parameters for each mode/segment combination */
    bprParametersPerLinkSegment = new BPRParameters[(int) networkLayer.getLinkSegments().size()];
    for (var macroscopicLinkSegment : networkLayer.getLinkSegments()) {
      final int id = (int) macroscopicLinkSegment.getLinkSegmentId(); // changed 7/9/21, see comment in #populateWithCost
      bprParametersPerLinkSegment[id] = new BPRParameters();
      final var macroscopicLinkSegmentType = macroscopicLinkSegment.getLinkSegmentType();
      for (var mode : network.getModes()) {
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
   * {@inheritDoc}
   */
  @Override
  public void updateTimePeriod(TimePeriod timePeriod) {
    // currently the settings for the BPR travel time are time period agnostic, so do nothing
  }

  /**
   * Return the travel time for the current link for a given mode
   *
   * If the input data are invalid, this method returns a negative value.
   *
   * @param mode        the current Mode of travel
   * @param linkSegment the current link segment
   * @return the travel time for the current link (in hours)
   */
  @Override
  public double getGeneralisedCost(final Mode mode, final MacroscopicLinkSegment linkSegment) {
    return computeCostInHours(linkSegment, mode, linkVolumeAccessee.getLinkSegmentVolume(linkSegment));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getTravelTimeCost(final Mode mode, final MacroscopicLinkSegment linkSegment) {
    return computeCostInHours(linkSegment, mode, linkVolumeAccessee.getLinkSegmentVolume(linkSegment));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getDTravelTimeDFlow(boolean uncongested /* not used */ , final Mode mode, final MacroscopicLinkSegment linkSegment) {
    if (!linkSegment.isModeAllowed(mode)) {
      return Double.MAX_VALUE;
    }
    final int id = (int) linkSegment.getId();

    final double freeFlowTravelTime = freeFlowTravelTimePerLinkSegment[(int) mode.getId()][id];
    final double capacity = linkSegment.getCapacityOrDefaultPcuH();

    final Pair<Double, Double> alphaBetaParameters = bprParametersPerLinkSegment[id].getAlphaBetaParameters(mode);
    final double alpha = alphaBetaParameters.first();
    final double beta = alphaBetaParameters.second();

    double currentFlow = linkVolumeAccessee.getLinkSegmentVolume(linkSegment);

    // assumed beta > 1
    return (beta - 1) * freeFlowTravelTime * alpha * Math.pow(currentFlow / capacity, beta - 1);
  }

  /**
   * populate the cost array with the BPR link travel times for all link segments for the specified mode
   * 
   * @param mode       the mode to use
   * @param costToFill the cost to populate (in hours)
   */
  @Override
  public void populateWithCost(UntypedPhysicalLayer<?, ?, MacroscopicLinkSegment> physicalLayer, Mode mode, double[] costToFill) {
    double[] linkSegmentFlows = linkVolumeAccessee.getLinkSegmentVolumes();

    for (var linkSegment : physicalLayer.getLinkSegments()) {
      // changed from id to link segment id 7/9/2021 since we array is created based on linksegments only, not all edge segments (so excluding connectoid segments). Therefore
      // we should not be using the id that is unique across both, just the one for physical link segments. By accident this did work so far due to connectoid segments being
      // created after the link segments. Verify if tests still succeed. IF so, remove this comment
      final int id = (int) linkSegment.getLinkSegmentId();
      costToFill[id] = computeCostInHours(linkSegment, mode, linkSegmentFlows[id]);
    }
  }

  /**
   * we expect a link volume accessee to be provided by the environment. This is our point of access
   * 
   * @param linkVolumeAccessee the accessee to extract link volumes from
   */
  @Override
  public void setAccessee(LinkVolumeAccessee linkVolumeAccessee) {
    this.linkVolumeAccessee = linkVolumeAccessee;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public BPRLinkTravelTimeCost clone() {
    return new BPRLinkTravelTimeCost(this);
  }

  /**
   * return to pre-{@link #initialiseBeforeSimulation(LayeredNetwork)} state
   */
  @Override
  public void reset() {
    // keep configuration, reset internal state
    this.freeFlowTravelTimePerLinkSegment = null;
    this.bprParametersPerLinkSegment = null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<String, String> collectSettingsAsKeyValueMap() {
    var keyValueMap = new HashMap<String, String>();
    keyValueMap.put("default-alpha/beta:", "" + defaultParameters.first() + ", " + defaultParameters.second());
    for (var modeEntry : defaultParametersPerMode.getModes()) {
      var modeAlphaBeta = defaultParametersPerMode.getAlphaBetaParameters(modeEntry);
      keyValueMap.put(modeEntry.getName() + "-alpha/beta:", "" + modeAlphaBeta.first() + ", " + modeAlphaBeta.second());
    }
    for (var typeEntry : defaultParametersPerLinkSegmentTypeAndMode.entrySet()) {
      var modesPerType = typeEntry.getValue();
      for (var modeEntry : modesPerType.getModes()) {
        var modeAlphaBeta = modesPerType.getAlphaBetaParameters(modeEntry);
        keyValueMap.put("type-" + typeEntry.getKey().getXmlId() + "-" + modeEntry.getName() + "-alpha/beta:", "" + modeAlphaBeta.first() + ", " + modeAlphaBeta.second());
      }
    }
    for (var segmentEntry : parametersPerLinkSegmentAndMode.entrySet()) {
      var modesPerSegment = segmentEntry.getValue();
      for (var modeEntry : modesPerSegment.getModes()) {
        var modeAlphaBeta = modesPerSegment.getAlphaBetaParameters(modeEntry);
        keyValueMap.put("segment-" + segmentEntry.getKey().getXmlId() + "-" + modeEntry.getName() + "-alpha/beta:", "" + modeAlphaBeta.first() + ", " + modeAlphaBeta.second());
      }
    }
    return keyValueMap;
  }

}
