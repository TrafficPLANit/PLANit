package org.planit.trafficassignment.builder;

import java.util.List;
import java.util.logging.Logger;

import org.planit.assignment.TrafficAssignment;
import org.planit.assignment.TrafficAssignmentComponentFactory;
import org.planit.assignment.TrafficComponentBuilder;
import org.planit.cost.physical.PhysicalCost;
import org.planit.cost.physical.initial.InitialLinkSegmentCost;
import org.planit.cost.physical.initial.InitialLinkSegmentCostPeriod;
import org.planit.cost.virtual.VirtualCost;
import org.planit.demands.Demands;
import org.planit.gap.GapFunction;
import org.planit.gap.LinkBasedRelativeDualityGapFunction;
import org.planit.gap.StopCriterion;
import org.planit.input.InputBuilderListener;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.network.virtual.Zoning;
import org.planit.output.OutputManager;
import org.planit.output.adapter.OutputTypeAdapter;
import org.planit.output.configuration.OutputConfiguration;
import org.planit.output.configuration.OutputTypeConfiguration;
import org.planit.output.enums.OutputType;
import org.planit.output.formatter.OutputFormatter;
import org.planit.sdinteraction.smoothing.SmoothingBuilder;
import org.planit.sdinteraction.smoothing.SmoothingBuilderFactory;
import org.planit.supply.networkloading.NetworkLoading;
import org.planit.time.TimePeriod;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.physical.Mode;

/**
 * All traffic assignment instances require a network, demand, and (equilibrium) smoothing procedure, all of which should be registered via this generic builder. Specific traffic
 * assignment methods might require special builders derived from this builder
 *
 * @author markr
 *
 */
public abstract class TrafficAssignmentBuilder<T extends TrafficAssignment> extends TrafficComponentBuilder<T> {

  /** the logger */
  protected static final Logger LOGGER = Logger.getLogger(TrafficAssignmentBuilder.class.getCanonicalName());

  /**
   * Output manager deals with all the output configurations for the registered traffic assignments
   */
  private final OutputManager outputManager;

  /**
   * Register the demands zoning and network objects
   *
   * @param demands Demands object to be registered
   * @param zoning  Zoning object to be registered
   * @param network network object to be registered
   * @throws PlanItException thrown if the number of zones in the Zoning and Demand objects is inconsistent
   */
  private void registerDemandZoningAndNetwork(final Demands demands, final Zoning zoning, final PhysicalNetwork<?, ?, ?> network) throws PlanItException {
    if (zoning == null || demands == null || network == null) {
      PlanItException.throwIf(zoning == null, "zoning in registerDemandZoningAndNetwork is null");
      PlanItException.throwIf(demands == null, "demands in registerDemandZoningAndNetwork is null");
      PlanItException.throwIf(network == null, "network in registerDemandZoningAndNetwork is null");
    }
    PlanItException.throwIf(!zoning.isCompatibleWithDemands(demands, network.modes),
        "Zoning structure is incompatible with one or more of the demands, likely the number of zones does not match the number of origins and/or destinations");

    for (final Mode mode : network.modes) {
      for (TimePeriod timePeriod : demands.timePeriods.asSortedSetByStartTime()) {
        if (demands.get(mode, timePeriod) == null) {
          LOGGER.warning("no demand matrix defined for Mode " + mode.getExternalId() + " and Time Period " + timePeriod.getExternalId());
        }
      }
    }
    registerDelayedMethodCall("setPhysicalNetwork", network);
    registerDelayedMethodCall("setZoning", zoning);
    registerDelayedMethodCall("setDemands", demands);
  }

  /**
   * the smoothing builder to use for the configuration of the smoothing
   */
  protected SmoothingBuilder<?> smoothingBuilder = null;

  /**
   * Currently, there exists only a single gap function (link based relative duality gap) that is created via this factory method. It should be injected by each traffic assignment
   * method until we have multiple gap functions, in which case, it becomes an option like other components.
   * 
   * @return the created gap function
   */
  protected GapFunction createGapFunction() {
    return new LinkBasedRelativeDualityGapFunction(new StopCriterion());
  }

