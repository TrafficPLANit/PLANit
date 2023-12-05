package org.goplanit.component;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.goplanit.assignment.ltm.sltm.StaticLtm;
import org.goplanit.assignment.traditionalstatic.TraditionalStaticAssignment;
import org.goplanit.component.event.PlanitComponentEvent;
import org.goplanit.component.event.PlanitComponentEventType;
import org.goplanit.component.event.PlanitComponentListener;
import org.goplanit.component.event.PopulateComponentEvent;
import org.goplanit.component.event.PopulateDemandsEvent;
import org.goplanit.component.event.PopulateFundamentalDiagramEvent;
import org.goplanit.component.event.PopulateGapFunctionEvent;
import org.goplanit.component.event.PopulateInitialLinkSegmentCostEvent;
import org.goplanit.component.event.PopulateNetworkEvent;
import org.goplanit.component.event.PopulatePhysicalCostEvent;
import org.goplanit.component.event.PopulateRoutedServicesEvent;
import org.goplanit.component.event.PopulateServiceNetworkEvent;
import org.goplanit.component.event.PopulateZoningEvent;
import org.goplanit.cost.physical.AbstractPhysicalCost;
import org.goplanit.cost.physical.BprLinkTravelTimeCost;
import org.goplanit.cost.physical.FreeFlowLinkTravelTimeCost;
import org.goplanit.cost.physical.SteadyStateTravelTimeCost;
import org.goplanit.cost.physical.initial.InitialMacroscopicLinkSegmentCost;
import org.goplanit.cost.physical.initial.InitialPhysicalCost;
import org.goplanit.cost.virtual.AbstractVirtualCost;
import org.goplanit.cost.virtual.FixedConnectoidTravelTimeCost;
import org.goplanit.cost.virtual.SpeedConnectoidTravelTimeCost;
import org.goplanit.demands.Demands;
import org.goplanit.gap.GapFunction;
import org.goplanit.gap.LinkBasedRelativeDualityGapFunction;
import org.goplanit.gap.NormBasedGapFunction;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.Network;
import org.goplanit.network.ServiceNetwork;
import org.goplanit.path.OdPathSets;
import org.goplanit.path.choice.PathChoice;
import org.goplanit.path.choice.StochasticPathChoice;
import org.goplanit.path.choice.logit.LogitChoiceModel;
import org.goplanit.path.choice.logit.MultinomialLogit;
import org.goplanit.sdinteraction.smoothing.MSASmoothing;
import org.goplanit.sdinteraction.smoothing.Smoothing;
import org.goplanit.service.routed.RoutedServices;
import org.goplanit.supply.fundamentaldiagram.FundamentalDiagramComponent;
import org.goplanit.supply.fundamentaldiagram.NewellFundamentalDiagramComponent;
import org.goplanit.supply.network.nodemodel.NodeModelComponent;
import org.goplanit.supply.network.nodemodel.TampereNodeModelComponent;
import org.goplanit.supply.networkloading.NetworkLoading;
import org.goplanit.utils.event.Event;
import org.goplanit.utils.event.EventListener;
import org.goplanit.utils.event.EventListenerPriority;
import org.goplanit.utils.event.EventProducerImpl;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.reflection.ReflectionUtils;
import org.goplanit.utils.time.TimePeriod;
import org.goplanit.zoning.Zoning;

/**
 * Generic factory class for registered subclasses of predefined PLANit components, so it does not create instances of T but of subclasses of T.
 *
 * @author markr
 *
 * @param <T> generic type of a type PLANit component for which we construct the eligible derived classes by class name
 */
public class PlanitComponentFactory<T extends PlanitComponent<?>> extends EventProducerImpl implements Serializable {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(PlanitComponentFactory.class.getCanonicalName());

  /** generated UID */
  private static final long serialVersionUID = -4507287133047792042L;

  /** instance of the super component class this factory creates subclass instances for */
  protected final String componentSuperTypeCanonicalName;

  /**
   * Register per traffic assignment component type the derived classes that are supported
   */
  protected static final HashMap<String, TreeSet<String>> registeredPlanitComponents;

