package org.planit.project;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.planit.cost.physical.initial.InitialLinkSegmentCost;
import org.planit.cost.physical.initial.InitialPhysicalCost;
import org.planit.demands.Demands;
import org.planit.exceptions.PlanItException;
import org.planit.input.InputBuilderListener;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.network.virtual.Zoning;
import org.planit.route.ODRouteSets;
import org.planit.time.TimePeriod;
import org.planit.trafficassignment.TrafficAssignmentComponentFactory;

/**
 * Class that holds all the input traffic components for a PLANit project. The
 * PLANit project holds an instance of this class and delegates all calls
 * relating to inputs to this class.
 * 
 * @author markr
 *
 */
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
     * Returns a List of networks
     *
     * @return List of networks
     */
    public List<PhysicalNetwork> toList() {
      return new ArrayList<PhysicalNetwork>(physicalNetworkMap.values());
    }

    /**
     * Get physical network by id
     *
     * @param id the id of the network
     * @return the retrieved network
     */
    public PhysicalNetwork getPhysicalNetwork(final long id) {
      return physicalNetworkMap.get(id);
    }

    /**
     * Get the number of networks
     *
     * @return the number of networks in the project
     */
    public int getNumberOfPhysicalNetworks() {
      return physicalNetworkMap.size();
    }

    /**
     * Check if networks have already been registered
     *
     * @return true if registered networks exist, false otherwise
     */
    public boolean hasRegisteredNetworks() {
      return !physicalNetworkMap.isEmpty();
    }

    /**
     * Collect the first network that is registered (if any). Otherwise return null
     * 
     * @return first network that is registered if none return null
     */
    public PhysicalNetwork getFirstNetwork() {
      return hasRegisteredNetworks() ? physicalNetworkMap.firstEntry().getValue() : null;
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
   * Internal class for registered od route sets
   *
   */
  public class ProjectODRouteSets {

    /**
     * Returns a List of od route sets
     *
     * @return List of od route sets
     */
    public List<ODRouteSets> toList() {
      return new ArrayList<ODRouteSets>(odRouteSetsMap.values());
    }

    /**
     * Get od rotue sets by id
     *
     * @param id the id of the link
     * @return the retrieved link
     */
    public ODRouteSets getODRouteSets(final long id) {
      return odRouteSetsMap.get(id);
    }

    /**
     * Get the number of od route sets
     *
     * @return the number of od route sets in the project
     */
    public int getNumberOfODRouteSets() {
      return odRouteSetsMap.size();
    }

    /**
     * Check if od route sets have already been registered
     *
     * @return true if registered od rotue sets exist, false otherwise
     */
    public boolean hasRegisteredODRouteSets() {
      return !odRouteSetsMap.isEmpty();
    }

    /**
     * Collect the first od route set that is registered (if any). Otherwise return
     * null
     * 
     * @return first od route set that is registered if none return null
     */
    public ODRouteSets getFirstODRouteSets() {
      return hasRegisteredODRouteSets() ? odRouteSetsMap.firstEntry().getValue() : null;
    }
  }

  /**
   * Initialise the input traffic assignment component factories
   * 
   * @param inputBuilderListener
   */
  private void initialiseFactories(InputBuilderListener inputBuilderListener) {
    initialPhysicalCostFactory = new TrafficAssignmentComponentFactory<InitialPhysicalCost>(InitialPhysicalCost.class);
    physicalNetworkFactory = new TrafficAssignmentComponentFactory<PhysicalNetwork>(PhysicalNetwork.class);
    zoningFactory = new TrafficAssignmentComponentFactory<Zoning>(Zoning.class);
    demandsFactory = new TrafficAssignmentComponentFactory<Demands>(Demands.class);

    physicalNetworkFactory.addListener(inputBuilderListener, TrafficAssignmentComponentFactory.TRAFFICCOMPONENT_CREATE);
    zoningFactory.addListener(inputBuilderListener, TrafficAssignmentComponentFactory.TRAFFICCOMPONENT_CREATE);
    demandsFactory.addListener(inputBuilderListener, TrafficAssignmentComponentFactory.TRAFFICCOMPONENT_CREATE);
    initialPhysicalCostFactory.addListener(inputBuilderListener, TrafficAssignmentComponentFactory.TRAFFICCOMPONENT_CREATE);
  }

  // CONTAINERS

  /**
   * The physical networks registered on this project
   */
  protected final TreeMap<Long, PhysicalNetwork> physicalNetworkMap;

  /**
   * The demands registered on this project
   */
  protected final TreeMap<Long, Demands> demandsMap;

  /**
   * The od route sets registered on this project
   */
  protected final TreeMap<Long, ODRouteSets> odRouteSetsMap;

  /**
   * The zonings registered on this project
   */
  protected final TreeMap<Long, Zoning> zoningsMap;

  /**
   * Map to store all InitialLinkSegmentCost objects for each physical network
   */
  protected final Map<PhysicalNetwork, List<InitialLinkSegmentCost>> initialLinkSegmentCosts = new HashMap<PhysicalNetwork, List<InitialLinkSegmentCost>>();

  // FACTORIES
  /**
   * Object Factory for physical network object
   */
  protected TrafficAssignmentComponentFactory<PhysicalNetwork> physicalNetworkFactory;

  /**
   * Object factory for demands object
   */
  protected TrafficAssignmentComponentFactory<Demands> demandsFactory;

  /**
   * Object factory for zoning objects
   */
  protected TrafficAssignmentComponentFactory<Zoning> zoningFactory;

  /**
   * Object factory for od route sets object
   */
  protected TrafficAssignmentComponentFactory<ODRouteSets> odRouteSetsFactory;

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
   * The registered OD route sets
   */
  public final ProjectODRouteSets odRouteSets = new ProjectODRouteSets();

  /**
   * Constructor
   * 
   * @param inputBuilderListener
   */
  public PlanItProjectInput(InputBuilderListener inputBuilderListener) {
    physicalNetworkMap = new TreeMap<Long, PhysicalNetwork>();
    demandsMap = new TreeMap<Long, Demands>();
    zoningsMap = new TreeMap<Long, Zoning>();
    odRouteSetsMap = new TreeMap<Long, ODRouteSets>();

    initialiseFactories(inputBuilderListener);
  }

  /**
   * Create and register a physical network on the project input
   *
   * @param physicalNetworkType name of physical network class to register
   * @return the generated physical network
   * @throws PlanItException thrown if there is an error
   */
  public PhysicalNetwork createAndRegisterPhysicalNetwork(final String physicalNetworkType) throws PlanItException {
    final PhysicalNetwork physicalNetwork = physicalNetworkFactory.create(physicalNetworkType);
    physicalNetworkMap.put(physicalNetwork.getId(), physicalNetwork);
    return physicalNetwork;
  }

  /**
   * Create and register the zoning system on the network and project input
   *
   * @param physicalNetwork the physical network on which the zoning will be based
   * @return the generated zoning object
   * @throws PlanItException thrown if there is an error
   */
  public Zoning createAndRegisterZoning(final PhysicalNetwork physicalNetwork) throws PlanItException {
    if (physicalNetwork == null) {
      String errorMessage = "The physical network must be defined before definition of zones can begin";
      LOGGER.severe(errorMessage);
      throw new PlanItException(errorMessage);
    }
    final Zoning zoning = zoningFactory.create(Zoning.class.getCanonicalName(), new Object[] { physicalNetwork });
    zoningsMap.put(zoning.getId(), zoning);
    return zoning;
  }

  /**
   * Create and register demands to the project inputs
   *
   * @param zoning          Zoning object which defines the zones which will be
   *                        used in the demand matrix to be created
   * @param physicalNetwork the physical network which stores the modes (demands
   *                        can different for each mode)
   * @return the generated demands object
   * @throws PlanItException thrown if there is an error
   */
  public Demands createAndRegisterDemands(final Zoning zoning, final PhysicalNetwork physicalNetwork) throws PlanItException {
    if (zoning == null) {
      String errorMessage = "Zones must be defined before definition of demands can begin";
      LOGGER.severe(errorMessage);
      throw new PlanItException(errorMessage);
    }
    if (physicalNetwork == null) {
      String errorMessage = "Physical network must be defined before definition of demands can begin";
      LOGGER.severe(errorMessage);
      throw new PlanItException(errorMessage);
    }
    final Demands demands = demandsFactory.create(Demands.class.getCanonicalName(), new Object[] { zoning, physicalNetwork });
    demandsMap.put(demands.getId(), demands);
    return demands;
  }

  /**
   * Create and register the OD route sets on the project input
   * 
   * @param physicalNetwork     network the routes must be compatible with
   * @param zoning              zoning to match od routes to
   * @param odRouteSetInputPath path to collect the routes from
   * @return od route sets that have been parsed
   * @throws PlanItException
   */
  public ODRouteSets createAndRegisterODRouteSets(final PhysicalNetwork physicalNetwork, final Zoning zoning, final String odRouteSetInputPath) throws PlanItException {
    if (zoning == null || physicalNetwork == null) {
      throw new PlanItException("Zones and network must be registered before definition of od route sets can proceed");
    }
    final ODRouteSets odRouteSets = odRouteSetsFactory.create(ODRouteSets.class.getCanonicalName(), new Object[] { odRouteSetInputPath });
    odRouteSetsMap.put(odRouteSets.getId(), odRouteSets);
    return odRouteSets;
  }

  /**
   * Create and register initial link segment costs from a (single) file which we
   * assume are available in the native xml/csv output format as provided in this
   * project
   *
   * @param network  physical network the InitialLinkSegmentCost object will be
   *                 registered for
   * @param fileName file containing the initial link segment cost values
   * @return the InitialLinkSegmentCost object
   * @throws PlanItException thrown if there is an error
   */
  public InitialLinkSegmentCost createAndRegisterInitialLinkSegmentCost(final PhysicalNetwork network, final String fileName) throws PlanItException {
    if (network == null) {
      String errorMessage = "Physical network must be read in before initial costs can be read.";
      LOGGER.severe(errorMessage);
      throw new PlanItException(errorMessage);
    }
    if (!initialLinkSegmentCosts.containsKey(network)) {
      initialLinkSegmentCosts.put(network, new ArrayList<InitialLinkSegmentCost>());
    }
    final InitialLinkSegmentCost initialLinkSegmentCost = (InitialLinkSegmentCost) initialPhysicalCostFactory.create(InitialLinkSegmentCost.class.getCanonicalName(),
        new Object[] { network, fileName });
    initialLinkSegmentCosts.get(network).add(initialLinkSegmentCost);
    return initialLinkSegmentCost;
  }

  /**
   * Create and register initial link segment costs from a (single) file for each
   * time period
   *
   * @param network    physical network the InitialLinkSegmentCost object will be
   *                   registered for
   * @param fileName   location of file containing the initial link segment cost
   *                   values
   * @param timePeriod the current time period
   * @return the InitialLinkSegmentCost object
   * @throws PlanItException thrown if there is an error
   */
  public InitialLinkSegmentCost createAndRegisterInitialLinkSegmentCost(final PhysicalNetwork network, final String fileName, final TimePeriod timePeriod) throws PlanItException {
    if (network == null) {
      String errorMessage = "Physical network must be read in before initial costs can be read.";
      LOGGER.severe(errorMessage);
      throw new PlanItException(errorMessage);
    }
    if (!initialLinkSegmentCosts.containsKey(network)) {
      initialLinkSegmentCosts.put(network, new ArrayList<InitialLinkSegmentCost>());
    }
    final InitialLinkSegmentCost initialLinkSegmentCost = (InitialLinkSegmentCost) initialPhysicalCostFactory.create(InitialLinkSegmentCost.class.getCanonicalName(),
        new Object[] { network, fileName, timePeriod });
    initialLinkSegmentCosts.get(network).add(initialLinkSegmentCost);
    return initialLinkSegmentCost;
  }

  /**
   * Create and register initial link segment costs from a (single) file for all
   * time periods in Demands object
   *
   * @param network  physical network the InitialLinkSegmentCost object will be
   *                 registered for
   * @param fileName location of file containing the initial link segment cost
   *                 values
   * @param demands  the Demands object
   * @return the InitialLinkSegmentCost object
   * @throws PlanItException thrown if there is an error
   */
  public Map<TimePeriod, InitialLinkSegmentCost> createAndRegisterInitialLinkSegmentCost(final PhysicalNetwork network, final String fileName, final Demands demands)
      throws PlanItException {
    if (network == null) {
      String errorMessage = "Physical network must be read in before initial costs can be read.";
      LOGGER.severe(errorMessage);
      throw new PlanItException(errorMessage);
    }
    final Map<TimePeriod, InitialLinkSegmentCost> initialCostsMap = new HashMap<TimePeriod, InitialLinkSegmentCost>();
    for (final TimePeriod timePeriod : demands.timePeriods.getRegisteredTimePeriods()) {
      LOGGER.info("Registering Initial Link Segment Costs for Time Period " + timePeriod.getId());
      final InitialLinkSegmentCost initialLinkSegmentCost = createAndRegisterInitialLinkSegmentCost(network, fileName, timePeriod);
      initialCostsMap.put(timePeriod, initialLinkSegmentCost);
    }
    return initialCostsMap;
  }

  /**
   * Return the initial link segment costs for a network
   *
   * @param network the specified physical network
   * @return the initial link segment costs for the specified physical network
   */
  public List<InitialLinkSegmentCost> getInitialLinkSegmentCost(final PhysicalNetwork network) {
    return initialLinkSegmentCosts.get(network);
  }

}
