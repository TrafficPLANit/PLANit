package org.goplanit.assignment;

import java.util.logging.Logger;

import org.goplanit.component.PlanitComponentBuilder;
import org.goplanit.component.PlanitComponentFactory;
import org.goplanit.cost.physical.AbstractPhysicalCost;
import org.goplanit.cost.virtual.AbstractVirtualCost;
import org.goplanit.demands.Demands;
import org.goplanit.gap.GapFunction;
import org.goplanit.gap.StopCriterion;
import org.goplanit.input.InputBuilderListener;
import org.goplanit.interactor.InteractorAccessor;
import org.goplanit.network.LayeredNetwork;
import org.goplanit.output.OutputManager;
import org.goplanit.output.enums.OutputType;
import org.goplanit.sdinteraction.smoothing.Smoothing;
import org.goplanit.supply.networkloading.NetworkLoading;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.zoning.Zoning;

/**
 * All traffic assignment instances require a network, demand, and (equilibrium) smoothing procedure, all of which should be registered via this generic builder. Specific traffic
 * assignment methods might require special builders derived from this builder
 *
 * @author markr
 *
 */
public abstract class TrafficAssignmentBuilder<T extends TrafficAssignment> extends PlanitComponentBuilder<T> {

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
  private void registerDemandZoningAndNetwork(final Demands demands, final Zoning zoning, final LayeredNetwork<?, ?> network) throws PlanItException {
    if (zoning == null || demands == null || network == null) {
      PlanItException.throwIf(zoning == null, "zoning in registerDemandZoningAndNetwork is null");
      PlanItException.throwIf(demands == null, "demands in registerDemandZoningAndNetwork is null");
      PlanItException.throwIf(network == null, "network in registerDemandZoningAndNetwork is null");
    }
    PlanItException.throwIf(!zoning.isCompatibleWithDemands(demands, network.getModes()),
        "Zoning structure is incompatible with one or more of the demands, likely the number of zones does not match the number of origins and/or destinations");

    for (var mode : network.getModes()) {
      for (var timePeriod : demands.timePeriods.asSortedSetByStartTime()) {
        if (demands.get(mode, timePeriod) == null) {
          LOGGER.warning("No demand matrix defined for Mode " + mode.getExternalId() + " and Time Period " + timePeriod.getExternalId());
        }
      }
    }

    // register on configurator
    getConfigurator().setInfrastructureNetwork(network);
    getConfigurator().setZoning(zoning);
    getConfigurator().setDemands(demands);
  }

  /**
   * create the output manager and register it on the configurator to activate it on assignment when built
   */
  private void createOutputManager() {
    getConfigurator().setOutputManager(new OutputManager());
  }

