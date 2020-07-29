package org.planit.trafficassignment.builder;

import java.util.List;
import java.util.logging.Logger;

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
import org.planit.output.configuration.OutputConfiguration;
import org.planit.output.configuration.OutputTypeConfiguration;
import org.planit.output.enums.OutputType;
import org.planit.output.formatter.OutputFormatter;
import org.planit.sdinteraction.smoothing.Smoothing;
import org.planit.time.TimePeriod;
import org.planit.trafficassignment.TrafficAssignment;
import org.planit.trafficassignment.TrafficAssignmentComponentFactory;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.misc.LoggingUtils;
import org.planit.utils.network.physical.Mode;

/**
 * All traffic assignment instances require a network, demand, and (equilibrium) smoothing procedure, all of which should be registered via this generic builder. Specific traffic
 * assignment methods might require special builders derived from this builder
 *
 * @author markr
 *
 */
public abstract class TrafficAssignmentBuilder {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(TrafficAssignmentBuilder.class.getCanonicalName());

  /**
   * log registering an item on this traffic assignment
   * 
   * @param item     to (un)register
   * @param register when true it signals activate, otherwise deactive
   */
  private void logRegisteredComponent(Object item, boolean register) {
    LOGGER.info(LoggingUtils.createRunIdPrefix(parentAssignment.getId()) + LoggingUtils.activateItemByClassName(item, register));
  }

