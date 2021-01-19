package org.planit.project;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.planit.assignment.TrafficAssignmentComponentFactory;
import org.planit.cost.physical.initial.InitialLinkSegmentCost;
import org.planit.cost.physical.initial.InitialLinkSegmentCostPeriod;
import org.planit.cost.physical.initial.InitialPhysicalCost;
import org.planit.demands.Demands;
import org.planit.input.InputBuilderListener;
import org.planit.network.InfrastructureLayer;
import org.planit.network.InfrastructureNetwork;
import org.planit.network.Network;
import org.planit.network.virtual.Zoning;
import org.planit.path.ODPathSets;
import org.planit.time.TimePeriod;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.misc.LoggingUtils;

/**
 * Class that holds all the input traffic components for a PLANit project. The PLANit project holds an instance of this class and delegates all calls relating to inputs to this
 * class.
 * 
 * @author markr
 *
 */
//@formatter:off
public class PlanItProjectInput {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(PlanItProjectInput.class.getCanonicalName());

  // INNER CLASSES

  /**
   * Internal class for registered physical networks
   *
   */
  public class ProjectNetworks {

    /**
     * Returns a List of infrastructure based networks
     *
     * @return List of networks
     */
    public List<InfrastructureNetwork> toList() {
      return new ArrayList<InfrastructureNetwork>(infrastructureNetworkMap.values());
    }

    /**
     * Get infrastructure network by id
     *
     * @param id the id of the network
     * @return the retrieved network
     */
    public InfrastructureNetwork getInfrastructureNetwork(final long id) {
      return infrastructureNetworkMap.get(id);
    }

    /**
     * Get the number of networks
     *
     * @return the number of networks in the project
     */
    public int getNumberOfInfrastructureNetworks() {
      return infrastructureNetworkMap.size();
    }

    /**
     * Check if infrastructure networks have already been registered
     *
     * @return true if registered networks exist, false otherwise
     */
    public boolean hasRegisteredInfrastructureNetworks() {
      return !infrastructureNetworkMap.isEmpty();
    }

    /**
     * Collect the first network that is registered (if any). Otherwise return null
     * 
     * @return first network that is registered if none return null
     */
    public InfrastructureNetwork getFirstInfrastructureNetwork() {
      return hasRegisteredInfrastructureNetworks() ? infrastructureNetworkMap.firstEntry().getValue() : null;
    }
  }

  /**
   * Internal class for registered demands
   *
   */
  public class ProjectDemands {

    /**
     * Returns a List of demands
     *
     * @return List of demands
     */
    public List<Demands> toList() {
      return new ArrayList<Demands>(demandsMap.values());
    }

    /**
     * Get demands by id
     *
     * @param id the id of the demands
     * @return the retrieved demands
     */
    public Demands getDemands(final long id) {
      return demandsMap.get(id);
    }

    /**
     * Get the number of demands
     *
     * @return the number of demands in the project
     */
    public int getNumberOfDemands() {
      return demandsMap.size();
    }

    /**
     * Check if demands have already been registered
     *
     * @return true if registered demands exist, false otherwise
     */
    public boolean hasRegisteredDemands() {
      return !demandsMap.isEmpty();
    }

    /**
     * Collect the first demands that are registered (if any). Otherwise return null
     * 
     * @return first demands that are registered if none return null
     */
    public Demands getFirstDemands() {
      return hasRegisteredDemands() ? demandsMap.firstEntry().getValue() : null;
    }
  }

  /**
   * Internal class for registered zonings
   *
   */
  public class ProjectZonings {

    /**
     * Returns a List of zoning
     *
     * @return List of zoning
     */
    public List<Zoning> toList() {
      return new ArrayList<Zoning>(zoningsMap.values());
    }

    /**
     * Get zoning by id
     *
     * @param id the id of the zoning
     * @return the retrieved zoning
     */
    public Zoning getZoning(final long id) {
      return zoningsMap.get(id);
    }

    /**
     * Get the number of zonings
     *
     * @return the number of zonings in the project
     */
    public int getNumberOfZonings() {
      return zoningsMap.size();
    }

