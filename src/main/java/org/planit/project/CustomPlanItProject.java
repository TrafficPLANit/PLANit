package org.planit.project;

import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.planit.cost.physical.initial.InitialLinkSegmentCost;
import org.planit.cost.physical.initial.InitialLinkSegmentCostPeriod;
import org.planit.demands.Demands;
import org.planit.exceptions.PlanItException;
import org.planit.input.InputBuilderListener;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.network.virtual.Zoning;
import org.planit.output.formatter.OutputFormatter;
import org.planit.output.formatter.OutputFormatterFactory;
import org.planit.project.PlanItProjectInput.ProjectDemands;
import org.planit.project.PlanItProjectInput.ProjectNetworks;
import org.planit.project.PlanItProjectInput.ProjectODRouteSets;
import org.planit.project.PlanItProjectInput.ProjectZonings;
import org.planit.route.ODRouteSets;
import org.planit.supply.networkloading.NetworkLoading;
import org.planit.time.TimePeriod;
import org.planit.trafficassignment.TrafficAssignment;
import org.planit.trafficassignment.TrafficAssignmentComponent;
import org.planit.trafficassignment.TrafficAssignmentComponentFactory;
import org.planit.trafficassignment.builder.TrafficAssignmentBuilder;
import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;

/**
 * The top-level class which hosts a single project.
 *
 * A project can consist of multiple networks, demands and traffic assignments all based on a single configuration (user classes, modes etc.)
 *
 * @author markr
 *
 */
public class CustomPlanItProject {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(CustomPlanItProject.class.getCanonicalName());

  /**
   * Internal class for registered traffic assignments
   *
   */
  public class ProjectAssignments implements Iterable<TrafficAssignment> {

    /**
     * The traffic assignment(s) registered on this project
     */
    protected final TreeMap<Long, TrafficAssignment> trafficAssignmentsMap = new TreeMap<Long, TrafficAssignment>();

    /**
     * add traffic assignment
     * 
     * @param trafficAssignment to add
     */
    protected void addTrafficAssignment(TrafficAssignment trafficAssignment) {
      trafficAssignmentsMap.put(trafficAssignment.getId(), trafficAssignment);
    }

    /**
     * Get traffic assignment by id
     *
     * @param id the id of the traffic assignment
     * @return the retrieved assignment
     */
    public TrafficAssignment getTrafficAssignment(final long id) {
      return trafficAssignmentsMap.get(id);
    }

    /**
     * Get the number of traffic assignment
     *
     * @return the number of traffic assignment in the project
     */
    public int getNumberOfTrafficAssignments() {
      return trafficAssignmentsMap.size();
    }

    /**
     * Check if assignments have already been registered
     *
     * @return true if registered assignments exist, false otherwise
     */
    public boolean hasRegisteredAssignments() {
      return !trafficAssignmentsMap.isEmpty();
    }

    /**
     * Collect the first traffic assignment that is registered (if any). Otherwise return null
     * 
     * @return first traffic assignment that is registered if none return null
     */
    public TrafficAssignment getFirstTrafficAssignment() {
      return hasRegisteredAssignments() ? trafficAssignmentsMap.firstEntry().getValue() : null;
    }

    /**
     * iterable over registered traffic assignments
     */
    @Override
    public Iterator<TrafficAssignment> iterator() {
      return trafficAssignmentsMap.values().iterator();
    }

  }

  /**
   * unique identifier for this project across all projects in the JVM
   */
  protected final long id;

  /**
   * id generation using this token will be contiguous and unique for all instances created with this token. This token is related to the current instance of this class, i.e., the
   * project
   */
  protected IdGroupingToken projectToken;

  /**
   * The input container holding all traffic assignment input components and related functionality with respect to project management
   */
  protected final PlanItProjectInput inputs;

  /**
   * the listener that we register on each traffic assignment component creation event for external initialization
   */
  protected final InputBuilderListener inputBuilderListener;

  /**
   * The output formatter(s) registered on this project
   */
  protected final TreeMap<Long, OutputFormatter> outputFormatters;

  /**
   * Object factory for network loading object
   */
  protected TrafficAssignmentComponentFactory<NetworkLoading> assignmentFactory;

  // Protected methods

  /**
   * Instantiate the factories and register the event manager on them
   *
   */
  protected void initialiseFactories() {
    assignmentFactory = new TrafficAssignmentComponentFactory<NetworkLoading>(NetworkLoading.class);
    assignmentFactory.addListener(inputBuilderListener, TrafficAssignmentComponentFactory.TRAFFICCOMPONENT_CREATE);
  }