  /**
   * create the output (type) adapters for the given assignment
   * 
   * @param trafficAssignment the assignment we are creating the adapters for
   */
  private void initialiseOutputAdapters(T trafficAssignment) {
    getConfigurator().getOutputManager().initialiseOutputAdapters(trafficAssignment);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected abstract TrafficAssignmentConfigurator<T> createConfigurator() throws PlanItException;

  /**
   * Factory method to create the instance of the desired type
   * 
   * @return instance of traffic assignment
   * @throws PlanItException thrown when error
   */
  @SuppressWarnings("unchecked")
  protected T createTrafficAssignmentInstance() throws PlanItException {
    String trafficAssignmentClassName = getClassToBuild().getCanonicalName();

    var assignmentFactory = new PlanitComponentFactory<TrafficAssignment>(NetworkLoading.class.getCanonicalName());
    assignmentFactory.addListener(getInputBuilderListener());

    var networkLoadingAndAssignment = assignmentFactory.create(trafficAssignmentClassName, new Object[] { groupId });
    PlanItException.throwIf(!(networkLoadingAndAssignment instanceof TrafficAssignment), "not a valid traffic assignment type");
    return (T) networkLoadingAndAssignment;
  }

  /**
   * create a smoothing instance based on configuration
   * 
   * @return smoothing instance
   * @throws PlanItException thrown if error
   */
  protected Smoothing createSmoothingInstance() throws PlanItException {
    var smoothingFactory = new PlanitComponentFactory<Smoothing>(Smoothing.class);
    smoothingFactory.addListener(getInputBuilderListener());
    return smoothingFactory.create(getConfigurator().getSmoothing().getClassTypeToConfigure().getCanonicalName(), new Object[] { getGroupIdToken() });
  }

  /**
   * create a physical cost instance based on configuration
   *
   * @return physical cost instance
   * @throws PlanItException thrown if error
   */
  protected AbstractPhysicalCost createPhysicalCostInstance() throws PlanItException {
    var physicalCostFactory = new PlanitComponentFactory<AbstractPhysicalCost>(AbstractPhysicalCost.class);
    physicalCostFactory.addListener(getInputBuilderListener());
    return physicalCostFactory.create(getConfigurator().getPhysicalCost().getClassTypeToConfigure().getCanonicalName(), new Object[] { getGroupIdToken() },
        new Object[] { getConfigurator().getInfrastructureNetwork() });
  }

  /**
   * create a virtual cost instance based on configuration
   * 
   * @return virtual cost instance
   * @throws PlanItException thrown if error
   */
  protected AbstractVirtualCost createVirtualCostInstance() throws PlanItException {
    var virtualCostFactory = new PlanitComponentFactory<AbstractVirtualCost>(AbstractVirtualCost.class);
    virtualCostFactory.addListener(getInputBuilderListener());
    return virtualCostFactory.create(getConfigurator().getVirtualCost().getClassTypeToConfigure().getCanonicalName(), new Object[] { getGroupIdToken() });
  }

  /**
   * create a gap function instance based on configuration
   * 
   * @param stopCriterion to use
   * @return gap function instance
   * @throws PlanItException thrown if error
   */
  protected GapFunction createGapFunctionInstance(StopCriterion stopCriterion) throws PlanItException {
    var gapFunctionFactory = new PlanitComponentFactory<GapFunction>(GapFunction.class);
    gapFunctionFactory.addListener(getInputBuilderListener());
    return gapFunctionFactory.create(getConfigurator().getGapFunction().getClassTypeToConfigure().getCanonicalName(), new Object[] { getGroupIdToken(), stopCriterion });
  }

  /**
   * call to build and configure all sub components of this builder
   * 
   * @param trafficAssignmentInstance instance to build subcomponents for
   * @throws PlanItException thrown if error
   */
  protected void buildSubComponents(T trafficAssignmentInstance) throws PlanItException {
    // build its subcomponents via their own builders

    // smoothing
    if (getConfigurator().getSmoothing() != null) {
      var smoothing = createSmoothingInstance();
      getConfigurator().getSmoothing().configure(smoothing);
      trafficAssignmentInstance.setSmoothing(smoothing);
    }

    // physical cost
    if (getConfigurator().getPhysicalCost() != null) {
      var physicalCost = createPhysicalCostInstance();
      getConfigurator().getPhysicalCost().configure(physicalCost);
      trafficAssignmentInstance.setPhysicalCost(physicalCost);

      /* Physical cost <-> assignment dependency */
      if (physicalCost instanceof InteractorAccessor) {
        /*
         * by decoupling cost and assignment, we make it possible to mix and match the way we compute costs and on which assignments these are used. Assignments can possibly
         * support more than a single cost computation as long as they implement the required interactor interfaces
         */
        ((InteractorAccessor<?>) physicalCost).setAccessee(trafficAssignmentInstance);
      }
    }

    // virtual cost
    if (getConfigurator().getVirtualCost() != null) {
      var virtualCost = createVirtualCostInstance();
      getConfigurator().getVirtualCost().configure(virtualCost);
      trafficAssignmentInstance.setVirtualCost(virtualCost);

      /* virtual cost <-> assignment dependency (see physical cost above for rationale) */
      if (virtualCost instanceof InteractorAccessor) {
        ((InteractorAccessor<?>) virtualCost).setAccessee(trafficAssignmentInstance);
      }
    }

    // gap function
    if (getConfigurator().getGapFunction() != null) {

      // stop criterion
      // TODO: technically should be handled by the gap function having a builder of its own
      // yet, the stop criterion is the only sub component and it will likely be moved,
      // also there is only one stop criterion available at present. Therefore, we construct
      // it here instead for now.
      var stopCriterion = new StopCriterion();
      getConfigurator().getGapFunction().getStopCriterion().configure(stopCriterion);

      var gapFunction = createGapFunctionInstance(stopCriterion);
      getConfigurator().getGapFunction().configure(gapFunction);
      trafficAssignmentInstance.setGapFunction(gapFunction);
    }

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
   * @param network                the network
   * @throws PlanItException thrown when error
   */
  protected TrafficAssignmentBuilder(final Class<T> trafficAssignmentClass, final IdGroupingToken projectToken, InputBuilderListener inputBuilderListener, final Demands demands,
      final Zoning zoning, final LayeredNetwork<?, ?> network) throws PlanItException {
    super(trafficAssignmentClass, projectToken, inputBuilderListener);

    /* register inputs (on configurator) */
    registerDemandZoningAndNetwork(demands, zoning, network);

    /* create an output manager for this assignment */
    createOutputManager();

    // By default, activate the link outputs (on configurator)
    getConfigurator().activateOutput(OutputType.LINK);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TrafficAssignmentConfigurator<T> getConfigurator() {
    return (TrafficAssignmentConfigurator<T>) super.getConfigurator();
  }

  /**
   * Build the traffic assignment
   * 
   * @return traffic assignment instance that is built
   * @throws PlanItException thrown if error
   */
  @Override
  public T build() throws PlanItException {
    // Build the assignment
    T trafficAssignment = createTrafficAssignmentInstance();

    // perform all delayed calls on the assignment to finalise the build
    getConfigurator().configure(trafficAssignment);

    // Allow derived classes to verify if the chosen inputs are compatible before proceeding, not mandatory
    trafficAssignment.verifyNetworkDemandZoningCompatibility();

    // build the sub components of the assignment as well
    buildSubComponents(trafficAssignment);

    /* information is now present to generate appropriate output type adapters (requires output manager which now has been set */
    initialiseOutputAdapters(trafficAssignment);

    return trafficAssignment;
  }

}