    /**
     * Check if zonings have already been registered
     *
     * @return true if registered zonings exist, false otherwise
     */
    public boolean hasRegisteredZonings() {
      return !zoningsMap.isEmpty();
    }

    /**
     * Collect the first zonings that are registered (if any). Otherwise return null
     * 
     * @return first zonings that are registered if none return null
     */
    public Zoning getFirstZoning() {
      return hasRegisteredZonings() ? zoningsMap.firstEntry().getValue() : null;
    }
  }

  /**
   * Internal class for registered od path sets
   *
   */
  public class ProjectODPathSets {

    /**
     * Returns a List of od path sets
     *
     * @return List of od path sets
     */
    public List<ODPathSets> toList() {
      return new ArrayList<ODPathSets>(odPathSetsMap.values());
    }

    /**
     * Get od rotue sets by id
     *
     * @param id the id of the link
     * @return the retrieved link
     */
    public ODPathSets getODPathSets(final long id) {
      return odPathSetsMap.get(id);
    }

    /**
     * Get the number of od path sets
     *
     * @return the number of od path sets in the project
     */
    public int getNumberOfOdPathSets() {
      return odPathSetsMap.size();
    }

    /**
     * Check if od path sets have already been registered
     *
     * @return true if registered od rotue sets exist, false otherwise
     */
    public boolean hasRegisteredOdPathSets() {
      return !odPathSetsMap.isEmpty();
    }

    /**
     * Collect the first od path set that is registered (if any). Otherwise return null
     * 
     * @return first od path set that is registered if none return null
     */
    public ODPathSets getFirstOdPathSets() {
      return hasRegisteredOdPathSets() ? odPathSetsMap.firstEntry().getValue() : null;
    }
  }
  
  /** the token for generating ids uniquely and contiguously within this projects context */
  private final IdGroupingToken projectGroupId;
  
  /** project id reference */
  private final long projectId;

  /**
   * Initialise the input traffic assignment component factories
   * 
   * @param inputBuilderListener the input builder to parse inputs with
   */
  private void initialiseFactories(InputBuilderListener inputBuilderListener) {
    initialPhysicalCostFactory = new TrafficAssignmentComponentFactory<InitialPhysicalCost>(InitialPhysicalCost.class);
    // due to nested generics, we supply class name rather than class
    infrastructureNetworkFactory = new TrafficAssignmentComponentFactory<Network>(Network.class.getCanonicalName());
    zoningFactory = new TrafficAssignmentComponentFactory<Zoning>(Zoning.class);
    demandsFactory = new TrafficAssignmentComponentFactory<Demands>(Demands.class);

    infrastructureNetworkFactory.addListener(inputBuilderListener, TrafficAssignmentComponentFactory.TRAFFICCOMPONENT_CREATE);
    zoningFactory.addListener(inputBuilderListener, TrafficAssignmentComponentFactory.TRAFFICCOMPONENT_CREATE);
    demandsFactory.addListener(inputBuilderListener, TrafficAssignmentComponentFactory.TRAFFICCOMPONENT_CREATE);
    initialPhysicalCostFactory.addListener(inputBuilderListener, TrafficAssignmentComponentFactory.TRAFFICCOMPONENT_CREATE);
  }

  // CONTAINERS

  /**
   * The physical networks registered on this project
   */
  protected final TreeMap<Long, InfrastructureNetwork> infrastructureNetworkMap;

  /**
   * The demands registered on this project
   */
  protected final TreeMap<Long, Demands> demandsMap;

  /**
   * The od path sets registered on this project
   */
  protected final TreeMap<Long, ODPathSets> odPathSetsMap;

  /**
   * The zonings registered on this project
   */
  protected final TreeMap<Long, Zoning> zoningsMap;

  /**
   * Map to store all InitialLinkSegmentCost objects for each physical network
   */
  protected final Map<InfrastructureNetwork, List<InitialLinkSegmentCost>> initialLinkSegmentCosts = new HashMap<InfrastructureNetwork, List<InitialLinkSegmentCost>>();