  /**
   * Execute a particular traffic assignment
   *
   * @param ta TrafficAssignment to be run
   */
  protected void executeTrafficAssignment(final TrafficAssignment ta) {
    try {
      ta.execute();
    } catch (final Exception e) {
      LOGGER.severe(e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * The registered physical networks
   */
  public final ProjectNetworks physicalNetworks;

  /**
   * The registered demands
   */
  public final ProjectDemands demands;

  /**
   * The registered zonings
   */
  public final ProjectZonings zonings;

  /**
   * The registered OD route sets
   */
  public final ProjectODRouteSets odRouteSets;

  /**
   * The registered assignments
   */
  public final ProjectAssignments trafficAssignments = new ProjectAssignments();

  // Public methods

  /**
   * Constructor which reads in the input builder listener and instantiates the object factory classes.
   *
   * This constructor instantiates the EventManager, which must be a singleton class for the whole application.
   *
   * @param inputBuilderListener InputBuilderListener used to read in data
   */
  public CustomPlanItProject(final InputBuilderListener inputBuilderListener) {
    this.id = IdGenerator.generateId(IdGroupingToken.collectGlobalToken(), CustomPlanItProject.class);
    this.projectToken = IdGenerator.createIdGroupingToken(this, this.id);

    this.inputBuilderListener = inputBuilderListener;

    // connect inputs
    this.inputs = new PlanItProjectInput(projectToken, inputBuilderListener);
    this.physicalNetworks = inputs.physicalNetworks;
    this.demands = inputs.demands;
    this.zonings = inputs.zonings;
    this.odRouteSets = inputs.odRouteSets;
    this.outputFormatters = new TreeMap<Long, OutputFormatter>();

    initialiseFactories();
  }

  /**
   * Register a class that we allow to be instantiated as a concrete implementation of a traffic assignment component that can be used in PLANit
   * 
   * @param theClazz the class that we want to mark as eligible from an outside source
   * @throws PlanItException thrown if class cannot be registered
   */
  public void registerEligibleTrafficComponentClass(Class<? extends TrafficAssignmentComponent<?>> theClazz) throws PlanItException {
    TrafficAssignmentComponentFactory.registerTrafficAssignmentComponentType(theClazz);
  }

  /**
   * Create and register a physical network on the project
   *
   * @param physicalNetworkType name of physical network class to register
   * @return the generated physical network
   * @throws PlanItException thrown if there is an error
   */
  public PhysicalNetwork createAndRegisterPhysicalNetwork(final String physicalNetworkType) throws PlanItException {
    return inputs.createAndRegisterPhysicalNetwork(physicalNetworkType);
  }

  /**
   * Create and register the zoning system on the network
   *
   * @param physicalNetwork the physical network on which the zoning will be based
   * @return the generated zoning object
   * @throws PlanItException thrown if there is an error
   */
  public Zoning createAndRegisterZoning(final PhysicalNetwork physicalNetwork) throws PlanItException {
    return inputs.createAndRegisterZoning(physicalNetwork);
  }

  /**
   * Create and register demands to the project
   *
   * @param zoning          Zoning object which defines the zones which will be used in the demand matrix to be created
   * @param physicalNetwork the physical network which stores the modes (demands can different for each mode)
   * @return the generated demands object
   * @throws PlanItException thrown if there is an error
   */
  public Demands createAndRegisterDemands(final Zoning zoning, final PhysicalNetwork physicalNetwork) throws PlanItException {
    return inputs.createAndRegisterDemands(zoning, physicalNetwork);
  }

  /**
   * Create and register the OD route sets as populated by the input builder through the path source
   * 
   * @param physicalNetwork     network the routes must be compatible with
   * @param zoning              zoning to match od routes to
   * @param odRouteSetInputPath path to collect the routes from
   * @return od route sets that have been parsed
   * @throws PlanItException thrown if there is an error
   */
  public ODRouteSets createAndRegisterODRouteSets(final PhysicalNetwork physicalNetwork, final Zoning zoning, final String odRouteSetInputPath) throws PlanItException {
    return inputs.createAndRegisterODRouteSets(physicalNetwork, zoning, odRouteSetInputPath);
  }

  /**
   * Create and register a deterministic traffic assignment instance of a given type
   *
   * @param trafficAssignmentType the class name of the traffic assignment type object to be created
   * @param theDemands            the demands
   * @param theZoning             the zoning
   * @param thePhysicalNetwork    the physical network
   * @return the traffic assignment builder object
   * @throws PlanItException thrown if there is an error
   */
  public TrafficAssignmentBuilder createAndRegisterTrafficAssignment(final String trafficAssignmentType, final Demands theDemands, final Zoning theZoning,
      final PhysicalNetwork thePhysicalNetwork) throws PlanItException {

    final NetworkLoading networkLoadingAndAssignment = assignmentFactory.create(trafficAssignmentType, new Object[] { projectToken });
    PlanItException.throwIf(!(networkLoadingAndAssignment instanceof TrafficAssignment), "not a valid traffic assignment type");

    final TrafficAssignment trafficAssignment = (TrafficAssignment) networkLoadingAndAssignment;
    final TrafficAssignmentBuilder trafficAssignmentBuilder = trafficAssignment.collectBuilder(inputBuilderListener, theDemands, theZoning, thePhysicalNetwork);
    // now initialize it, since initialization depends on the concrete class we
    // cannot do this on the constructor of the superclass nor
    // can we do it in the derived constructors as some components are the same
    // across assignments and we want to avoid duplicate code
    trafficAssignmentBuilder.initialiseDefaults();
    trafficAssignments.addTrafficAssignment(trafficAssignment);
    // do not allow direct access to the traffic assignment component. Instead,
    // provide the traffic assignment builder object which is dedicated to providing
    // all the configuration options relevant to the end user while hiding any
    // internals of the traffic assignment concrete class instance
    return trafficAssignmentBuilder;
  }

  /**
   * Create and register initial link segment costs from a (single) file which we assume are available in the native xml/csv output format as provided in this project
   *
   * @param network  physical network the InitialLinkSegmentCost object will be registered for
   * @param fileName file containing the initial link segment cost values
   * @return the InitialLinkSegmentCost object
   * @throws PlanItException thrown if there is an error
   */
  public InitialLinkSegmentCost createAndRegisterInitialLinkSegmentCost(final PhysicalNetwork network, final String fileName) throws PlanItException {
    return inputs.createAndRegisterInitialLinkSegmentCost(network, fileName);
  }

  /**
   * Create and register initial link segment costs from a (single) file and register it to the provided time period
   *
   * @param network    physical network the InitialLinkSegmentCost object will be registered for
   * @param fileName   location of file containing the initial link segment cost values
   * @param timePeriod to register the initial cost on
   * @return the InitialLinkSegmentCostPeriod object
   * @throws PlanItException thrown if there is an error
   */
  public InitialLinkSegmentCostPeriod createAndRegisterInitialLinkSegmentCost(final PhysicalNetwork network, final String fileName, final TimePeriod timePeriod)
      throws PlanItException {
    return inputs.createAndRegisterInitialLinkSegmentCost(network, fileName, timePeriod);
  }

  /**
   * Create and register initial link segment costs from a (single) file for all time periods in Demands object
   *
   * @param network  physical network the InitialLinkSegmentCost object will be registered for
   * @param fileName location of file containing the initial link segment cost values
   * @param demands  the Demands object to extract all eligible time periods from
   * @return the InitialLinkSegmentCostPeriod objects
   * @throws PlanItException thrown if there is an error
   */
  public List<InitialLinkSegmentCostPeriod> createAndRegisterInitialLinkSegmentCost(final PhysicalNetwork network, final String fileName, final Demands demands)
      throws PlanItException {
    return inputs.createAndRegisterInitialLinkSegmentCost(network, fileName, demands);
  }

  /**
   * Create and register an output formatter instance of a given type
   *
   * @param outputFormatterType the class name of the output formatter type object to be created
   * @return the generated output formatter object
   * @throws PlanItException thrown if there is an error
   */
  public OutputFormatter createAndRegisterOutputFormatter(final String outputFormatterType) throws PlanItException {
    final OutputFormatter outputFormatter = OutputFormatterFactory.createOutputFormatter(outputFormatterType);
    PlanItException.throwIf(outputFormatter == null, "Output writer of type " + outputFormatterType + " could not be created");

    outputFormatters.put(outputFormatter.getId(), outputFormatter);
    return outputFormatter;
  }

  /**
   * Return the initial link segment costs for a network
   *
   * @param network the specified physical network
   * @return the initial link segment costs for the specified physical network
   */
  public List<InitialLinkSegmentCost> getInitialLinkSegmentCost(final PhysicalNetwork network) {
    return inputs.getInitialLinkSegmentCost(network);
  }

  /**
   * Retrieve an output formatter object given its id
   *
   * @param id the id of the output formatter object
   * @return the retrieved output formatter object
   */
  public OutputFormatter getOutputFormatter(final long id) {
    return outputFormatters.get(id);
  }

  /**
   * Execute all registered traffic assignments
   *
   * Top-level error recording is done in this class. If several traffic assignments are registered and one fails, we record its error and continue with the next assignment.
   *
   * @throws PlanItException required for subclasses which override this method and generate an exception before the runs start
   */
  public void executeAllTrafficAssignments() throws PlanItException {
    for (TrafficAssignment ta : trafficAssignments) {
      try {
        ta.execute();
      } catch (final PlanItException pe) {
        LOGGER.severe(pe.getMessage());
      }
    }
  }

}