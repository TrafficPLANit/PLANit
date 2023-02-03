package org.goplanit.assignment;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.goplanit.assignment.algorithmb.AlgorithmB;
import org.goplanit.assignment.ltm.eltm.EventBasedLtm;
import org.goplanit.assignment.ltm.sltm.StaticLtm;
import org.goplanit.assignment.traditionalstatic.TraditionalStaticAssignment;
import org.goplanit.component.PlanitComponent;
import org.goplanit.cost.physical.AbstractPhysicalCost;
import org.goplanit.cost.physical.initial.InitialModesLinkSegmentCost;
import org.goplanit.cost.virtual.AbstractVirtualCost;
import org.goplanit.demands.Demands;
import org.goplanit.gap.GapFunction;
import org.goplanit.interactor.TrafficAssignmentComponentAccessee;
import org.goplanit.network.LayeredNetwork;
import org.goplanit.network.layer.macroscopic.MacroscopicLinkSegmentImpl;
import org.goplanit.network.transport.TransportModelNetwork;
import org.goplanit.output.OutputManager;
import org.goplanit.output.adapter.OutputTypeAdapter;
import org.goplanit.output.enums.OutputType;
import org.goplanit.sdinteraction.smoothing.Smoothing;
import org.goplanit.supply.networkloading.NetworkLoading;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.misc.LoggingUtils;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.time.TimePeriod;
import org.goplanit.zoning.Zoning;

/**
 * Traffic assignment class which simultaneously is responsible for the loading hence it is also considered as a traffic assignment component of this type
 *
 * @author markr
 *
 */
public abstract class TrafficAssignment extends NetworkLoading implements TrafficAssignmentComponentAccessee {

  // Private

  /** generated UID */
  private static final long serialVersionUID = 801775330292422910L;

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(MacroscopicLinkSegmentImpl.class.getCanonicalName());

  /**
   * Output manager deals with all the output configurations for the registered traffic assignments
   */
  private OutputManager outputManager;

  /* INPUT COMPONENTS */

  /**
   * network to use
   */
  private LayeredNetwork<?, ?> physicalNetwork = null;

  /**
   * The transport network to use which is an adaptor around the physical network and the zoning
   */
  private TransportModelNetwork transportNetwork = null;

  /**
   * The zoning to use
   */
  private Zoning zoning = null;

  /**
   * The demand to use
   */
  private Demands demands = null;

  /* TRAFFIC ASSIGNMENT COMPONENTS */

  /**
   * track the registered traffic assignment components, including the standard components (except initial cost) expected to be available on each assignment
   */
  private final Map<Class<? extends PlanitComponent<?>>, PlanitComponent<?>> trafficAssignmentComponents;

  // Getters - Setters

  /**
   * Helper, log the settings of the provided component
   * 
   * @param component
   */
  private void logComponentSettings(PlanitComponent<?> component) {
    var settingsMap = component.collectSettingsAsKeyValueMap();
    if (settingsMap != null) {
      String componentPrefix = LoggingUtils.runIdPrefix(getId()) + LoggingUtils.surroundwithBrackets(component.getClass().getSimpleName());
      settingsMap.forEach((k, v) -> LOGGER.info(componentPrefix + k + " " + v));
    }
  }

  /**
   * The initial link segment cost to use where the mapping is based on the user provided time period. Note that the registered InitialLinkSegmentCostMode is part of another
   * registered initialCost that is present on the project. However, a user might have decided that while they were parsed under one time period, to apply them on the assignment in
   * another time period. Hence, we only track the initialcosts themselves and not their original parsed time periods nor their umbrella instance
   */
  protected Map<TimePeriod, InitialModesLinkSegmentCost> initialLinkSegmentCostByTimePeriod;

  /**
   * Default initial costs to apply in case no time period specific costs are provided
   */
  protected InitialModesLinkSegmentCost initialLinkSegmentCostTimePeriodAgnostic;

  // Protected methods

  /**
   * create the logging prefix for logging statements during equilibration
   * 
   * @param iterationIndex the iteration
   * @return prefix for logging of traffic assignment messages
   */
  protected String createLoggingPrefix(int iterationIndex) {
    return LoggingUtils.runIdPrefix(getId()) + LoggingUtils.iterationPrefix(iterationIndex);
  }