  // FACTORIES
  /**
   * Object Factory for infrastructure network object
   */
  protected TrafficAssignmentComponentFactory<Network> infrastructureNetworkFactory;

  /**
   * Object factory for demands object
   */
  protected TrafficAssignmentComponentFactory<Demands> demandsFactory;

  /**
   * Object factory for zoning objects
   */
  protected TrafficAssignmentComponentFactory<Zoning> zoningFactory;

  /**
   * Object factory for od path sets object
   */
  protected TrafficAssignmentComponentFactory<ODPathSets> odPathSetsFactory;

  /**
   * Object factory for physical costs
   */
  protected TrafficAssignmentComponentFactory<InitialPhysicalCost> initialPhysicalCostFactory;

  // INNER CLASS INSTANCES

  /**
   * The registered physical networks
   */
  public final ProjectNetworks physicalNetworks = new ProjectNetworks();

  /**
   * The registered demands
   */
  public final ProjectDemands demands = new ProjectDemands();

  /**
   * The registered zonings
   */
  public final ProjectZonings zonings = new ProjectZonings();

  /**
   * The registered OD path sets
   */
  public final ProjectODPathSets odPathSets = new ProjectODPathSets();

  /**
   * Constructor
   * 
   * @param projectId the projectId
   * @param projectGroupId the id generator token
   * @param inputBuilderListener the input builder to parse inputs
   */
  public PlanItProjectInput(long projectId, IdGroupingToken projectGroupId, InputBuilderListener inputBuilderListener) {
    this.projectId = projectId;
    this.infrastructureNetworkMap = new TreeMap<Long, InfrastructureNetwork>();
    this.demandsMap = new TreeMap<Long, Demands>();
    this.zoningsMap = new TreeMap<Long, Zoning>();
    this.odPathSetsMap = new TreeMap<Long, ODPathSets>();

    this.projectGroupId = projectGroupId;

    initialiseFactories(inputBuilderListener);
  }

  /**
   * Create and register an infrastructure based network on the project input
   *
   * @param infrastructureNetworkType name of infrastructure network class to register
   * @return the generated network
   * @throws PlanItException thrown if there is an error
   */
  public InfrastructureNetwork createAndRegisterInfrastructureNetwork(final String infrastructureNetworkType) throws PlanItException {
    LOGGER.info(LoggingUtils.createProjectPrefix(this.projectId)+"populating network");
    final Network theNetwork = infrastructureNetworkFactory.create(infrastructureNetworkType, new Object[] { projectGroupId });
    
    /* for now we only support infrastructure based networks even though class heirarchy is more generic */
    if(!(theNetwork instanceof InfrastructureNetwork)){
      throw new PlanItException("we currently only support networks derived from InfrastructureNetwork");
    }
    InfrastructureNetwork infrastructureNetwork = (InfrastructureNetwork) theNetwork;

    /* log info across layers */
    String prefix = LoggingUtils.createProjectPrefix(this.projectId)+LoggingUtils.createNetworkPrefix(infrastructureNetwork.getId());
    LOGGER.info(String.format("%s#modes: %d", prefix, infrastructureNetwork.modes.size()));    
    
    /* for each layer log information regarding contents */
    for(InfrastructureLayer networkLayer : infrastructureNetwork.infrastructureLayers) {
      networkLayer.logInfo(prefix);
    }
    
    infrastructureNetworkMap.put(infrastructureNetwork.getId(), infrastructureNetwork);
    return infrastructureNetwork;
  }