  /**
   * Create the output type adapter for the current output type, specifically tailored towards the assignment type that we are builing
   *
   * @param outputType the current output type
   * @return the output type adapter corresponding to the current traffic assignment and output type
   */
  public abstract OutputTypeAdapter createOutputTypeAdapter(OutputType outputType);

  // PUBLIC

  /**
   * Constructor
   * 
   * @param trafficAssignmentClass class to build
   * @param projectToken           idGrouping token
   * @param inputBuilderListener   the input builder listener
   * @param demands                the demands
   * @param zoning                 the zoning
   * @param physicalNetwork        the physical network
   * @throws PlanItException
   */
  protected TrafficAssignmentBuilder(final Class<T> trafficAssignmentClass, final IdGroupingToken projectToken, InputBuilderListener inputBuilderListener, final Demands demands,
      final Zoning zoning, final PhysicalNetwork<?, ?, ?> physicalNetwork) throws PlanItException {
    super(trafficAssignmentClass, projectToken, inputBuilderListener);

    this.outputManager = new OutputManager();
    registerDelayedMethodCall("setOutputManager", outputManager);

    GapFunction theGapFunction = createGapFunction();
    registerDelayedMethodCall("setGapFunction", theGapFunction);

    registerDemandZoningAndNetwork(demands, zoning, physicalNetwork);

    // By default, activate the link outputs
    activateOutput(OutputType.LINK);
  }

  // PUBLIC FACTORY METHODS

  /**
   * Create and Register smoothing component
   *
   * @param smoothingType the type of smoothing component to be created
   * @return Smoothing object created
   * @throws PlanItException thrown if there is an error
   */
  public SmoothingBuilder<?> createAndRegisterSmoothing(final String smoothingType) throws PlanItException {
    smoothingBuilder = SmoothingBuilderFactory.createBuilder(smoothingType, groupId);
    return smoothingBuilder;
  }

  /**
   * Create and register physical link cost function to determine travel time
   *
   * @param physicalTraveltimeCostFunctionType the type of cost function to be created
   * @return the physical cost created
   * @throws PlanItException thrown if there is an error
   */
  public PhysicalCost createAndRegisterPhysicalCost(final String physicalTraveltimeCostFunctionType) throws PlanItException {
    final PhysicalCost physicalCost = physicalCostFactory.create(physicalTraveltimeCostFunctionType, new Object[] { parentAssignment.getIdGroupingtoken() });
    registerDelayedMethodCall("setPhysicalCost", physicalCost);
    return physicalCost;
  }

  /**
   * Create and Register virtual link cost function to determine travel time
   *
   * @param virtualTraveltimeCostFunctionType the type of cost function to be created
   * @return the cost function created
   * @throws PlanItException thrown if there is an error
   */
  public VirtualCost createAndRegisterVirtualCost(final String virtualTraveltimeCostFunctionType) throws PlanItException {
    final VirtualCost virtualCost = virtualCostFactory.create(virtualTraveltimeCostFunctionType, new Object[] { parentAssignment.getIdGroupingtoken() });
    registerDelayedMethodCall("setVirtualCost", virtualCost);
    return virtualCost;
  }

  /**
   * Register an output formatter
   *
   * @param outputFormatter OutputFormatter being registered
   * @throws PlanItException thrown if there is an error or validation failure during setup of the output formatter
   */
  public void registerOutputFormatter(final OutputFormatter outputFormatter) throws PlanItException {
    outputManager.registerOutputFormatter(outputFormatter);
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
    outputManager.unregisterOutputFormatter(outputFormatter);
  }

  /**
   * Returns a list of output formatters registered on this assignment
   *
   * @return List of OutputFormatter objects registered on this assignment
   */
  public List<OutputFormatter> getOutputFormatters() {
    return outputManager.getOutputFormatters();
  }