  /**
   * Check if any components are undefined, if so throw exception
   *
   * @throws PlanItException thrown if any components are undefined
   */
  protected void checkForEmptyComponents() throws PlanItException {
    // input components
    PlanItException.throwIf(demands == null, "Demand is null");
    PlanItException.throwIf(physicalNetwork == null, "Network is null");
    PlanItException.throwIf(zoning == null, "Zoning is null");
    // traffic assignment components
    PlanItException.throwIf(getSmoothing() == null, "Smoothing is null");
    PlanItException.throwIf(getGapFunction() == null, "GapFunction is null");
    PlanItException.throwIf(getPhysicalCost() == null, "PhysicalCost is null");
    PlanItException.throwIf(getVirtualCost() == null, "VirtualCost is null");
  }

  /**
   * Verify if the create traffic assignment (sbu)components are compatible with each other and the created transport network. Called before starting the simulation and after the
   * transport network has been generated from physical and virtual network. So this is called after the build of the assignment instance
   *
   * @throws PlanItException thrown if the components are not compatible
   */
  protected abstract void verifyComponentCompatibility() throws PlanItException;

  /**
   * Verify if the traffic assignment inputs (components which are provided upon creation and not subcomponents that are created as part of the build process of the assignment are
   * compatible). Called after creation of the assignment instance by the builder, but before the traffic assignment's (sub)components have been created. So this is invoked during
   * the build of the assignment instance, not after.
   *
   * @throws PlanItException thrown if the components are not compatible
   */
  protected abstract void verifyNetworkDemandZoningCompatibility() throws PlanItException;

  /**
   * Initialize the transport network by combining the physical and virtual components
   * 
   * @throws PlanItException thrown if there is an error
   */
  protected void createTransportNetwork() throws PlanItException {
    transportNetwork = new TransportModelNetwork(physicalNetwork, zoning);
    transportNetwork.integrateTransportNetworkViaConnectoids();
    if (getTransportNetwork().getNumberOfEdgeSegmentsAllLayers() > Integer.MAX_VALUE) {
      throw new PlanItException("currently assignment internals expect to be castable to int, but max value is exceeded for link segments");
    }
    if (getTransportNetwork().getNumberOfVerticesAllLayers() > Integer.MAX_VALUE) {
      throw new PlanItException("currently assignment internals expect to be castable to int, but max value is exceeded for vertices");
    }
  }

  /**
   * Total number of edge segments across the entire network and all layers (physical and virtual)
   * 
   * @return Total number of edge segments across the entire network and all layers
   */
  protected int getTotalNumberOfNetworkSegments() {
    return getTransportNetwork().getNumberOfEdgeSegmentsAllLayers();
  }

  /**
   * Total number of vertices across the entire network (physical and virtual) and all layers
   * 
   * @return Total number of vertices across the entire network and all layers
   */
  protected int getTotalNumberOfNetworkVertices() {
    return getTransportNetwork().getNumberOfVerticesAllLayers();
  }

  // Public

  // Public abstract methods

  /**
   * detach the virtual and physical transport network again
   * 
   * @throws PlanItException thrown if there is an error
   */
  protected void disbandTransportNetwork() throws PlanItException {
    // Disconnect here since the physical network might be reused in a different
    // assignment
    transportNetwork.removeVirtualNetworkFromPhysicalNetwork();
  }

  /**
   * Initialize all relevant traffic assignment components before execution of the assignment commences
   *
   * @throws PlanItException thrown if there is an error
   */
  protected void initialiseBeforeExecution() throws PlanItException {
    // verify validity
    checkForEmptyComponents();
    createTransportNetwork();
    /* check components, including the transport network that just has been created */
    verifyComponentCompatibility();

    outputManager.initialiseBeforeSimulation(getId());

    getPhysicalCost().initialiseBeforeSimulation(physicalNetwork);
    getVirtualCost().initialiseBeforeSimulation(zoning.getVirtualNetwork());
  }

  /**
   * Run equilibration after resources initialized, including saving results
   *
   * @throws PlanItException thrown if there is an error
   */
  protected abstract void executeEquilibration() throws PlanItException;