  /**
   * Create and register the zoning system on the network and project input
   *
   * @param infrastructureNetwork the infrastructure network on which the zoning will be based
   * @return the generated zoning object
   * @throws PlanItException thrown if there is an error
   */
  public Zoning createAndRegisterZoning(final InfrastructureNetwork infrastructureNetwork) throws PlanItException {
    PlanItException.throwIf(infrastructureNetwork == null, "The physical network must be defined before definition of zones can begin");

    LOGGER.info(LoggingUtils.createProjectPrefix(this.projectId)+"populating zoning");
    final Zoning zoning = 
        zoningFactory.create(
            Zoning.class.getCanonicalName(), 
            new Object[] { projectGroupId, infrastructureNetwork.getNetworkGroupingTokenId() }, 
            infrastructureNetwork);
    
    String prefix = LoggingUtils.createProjectPrefix(this.projectId)+LoggingUtils.createZoningPrefix(zoning.getId());
    LOGGER.info(String.format("%s#zones: %d", prefix, zoning.zones.getNumberOfZones()));
    LOGGER.info(String.format("%s#centroids: %d", prefix, zoning.getVirtualNetwork().centroids.getNumberOfCentroids()));
    LOGGER.info(String.format("%s#connectoids: %d", prefix, zoning.getVirtualNetwork().connectoids.getNumberOfConnectoids()));
    LOGGER.info(String.format("%s#connectoid segments: %d", prefix, zoning.getVirtualNetwork().connectoidSegments.getNumberOfConnectoidSegments()));

    zoningsMap.put(zoning.getId(), zoning);
    return zoning;
  }

  /**
   * Create and register demands to the project inputs
   *
   * @param zoning      zoning object which defines the zones which will be used in the demand matrix to be created
   * @param network     the network which stores the modes (demands can different for each mode)
   * @return            the generated demands object
   * @throws PlanItException thrown if there is an error
   */
  public Demands createAndRegisterDemands(final Zoning zoning, final InfrastructureNetwork network) throws PlanItException {
    PlanItException.throwIf(zoning == null, "Zones must be defined before definition of demands can begin");
    PlanItException.throwIf(network == null, "network must be defined before definition of demands can begin");

    LOGGER.info(LoggingUtils.createProjectPrefix(this.projectId)+"populating demands");
    final Demands demands = 
        demandsFactory.create(Demands.class.getCanonicalName(), new Object[] { projectGroupId }, zoning, network);  
    
    String prefix = LoggingUtils.createProjectPrefix(this.projectId)+LoggingUtils.createDemandsPrefix(demands.getId());
    LOGGER.info(String.format("%s#time periods: %d", prefix, demands.timePeriods.getNumberOfTimePeriods()));
    LOGGER.info(String.format("%s#traveler types: %d", prefix, demands.travelerTypes.getNumberOfTravelerTypes()));    
    LOGGER.info(String.format("%s#user classes: %d", prefix, demands.userClasses.getNumberOfUserClasses()));

    demandsMap.put(demands.getId(), demands);
    return demands;
  }

  /**
   * Create and register the OD path sets on the project input
   * 
   * @param networkLayer     network the paths must be compatible with
   * @param zoning              zoning to match od paths to
   * @param odPathSetInputPath path to collect the paths from
   * @return od path sets that have been parsed
   * @throws PlanItException thrown if there is an error
   */
  public ODPathSets createAndRegisterOdPathSets(final InfrastructureLayer networkLayer, final Zoning zoning, final String odPathSetInputPath) throws PlanItException {
    PlanItException.throwIf(zoning == null, "Zones must be defined before definition of od path sets can proceed");
    PlanItException.throwIf(networkLayer == null, "Physical network must be defined before of od path sets can proceed");

    LOGGER.info(LoggingUtils.createProjectPrefix(this.projectId)+"populating od path sets");
    final ODPathSets odPathSets = 
        odPathSetsFactory.create(
            ODPathSets.class.getCanonicalName(), 
            new Object[] { projectGroupId }, 
            odPathSetInputPath);
    
    String prefix = LoggingUtils.createProjectPrefix(this.projectId)+LoggingUtils.createOdPathSetsPrefix(odPathSets.getId());
    LOGGER.info(String.format("%s#od path sets: %d", prefix, odPathSets.getNumberOfOdPathSets()));

    odPathSetsMap.put(odPathSets.getId(), odPathSets);
    return odPathSets;
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
    PlanItException.throwIf(network == null, "Physical network must be read in before initial costs can be read");

    if (!initialLinkSegmentCosts.containsKey(network)) {
      initialLinkSegmentCosts.put(network, new ArrayList<InitialLinkSegmentCost>());
    }

    LOGGER.info(LoggingUtils.createProjectPrefix(this.projectId)+"populating initial link segment costs");    
    final InitialLinkSegmentCost initialLinkSegmentCost = 
        (InitialLinkSegmentCost) initialPhysicalCostFactory.create(InitialLinkSegmentCost.class.getCanonicalName(), new Object[] { projectGroupId }, network, fileName);

    initialLinkSegmentCosts.get(network).add(initialLinkSegmentCost);
    return initialLinkSegmentCost;
  }

