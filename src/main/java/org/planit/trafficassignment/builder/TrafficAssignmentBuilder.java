package org.planit.trafficassignment.builder;

import java.util.logging.Logger;

import org.planit.assignment.TrafficAssignment;
import org.planit.assignment.TrafficAssignmentComponentFactory;
import org.planit.assignment.TrafficComponentBuilder;
import org.planit.cost.physical.PhysicalCost;
import org.planit.cost.virtual.VirtualCost;
import org.planit.demands.Demands;
import org.planit.gap.GapFunction;
import org.planit.gap.LinkBasedRelativeDualityGapFunction;
import org.planit.gap.StopCriterion;
import org.planit.input.InputBuilderListener;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.network.virtual.Zoning;
import org.planit.output.enums.OutputType;
import org.planit.sdinteraction.smoothing.Smoothing;
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
    getConfigurator().registerDelayedMethodCall("setPhysicalNetwork", network);
    getConfigurator().registerDelayedMethodCall("setZoning", zoning);
    getConfigurator().registerDelayedMethodCall("setDemands", demands);
  }
     
  /**
   * Factory method to create the instance of the desired type
   * @return instance of traffic assignment 
   * @throws PlanItException thrown when error
   */
  @SuppressWarnings("unchecked")
  protected T createTrafficAssignmentInstance() throws PlanItException {
    String trafficAssignmentClassName = getClassToBuild().getClass().getCanonicalName();
    TrafficAssignmentComponentFactory<TrafficAssignment> assignmentFactory = new TrafficAssignmentComponentFactory<TrafficAssignment>(trafficAssignmentClassName);
    final NetworkLoading networkLoadingAndAssignment = assignmentFactory.create(trafficAssignmentClassName, new Object[] { groupId });
    PlanItException.throwIf(!(networkLoadingAndAssignment instanceof TrafficAssignment), "not a valid traffic assignment type");
    return (T) networkLoadingAndAssignment;
  }
  
  /** create a smoothing instance based on configuration
   * 
   * @return smoothing instance
   * @throws PlanItException thrown if error
   */
  protected Smoothing createSmoothingInstance(TrafficAssignmentConfigurator<?> configurator) throws PlanItException{
    TrafficAssignmentComponentFactory<Smoothing> smoothingFactory = new TrafficAssignmentComponentFactory<Smoothing>(Smoothing.class);
    smoothingFactory.addListener(getInputBuilderListener(), TrafficAssignmentComponentFactory.TRAFFICCOMPONENT_CREATE);
    return smoothingFactory.create(configurator.getSmoothing().getClassTypeToConfigure().getCanonicalName(), new Object[] { getGroupIdToken() });
  }
  
  /** create a physical cost instance based on configuration
   * 
   * @return physical cost instance
   * @throws PlanItException thrown if error
   */  
  protected PhysicalCost createPhysicalCostInstance(TrafficAssignmentConfigurator<?> configurator) throws PlanItException {
    TrafficAssignmentComponentFactory<PhysicalCost> physicalCostFactory = new TrafficAssignmentComponentFactory<PhysicalCost>(PhysicalCost.class);
    physicalCostFactory.addListener(getInputBuilderListener(), TrafficAssignmentComponentFactory.TRAFFICCOMPONENT_CREATE);    
    return physicalCostFactory.create(configurator.getPhysicalCost().getClassTypeToConfigure().getCanonicalName(), new Object[] { getGroupIdToken() });
  }
  
  /** create a virtual cost instance based on configuration
   * 
   * @return virtual cost instance
   * @throws PlanItException thrown if error
   */  
  protected VirtualCost createVirtualCostInstance(TrafficAssignmentConfigurator<?> configurator) throws PlanItException {
    TrafficAssignmentComponentFactory<VirtualCost> virtualCostFactory = new TrafficAssignmentComponentFactory<VirtualCost>(VirtualCost.class);
    virtualCostFactory.addListener(getInputBuilderListener(), TrafficAssignmentComponentFactory.TRAFFICCOMPONENT_CREATE);    
    return virtualCostFactory.create(configurator.getVirtualCost().getClassTypeToConfigure().getCanonicalName(), new Object[] { getGroupIdToken() });
  }  
    
  /**
   * call to build and configure all sub components of this builder
   * 
   * @throws PlanItException  thrown if error
   */
  private void buildSubComponents(T trafficAssignmentInstance) throws PlanItException {
    TrafficAssignmentConfigurator<?> configurator = ((TrafficAssignmentConfigurator<?>)getConfigurator());
    // build its subcomponents via their own builders
    
    // smoothing
    if (configurator.getSmoothing() != null) {
      Smoothing smoothing = createSmoothingInstance(configurator);
      configurator.getSmoothing().configure(smoothing);
      trafficAssignmentInstance.setSmoothing(smoothing);
    }
    
    // physical cost
    if (configurator.getPhysicalCost() != null) {
      PhysicalCost physicalCost = createPhysicalCostInstance(configurator);
      configurator.getPhysicalCost().configure(physicalCost);
      trafficAssignmentInstance.setPhysicalCost(physicalCost);
    }   
    
    // virtual cost
    if (configurator.getVirtualCost() != null) {
      VirtualCost virtualCost = createVirtualCostInstance(configurator);
      configurator.getVirtualCost().configure(virtualCost);
      trafficAssignmentInstance.setVirtualCost(virtualCost);
    }     

  }  

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
    
    /* register inputs (on configurator) */
    registerDemandZoningAndNetwork(demands, zoning, physicalNetwork);    

    /* register gap function (on configurator) */
    GapFunction theGapFunction = createGapFunction();
    ((TrafficAssignmentConfigurator<?>)getConfigurator()).setGapFunction(theGapFunction);

    // By default, activate the link outputs (on configurator)
    ((TrafficAssignmentConfigurator<?>)getConfigurator()).activateOutput(OutputType.LINK);
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
    T trafficAssignment = createTrafficAssignmentInstance();
    
    // build the sub components of the assignment as well
    buildSubComponents(trafficAssignment);

    // perform all delayed calls on the assignment to finalise the build
    getConfigurator().configure(trafficAssignment);
    
    /* information is now present to generate appropriate output type adapters (requires output manager which now has been set */
    trafficAssignment.createOutputTypeAdapters();    

    return trafficAssignment;
  }

}