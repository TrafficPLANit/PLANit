package org.planit.trafficassignment;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.djutils.event.EventProducer;
import org.djutils.event.EventType;
import org.planit.cost.physical.BPRLinkTravelTimeCost;
import org.planit.cost.physical.PhysicalCost;
import org.planit.cost.physical.initial.InitialLinkSegmentCost;
import org.planit.cost.physical.initial.InitialLinkSegmentCostPeriod;
import org.planit.cost.physical.initial.InitialPhysicalCost;
import org.planit.cost.virtual.FixedConnectoidTravelTimeCost;
import org.planit.cost.virtual.SpeedConnectoidTravelTimeCost;
import org.planit.cost.virtual.VirtualCost;
import org.planit.demands.Demands;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.network.physical.macroscopic.MacroscopicNetwork;
import org.planit.network.virtual.Zoning;
import org.planit.path.ODPathSets;
import org.planit.path.choice.PathChoice;
import org.planit.path.choice.logit.LogitChoiceModel;
import org.planit.path.choice.logit.MultinomialLogit;
import org.planit.sdinteraction.smoothing.MSASmoothing;
import org.planit.sdinteraction.smoothing.Smoothing;
import org.planit.supply.fundamentaldiagram.FundamentalDiagram;
import org.planit.supply.fundamentaldiagram.NewellFundamentalDiagram;
import org.planit.supply.network.nodemodel.NodeModel;
import org.planit.supply.network.nodemodel.TampereNodeModel;
import org.planit.supply.networkloading.NetworkLoading;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.reflection.ReflectionUtils;

/**
 * Generic factory class for registered subclasses of predefined traffic assignment components, so it does not create instances of T but of sublcasses of T.
 *
 * @author markr
 *
 * @param <T> generic type of a type traffic assignment component for which we construct the eligible derived classes by class name
 */
public class TrafficAssignmentComponentFactory<T extends TrafficAssignmentComponent<T> & Serializable> extends EventProducer implements Serializable {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(TrafficAssignmentComponentFactory.class.getCanonicalName());

  /** generated UID */
  private static final long serialVersionUID = -4507287133047792042L;

  /** event type fired off when a new traffic assignment component is created */
  public static final EventType TRAFFICCOMPONENT_CREATE = new EventType("TRAFFICCOMPONENT.CREATE");

  /** instance of the super component class this factory creates subclass instances for */
  protected final Class<T> componentSuperType;

  /**
   * Register per traffic assignment component type the derived classes that are supported
   */
  protected static final HashMap<Class<? extends TrafficAssignmentComponent<?>>, TreeSet<String>> registeredTrafficAssignmentComponents;

  // register the traffic component types that we allow
  static {
    registeredTrafficAssignmentComponents = new HashMap<>();
    registeredTrafficAssignmentComponents.put(Zoning.class, new TreeSet<>());
    registeredTrafficAssignmentComponents.put(NetworkLoading.class, new TreeSet<>());
    registeredTrafficAssignmentComponents.put(Smoothing.class, new TreeSet<>());
    registeredTrafficAssignmentComponents.put(Demands.class, new TreeSet<>());
    registeredTrafficAssignmentComponents.put(PhysicalNetwork.class, new TreeSet<>());
    registeredTrafficAssignmentComponents.put(PhysicalCost.class, new TreeSet<>());
    registeredTrafficAssignmentComponents.put(InitialPhysicalCost.class, new TreeSet<>());
    registeredTrafficAssignmentComponents.put(VirtualCost.class, new TreeSet<>());
    registeredTrafficAssignmentComponents.put(FundamentalDiagram.class, new TreeSet<>());
    registeredTrafficAssignmentComponents.put(NodeModel.class, new TreeSet<>());
    registeredTrafficAssignmentComponents.put(PathChoice.class, new TreeSet<>());
    registeredTrafficAssignmentComponents.put(LogitChoiceModel.class, new TreeSet<>());
    registeredTrafficAssignmentComponents.put(ODPathSets.class, new TreeSet<>());

    registerDefaultImplementations();
  }