  /**
   * Finalize all relevant traffic assignment components after execution of the assignment has ended
   * 
   * @throws PlanItException thrown if there is an error
   */
  protected void finalizeAfterExecution() throws PlanItException {

    outputManager.finaliseAfterSimulation();

    disbandTransportNetwork();
  }

  /**
   * Collect the output manager
   * 
   * @return output manager for this assignment
   */
  protected OutputManager getOutputManager() {
    return outputManager;
  }

  /**
   * log registering an item on this traffic assignment
   * 
   * @param item     to (un)register
   * @param register when true it signals activate, otherwise deactivate
   */
  protected void logRegisteredComponentName(Object item, boolean register) {
    LOGGER.info(LoggingUtils.runIdPrefix(getId()) + LoggingUtils.logActiveStateByClassName(item, register));
  }

  /**
   * Register a PLANit component on this assignment
   * 
   * @param componentKey to use
   * @param component    to register
   */
  protected void registerComponent(Class<? extends PlanitComponent<?>> componentKey, PlanitComponent<?> component) {
    trafficAssignmentComponents.put(componentKey, component);
  }

  // Public

  // Public abstract methods

  /** short hand to choose traditional static assignment as assignment type */
  public static String TRADITIONAL_STATIC_ASSIGNMENT = TraditionalStaticAssignment.class.getCanonicalName();

  /** short hand to choose algorithmB as assignment type */
  public static String ALGORITHM_B = AlgorithmB.class.getCanonicalName();

  /** short hand to choose eLTM as assignment type */
  public static String ELTM = EventBasedLtm.class.getCanonicalName();

  /** short hand to choose sLTM as assignment type */
  public static String SLTM = StaticLtm.class.getCanonicalName();

  /**
   * Constructor. Note that defaults that partly depend on derived classes are assumed to be invoked by the calling method via this.initialiseDefaults()
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   */
  public TrafficAssignment(IdGroupingToken groupId) {
    super(groupId);
    trafficAssignmentComponents = new HashMap<Class<? extends PlanitComponent<?>>, PlanitComponent<?>>();
    initialLinkSegmentCostByTimePeriod = new HashMap<TimePeriod, InitialModesLinkSegmentCost>();
  }

  /**
   * Copy Constructor
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected TrafficAssignment(TrafficAssignment other, boolean deepCopy) {
    super(other, deepCopy);

    this.demands        = deepCopy ? other.demands.deepClone()          : other.demands.shallowClone();
    this.physicalNetwork= deepCopy ? other.physicalNetwork.deepClone()  : other.physicalNetwork.shallowClone();
    this.zoning         = deepCopy ? other.zoning.deepClone()           : other.zoning.shallowClone();

    this.trafficAssignmentComponents = new HashMap<>();
    other.trafficAssignmentComponents.entrySet().forEach(
            entry -> trafficAssignmentComponents.put(entry.getKey(), deepCopy ? entry.getValue().deepClone() : entry.getValue()));

    // primitive container wrapper, so clone equates to deep clone
    this.initialLinkSegmentCostTimePeriodAgnostic = other.initialLinkSegmentCostTimePeriodAgnostic.shallowClone();
    // map of primitive container wrappers, so clone equates to deep clone
    this.initialLinkSegmentCostByTimePeriod = new HashMap<>();
    other.initialLinkSegmentCostByTimePeriod.forEach( (p,e) -> initialLinkSegmentCostByTimePeriod.put(p, e.shallowClone()));
  }

  /**
   * Collect the current iteration index of the simulation
   * 
   * @return current iteration index
   */
  public abstract int getIterationIndex();

  /**
   * Create the output type adapter for the current output type, specifically tailored towards the assignment type that we are builing
   *
   * @param outputType the current output type
   * @return the output type adapter corresponding to the current traffic assignment and output type
   */
  public abstract OutputTypeAdapter createOutputTypeAdapter(OutputType outputType);

  // Public methods

  /**
   * Execute assignment, including initializing resources, running equilibration and then closing resources
   *
   * @throws PlanItException thrown if there is an error
   */
  public void execute() throws PlanItException {

    LOGGER.info(LoggingUtils.runIdPrefix(getId()) + LoggingUtils.surround(this.getClass().getSimpleName(), '-', 17));

    initialiseBeforeExecution();

    executeEquilibration();

    finalizeAfterExecution();

    LOGGER.info(LoggingUtils.runIdPrefix(getId()) + LoggingUtils.surround(this.getClass().getSimpleName(), '-', 17));
  }