  // register the PLANit component types that we allow
  static {
    registeredPlanitComponents = new HashMap<>();
    registeredPlanitComponents.put(Zoning.class.getCanonicalName(), new TreeSet<>());
    registeredPlanitComponents.put(NetworkLoading.class.getCanonicalName(), new TreeSet<>());
    registeredPlanitComponents.put(Smoothing.class.getCanonicalName(), new TreeSet<>());
    registeredPlanitComponents.put(Demands.class.getCanonicalName(), new TreeSet<>());
    registeredPlanitComponents.put(RoutedServices.class.getCanonicalName(), new TreeSet<>());
    registeredPlanitComponents.put(Network.class.getCanonicalName(), new TreeSet<>());
    registeredPlanitComponents.put(AbstractPhysicalCost.class.getCanonicalName(), new TreeSet<>());
    registeredPlanitComponents.put(InitialPhysicalCost.class.getCanonicalName(), new TreeSet<>());
    registeredPlanitComponents.put(AbstractVirtualCost.class.getCanonicalName(), new TreeSet<>());
    registeredPlanitComponents.put(FundamentalDiagramComponent.class.getCanonicalName(), new TreeSet<>());
    registeredPlanitComponents.put(NodeModelComponent.class.getCanonicalName(), new TreeSet<>());
    registeredPlanitComponents.put(PathChoice.class.getCanonicalName(), new TreeSet<>());
    registeredPlanitComponents.put(LogitChoiceModel.class.getCanonicalName(), new TreeSet<>());
    registeredPlanitComponents.put(OdPathSets.class.getCanonicalName(), new TreeSet<>());
    registeredPlanitComponents.put(GapFunction.class.getCanonicalName(), new TreeSet<>());

    registerDefaultImplementations();
  }

  /**
   * register all implementations that are by default available as they are provided in the PLANit core packages
   */
  private static void registerDefaultImplementations() {
    registerPlanitComponentType(Zoning.class);
    registerPlanitComponentType(TraditionalStaticAssignment.class);
    registerPlanitComponentType(StaticLtm.class);
    registerPlanitComponentType(MSASmoothing.class);
    registerPlanitComponentType(Demands.class);
    registerPlanitComponentType(RoutedServices.class);
    registerPlanitComponentType(MacroscopicNetwork.class);
    registerPlanitComponentType(ServiceNetwork.class);
    registerPlanitComponentType(BprLinkTravelTimeCost.class);
    registerPlanitComponentType(FreeFlowLinkTravelTimeCost.class);
    registerPlanitComponentType(SteadyStateTravelTimeCost.class);
    registerPlanitComponentType(InitialMacroscopicLinkSegmentCost.class);
    registerPlanitComponentType(FixedConnectoidTravelTimeCost.class);
    registerPlanitComponentType(SpeedConnectoidTravelTimeCost.class);
    registerPlanitComponentType(NewellFundamentalDiagramComponent.class);
    registerPlanitComponentType(TampereNodeModelComponent.class);
    registerPlanitComponentType(StochasticPathChoice.class);
    registerPlanitComponentType(MultinomialLogit.class);
    registerPlanitComponentType(OdPathSets.class);
    registerPlanitComponentType(LinkBasedRelativeDualityGapFunction.class);
    registerPlanitComponentType(NormBasedGapFunction.class);
  }

  /**
   * Create a PLANit component with no parameters
   *
   * @param planitComponentClassName the name of the PLANit component to be created
   * @param constructorParameters    parameters to pass to the constructor
   * @return the created component object
   */
  private <T extends PlanitComponent<?>> T createTrafficComponent(final String planitComponentClassName, final Object[] constructorParameters) {
    final var eligibleComponentTypes = registeredPlanitComponents.get(componentSuperTypeCanonicalName);
    PlanItRunTimeException.throwIf(eligibleComponentTypes == null || !eligibleComponentTypes.contains(planitComponentClassName),
        "Provided PLANit Component class %s is not eligible for construction", planitComponentClassName != null ? planitComponentClassName : "");

    T typedInstance = null;
    try {
      Object instance = ReflectionUtils.createInstance(planitComponentClassName, constructorParameters);
      PlanItRunTimeException.throwIf(!(instance instanceof PlanitComponent<?>), "provided factory class is not eligible for construction since it is not derived from PLANitComponent<?>");
      typedInstance = (T) instance;
    }catch(Exception e){
      throw new PlanItRunTimeException(e);
    }


    /* register as listener for PLANit component events, so it can act upon them before other registered listeners */
    addListener(typedInstance, EventListenerPriority.HIGH);
    return typedInstance;
  }

