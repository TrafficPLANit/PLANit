package org.planit.assignment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.djutils.event.Event;
import org.djutils.event.EventType;
import org.planit.assignment.traditionalstatic.TraditionalStaticAssignment;
import org.planit.cost.physical.PhysicalCost;
import org.planit.cost.physical.initial.InitialLinkSegmentCost;
import org.planit.cost.virtual.VirtualCost;
import org.planit.demands.Demands;
import org.planit.gap.GapFunction;
import org.planit.input.InputBuilderListener;
import org.planit.interactor.InteractorAccessor;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.network.physical.macroscopic.MacroscopicLinkSegmentImpl;
import org.planit.network.transport.TransportNetwork;
import org.planit.network.virtual.Zoning;
import org.planit.output.OutputManager;
import org.planit.output.adapter.OutputTypeAdapter;
import org.planit.output.configuration.OutputConfiguration;
import org.planit.output.configuration.OutputTypeConfiguration;
import org.planit.output.enums.OutputType;
import org.planit.output.formatter.OutputFormatter;
import org.planit.sdinteraction.smoothing.Smoothing;
import org.planit.supply.networkloading.NetworkLoading;
import org.planit.time.TimePeriod;
import org.planit.trafficassignment.builder.TrafficAssignmentBuilder;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.misc.LoggingUtils;

/**
 * Traffic assignment class which simultaneously is responsible for the loading hence it is also considered as a traffic assignment component of this type
 *
 * @author markr
 *
 */
public abstract class TrafficAssignment extends NetworkLoading {

  // Private

  /** generated UID */
  private static final long serialVersionUID = 801775330292422910L;

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(MacroscopicLinkSegmentImpl.class.getCanonicalName());

  /**
   * The zoning to use
   */
  private Zoning zoning;

  /**
   * Gap function instance containing functionality to compute the gap between equilibrium and the current state gap
   */
  private GapFunction gapFunction;

  /**
   * Output manager deals with all the output configurations for the registered traffic assignments
   */
  private OutputManager outputManager;

  // Protected

  /**
   * The builder for all traffic assignment instances
   */
  protected TrafficAssignmentBuilder trafficAssignmentBuilder;

  /**
   * Physical network to use
   */
  protected PhysicalNetwork physicalNetwork;

  /**
   * The transport network to use which is an adaptor around the physical network and the zoning
   */
  protected TransportNetwork transportNetwork = null;

  /**
   * The virtual cost function
   */
  protected VirtualCost virtualCost;

  /**
   * holds the count of all link segments in the transport network
   */
  protected int numberOfNetworkSegments;

  /**
   * holds the count of all vertices in the transport network
   */
  protected int numberOfNetworkVertices;

  /**
   * the smoothing to use
   */
  protected Smoothing smoothing = null;

  /**
   * The demand to use
   */
  protected Demands demands = null;

  /**
   * The initial link segment cost
   */
  protected InitialLinkSegmentCost initialLinkSegmentCost;

  /**
   * The physical generalized cost approach
   */
  protected PhysicalCost physicalCost;

  /**
   * Map storing InitialLinkSegmentCost objects for each time period
   */
  protected Map<TimePeriod, InitialLinkSegmentCost> initialLinkSegmentCostByTimePeriod;

  /**
   * create the traffic assignment builder for this traffic assignment
   * 
   * @param trafficComponentCreateListener listener to register on all traffic assignment components that this builder can build
   * @param demands                        the demands this assignment works on
   * @param zoning                         the zoning this assignment works on
   * @param physicalNetwork                the physical network this assignment works on
   * @return created traffic assignment builder
   * @throws PlanItException thrown if there is an error
   */
  protected abstract TrafficAssignmentBuilder createTrafficAssignmentBuilder(InputBuilderListener trafficComponentCreateListener, Demands demands, Zoning zoning,
      PhysicalNetwork physicalNetwork) throws PlanItException;

  // Protected methods

  /**
   * register all the known listeners for the passed in eventType on this producer for this event type
   * 
   * @param eventType the event type to register
   */
  protected abstract void addRegisteredEventTypeListeners(EventType eventType);