  /**
   * Create and register initial link segment costs from a (single) file for each time period
   *
   * @param network    network the InitialLinkSegmentCost object will be registered for
   * @param fileName   location of file containing the initial link segment cost values
   * @param timePeriod the current time period
   * @return the InitialLinkSegmentCost object
   * @throws PlanItException thrown if there is an error
   */
  public InitialLinkSegmentCostPeriod createAndRegisterInitialLinkSegmentCost(final InfrastructureNetwork network, final String fileName, final TimePeriod timePeriod)
      throws PlanItException {
    PlanItException.throwIf(network == null, "Physical network must be read in before initial costs can be read");

    if (!initialLinkSegmentCosts.containsKey(network)) {
      initialLinkSegmentCosts.put(network, new ArrayList<InitialLinkSegmentCost>());
    }

    LOGGER.info(
        LoggingUtils.createProjectPrefix(this.projectId)+
        LoggingUtils.createTimePeriodPrefix(timePeriod.getExternalId(), timePeriod.getId())+
        "populating initial link segment costs");
    
    final InitialPhysicalCost initialLinkSegmentCostPeriod = 
        (InitialLinkSegmentCostPeriod) initialPhysicalCostFactory.create(
            InitialLinkSegmentCostPeriod.class.getCanonicalName(),
            new Object[] { projectGroupId }, 
            network, 
            fileName, 
            timePeriod);

    // explicitly register time period on the instance, since it is more specific than the regular initial cost without this information
    ((InitialLinkSegmentCostPeriod) initialLinkSegmentCostPeriod).setTimePeriod(timePeriod);

    initialLinkSegmentCosts.get(network).add((InitialLinkSegmentCost) initialLinkSegmentCostPeriod);
    return (InitialLinkSegmentCostPeriod) initialLinkSegmentCostPeriod;
  }

  /**
   * Create and register initial link segment costs from a (single) file for all time periods in Demands object
   *
   * @param network  network the InitialLinkSegmentCost object will be registered for
   * @param fileName location of file containing the initial link segment cost values
   * @param demands  the Demands object
   * @return the InitialLinkSegmentCost object
   * @throws PlanItException thrown if there is an error
   */
  public List<InitialLinkSegmentCostPeriod> createAndRegisterInitialLinkSegmentCost(final InfrastructureNetwork network, final String fileName, final Demands demands)
      throws PlanItException {

    PlanItException.throwIf(network == null, "Physical network must be read in before initial costs can be read");

    final List<InitialLinkSegmentCostPeriod> initialCostsList = new ArrayList<InitialLinkSegmentCostPeriod>();
    for (final TimePeriod timePeriod : demands.timePeriods.asSortedSetByStartTime()) {
      final InitialLinkSegmentCostPeriod initialLinkSegmentCostPeriod = createAndRegisterInitialLinkSegmentCost(network, fileName, timePeriod);
      initialCostsList.add(initialLinkSegmentCostPeriod);
    }
    return initialCostsList;
  }

  /**
   * Return the initial link segment costs for a network
   *
   * @param network the specified network
   * @return the initial link segment costs for the specified physical network
   */
  public List<InitialLinkSegmentCost> getInitialLinkSegmentCost(final InfrastructureNetwork network) {
    return initialLinkSegmentCosts.get(network);
  }
}
//@formatter:on