  /**
   * Log settings of this traffic assignment
   */
  public void logAllSettings() {
    LOGGER.info(LoggingUtils.runIdPrefix(getId()) + LoggingUtils.surround("ASSIGNMENT SETTINGS - START", '-', 17));
    logComponentSettings(this);
    for (var componentEntry : this.trafficAssignmentComponents.entrySet()) {
      logComponentSettings(componentEntry.getValue());
    }
    // TODO output manager settings
    // this.getOutputManager().collectSettingsAsKeyValueMap();
    LOGGER.info(LoggingUtils.runIdPrefix(getId()) + LoggingUtils.surround("ASSIGNMENT SETTINGS - END", '-', 17));
  }

  // Getters - Setters

  /**
   * {@inheritDoc}
   */
  public void reset() {
    // do not reset input components because they are considered configuration, not internal state. Also, initial cost is not reset since
    // this is considered a fixed input as well without an internal state, we only remove them
    this.initialLinkSegmentCostByTimePeriod.clear();
    this.initialLinkSegmentCostTimePeriodAgnostic = null;

    // disband the transport network, this is considered internal state
    try {
      disbandTransportNetwork();
    } catch (PlanItException e) {
      LOGGER.severe(String.format("Unable to reset assignment %s, transport network could not be disbanded", getXmlId()));
    }

    // do reset internal traffic assignment components, they are considered internal state.
    this.trafficAssignmentComponents.forEach((clazz, component) -> component.reset());
  }

  /**
   * Get the TransportNetwork used in the current assignment
   *
   * @return TransportNetwork used in current assignment
   */
  public TransportModelNetwork getTransportNetwork() {
    return transportNetwork;
  }

  /**
   * Set the physicalNetwork for the current assignment
   *
   * @param physicalNetwork the network object for the current assignment
   */
  public void setInfrastructureNetwork(final LayeredNetwork<?, ?> physicalNetwork) {
    logRegisteredComponentName(physicalNetwork, true);
    this.physicalNetwork = physicalNetwork;
  }

  /**
   * Get the physical network for the current assignment
   *
   * @return physical network for the current assignment
   */
  public LayeredNetwork<?, ?> getInfrastructureNetwork() {
    return this.physicalNetwork;
  }

  /**
   * Get the demands for the current assignment
   *
   * @return demands for the current assignment
   */
  public Demands getDemands() {
    return this.demands;
  }

  /**
   * Set the Demands object for the current assignment
   *
   * @param demands the Demands object for the current assignment
   */
  public void setDemands(final Demands demands) {
    logRegisteredComponentName(demands, true);
    this.demands = demands;
  }

  /**
   * Get the zoning for the current assignment
   *
   * @return zoning for the current assignment
   */
  public Zoning getZoning() {
    return this.zoning;
  }

  /**
   * Set the zoning object for the current assignment
   *
   * @param zoning the Zoning object for the current assignment
   */
  public void setZoning(final Zoning zoning) {
    logRegisteredComponentName(zoning, true);
    this.zoning = zoning;
  }

  /**
   * Set the Smoothing object for the current assignment
   *
   * @param smoothing Smoothing object for the current assignment
   */
  public void setSmoothing(final Smoothing smoothing) {
    logRegisteredComponentName(smoothing, true);
    registerComponent(Smoothing.class, smoothing);
  }

  /**
   * Collect the smoothing object for the current traffic assignment
   * 
   * @return smoothing
   */
  public Smoothing getSmoothing() {
    return getTrafficAssignmentComponent(Smoothing.class);
  }

  /**
   * Collect the gap function which is to be set by a derived class of TrafficAssignment via the initialiseDefaults() right after construction
   *
   * @param gapfunction the gap function to set
   */
  public void setGapFunction(final GapFunction gapfunction) {
    logRegisteredComponentName(gapfunction, true);
    registerComponent(GapFunction.class, gapfunction);
  }