  /**
   * Dispatch an event on creation of a PLANit component with variable parameters
   *
   * @param newPlanitComponent the PLANit component being created
   * @param parameters         parameter object array to be used by the event
   */
  private <T extends PlanitComponent<?>> void dispatchPopulatePlanitComponentEvent(final T newPlanitComponent, final Object[] parameters) {

    /* when possible use more specific event for user friendly access to event content on listeners */
    /* TODO: type check parameters and issue message when not correct */
    PlanitComponentEvent event = null;
    if (newPlanitComponent instanceof MacroscopicNetwork && hasListener(PopulateNetworkEvent.EVENT_TYPE)) {
      event = new PopulateNetworkEvent(this, (MacroscopicNetwork) newPlanitComponent);
    } else if (newPlanitComponent instanceof Zoning && hasListener(PopulateZoningEvent.EVENT_TYPE)) {
      event = new PopulateZoningEvent(this, (Zoning) newPlanitComponent, (MacroscopicNetwork) parameters[0]);
    } else if (newPlanitComponent instanceof Demands && hasListener(PopulateDemandsEvent.EVENT_TYPE)) {
      event = new PopulateDemandsEvent(this, (Demands) newPlanitComponent, (Zoning) parameters[0], (MacroscopicNetwork) parameters[1]);
    } else if (newPlanitComponent instanceof ServiceNetwork && hasListener(PopulateServiceNetworkEvent.EVENT_TYPE)) {
      event = new PopulateServiceNetworkEvent(this, (ServiceNetwork) newPlanitComponent);
    } else if (newPlanitComponent instanceof RoutedServices && hasListener(PopulateRoutedServicesEvent.EVENT_TYPE)) {
      event = new PopulateRoutedServicesEvent(this, (RoutedServices) newPlanitComponent);
    } else if (newPlanitComponent instanceof GapFunction && hasListener(PopulateGapFunctionEvent.EVENT_TYPE)) {
      event = new PopulateGapFunctionEvent(this, (GapFunction) newPlanitComponent);
    } else if (newPlanitComponent instanceof FundamentalDiagramComponent && hasListener(PopulateFundamentalDiagramEvent.EVENT_TYPE)) {
      event = new PopulateFundamentalDiagramEvent(this, (FundamentalDiagramComponent) newPlanitComponent, (MacroscopicNetworkLayer) parameters[0]);
    } else if (newPlanitComponent instanceof InitialMacroscopicLinkSegmentCost && hasListener(PopulateInitialLinkSegmentCostEvent.EVENT_TYPE)) {
      event = new PopulateInitialLinkSegmentCostEvent(this, (InitialMacroscopicLinkSegmentCost) newPlanitComponent, (String) parameters[0], (MacroscopicNetwork) parameters[1], (TimePeriod) parameters[2]);
    } else if (newPlanitComponent instanceof AbstractPhysicalCost && hasListener(PopulatePhysicalCostEvent.EVENT_TYPE)) {
      event = new PopulatePhysicalCostEvent(this, (AbstractPhysicalCost) newPlanitComponent, (MacroscopicNetwork) parameters[0]);
    } else if(hasListener(PopulateComponentEvent.EVENT_TYPE)){
      /* fire generic populate component event, likely third party class, or one that is likely not meant for user listeners to do anything with */
      event = new PopulateComponentEvent(this, newPlanitComponent, parameters);
    }

    if(event != null) {
      fireEvent(event);
    }
  }

  // PUBLIC

  /**
   * {@inheritDoc}
   */
  @Override
  protected void fireEvent(EventListener eventListener, Event event) {
    try {
      PlanitComponentListener.class.cast(eventListener).onPlanitComponentEvent(PlanitComponentEvent.class.cast(event));
    } catch (PlanItException e) {
      /* log exception information and rethrow as run time exception to keep method signature clean */
      if (e.getCause() != null) {
        LOGGER.severe(e.getCause().getMessage());
      }
      LOGGER.severe(e.getMessage());
      throw new RuntimeException("Unable to complete fired event" + event.toString());
    }
  }

