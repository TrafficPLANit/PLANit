package org.planit.assignment;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.planit.assignment.algorithmb.AlgorithmB;
import org.planit.assignment.traditionalstatic.TraditionalStaticAssignment;
import org.planit.cost.physical.AbstractPhysicalCost;
import org.planit.cost.physical.initial.InitialLinkSegmentCost;
import org.planit.cost.virtual.AbstractVirtualCost;
import org.planit.demands.Demands;
import org.planit.gap.GapFunction;
import org.planit.network.TransportLayerNetwork;
import org.planit.network.layer.macroscopic.MacroscopicLinkSegmentImpl;
import org.planit.network.transport.TransportModelNetwork;
import org.planit.output.OutputManager;
import org.planit.output.adapter.OutputTypeAdapter;
import org.planit.output.enums.OutputType;
import org.planit.sdinteraction.smoothing.Smoothing;
import org.planit.supply.networkloading.NetworkLoading;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.misc.LoggingUtils;
import org.planit.utils.time.TimePeriod;
import org.planit.zoning.Zoning;

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

  /**
   * log registering an item on this traffic assignment
   * 
   * @param item     to (un)register
   * @param register when true it signals activate, otherwise deactive
   */
  private void logRegisteredComponent(Object item, boolean register) {
    LOGGER.info(LoggingUtils.createRunIdPrefix(getId()) + LoggingUtils.logActiveStateByClassName(item, register));
  }

  // Protected

  /**
   * network to use
   */
  private TransportLayerNetwork<?, ?> network;

  /**
   * The transport network to use which is an adaptor around the physical network and the zoning
   */
  protected TransportModelNetwork transportNetwork = null;

  /**
   * The virtual cost function
   */
  protected AbstractVirtualCost virtualCost;

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
  protected AbstractPhysicalCost physicalCost;

  /**
   * Map storing InitialLinkSegmentCost objects for each time period
   */
  protected Map<TimePeriod, InitialLinkSegmentCost> initialLinkSegmentCostByTimePeriod;

  // Protected methods

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
    PlanItException.throwIf(network == null, "Network is null");
    PlanItException.throwIf(smoothing == null, "Smoothing is null");
    PlanItException.throwIf(zoning == null, "Zoning is null");
  }

  /**
   * Verify if the traffic assignment components are compatible and nonnull
   *
   * @throws PlanItException thrown if the components are not compatible
   */
  protected void verifyComponentCompatibility() throws PlanItException {
    /*
     * allow derived classes to verify if the chosen components are valid before proceeding, not mandatory
     */
  }

  /**
   * Initialize the transport network by combining the physical and virtual components
   * 
   * @throws PlanItException thrown if there is an error
   */
  protected void createTransportNetwork() throws PlanItException {
    transportNetwork = new TransportModelNetwork(network, zoning);
    transportNetwork.integrateTransportNetworkViaConnectoids();
    if (getTransportNetwork().getTotalNumberOfEdgeSegments() > Integer.MAX_VALUE) {
      throw new PlanItException("currently assignment internals expect to be castable to int, but max value is exceeded for link segments");
    }
    if (getTransportNetwork().getTotalNumberOfVertices() > Integer.MAX_VALUE) {
      throw new PlanItException("currently assignment internals expect to be castable to int, but max value is exceeded for vertices");
    }
    this.numberOfNetworkSegments = (int) getTransportNetwork().getTotalNumberOfEdgeSegments();
    this.numberOfNetworkVertices = (int) getTransportNetwork().getTotalNumberOfVertices();
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
    createTransportNetwork();
    /* check components, including the transport network that just has been created */
    verifyComponentCompatibility();

    outputManager.initialiseBeforeSimulation(getId());

    physicalCost.initialiseBeforeSimulation(network);
    virtualCost.initialiseBeforeSimulation(zoning.getVirtualNetwork());
  }

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

  // Public

  /** short hand to choose traditional static assignment as assignment type */
  public static String TRADITIONAL_STATIC_ASSIGNMENT = TraditionalStaticAssignment.class.getCanonicalName();

  /** short hand to choose algorithmB as assignment type */
  public static String ALGORITHM_B = AlgorithmB.class.getCanonicalName();

  /** short hand to choose eLTM as assignment type */
  public static String ELTM = org.planit.assignment.eltm.ELTM.class.getCanonicalName();

  /**
   * Constructor. Note that defaults that partly depend on derived classes are assumed to be invoked by the calling method via this.initialiseDefaults()
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   */
  public TrafficAssignment(IdGroupingToken groupId) {
    super(groupId);
    initialLinkSegmentCostByTimePeriod = new HashMap<TimePeriod, InitialLinkSegmentCost>();
  }

  /**
   * Copy Constructor
   * 
   * @param trafficAssignment to copy
   */
  protected TrafficAssignment(TrafficAssignment trafficAssignment) {
    super(trafficAssignment);
    this.network = trafficAssignment.network;
    this.transportNetwork = trafficAssignment.transportNetwork;
    this.virtualCost = trafficAssignment.virtualCost;
    this.numberOfNetworkSegments = trafficAssignment.numberOfNetworkSegments;
    this.numberOfNetworkVertices = trafficAssignment.numberOfNetworkVertices;
    this.smoothing = trafficAssignment.smoothing;
    this.demands = trafficAssignment.demands;
    this.initialLinkSegmentCost = trafficAssignment.initialLinkSegmentCost;
    this.physicalCost = trafficAssignment.physicalCost;
    this.initialLinkSegmentCostByTimePeriod = trafficAssignment.initialLinkSegmentCostByTimePeriod;
  }

  // Public abstract methods

  /**
   * Create the output type adapter for the current output type, specifically tailored towards the assignment type that we are builing
   *
   * @param outputType the current output type
   * @return the output type adapter corresponding to the current traffic assignment and output type
   */
  public abstract OutputTypeAdapter createOutputTypeAdapter(OutputType outputType);

  /**
   * Run equilibration after resources initialized, including saving results
   *
   * @throws PlanItException thrown if there is an error
   */
  public abstract void executeEquilibration() throws PlanItException;

  /**
   * Collect the current iteration index of the simulation
   * 
   * @return current iteration index
   */
  public abstract int getIterationIndex();

  // Public methods

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
  public TransportModelNetwork getTransportNetwork() {
    return transportNetwork;
  }

  /**
   * Set the Smoothing object for the current assignment
   *
   * @param smoothing Smoothing object for the current assignment
   */
  public void setSmoothing(final Smoothing smoothing) {
    logRegisteredComponent(smoothing, true);
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
   * @param gapfunction the gap function to set
   */
  public void setGapFunction(final GapFunction gapfunction) {
    logRegisteredComponent(gapfunction, true);
    this.gapFunction = gapfunction;
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
   * Set the network for the current assignment
   *
   * @param network the network object for the current assignment
   */
  public void setInfrastructureNetwork(final TransportLayerNetwork<?, ?> network) {
    logRegisteredComponent(network, true);
    this.network = network;
  }

  /**
   * Set the Demands object for the current assignment
   *
   * @param demands the Demands object for the current assignment
   */
  public void setDemands(final Demands demands) {
    logRegisteredComponent(demands, true);
    this.demands = demands;
  }

  /**
   * Set the zoning object for the current assignment
   *
   * @param zoning the Zoning object for the current assignment
   */
  public void setZoning(final Zoning zoning) {
    logRegisteredComponent(zoning, true);
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
   * Set the physical cost where in case the cost is an InteractorAccessor will trigger an event to get access to the required data via requesting an InteractorAccessee
   *
   * @param physicalCost the physical cost object for the current assignment
   * @throws PlanItException thrown if there is an error
   */
  public void setPhysicalCost(final AbstractPhysicalCost physicalCost) throws PlanItException {
    logRegisteredComponent(physicalCost, true);
    this.physicalCost = physicalCost;
  }

  /**
   * Get the dynamic physical cost object for the current assignment
   *
   * @return the physical cost object for the current assignment
   */
  public AbstractPhysicalCost getPhysicalCost() {
    return physicalCost;
  }

  /**
   * Returns the virtual cost object for the current assignment
   *
   * @return the virtual cost object for the current assignments
   */
  public AbstractVirtualCost getVirtualCost() {
    return virtualCost;
  }

  /**
   * Set the virtual cost where in case the cost is an InteractorAccessor will trigger an event to get access to the required data via requesting an InteractorAccessee
   *
   * @param virtualCost the virtual cost object to be assigned
   * @throws PlanItException thrown if there is an error
   */
  public void setVirtualCost(final AbstractVirtualCost virtualCost) throws PlanItException {
    logRegisteredComponent(virtualCost, true);
    this.virtualCost = virtualCost;
  }

  /**
   * Set the output manager which holds all the configuration options regarding this assignment
   * 
   * @param outputManager to set
   */
  public void setOutputManager(OutputManager outputManager) {
    this.outputManager = outputManager;
    // TODO: move all logging of components to one central place instead of in setters
    outputManager.getOutputFormatters().forEach(of -> logRegisteredComponent(of, false));
    outputManager.getRegisteredOutputTypeConfigurations().forEach(oc -> LOGGER.info(LoggingUtils.createRunIdPrefix(this.getId()) + "activated: OutputType." + oc.getOutputType()));
  }

}
