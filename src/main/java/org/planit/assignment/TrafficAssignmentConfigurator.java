package org.planit.assignment;

import java.util.List;
import java.util.logging.Logger;

import org.planit.cost.physical.AbstractPhysicalCost;
import org.planit.cost.physical.PhysicalCostConfigurator;
import org.planit.cost.physical.PhysicalCostConfiguratorFactory;
import org.planit.cost.physical.initial.InitialLinkSegmentCost;
import org.planit.cost.physical.initial.InitialLinkSegmentCostPeriod;
import org.planit.cost.virtual.AbstractVirtualCost;
import org.planit.cost.virtual.VirtualCostConfigurator;
import org.planit.cost.virtual.VirtualCostConfiguratorFactory;
import org.planit.demands.Demands;
import org.planit.gap.GapFunction;
import org.planit.gap.GapFunctionConfigurator;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.network.virtual.Zoning;
import org.planit.output.OutputManager;
import org.planit.output.configuration.OutputConfiguration;
import org.planit.output.configuration.OutputTypeConfiguration;
import org.planit.output.enums.OutputType;
import org.planit.output.formatter.OutputFormatter;
import org.planit.sdinteraction.smoothing.Smoothing;
import org.planit.sdinteraction.smoothing.SmoothingConfigurator;
import org.planit.sdinteraction.smoothing.SmoothingConfiguratorFactory;
import org.planit.time.TimePeriod;
import org.planit.utils.builder.Configurator;
import org.planit.utils.exceptions.PlanItException;

/**
 * Configurator class for traffic assignment. Hides builder pattern from user while allowing for easy way to configure an assignment without having actual access to it.
 * 
 * @author markr
 *
 * @param <T>
 */
public class TrafficAssignmentConfigurator<T extends TrafficAssignment> extends Configurator<T> {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(TrafficAssignmentConfigurator.class.getCanonicalName());

  protected static final String SET_OUTPUT_MANAGER = "setOutputManager";

  protected static final String SET_GAP_FUNCTION = "setGapFunction";

  protected static final String SET_VIRTUAL_COST = "setVirtualCost";

  protected static final String SET_PHYSICAL_COST = "setPhysicalCost";

  protected static final String SET_INITIAL_LINK_SEGMENT_COST = "setInitialLinkSegmentCost";

  protected static final String SET_PHYSICAL_NETWORK = "setPhysicalNetwork";

  protected static final String SET_ZONING = "setZoning";

  protected static final String SET_DEMANDS = "setDemands";

  /**
   * Nested configurator for smoothing within this assignment
   */
  private SmoothingConfigurator<? extends Smoothing> smoothingConfigurator = null;

  /**
   * Nested configurator for physical cost within this assignment
   */
  private PhysicalCostConfigurator<? extends AbstractPhysicalCost> physicalCostConfigurator = null;

  /**
   * Nested configurator for virtual cost within this assignment
   */
  private VirtualCostConfigurator<? extends AbstractVirtualCost> virtualCostConfigurator = null;

  /**
   * Nested configurator for vgap function within this assignment
   */
  private GapFunctionConfigurator<? extends GapFunction> gapFunctionConfigurator = null;

  /**
   * Set the gap function configurator for this assignment
   * 
   * @param theGapFunction
   */
  protected void setGapFunction(GapFunctionConfigurator<? extends GapFunction> gapFunctionConfigurator) {
    this.gapFunctionConfigurator = gapFunctionConfigurator;
  }

  /**
   * Set the network
   * 
   * @param network to set
   */
  protected void setPhysicalNetwork(PhysicalNetwork<?, ?, ?> network) {
    registerDelayedMethodCall(SET_PHYSICAL_NETWORK, network);
  }

  /**
   * Set the zoning
   * 
   * @param zoning to set
   */
  protected void setZoning(Zoning zoning) {
    registerDelayedMethodCall(SET_ZONING, zoning);
  }

  /**
   * Set the demands
   * 
   * @param demands to set
   */
  protected void setDemands(Demands demands) {
    registerDelayedMethodCall(SET_DEMANDS, demands);
  }