  /**
   * Collect the gap function which is to be set by a derived class of TrafficAssignment via the initialiseDefaults() right after construction
   *
   * @return gapFunction
   */
  public GapFunction getGapFunction() {
    return getTrafficAssignmentComponent(GapFunction.class);
  }

  /**
   * Set the initial link segment cost that is mode agnostic and can be used as the fallback if no time period specific initial cost is registered
   *
   * @param initialLinkSegmentCost the initial link segment cost
   */
  public void setInitialLinkSegmentCost(final InitialModesLinkSegmentCost initialLinkSegmentCost) {
    this.initialLinkSegmentCostTimePeriodAgnostic = initialLinkSegmentCost;
  }

  /**
   * Set the initial link segment cost for a specified time period on the assignment, otherwise revert to the general initial link segment cost (if any). Note thta the provided
   * time period might differ from the how the provided initial costs were parsed on the project if so desired.
   *
   * @param timePeriod             the specified time period
   * @param initialLinkSegmentCost the initial link segment cost to apply for the assignment time period
   */
  public void setInitialLinkSegmentCost(final TimePeriod timePeriod, final InitialModesLinkSegmentCost initialLinkSegmentCost) {
    initialLinkSegmentCostByTimePeriod.put(timePeriod, initialLinkSegmentCost);
  }

  /**
   * Set the physical cost where in case the cost is an InteractorAccessor will trigger an event to get access to the required data via requesting an InteractorAccessee
   *
   * @param physicalCost the physical cost object for the current assignment
   */
  public <LS extends MacroscopicLinkSegment> void setPhysicalCost(final AbstractPhysicalCost physicalCost) {
    logRegisteredComponentName(physicalCost, true);
    registerComponent(AbstractPhysicalCost.class, physicalCost);
  }

  /**
   * Get the dynamic physical cost object for the current assignment
   *
   * @return the physical cost object for the current assignment
   */
  public AbstractPhysicalCost getPhysicalCost() {
    return getTrafficAssignmentComponent(AbstractPhysicalCost.class);
  }

  /**
   * Returns the virtual cost object for the current assignment
   *
   * @return the virtual cost object for the current assignments
   */
  public AbstractVirtualCost getVirtualCost() {
    return getTrafficAssignmentComponent(AbstractVirtualCost.class);
  }

  /**
   * Set the virtual cost where in case the cost is an InteractorAccessor will trigger an event to get access to the required data via requesting an InteractorAccessee
   *
   * @param virtualCost the virtual cost object to be assigned
   * @throws PlanItException thrown if there is an error
   */
  public void setVirtualCost(final AbstractVirtualCost virtualCost) throws PlanItException {
    logRegisteredComponentName(virtualCost, true);
    registerComponent(AbstractVirtualCost.class, virtualCost);
  }

  /**
   * Collect the desired traffic assignment component by its class assuming it is available on the assignment. These are traffic assignment components that are created and
   * registered upon the assignment, so not component inputs that are readily available upon creation, but components specific to the assignment itself. Derived assignments might
   * also register additional components as well beyond the standard components registered here on the base class (gapfunction, smoothing, physical, virtual cost).
   * 
   * @param <T>                  component type
   * @param planitComponentClass to collect of type T
   * @return component, null if not available
   */
  @SuppressWarnings("unchecked")
  @Override
  public <T> T getTrafficAssignmentComponent(final Class<T> planitComponentClass) {
    T component = (T) trafficAssignmentComponents.get(planitComponentClass);
    if (component == null) {
      LOGGER.warning(String.format("Unable to access component supposed to be registered under %s, consider registering it first", planitComponentClass.getName()));
    }
    return component;
  }

  /**
   * Set the output manager which holds all the configuration options regarding this assignment
   * 
   * @param outputManager to set
   */
  public void setOutputManager(OutputManager outputManager) {
    this.outputManager = outputManager;
    // TODO: move all logging of components to one central place instead of in setters
    outputManager.getOutputFormatters().forEach(of -> logRegisteredComponentName(of, true));
    outputManager.getRegisteredOutputTypeConfigurations().forEach(oc -> LOGGER.info(LoggingUtils.runIdPrefix(this.getId()) + "activated: OutputType." + oc.getOutputType()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract TrafficAssignment shallowClone();

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract TrafficAssignment deepClone();

}