  /**
   * register all implementations that are by default available as they are provided in the PlanIt core packages
   */
  private static void registerDefaultImplementations() {
    try {
      registerTrafficAssignmentComponentType(Zoning.class);
      registerTrafficAssignmentComponentType(TraditionalStaticAssignment.class);
      registerTrafficAssignmentComponentType(MSASmoothing.class);
      registerTrafficAssignmentComponentType(Demands.class);
      registerTrafficAssignmentComponentType(MacroscopicNetwork.class);
      registerTrafficAssignmentComponentType(BPRLinkTravelTimeCost.class);
      registerTrafficAssignmentComponentType(InitialLinkSegmentCost.class);
      registerTrafficAssignmentComponentType(InitialLinkSegmentCostPeriod.class);
      registerTrafficAssignmentComponentType(FixedConnectoidTravelTimeCost.class);
      registerTrafficAssignmentComponentType(SpeedConnectoidTravelTimeCost.class);
      registerTrafficAssignmentComponentType(NewellFundamentalDiagram.class);
      registerTrafficAssignmentComponentType(TampereNodeModel.class);
      registerTrafficAssignmentComponentType(MultinomialLogit.class);
      registerTrafficAssignmentComponentType(ODPathSets.class);
    } catch (final PlanItException e) {
      LOGGER.severe(e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Create a traffic component with no parameters
   *
   * @param trafficAssignmentComponentClassName the name of the traffic component to be created
   * @param constructorParameters               parameters to pass to the constructor
   * @return the created traffic component object
   * @throws PlanItException thrown if there is an error
   */
  @SuppressWarnings("unchecked")
  private T createTrafficComponent(final String trafficAssignmentComponentClassName, final Object[] constructorParameters) throws PlanItException {
    final TreeSet<String> eligibleComponentTypes = registeredTrafficAssignmentComponents.get(componentSuperType);
    PlanItException.throwIf(!eligibleComponentTypes.contains(trafficAssignmentComponentClassName), "Provided Traffic Assignment Component class is not eligible for construction");
    return (T) ReflectionUtils.createInstance(trafficAssignmentComponentClassName, constructorParameters);
  }

  /**
   * Dispatch an event on creation of a traffic component with variable parameters
   *
   * @param newTrafficComponent the traffic component being created
   * @param parameters          parameter object array to be used by the event
   * @throws PlanItException thrown if there is an exception
   */
  private void dispatchTrafficComponentEvent(final T newTrafficComponent, final Object[] parameters) throws PlanItException {
    fireEvent(new org.djutils.event.Event(TRAFFICCOMPONENT_CREATE, this, new Object[] { newTrafficComponent, parameters }));
    PlanItException.throwIf(!listeners.containsKey(TRAFFICCOMPONENT_CREATE), "Error during dispatchTrafficComponentEvent");
  }

  // PUBLIC

  /**
   * Constructor
   *
   * @param componentSuperType super type for this factory
   */
  public TrafficAssignmentComponentFactory(final Class<T> componentSuperType) {
    this.componentSuperType = componentSuperType;
  }

  /**
   * Register a component type that one can choose for the given traffic component
   *
   * @param trafficAssignmentComponent TrafficAssignmentComponent to be registered
   * @throws PlanItException thrown if there is an error
   */
  @SuppressWarnings("unchecked")
  public static void registerTrafficAssignmentComponentType(final Class<? extends TrafficAssignmentComponent<?>> trafficAssignmentComponent) throws PlanItException {
    Class<? extends TrafficAssignmentComponent<?>> currentClass = trafficAssignmentComponent;
    while (currentClass != null) {
      final Type currentSuperClass = currentClass.getGenericSuperclass();
      if (currentSuperClass instanceof ParameterizedType && ((ParameterizedType) currentSuperClass).getRawType() == TrafficAssignmentComponent.class) {
        // superclass is a trafficAssignmentComponent class, so the current class is the
        // class that we need
        // register by collecting the component entry and placing the component
        final TreeSet<String> treeSet = registeredTrafficAssignmentComponents.get(currentClass);
        PlanItException.throwIf(treeSet == null, "Base class of traffic assignment component not registered as eligible on PLANit");

        treeSet.add(trafficAssignmentComponent.getCanonicalName());
        registeredTrafficAssignmentComponents.get(currentClass).add(trafficAssignmentComponent.getCanonicalName());
        return;
      } else {
        currentClass = (Class<? extends TrafficAssignmentComponent<?>>) currentClass.getSuperclass(); // move
      }
    }
    throw new PlanItException("trafficAssignmentComponent not eligible for registration");
  }

  /**
   * Create traffic assignment component
   *
   * @param trafficAssignmentComponentClassName the derived class name of the traffic assignment component (without packages)
   * @param constructorParameters               parameters to pass to the constructor
   * @return the created TrafficAssignmentComponent
   * @throws PlanItException thrown if there is an error
   */
  public T create(final String trafficAssignmentComponentClassName, final Object[] constructorParameters) throws PlanItException {
    final T newTrafficComponent = createTrafficComponent(trafficAssignmentComponentClassName, constructorParameters);
    dispatchTrafficComponentEvent(newTrafficComponent, constructorParameters);
    return newTrafficComponent;
  }

  /**
   * Create traffic assignment component
   *
   * @param trafficAssignmentComponentClassName the derived class name of the traffic assignment component (without packages)
   * @param constructorParameters               parameters to pass to the constructor
   * @param eventParameters                     object array which contains any data required to create the component
   * @return the created TrafficAssignmentComponent
   * @throws PlanItException thrown if there is an error
   */
  public T create(final String trafficAssignmentComponentClassName, final Object[] constructorParameters, final Object... eventParameters) throws PlanItException {
    final T newTrafficComponent = createTrafficComponent(trafficAssignmentComponentClassName, constructorParameters);
    dispatchTrafficComponentEvent(newTrafficComponent, eventParameters);
    return newTrafficComponent;
  }

  /**
   * sourceId provides information of the source of an event fired from this class instance
   *
   * @return this class instance
   */
  @Override
  public Serializable getSourceId() {
    return this;
  }

}