  /**
   * Set the output manager
   * 
   * @param outputManager
   */
  protected void setOutputManager(OutputManager outputManager) {
    registerDelayedMethodCall(SET_OUTPUT_MANAGER, outputManager);
  }

  /**
   * Collect the registered output manager
   * 
   * @return outputManager
   */
  protected OutputManager getOutputManager() {
    return (OutputManager) getFirstParameterOfDelayedMethodCall(SET_OUTPUT_MANAGER);
  }

  /**
   * Constructor
   * 
   * @param instanceType the class type of the instance we are configuring
   */
  public TrafficAssignmentConfigurator(Class<T> instanceType) {
    super(instanceType);
  }

  /**
   * collect the registered network
   * 
   * @return network
   */
  public PhysicalNetwork<?, ?, ?> getPhysicalNetwork() {
    return (PhysicalNetwork<?, ?, ?>) getFirstParameterOfDelayedMethodCall(SET_PHYSICAL_NETWORK);
  }

  /**
   * collect the registered zoning
   * 
   * @return zoning
   */
  public Zoning getZoning() {
    return (Zoning) getFirstParameterOfDelayedMethodCall(SET_ZONING);
  }

  /**
   * collect the registered demands
   * 
   * @return demands
   */
  public Demands getDemands() {
    return (Demands) getFirstParameterOfDelayedMethodCall(SET_DEMANDS);
  }

  /**
   * Create and Register smoothing component
   *
   * @param smoothingType the type of smoothing component to be created
   * @return Smoothing configuration object
   * @throws PlanItException thrown if there is an error
   */
  public SmoothingConfigurator<? extends Smoothing> createAndRegisterSmoothing(final String smoothingType) throws PlanItException {
    smoothingConfigurator = SmoothingConfiguratorFactory.createConfigurator(smoothingType);
    return smoothingConfigurator;
  }

  /**
   * Create and register physical link cost function to determine travel time
   *
   * @param physicalTraveltimeCostFunctionType the type of cost function to be created
   * @return the physical cost created
   * @throws PlanItException thrown if there is an error
   */
  public PhysicalCostConfigurator<? extends AbstractPhysicalCost> createAndRegisterPhysicalCost(final String physicalTraveltimeCostFunctionType) throws PlanItException {
    physicalCostConfigurator = PhysicalCostConfiguratorFactory.createConfigurator(physicalTraveltimeCostFunctionType);
    return physicalCostConfigurator;
  }

  /**
   * Create and Register virtual link cost function to determine travel time
   *
   * @param virtualTraveltimeCostFunctionType the type of cost function to be created
   * @return the cost function created
   * @throws PlanItException thrown if there is an error
   */
  public VirtualCostConfigurator<? extends AbstractVirtualCost> createAndRegisterVirtualCost(final String virtualTraveltimeCostFunctionType) throws PlanItException {
    virtualCostConfigurator = VirtualCostConfiguratorFactory.createConfigurator(virtualTraveltimeCostFunctionType);
    return virtualCostConfigurator;
  }

  /**
   * Register an output formatter
   *
   * @param outputFormatter OutputFormatter being registered
   * @throws PlanItException thrown if there is an error or validation failure during setup of the output formatter
   */
  public void registerOutputFormatter(final OutputFormatter outputFormatter) throws PlanItException {
    getOutputManager().registerOutputFormatter(outputFormatter);
  }

  /**
   * Remove an output formatter which has already been registered
   * 
   * This is used by the Python interface, which registers the PlanItIO formatter by default
   * 
   * @param outputFormatter the output formatter to be removed
   * @throws PlanItException thrown if there is an error during removal of the output formatter
   */
  public void unregisterOutputFormatter(final OutputFormatter outputFormatter) throws PlanItException {
    getOutputManager().unregisterOutputFormatter(outputFormatter);
  }

  /**
   * Returns a list of output formatters registered on this assignment
   *
   * @return List of OutputFormatter objects registered on this assignment
   */
  public List<OutputFormatter> getOutputFormatters() {
    return getOutputManager().getOutputFormatters();
  }