  /**
   * Register the demands zoning and network objects
   *
   * @param demands Demands object to be registered
   * @param zoning  Zoning object to be registered
   * @param network network object to be registered
   * @throws PlanItException thrown if the number of zones in the Zoning and Demand objects is inconsistent
   */
  private void registerDemandZoningAndNetwork(final Demands demands, final Zoning zoning, final PhysicalNetwork network) throws PlanItException {
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
          LOGGER.warning(LoggingUtils.createRunIdPrefix(parentAssignment.getId()) + "no demand matrix defined for Mode " + mode.getExternalId() + " and Time Period " + timePeriod.getExternalId());
        }
      }
    }
    parentAssignment.setPhysicalNetwork(network);
    logRegisteredComponent(network, true);
    parentAssignment.setZoning(zoning);
    logRegisteredComponent(zoning, true);
    parentAssignment.setDemands(demands);
    logRegisteredComponent(demands, true);
  }

  /**
   * The smoothing factory used in the assignment algorithm
   *
   * NB: The smoothing factory is defined here because the same smoothing algorithm is used for all assignments. If we later decide to use more than one smoothing algorithm and
   * allow different traffic assignments to use different smoothing algorithms, we would need to move this property and its handler methods to CustomPlanItProject and treat it like
   * the factories for PhysicalNetwork, Demands and Zoning (and allow the different smoothing algorithms to be registered on the project).
   */
  protected final TrafficAssignmentComponentFactory<Smoothing> smoothingFactory;

  /**
   * Cost factory to create physical costs to register on the generalized cost.
   */
  protected final TrafficAssignmentComponentFactory<PhysicalCost> physicalCostFactory;

  /**
   * Cost factory to create physical costs to register on the generalized cost.
   */
  protected final TrafficAssignmentComponentFactory<VirtualCost> virtualCostFactory;

  /**
   * The assignment all components will be registered on
   */
  protected final TrafficAssignment parentAssignment;

  /**
   * Currently, there exists only a single gap function (link based relative duality gap) that is created via this factory method. It should be injected by each traffic assignment
   * method until we have multiple gap functions, in which case, it becomes an option like other components.
   * 
   * @return the created gap function
   */
  protected GapFunction createGapFunction() {
    return new LinkBasedRelativeDualityGapFunction(new StopCriterion());
  }

  // PUBLIC

  /**
   * Constructor
   * 
   * @param parentAssignment               the parent assignment
   * @param trafficComponentCreateListener the input builder
   * @param demands                        the demands
   * @param zoning                         the zoning
   * @param physicalNetwork                the physical network
   * @throws PlanItException if registration of demands, zoning, or network does not work
   */
  TrafficAssignmentBuilder(final TrafficAssignment parentAssignment, final InputBuilderListener trafficComponentCreateListener, final Demands demands, final Zoning zoning,
      final PhysicalNetwork physicalNetwork) throws PlanItException {
    this.parentAssignment = parentAssignment;
    registerDemandZoningAndNetwork(demands, zoning, physicalNetwork);

    smoothingFactory = new TrafficAssignmentComponentFactory<Smoothing>(Smoothing.class);
    physicalCostFactory = new TrafficAssignmentComponentFactory<PhysicalCost>(PhysicalCost.class);
    virtualCostFactory = new TrafficAssignmentComponentFactory<VirtualCost>(VirtualCost.class);
    // register listener on factories
    smoothingFactory.addListener(trafficComponentCreateListener, TrafficAssignmentComponentFactory.TRAFFICCOMPONENT_CREATE);
    physicalCostFactory.addListener(trafficComponentCreateListener, TrafficAssignmentComponentFactory.TRAFFICCOMPONENT_CREATE);
    virtualCostFactory.addListener(trafficComponentCreateListener, TrafficAssignmentComponentFactory.TRAFFICCOMPONENT_CREATE);
  }

  /**
   * Initialize the traffic assignment defaults for the activated assignment method:
   *
   * @throws PlanItException thrown when there is an error
   */
  public void initialiseDefaults() throws PlanItException {
    // default gap function
    GapFunction theGapFunction = createGapFunction();
    parentAssignment.setGapFunction(theGapFunction);
    logRegisteredComponent(theGapFunction, true);

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
  public Smoothing createAndRegisterSmoothing(final String smoothingType) throws PlanItException {
    final Smoothing smoothing = smoothingFactory.create(smoothingType, new Object[] { parentAssignment.getIdGroupingtoken() });
    parentAssignment.setSmoothing(smoothing);
    logRegisteredComponent(smoothing, true);
    return smoothing;
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
    parentAssignment.setPhysicalCost(physicalCost);
    logRegisteredComponent(physicalCost, true);
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
    final VirtualCost createdCost = virtualCostFactory.create(virtualTraveltimeCostFunctionType, new Object[] { parentAssignment.getIdGroupingtoken() });
    parentAssignment.setVirtualCost(createdCost);
    logRegisteredComponent(createdCost, true);
    return createdCost;
  }

  /**
   * Register an output formatter
   *
   * @param outputFormatter OutputFormatter being registered
   * @throws PlanItException thrown if there is an error or validation failure during setup of the output formatter
   */
  public void registerOutputFormatter(final OutputFormatter outputFormatter) throws PlanItException {
    parentAssignment.registerOutputFormatter(outputFormatter);
    logRegisteredComponent(outputFormatter, true);
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
    parentAssignment.unregisterOutputFormatter(outputFormatter);
    logRegisteredComponent(outputFormatter, false);
  }

  /**
   * Returns a list of output formatters registered on this assignment
   *
   * @return List of OutputFormatter objects registered on this assignment
   */
  public List<OutputFormatter> getOutputFormatters() {
    return parentAssignment.getOutputFormatters();
  }

  /**
   * Register the initial link segment cost without relating it to a particular period, meaning that it is applied to all time periods that do not have a specified initial link
   * segment costs registered for them
   *
   * @param initialLinkSegmentCost initial link segment cost for the current traffic assignment
   */
  public void registerInitialLinkSegmentCost(final InitialLinkSegmentCost initialLinkSegmentCost) {
    parentAssignment.setInitialLinkSegmentCost(initialLinkSegmentCost);
  }

  /**
   * Register the initial link segment cost for the time period embedded in it
   *
   * @param initialLinkSegmentCostPeriod initial link segment cost for the current traffic assignment
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
    parentAssignment.setInitialLinkSegmentCost(timePeriod, initialLinkSegmentCost);
  }

  /**
   * Method that allows one to activate specific output types for persistence on the traffic assignment instance
   *
   * @param outputType OutputType object to be used
   * @return outputTypeConfiguration the output type configuration that is now active
   * @throws PlanItException thrown if there is an error activating the output
   */
  public OutputTypeConfiguration activateOutput(final OutputType outputType) throws PlanItException {
    LOGGER.info(LoggingUtils.createRunIdPrefix(parentAssignment.getId()) + "activated: OutputType." + outputType);
    return parentAssignment.activateOutput(outputType);
  }

  /**
   * Deactivate an output type
   * 
   * @param outputType OutputType to be deactivated
   */
  public void deactivateOutput(final OutputType outputType) {
    LOGGER.info(LoggingUtils.createRunIdPrefix(parentAssignment.getId()) + "deactivated: OutputType." + outputType);
    parentAssignment.deactivateOutput(outputType);
  }

  /**
   * Verify if a given output type is active
   * 
   * @param outputType the output type to verify for
   * @return true if active, false otherwise
   */
  public boolean isOutputTypeActive(final OutputType outputType) {
    return parentAssignment.isOutputTypeActive(outputType);
  }

  /**
   * Provide the output configuration for user access
   *
   * @return outputConfiguration for this traffic assignment
   */
  public OutputConfiguration getOutputConfiguration() {
    return parentAssignment.getOutputConfiguration();
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
   * @return smoothing
   */
  public VirtualCost getVirtualCost() {
    return parentAssignment.getVirtualCost();
  }

  /**
   * Collect the smoothing entity registered on the traffic assignment
   * 
   * @return smoothing
   */
  public Smoothing getSmoothing() {
    return parentAssignment.getSmoothing();
  }

}