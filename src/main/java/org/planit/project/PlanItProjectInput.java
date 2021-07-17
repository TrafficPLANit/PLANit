package org.planit.project;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.planit.component.PlanitComponent;
import org.planit.component.PlanitComponentFactory;
import org.planit.cost.physical.initial.InitialLinkSegmentCost;
import org.planit.cost.physical.initial.InitialLinkSegmentCostPeriod;
import org.planit.cost.physical.initial.InitialPhysicalCost;
import org.planit.demands.Demands;
import org.planit.input.InputBuilderListener;
import org.planit.network.MacroscopicNetwork;
import org.planit.network.Network;
import org.planit.network.ServiceNetwork;
import org.planit.network.TransportLayerNetwork;
import org.planit.path.OdPathSets;
import org.planit.service.routed.RoutedServices;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.misc.LoggingUtils;
import org.planit.utils.network.layer.TransportLayer;
import org.planit.utils.time.TimePeriod;
import org.planit.zoning.Zoning;

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
    planitComponentFactories.clear();
    
    planitComponentFactories.add(new PlanitComponentFactory<InitialPhysicalCost>(InitialPhysicalCost.class));

    // due to nested generics, we supply class name rather than class
    planitComponentFactories.add(new PlanitComponentFactory<Network>(Network.class.getCanonicalName()));
    
    planitComponentFactories.add(new PlanitComponentFactory<Zoning>(Zoning.class));
    
    planitComponentFactories.add(new PlanitComponentFactory<Demands>(Demands.class));       
    
    planitComponentFactories.add(new PlanitComponentFactory<RoutedServices>(RoutedServices.class));    
    
    /* register input builder as listener whenever an instance is created */
    planitComponentFactories.forEach( (factory) -> factory.addListener(inputBuilderListener));
  }

  // CONTAINERS

  /** Collect component factory cast to right instance
   * 
   * @param <T> type of component factory
   * @param clazz to abse type of component factory to collect on
   * @return component factory for clazz instances
   * @throws PlanItException  when not available
   */
  @SuppressWarnings("unchecked")
  private <T extends PlanitComponent<?>> PlanitComponentFactory<T> getComponentFactory(Class<T> clazz) throws PlanItException {
    return (PlanitComponentFactory<T>) planitComponentFactories.stream().filter(
        factory -> factory.isFactoryForDerivedClassesOf(clazz)).findFirst().orElseThrow(() -> new PlanItException("component factory unavailable for %s", clazz.getCanonicalName()));
  }

  /**
   * Map to store all InitialLinkSegmentCost objects for each physical network
   */
  protected final Map<TransportLayerNetwork<?,?>, List<InitialLinkSegmentCost>> initialLinkSegmentCosts = new HashMap<TransportLayerNetwork<?,?>, List<InitialLinkSegmentCost>>();

  // FACTORIES
  
  /** available traffic assignment component factories by their derived class implementation */
  protected final Collection<PlanitComponentFactory<?>> planitComponentFactories = new ArrayList<PlanitComponentFactory<?>>();
  
  // INNER CLASS INSTANCES

  /**
   * The registered physical networks
   */
  protected final ProjectNetworks physicalNetworks = new ProjectNetworks();

  /**
   * The registered demands
   */
  protected final ProjectDemands demands = new ProjectDemands();

  /**
   * The registered zonings
   */
  protected final ProjectZonings zonings = new ProjectZonings();
  
  /**
   * The registered service networks
   */
  protected final ProjectServiceNetworks serviceNetworks = new ProjectServiceNetworks();  
  
  /**
   * The registered routed services
   */
  protected final ProjectRoutedServices routedServices = new ProjectRoutedServices();  

  /**
   * The registered OD path sets
   */
  protected final ProjectOdPathSets odPathSets = new ProjectOdPathSets();

  /**
   * Constructor
   * 
   * @param projectId the projectId
   * @param projectGroupId the id generator token
   * @param inputBuilderListener the input builder to parse inputs
   */
  public PlanItProjectInput(long projectId, IdGroupingToken projectGroupId, InputBuilderListener inputBuilderListener) {
    this.projectId = projectId;
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
  public TransportLayerNetwork<?,?> createAndRegisterInfrastructureNetwork(final String infrastructureNetworkType) throws PlanItException {
    LOGGER.info(LoggingUtils.createProjectPrefix(this.projectId)+"populating network");
    final Network theNetwork = getComponentFactory(Network.class).create(infrastructureNetworkType, new Object[] { projectGroupId });
    
    /* for now we only support infrastructure based networks even though class heirarchy is more generic */
    if(!(theNetwork instanceof TransportLayerNetwork)){
      throw new PlanItException("we currently only support networks derived from InfrastructureNetwork");
    }
    TransportLayerNetwork<?,?> infrastructureNetwork = (TransportLayerNetwork<?,?>) theNetwork;

    /* log info across layers */
    String prefix = LoggingUtils.createProjectPrefix(this.projectId)+LoggingUtils.createNetworkPrefix(infrastructureNetwork.getId());
    LOGGER.info(String.format("%s#modes: %d", prefix, infrastructureNetwork.getModes().size()));    
    
    /* for each layer log information regarding contents */
    for(TransportLayer networkLayer : infrastructureNetwork.getTransportLayers()) {
      networkLayer.logInfo(prefix);
    }
    
    physicalNetworks.register(infrastructureNetwork);
    return infrastructureNetwork;
  }

  /**
   * Create and register the zoning system on the network and project input
   *
   * @param infrastructureNetwork the infrastructure network on which the zoning will be based
   * @return the generated zoning object
   * @throws PlanItException thrown if there is an error
   */
  public Zoning createAndRegisterZoning(final TransportLayerNetwork<?,?> infrastructureNetwork) throws PlanItException {
    PlanItException.throwIf(infrastructureNetwork == null, "The physical network must be defined before definition of zones can begin");

    LOGGER.info(LoggingUtils.createProjectPrefix(this.projectId)+"populating zoning");
    final Zoning zoning = 
        getComponentFactory(Zoning.class).create(
            Zoning.class.getCanonicalName(), 
            new Object[] { projectGroupId, infrastructureNetwork.getNetworkGroupingTokenId() }, 
            infrastructureNetwork);
    
    String prefix = LoggingUtils.createProjectPrefix(this.projectId)+LoggingUtils.createZoningPrefix(zoning.getId());
    zoning.logInfo(prefix);

    zonings.register(zoning);
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
  public Demands createAndRegisterDemands(final Zoning zoning, final TransportLayerNetwork<?,?> network) throws PlanItException {
    PlanItException.throwIf(zoning == null, "Zones must be defined before definition of demands can begin");
    PlanItException.throwIf(network == null, "network must be defined before definition of demands can begin");

    LOGGER.info(LoggingUtils.createProjectPrefix(this.projectId)+"populating demands");
    final Demands demands = 
        getComponentFactory(Demands.class).create(
            Demands.class.getCanonicalName(), new Object[] { projectGroupId }, zoning, network);  
    
    String prefix = LoggingUtils.createProjectPrefix(this.projectId)+LoggingUtils.createDemandsPrefix(demands.getId());
    LOGGER.info(String.format("%s#time periods: %d", prefix, demands.timePeriods.size()));
    LOGGER.info(String.format("%s#traveler types: %d", prefix, demands.travelerTypes.size()));    
    LOGGER.info(String.format("%s#user classes: %d", prefix, demands.userClasses.size()));

    this.demands.register(demands);
    return demands;
  }
  
  /**
   * Create and register service networks to the project inputs
   *
   * @param network     the network upon which the service network is built
   * @return            the generated service network object
   * @throws PlanItException thrown if there is an error
   */
  public ServiceNetwork createAndRegisterServiceNetwork(final MacroscopicNetwork network) throws PlanItException {
    PlanItException.throwIf(network == null, "Physical network must be defined before definition of service network can begin");

    LOGGER.info(LoggingUtils.createProjectPrefix(this.projectId)+"populating service network");
    final Network theNetwork = 
        getComponentFactory(Network.class).create(
            ServiceNetwork.class.getCanonicalName(), new Object[] { projectGroupId, network });
        
    /* for now we only support infrastructure based networks even though class heirarchy is more generic */
    if(!(theNetwork instanceof ServiceNetwork)){
      throw new PlanItException("we currently only service network derived classes when creating service networks");
    }
    ServiceNetwork serviceNetwork = (ServiceNetwork) theNetwork;    
    
    String prefix = LoggingUtils.createProjectPrefix(this.projectId)+LoggingUtils.createServiceNetworkPrefix(serviceNetwork.getId());
    /* log info across layers */
    LOGGER.info(String.format("%s#modes: %d", prefix, serviceNetwork.getModes().size()));    
    
    /* for each layer log information regarding contents */
    for(TransportLayer networkLayer : serviceNetwork.getTransportLayers()) {
      networkLayer.logInfo(prefix);
    }
    
    this.serviceNetworks.register(serviceNetwork);
    return serviceNetwork;
  }   
  
  /**
   * Create and register routed services to the project inputs
   *
   * @param serviceNetwork     the service network upon which the service are defined
   * @return            the generated routed services object
   * @throws PlanItException thrown if there is an error
   */
  public RoutedServices createAndRegisterRoutedServices(final ServiceNetwork serviceNetwork) throws PlanItException {
    PlanItException.throwIf(serviceNetwork == null, "serviceNetwork must be defined before definition of routed services can begin");

    LOGGER.info(LoggingUtils.createProjectPrefix(this.projectId)+"populating routed services");
    final RoutedServices routedServices = 
        getComponentFactory(RoutedServices.class).create(
            RoutedServices.class.getCanonicalName(), new Object[] { projectGroupId, serviceNetwork});  
    
    String prefix = LoggingUtils.createProjectPrefix(this.projectId)+LoggingUtils.createRoutedServicesPrefix(routedServices.getId());
    //TODO: add aggregate logging stats

    this.routedServices.register(routedServices);
    return routedServices;
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
  public OdPathSets createAndRegisterOdPathSets(final TransportLayer networkLayer, final Zoning zoning, final String odPathSetInputPath) throws PlanItException {
    PlanItException.throwIf(zoning == null, "Zones must be defined before definition of od path sets can proceed");
    PlanItException.throwIf(networkLayer == null, "Physical network must be defined before of od path sets can proceed");

    LOGGER.info(LoggingUtils.createProjectPrefix(this.projectId)+"populating od path sets");
    final OdPathSets odPathSets = 
        getComponentFactory(OdPathSets.class).create(
            OdPathSets.class.getCanonicalName(), new Object[] { projectGroupId }, odPathSetInputPath);
    
    String prefix = LoggingUtils.createProjectPrefix(this.projectId)+LoggingUtils.createOdPathSetsPrefix(odPathSets.getId());
    LOGGER.info(String.format("%s#od path sets: %d", prefix, odPathSets.getNumberOfOdPathSets()));

    this.odPathSets.register(odPathSets);
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
  public InitialLinkSegmentCost createAndRegisterInitialLinkSegmentCost(final TransportLayerNetwork<?,?> network, final String fileName) throws PlanItException {
    PlanItException.throwIf(network == null, "Physical network must be read in before initial costs can be read");

    if (!initialLinkSegmentCosts.containsKey(network)) {
      initialLinkSegmentCosts.put(network, new ArrayList<InitialLinkSegmentCost>());
    }

    LOGGER.info(LoggingUtils.createProjectPrefix(this.projectId)+"populating initial link segment costs");    
    final InitialLinkSegmentCost initialLinkSegmentCost = 
        (InitialLinkSegmentCost) getComponentFactory(InitialPhysicalCost.class).create(
            InitialLinkSegmentCost.class.getCanonicalName(), new Object[] { projectGroupId }, network, fileName);

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
  public InitialLinkSegmentCostPeriod createAndRegisterInitialLinkSegmentCost(final TransportLayerNetwork<?,?> network, final String fileName, final TimePeriod timePeriod)
      throws PlanItException {
    PlanItException.throwIf(network == null, "Physical network must be read in before initial costs can be read");

    if (!initialLinkSegmentCosts.containsKey(network)) {
      initialLinkSegmentCosts.put(network, new ArrayList<InitialLinkSegmentCost>());
    }

    LOGGER.info(
        LoggingUtils.createProjectPrefix(this.projectId)+
        LoggingUtils.createTimePeriodPrefix(timePeriod)+"populating initial link segment costs");
    
    final InitialPhysicalCost initialLinkSegmentCostPeriod = 
        (InitialLinkSegmentCostPeriod) getComponentFactory(InitialPhysicalCost.class).create(
            InitialLinkSegmentCostPeriod.class.getCanonicalName(), new Object[] { projectGroupId }, network, fileName, timePeriod);

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
  public List<InitialLinkSegmentCostPeriod> createAndRegisterInitialLinkSegmentCost(final TransportLayerNetwork<?,?> network, final String fileName, final Demands demands)
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
  public List<InitialLinkSegmentCost> getInitialLinkSegmentCost(final TransportLayerNetwork<?,?> network) {
    return initialLinkSegmentCosts.get(network);
  }
}
//@formatter:on