  /**
   * Register the initial link segment cost without relating it to a particular period, meaning that it is applied to all time periods that do not have a specified initial link
   * segment costs registered for them
   *
   * @param initialLinkSegmentCost initial link segment cost for the current traffic assignment
   */
  public void registerInitialLinkSegmentCost(final InitialLinkSegmentCost initialLinkSegmentCost) {
    registerDelayedMethodCall(SET_INITIAL_LINK_SEGMENT_COST, initialLinkSegmentCost);
  }

  /**
   * Register the initial link segment cost for the time period embedded in it
   *
   * @param initialLinkSegmentCost initial link segment cost for the current traffic assignment
   * @throws PlanItException thrown if time period in initial costs is null
   */
  public void registerInitialLinkSegmentCost(final InitialLinkSegmentCostPeriod initialLinkSegmentCost) throws PlanItException {
    registerInitialLinkSegmentCost(initialLinkSegmentCost.getTimePeriod(), initialLinkSegmentCost);
  }

  /**
   * Register the initial link segment cost for a specified time period
   *
   * @param timePeriod             the specified time period
   * @param initialLinkSegmentCost initial link segment cost for the current traffic assignment
   * @throws PlanItException thrown if time period is null
   */
  public void registerInitialLinkSegmentCost(final TimePeriod timePeriod, final InitialLinkSegmentCost initialLinkSegmentCost) throws PlanItException {
    PlanItException.throwIf(timePeriod == null, "time period null when registering initial link segment costs");
    registerDelayedMethodCall(SET_INITIAL_LINK_SEGMENT_COST, timePeriod, initialLinkSegmentCost);
  }

  /**
   * Method that allows one to activate specific output types for persistence on the traffic assignment instance
   *
   * @param outputType OutputType object to be used
   * @return outputTypeConfiguration the output type configuration that is now active
   * @throws PlanItException thrown if there is an error activating the output
   */
  public OutputTypeConfiguration activateOutput(final OutputType outputType) throws PlanItException {
    if (!isOutputTypeActive(outputType)) {
      return getOutputManager().createAndRegisterOutputTypeConfiguration(outputType);
    } else {
      return getOutputManager().getOutputTypeConfiguration(outputType);
    }
  }

  /**
   * Deactivate an output type
   * 
   * @param outputType OutputType to be deactivated
   */
  public void deactivateOutput(final OutputType outputType) {
    if (isOutputTypeActive(outputType)) {
      getOutputManager().deregisterOutputTypeConfiguration(outputType);
      getOutputManager().deregisterOutputTypeAdapter(outputType);
    }
  }

  /**
   * Verify if a given output type is active
   * 
   * @param outputType the output type to verify for
   * @return true if active, false otherwise
   */
  public boolean isOutputTypeActive(final OutputType outputType) {
    return getOutputManager().isOutputTypeActive(outputType);
  }

  /**
   * Provide the output configuration for user access
   *
   * @return outputConfiguration for this traffic assignment
   */
  public OutputConfiguration getOutputConfiguration() {
    return getOutputManager().getOutputConfiguration();
  }

  /**
   * Collect the gap function of the trafficAssignment instance
   *
   * @return gapFunction
   */
  public GapFunctionConfigurator<? extends GapFunction> getGapFunction() {
    return this.gapFunctionConfigurator;
  }

  /**
   * Collect the physical cost entity registered on the traffic assignment
   * 
   * @return physicalCost
   */
  public PhysicalCostConfigurator<? extends AbstractPhysicalCost> getPhysicalCost() {
    return physicalCostConfigurator;
  }

  /**
   * Collect the virtual cost entity registered on the traffic assignment
   * 
   * @return virtual cost
   */
  public VirtualCostConfigurator<? extends AbstractVirtualCost> getVirtualCost() {
    return virtualCostConfigurator;
  }

  /**
   * Collect the smoothing entity registered on the traffic assignment
   * 
   * @return smoothing
   */
  public SmoothingConfigurator<? extends Smoothing> getSmoothing() {
    return smoothingConfigurator;
  }

}