  /**
   * create the logging prefix for logging statements during equilibration
   * 
   * @param iterationIndex the iteration
   * @return prefix for logging of traffic assignment messages
   */
  protected String createLoggingPrefix(int iterationIndex) {
    return LoggingUtils.createRunIdPrefix(getId()) + LoggingUtils.createIterationPrefix(iterationIndex);
  }

  /**
   * Check if any components are undefined, if so throw exception
   *
   * @throws PlanItException thrown if any components are undefined
   */
  protected void checkForEmptyComponents() throws PlanItException {
    PlanItException.throwIf(demands == null, "Demand is null");
    PlanItException.throwIf(physicalNetwork == null, "Network is null");
    PlanItException.throwIf(smoothing == null, "Smoothing is null");
    PlanItException.throwIf(zoning == null, "Zoning is null");
  }

  /**
   * Verify if the traffic assignment components are compatible and nonnull
   *
   * @throws PlanItException thrown if the components are not compatible
   */
  protected void verifyComponentCompatibility() throws PlanItException {
    // TODO
  }

  /**
   * Initialize the transport network by combining the physical and virtual components
   * 
   * @throws PlanItException thrown if there is an error
   */
  protected void createTransportNetwork() throws PlanItException {
    transportNetwork = new TransportNetwork(physicalNetwork, zoning);
    transportNetwork.integrateConnectoidsAndLinks();
    this.numberOfNetworkSegments = getTransportNetwork().getTotalNumberOfEdgeSegments();
    this.numberOfNetworkVertices = getTransportNetwork().getTotalNumberOfVertices();
  }

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
    verifyComponentCompatibility();
    createTransportNetwork();
    outputManager.initialiseBeforeSimulation(getId());
    physicalCost.initialiseBeforeSimulation(physicalNetwork);
    virtualCost.initialiseBeforeSimulation(zoning.getVirtualNetwork());
  }

  /**
   * Finalize all relevant traffic assignment components after execution of the assignment has ended
   * 
   * @throws PlanItException thrown if there is an error
   */
  protected void finalizeAfterExecution() throws PlanItException {

    disbandTransportNetwork();

    outputManager.finaliseAfterSimulation();
  }

  /**
   * Collect the output manager
   * 
   * @return output manager for this assignment
   */
  protected OutputManager getOutputManager() {
    return outputManager;
  }

  // Public

  // short hand to choose traditional static assignment as assignment type
  public static String TRADITIONAL_STATIC_ASSIGNMENT = TraditionalStaticAssignment.class.getCanonicalName();

  /**
   * Constructor. Note that defaults that partly depend on derived classes are assumed to be invoked by the calling method via this.initialiseDefaults()
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   */
  public TrafficAssignment(IdGroupingToken groupId) {
    super(groupId);
    outputManager = new OutputManager(this);
    initialLinkSegmentCostByTimePeriod = new HashMap<TimePeriod, InitialLinkSegmentCost>();
  }

  // Public abstract methods

  /**
   * Each traffic assignment class can have its own builder which reveals what components need to be registered on the traffic assignment instance in order to function properly.
   *
   * @param trafficComponentCreateListener, the listener should be registered on all traffic component factories the traffic assignment utilises
   * @param theDemands                      this assignment works on
   * @param theZoning                       this assignment works on
   * @param thePhysicalNetwork              this assignment works on
   * @return trafficAssignmentBuilder to use
   * @throws PlanItException thrown if there is an error
   */
  public TrafficAssignmentBuilder collectBuilder(final InputBuilderListener trafficComponentCreateListener, final Demands theDemands, final Zoning theZoning,
      final PhysicalNetwork thePhysicalNetwork) throws PlanItException {
    if (this.trafficAssignmentBuilder == null) {
      this.trafficAssignmentBuilder = createTrafficAssignmentBuilder(trafficComponentCreateListener, theDemands, theZoning, thePhysicalNetwork);
    }
    return this.trafficAssignmentBuilder;
  }

  /**
   * Run equilibration after resources initialized, including saving results
   *
   * @throws PlanItException thrown if there is an error
   */
  public abstract void executeEquilibration() throws PlanItException;

  /**
   * Create the output type adapter for the current output type
   *
   * @param outputType the current output type
   * @return the output type adapter corresponding to the current traffic assignment and output type
   */
  public abstract OutputTypeAdapter createOutputTypeAdapter(OutputType outputType);

  /**
   * Collect the current iteration index of the simulation
   * 
   * @return current iteration index
   */
  public abstract int getIterationIndex();

  // Public methods

  /**
   * Method that allows one to activate specific output types for persistence which is passed on to the output manager
   *
   * @param outputType OutputType object to be used
   * @return outputTypeConfiguration the output type configuration that is now active
   * @throws PlanItException thrown if there is an error activating the output
   */
  public OutputTypeConfiguration activateOutput(final OutputType outputType) throws PlanItException {
    OutputTypeConfiguration theOutputTypeConfiguration = null;
    if (!isOutputTypeActive(outputType)) {
      final OutputTypeAdapter outputTypeAdapter = createOutputTypeAdapter(outputType);
      outputManager.registerOutputTypeAdapter(outputType, outputTypeAdapter);
      theOutputTypeConfiguration = outputManager.createAndRegisterOutputTypeConfiguration(outputType, this);
    } else {
      theOutputTypeConfiguration = outputManager.getOutputTypeConfiguration(outputType);
    }
    return theOutputTypeConfiguration;
  }

  /**
   * Deactivate the specified output type
   * 
   * @param outputType the output type to be deactivated
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
   * @param outputType the output type to check if active
   * @return true if active, false otherwise
   */
  public boolean isOutputTypeActive(final OutputType outputType) {
    return outputManager.isOutputTypeActive(outputType);
  }

  /**
   * Execute assignment, including initializing resources, running equilibration and then closing resources
   *
   * @throws PlanItException thrown if there is an error
   */
  public void execute() throws PlanItException {

    LOGGER.info(LoggingUtils.createRunIdPrefix(getId()) + String.format("----------------- %s -----------------", this.getClass().getSimpleName()));

    initialiseBeforeExecution();

    executeEquilibration();

    finalizeAfterExecution();

    LOGGER.info(LoggingUtils.createRunIdPrefix(getId()) + String.format("----------------- %s ----------------", this.getClass().getSimpleName()));
  }

  // Getters - Setters

  /**
   * Get the TransportNetwork used in the current assignment
   *
   * @return TransportNetwork used in current assignment
   */
  public TransportNetwork getTransportNetwork() {
    return transportNetwork;
  }

  /**
   * Provide the output configuration for user access (via the output manager)
   *
   * @return outputConfiguration for this traffic assignment
   */
  public OutputConfiguration getOutputConfiguration() {
    return outputManager.getOutputConfiguration();
  }

  /**
   * Set the Smoothing object for the current assignment
   *
   * @param smoothing Smoothing object for the current assignment
   */
  public void setSmoothing(final Smoothing smoothing) {
    this.smoothing = smoothing;
  }

  /**
   * Collect the smoothing object for the current traffic assignment
   * 
   * @return smoothing
   */
  public Smoothing getSmoothing() {
    return this.smoothing;
  }

  /**
   * Collect the gap function which is to be set by a derived class of TrafficAssignment via the initialiseDefaults() right after construction
   *
   * @return gapFunction
   */
  public GapFunction getGapFunction() {
    return gapFunction;
  }

  /**
   * Collect the gap function which is to be set by a derived class of TrafficAssignment via the initialiseDefaults() right after construction
   *
   * @param gapfunction the gap function to set
   */
  public void setGapFunction(final GapFunction gapfunction) {
    this.gapFunction = gapfunction;
  }

  /**
   * Set the PhysicalNetwork for the current assignment
   *
   * @param physicalNetwork the PhysicalNetwork object for the current assignment
   */
  public void setPhysicalNetwork(final PhysicalNetwork physicalNetwork) {
    this.physicalNetwork = physicalNetwork;
  }

  /**
   * Set the Demands object for the current assignment
   *
   * @param demands the Demands object for the current assignment
   */
  public void setDemands(final Demands demands) {
    this.demands = demands;
  }

  /**
   * Set the zoning object for the current assignment
   *
   * @param zoning the Zoning object for the current assignment
   */
  public void setZoning(final Zoning zoning) {
    this.zoning = zoning;
  }

  /**
   * Set the initial link segment cost unrelated to any particular time period, i.e., used for all time periods that do not have designated initial costs specified for them
   *
   * @param initialLinkSegmentCost the initial link segment cost
   */
  public void setInitialLinkSegmentCost(final InitialLinkSegmentCost initialLinkSegmentCost) {
    this.initialLinkSegmentCost = initialLinkSegmentCost;
  }

  /**
   * Set the initial link segment cost for a specified time period, otherwise revert to the general initial link segment cost (if any)
   *
   * @param timePeriod             the specified time period
   * @param initialLinkSegmentCost the initial link segment cost
   */
  public void setInitialLinkSegmentCost(final TimePeriod timePeriod, final InitialLinkSegmentCost initialLinkSegmentCost) {
    initialLinkSegmentCostByTimePeriod.put(timePeriod, initialLinkSegmentCost);
  }

  /**
   * Get the dynamic physical cost object for the current assignment
   *
   * @return the physical cost object for the current assignment
   */
  public PhysicalCost getPhysicalCost() {
    return physicalCost;
  }

  /**
   * Set the physical cost where in case the cost is an InteractorAccessor will trigger an event to get access to the required data via requesting an InteractorAccessee
   *
   * @param physicalCost the physical cost object for the current assignment
   * @throws PlanItException thrown if there is an error
   */
  public void setPhysicalCost(final PhysicalCost physicalCost) throws PlanItException {
    this.physicalCost = physicalCost;
    if (this.physicalCost instanceof InteractorAccessor) {
      // request an accessee instance that we can use to collect the relevant
      // information for the cost
      final EventType requestAccessee = ((InteractorAccessor) physicalCost).getRequestedAccesseeEventType();
      addRegisteredEventTypeListeners(requestAccessee);
      fireEvent(new Event(requestAccessee, this, this.physicalCost));
      PlanItException.throwIf(!listeners.containsKey(requestAccessee), "Error during setPhysicalCost");
    }
  }

  /**
   * Returns the virtual cost object for the current assignment
   *
   * @return the virtual cost object for the current assignments
   */
  public VirtualCost getVirtualCost() {
    return virtualCost;
  }

  /**
   * Set the virtual cost where in case the cost is an InteractorAccessor will trigger an event to get access to the required data via requesting an InteractorAccessee
   *
   * @param virtualCost the virtual cost object to be assigned
   * @throws PlanItException thrown if there is an error
   */
  public void setVirtualCost(final VirtualCost virtualCost) throws PlanItException {
    this.virtualCost = virtualCost;
    if (this.virtualCost instanceof InteractorAccessor) {
      // request an accessee instance that we can use to collect the relevant
      // information for the virtual cost
      final EventType requestAccesseeType = ((InteractorAccessor) virtualCost).getRequestedAccesseeEventType();
      addRegisteredEventTypeListeners(requestAccesseeType);
      fireEvent(new Event(requestAccesseeType, this, this.virtualCost));
      if (!listeners.containsKey(requestAccesseeType)) {
        String errorMessage = "Error during setVirtualCost";
        throw new PlanItException(errorMessage);
      }
    }
  }

  /**
   * Register the output formatter on the assignment
   *
   * @param outputFormatter OutputFormatter to be registered
   */
  public void registerOutputFormatter(final OutputFormatter outputFormatter) {
    outputManager.registerOutputFormatter(outputFormatter);
  }

  /**
   * Unregister an output formatter
   * 
   * @param outputFormatter the output formatter to be removed
   */
  public void unregisterOutputFormatter(final OutputFormatter outputFormatter) {
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

}