  /**
   * Constructor. Here we make sure it is a type that extends the PLANit component class. We do not do so generally on the class level since this might lead to conflicts when the
   * class has generic arguments itself which leads to issues (that I have not been able to solve). In the latter case use the other constructor which gets around this problem by
   * simply providing the canonical class name corresponding to type T
   *
   * @param <U>                PLANit component type
   * @param componentSuperType super type for this factory
   */
  public <U extends PlanitComponent<U> & Serializable> PlanitComponentFactory(final Class<U> componentSuperType) {
    this.componentSuperTypeCanonicalName = componentSuperType.getCanonicalName();
  }

  /**
   * Constructor.
   * 
   * Use this constructor when the component super type that you use is not compatible with ClassT, for example because the super type itself uses generics, i.e., T(U,V), in which
   * case the default constructor does not work. Make sure however, that the provided canonical class name is compatible with T, i.e., it must extend from TrafficAssigmentComponent
   *
   * @param componentSuperTypeCanonicalName super type's canonical class name for this factory which should be the same as ClassT.getCanonicalName()
   */
  public PlanitComponentFactory(String componentSuperTypeCanonicalName) {
    this.componentSuperTypeCanonicalName = componentSuperTypeCanonicalName;
  }

  /**
   * Register a component type that one can choose for the given PLANit component
   *
   * @param planitComponent PLANit component to be registered
   */
  public static void registerPlanitComponentType(final Class<? extends PlanitComponent<?>> planitComponent) {
    Class<?> currentClass = planitComponent;
    while (currentClass != null) {
      final Type currentSuperClass = currentClass.getGenericSuperclass();
      if (currentSuperClass instanceof ParameterizedType && ((ParameterizedType) currentSuperClass).getRawType() == PlanitComponent.class) {
        // superclass is a PLANitComponent class, so the current class is the
        // class that we need register by collecting the component entry and placing the component
        final var treeSet = registeredPlanitComponents.get(currentClass.getCanonicalName());
        if (treeSet == null) {
          LOGGER.severe("Base class of PLANit component not registered as eligible on PLANit, component not eligible and therefore ignored");
          return;
        }

        treeSet.add(planitComponent.getCanonicalName());
        registeredPlanitComponents.get(currentClass.getCanonicalName()).add(planitComponent.getCanonicalName());
        return;
      } else {
        currentClass = currentClass.getSuperclass(); // move
      }
    }
    LOGGER.severe("PLANit component not eligible for registration");
  }

  /**
   * Simplified factory method which creates the component, registers the provided listeners for the temporary factory instance component and
   * then invokes the creation with those listeners as a one-off call
   *
   * @param <C> type of component
   * @param <U> specific type of component
   * @param clazzCategory the concrete component belongs to
   * @param concreteClass to create
   * @param constructorParameters to use
   * @param eventParameters to apply for the event listeners that are listening (if any)
   * @param listeners to register (may be null)
   * @return created component
   */
  public static <C extends PlanitComponent<?>, U extends C> U createWithListeners(
      Class<C> clazzCategory, Class<U> concreteClass, final Object[] constructorParameters, final Object[] eventParameters, PlanitComponentListener... listeners) {
    return (U) createWithListeners(clazzCategory, concreteClass.getCanonicalName(), constructorParameters, eventParameters, listeners);
  }

  /**
   * Simplified factory method which creates the component, registers the provided listeners for the temporary factory instance component and
   * then invokes the creation with those listeners as a one-off call
   *
   * @param <C> type of component
   * @param clazzCategory the concrete component belongs to
   * @param concreteClassCanonicalName to create
   * @param constructorParameters to use
   * @param eventParameters to apply for the event listeners that are listening (if any)
   * @param listeners to register (may be null)
   * @return created component
   */
  public static <C extends PlanitComponent<?>> C createWithListeners(
      Class<C> clazzCategory, String concreteClassCanonicalName, final Object[] constructorParameters, final Object[] eventParameters, PlanitComponentListener... listeners) {
    var factoryInstance = new PlanitComponentFactory<C>(clazzCategory.getCanonicalName());
    if(listeners != null) {
      for (var listener : listeners) {
        factoryInstance.addListener(listener);
      }
      return factoryInstance.create(concreteClassCanonicalName, constructorParameters, eventParameters);
    }
    return factoryInstance.create(concreteClassCanonicalName, constructorParameters);
  }