  /**
   * Register the initial link segment cost without relating it to a particular period, meaning that it is applied to all time periods that do not have a specified initial link
   * segment costs registered for them
   *
   * @param initialLinkSegmentCost initial link segment cost for the current traffic assignment
   */
  public void registerInitialLinkSegmentCost(final InitialLinkSegmentCost initialLinkSegmentCost) {
    registerDelayedMethodCall("setInitialLinkSegmentCost", initialLinkSegmentCost);
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
    registerDelayedMethodCall("setInitialLinkSegmentCost", timePeriod, initialLinkSegmentCost);
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
      return outputManager.createAndRegisterOutputTypeConfiguration(outputType);
    } else {
      return outputManager.getOutputTypeConfiguration(outputType);
    }
  }

  /**
   * Deactivate an output type
   * 
   * @param outputType OutputType to be deactivated
   */
  public void deactivateOutput(final OutputType outputType) {
    if (isOutputTypeActive(outputType)) {
      outputManager.deregisterOutputTypeConfiguration(outputType);
      outputManager.deregisterOutputTypeAdapter(outputType);
    }
  }

  /**
   * Verify if a given output type is active
   * 
   * @param outputType the output type to verify for
   * @return true if active, false otherwise
   */
  public boolean isOutputTypeActive(final OutputType outputType) {
    return outputManager.isOutputTypeActive(outputType);
  }

  /**
   * Provide the output configuration for user access
   *
   * @return outputConfiguration for this traffic assignment
   */
  public OutputConfiguration getOutputConfiguration() {
    return outputManager.getOutputConfiguration();
  }

  /**
   * Collect the gap function of the trafficAssignment instance
   *
   * @return gapFunction
   */
  public GapFunction getGapFunction() {
    return parentAssignment.getGapFunction();
  }

  /**
   * Collect the physical cost entity registered on the traffic assignment
   * 
   * @return physicalCost
   */
  public PhysicalCost getPhysicalCost() {
    return parentAssignment.getPhysicalCost();
  }

  /**
   * Collect the virtual cost entity registered on the traffic assignment
   * 
   * @return virtual cost
   */
  public VirtualCost getVirtualCost() {
    return parentAssignment.getVirtualCost();
  }

  /**
   * Collect the smoothing entity registered on the traffic assignment
   * 
   * @return smoothing
   */
  public SmoothingBuilder<?> getSmoothing() {
    return smoothingBuilder;
  }

  /**
   * Build the traffic assignment
   * 
   * @param inputBuilderListener which is registered for all traffic assignment components that are built in case they need to be configured by some external entity
   * @return traffic assignment instance that is built
   * @throws PlanItException thrown if error
   */
  @Override
  public T build() throws PlanItException {
    // Build the assignment
    String trafficAssignmentClassName = getClassToBuild().getClass().getCanonicalName();
    TrafficAssignmentComponentFactory<TrafficAssignment> assignmentFactory = new TrafficAssignmentComponentFactory<TrafficAssignment>(trafficAssignmentClassName);
    final NetworkLoading networkLoadingAndAssignment = assignmentFactory.create(trafficAssignmentClassName, new Object[] { groupId });
    PlanItException.throwIf(!(networkLoadingAndAssignment instanceof TrafficAssignment), "not a valid traffic assignment type");
    @SuppressWarnings("unchecked")
    T trafficAssignment = (T) networkLoadingAndAssignment;

    // build its subcomponents via its own builders
    if (smoothingBuilder != null) {
      trafficAssignment.setSmoothing(smoothingBuilder.build());
    }

    TrafficAssignmentComponentFactory<PhysicalCost> physicalCostFactory = new TrafficAssignmentComponentFactory<PhysicalCost>(PhysicalCost.class);
    TrafficAssignmentComponentFactory<VirtualCost> virtualCostFactory = new TrafficAssignmentComponentFactory<VirtualCost>(VirtualCost.class);
    // register listener on factories

    physicalCostFactory.addListener(getInputBuilderListener(), TrafficAssignmentComponentFactory.TRAFFICCOMPONENT_CREATE);
    virtualCostFactory.addListener(getInputBuilderListener(), TrafficAssignmentComponentFactory.TRAFFICCOMPONENT_CREATE);

    // for all output types --> create the adapters and register them on the manager
    for (OutputTypeConfiguration otc : outputManager.getRegisteredOutputTypeConfigurations()) {
      final OutputTypeAdapter outputTypeAdapter = createOutputTypeAdapter(otc.getOutputType());
      outputManager.registerOutputTypeAdapter(otc.getOutputType(), outputTypeAdapter);
    }

    // perform all delayed calls on the assignment to finalise the build
    invokeDelayedMethodCalls(trafficAssignment);

    return trafficAssignment;
  }

}