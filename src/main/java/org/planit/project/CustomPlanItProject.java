package org.planit.project;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.planit.assignment.TrafficAssignment;
import org.planit.assignment.TrafficAssignmentBuilder;
import org.planit.assignment.TrafficAssignmentBuilderFactory;
import org.planit.assignment.TrafficAssignmentComponent;
import org.planit.assignment.TrafficAssignmentComponentFactory;
import org.planit.assignment.TrafficAssignmentConfigurator;
import org.planit.cost.physical.initial.InitialLinkSegmentCost;
import org.planit.cost.physical.initial.InitialLinkSegmentCostPeriod;
import org.planit.demands.Demands;
import org.planit.input.InputBuilderListener;
import org.planit.network.InfrastructureLayer;
import org.planit.network.InfrastructureNetwork;
import org.planit.network.virtual.Zoning;
import org.planit.output.formatter.OutputFormatter;
import org.planit.output.formatter.OutputFormatterFactory;
import org.planit.path.ODPathSets;
import org.planit.project.PlanItProjectInput.ProjectDemands;
import org.planit.project.PlanItProjectInput.ProjectNetworks;
import org.planit.project.PlanItProjectInput.ProjectODPathSets;
import org.planit.project.PlanItProjectInput.ProjectZonings;
import org.planit.time.TimePeriod;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.misc.LoggingUtils;

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
  public class ProjectAssignmentBuilders implements Iterable<TrafficAssignmentBuilder<?>> {

    /**
     * The traffic assignment(s) registered on this project
     */
    protected final Set<TrafficAssignmentBuilder<?>> builders = new HashSet<TrafficAssignmentBuilder<?>>();

    /**
     * add traffic assignment
     * 
     * @param trafficAssignmentBuilder to add
     */
    protected void addTrafficAssignmentBuilder(TrafficAssignmentBuilder<?> trafficAssignmentBuilder) {
      builders.add(trafficAssignmentBuilder);
    }

    /**
     * Get the number of traffic assignment
     *
     * @return the number of traffic assignment in the project
     */
    public int getNumberOfTrafficAssignmentBuilders() {
      return builders.size();
    }

    /**
     * Check if assignments have already been registered
     *
     * @return true if registered assignments exist, false otherwise
     */
    public boolean hasRegisteredAssignmentBuilders() {
      return !builders.isEmpty();
    }

    /**
     * Collect the first traffic assignment that is registered (if any). Otherwise return null
     * 
     * @return first traffic assignment that is registered if none return null
     */
    public TrafficAssignmentBuilder<?> getFirstTrafficAssignmentBuilder() {
      return hasRegisteredAssignmentBuilders() ? builders.iterator().next() : null;
    }

    /**
     * iterable over registered traffic assignments
     */
    @Override
    public Iterator<TrafficAssignmentBuilder<?>> iterator() {
      return builders.iterator();
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

  // Protected methods

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
   * The registered OD path sets
   */
  public final ProjectODPathSets odPathSets;

  /**
   * The registered assignment builders
   */
  public final ProjectAssignmentBuilders assignmentBuilders = new ProjectAssignmentBuilders();

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
    LOGGER.info(LoggingUtils.createProjectPrefix(this.id) + LoggingUtils.logActiveStateByClassName(inputBuilderListener, true));

    // connect inputs
    this.inputs = new PlanItProjectInput(this.id, projectToken, inputBuilderListener);
    this.physicalNetworks = inputs.physicalNetworks;
    this.demands = inputs.demands;
    this.zonings = inputs.zonings;
    this.odPathSets = inputs.odPathSets;
    this.outputFormatters = new TreeMap<Long, OutputFormatter>();
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
   * Create and register an infrastructure based network on the project
   *
   * @param infrastructureNetworkType name of infrastructure network class to register
   * @return the generated infrastructure network
   * @throws PlanItException thrown if there is an error
   */
  public InfrastructureNetwork createAndRegisterInfrastructureNetwork(final String infrastructureNetworkType) throws PlanItException {
    return inputs.createAndRegisterInfrastructureNetwork(infrastructureNetworkType);
  }

  /**
   * Create and register the zoning system on the network
   *
   * @param network the network on which the zoning will be based
   * @return the generated zoning object
   * @throws PlanItException thrown if there is an error
   */
  public Zoning createAndRegisterZoning(final InfrastructureNetwork network) throws PlanItException {
    return inputs.createAndRegisterZoning(network);
  }

  /**
   * Create and register demands to the project
   *
   * @param zoning  Zoning object which defines the zones which will be used in the demand matrix to be created
   * @param network the network which stores the modes (demands can different for each mode)
   * @return the generated demands object
   * @throws PlanItException thrown if there is an error
   */
  public Demands createAndRegisterDemands(final Zoning zoning, final InfrastructureNetwork network) throws PlanItException {
    return inputs.createAndRegisterDemands(zoning, network);
  }

  /**
   * Create and register the OD path sets as populated by the input builder through the path source
   * 
   * @param networkLayer       network layer the paths must be compatible with
   * @param zoning             zoning to match od paths to
   * @param odPathSetInputPath path to collect the paths from
   * @return od path sets that have been parsed
   * @throws PlanItException thrown if there is an error
   */
  public ODPathSets createAndRegisterOdPathSets(final InfrastructureLayer networkLayer, final Zoning zoning, final String odPathSetInputPath) throws PlanItException {
    return inputs.createAndRegisterOdPathSets(networkLayer, zoning, odPathSetInputPath);
  }

  /**
   * Create and register a deterministic traffic assignment instance of a given type
   *
   * @param trafficAssignmentType the class name of the traffic assignment type object to be created
   * @param theDemands            the demands
   * @param theZoning             the zoning
   * @param theNetwork            the network
   * @return the traffic assignment configurator object
   * @throws PlanItException thrown if there is an error
   */
  public TrafficAssignmentConfigurator<? extends TrafficAssignment> createAndRegisterTrafficAssignment(final String trafficAssignmentType, final Demands theDemands,
      final Zoning theZoning, final InfrastructureNetwork theNetwork) throws PlanItException {

    TrafficAssignmentBuilder<?> taBuilder = TrafficAssignmentBuilderFactory.createBuilder(trafficAssignmentType, projectToken, inputBuilderListener, theDemands, theZoning,
        theNetwork);
    assignmentBuilders.addTrafficAssignmentBuilder(taBuilder);

    /*
     * unconventional but useful in our context: the configuration of the builder is exposed via its configurator. This ensures that the end user remains unaware of the builder
     * pattern, but instead simply configures a proxy. The builder in turn is built from within the project leveraging the configuration that the user interacted with
     */
    return (TrafficAssignmentConfigurator<? extends TrafficAssignment>) taBuilder.getConfigurator();
  }

  /**
   * Create and register initial link segment costs from a (single) file which we assume are available in the native xml/csv output format as provided in this project
   *
   * @param network  network the InitialLinkSegmentCost object will be registered for
   * @param fileName file containing the initial link segment cost values
   * @return the InitialLinkSegmentCost object
   * @throws PlanItException thrown if there is an error
   */
  public InitialLinkSegmentCost createAndRegisterInitialLinkSegmentCost(final InfrastructureNetwork network, final String fileName) throws PlanItException {
    return inputs.createAndRegisterInitialLinkSegmentCost(network, fileName);
  }

  /**
   * Create and register initial link segment costs from a (single) file and register it to the provided time period
   *
   * @param network    network the InitialLinkSegmentCost object will be registered for
   * @param fileName   location of file containing the initial link segment cost values
   * @param timePeriod to register the initial cost on
   * @return the InitialLinkSegmentCostPeriod object
   * @throws PlanItException thrown if there is an error
   */
  public InitialLinkSegmentCostPeriod createAndRegisterInitialLinkSegmentCost(final InfrastructureNetwork network, final String fileName, final TimePeriod timePeriod)
      throws PlanItException {
    return inputs.createAndRegisterInitialLinkSegmentCost(network, fileName, timePeriod);
  }

  /**
   * Create and register initial link segment costs from a (single) file for all time periods in Demands object
   *
   * @param network  network the InitialLinkSegmentCost object will be registered for
   * @param fileName location of file containing the initial link segment cost values
   * @param demands  the Demands object to extract all eligible time periods from
   * @return the InitialLinkSegmentCostPeriod objects
   * @throws PlanItException thrown if there is an error
   */
  public List<InitialLinkSegmentCostPeriod> createAndRegisterInitialLinkSegmentCost(final InfrastructureNetwork network, final String fileName, final Demands demands)
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
   * @param network the specified network
   * @return the initial link segment costs for the specified physical network
   */
  public List<InitialLinkSegmentCost> getInitialLinkSegmentCost(final InfrastructureNetwork network) {
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
    Set<TrafficAssignment> failedAssignments = new HashSet<TrafficAssignment>();
    for (TrafficAssignmentBuilder<?> tab : assignmentBuilders) {
      TrafficAssignment ta = null;
      try {
        ta = tab.build();
        LOGGER.info(LoggingUtils.createProjectPrefix(this.id) + LoggingUtils.logActiveStateByClassName(ta, true));
        LOGGER.info(LoggingUtils.createProjectPrefix(this.id) + LoggingUtils.createRunIdPrefix(ta.getId()) + "assignment created");
        ta.execute();
      } catch (final PlanItException pe) {
        LOGGER.severe(pe.getMessage());
        if (ta != null) {
          failedAssignments.add(ta);
        }
      }
    }

    if (!failedAssignments.isEmpty()) {
      String failedAssignmentessage = "the following assignments failed:";
      failedAssignments.forEach(ta -> failedAssignmentessage.concat(" ").concat(String.valueOf(ta.getId())));
      throw new PlanItException(failedAssignmentessage);
    }
  }

}