  /**
   * Simplified factory method which creates the component, registers the provided listeners for the temporary factory instance component and
   * then invokes the creation with those listeners as a one-off call
   *
   * @param <C> type of component
   * @param clazzCategory the concrete component belongs to
   * @param concreteClassCanonicalName to create
   * @param constructorParameters to use
   * @param listeners to register (may be null)
   * @return created component
   */
  public static <C extends PlanitComponent<?>> C createWithListeners(
      Class<C> clazzCategory, String concreteClassCanonicalName, final Object[] constructorParameters, PlanitComponentListener... listeners) {
    return createWithListeners(clazzCategory, concreteClassCanonicalName, constructorParameters, null, listeners);
  }

  /**
   * Simplified factory method which creates the component (without any listeners)
   *
   * @param <C> type of component
   * @param clazzCategory the concrete component belongs to
   * @param concreteClassCanonicalName to create
   * @param constructorParameters to use
   * @return created component
   */
  public static <C extends PlanitComponent<?>> C create(
      Class<C> clazzCategory, String concreteClassCanonicalName, final Object[] constructorParameters) {
    return createWithListeners(clazzCategory, concreteClassCanonicalName, constructorParameters, null, null);
  }

  /**
   * Simplified factory method which creates the component, registers the provided listeners for the temporary factory instance component and
   * then invokes the creation with those listeners as a one-off call
   *
   * @param <C> type of component
   * @param <U> specific type of component
   * @param clazzCategory the concrete component belongs to
   * @param concreteClass to create
   * @param constructorParameters to use
   * @param listeners to register (may be null)
   * @return created component
   */
  public static <C extends PlanitComponent<?>, U extends C> U createWithListeners(
      Class<C> clazzCategory, Class<U> concreteClass, final Object[] constructorParameters, PlanitComponentListener... listeners) {
    return createWithListeners(clazzCategory, concreteClass, constructorParameters, null, listeners);
  }

  /**
   * Create PLANit component
   *
   * @param <C> type of component
   * @param planitComponentClassName the derived class name of the PLANit component (without packages)
   * @param constructorParameters    parameters to pass to the constructor
   * @return the created TrafficAssignmentComponent
   */
  public <C extends PlanitComponent<?>> C create(final String planitComponentClassName, final Object[] constructorParameters) {
    final C newTrafficComponent = createTrafficComponent(planitComponentClassName, constructorParameters);
    dispatchPopulatePlanitComponentEvent(newTrafficComponent, null);
    return newTrafficComponent;
  }

  /**
   * Create PLANit component
   *
   * @param <C> type of component
   * @param planitComponentClassName the derived class name of the PLANit component (without packages)
   * @param constructorParameters    parameters to pass to the constructor
   * @param eventParameters          object array which contains any additional data that might be required to populate the component
   * @return the created component
   */
  public <C extends PlanitComponent<?>> C create(final String planitComponentClassName, final Object[] constructorParameters, final Object... eventParameters) {
    final C newTrafficComponent = createTrafficComponent(planitComponentClassName, constructorParameters);
    dispatchPopulatePlanitComponentEvent(newTrafficComponent, eventParameters);
    return newTrafficComponent;
  }

  /**
   * Allows one to verify if this factory creates derived classes of the provided class super type. Useful in case the generic type parameter is not available at run time for this
   * factory
   *
   * @param <U>        type to verify
   * @param superClazz class of type
   * @return true when factory is compatible, false otherwise
   */
  public <U> boolean isFactoryForDerivedClassesOf(Class<U> superClazz) {
    return superClazz.getCanonicalName().equals(componentSuperTypeCanonicalName);
  }

  /**
   * Add a listener for PLANit component event types fired
   * 
   * @param listener   to register
   * @param eventTypes to register for
   */
  public void addListener(PlanitComponentListener listener, PlanitComponentEventType... eventTypes) {
    super.addListener(listener, eventTypes);
  }

  /**
   * Add a listener for all its known supported PLANit component event types
   * 
   * @param listener to register, when null nothing is registered
   */
  public void addListener(PlanitComponentListener listener) {
    if (listener == null) {
      return;
    }
    super.addListener(listener);
  }

  /**
   * Remove listener for given event type
   * 
   * @param listener  to remove
   * @param eventType to remove for
   */
  public void removeListener(PlanitComponentListener listener, PlanitComponentEventType eventType) {
    super.removeListener(listener, eventType);
  }

  /**
   * Remove listener for all event types of this producer
   * 
   * @param listener to remove
   */
  public void removeListener(PlanitComponentListener listener) {
    super.removeListener(listener);
  }

}
