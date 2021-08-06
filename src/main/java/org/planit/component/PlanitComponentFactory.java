package org.planit.component;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.planit.assignment.traditionalstatic.TraditionalStaticAssignment;
import org.planit.component.event.PlanitComponentEvent;
import org.planit.component.event.PlanitComponentEventType;
import org.planit.component.event.PlanitComponentListener;
import org.planit.component.event.PopulateComponentEvent;
import org.planit.component.event.PopulateDemandsEvent;
import org.planit.component.event.PopulateInitialLinkSegmentCostEvent;
import org.planit.component.event.PopulateNetworkEvent;
import org.planit.component.event.PopulatePhysicalCostEvent;
import org.planit.component.event.PopulateRoutedServicesEvent;
import org.planit.component.event.PopulateServiceNetworkEvent;
import org.planit.component.event.PopulateZoningEvent;
import org.planit.cost.physical.AbstractPhysicalCost;
import org.planit.cost.physical.BPRLinkTravelTimeCost;
import org.planit.cost.physical.initial.InitialLinkSegmentCost;
import org.planit.cost.physical.initial.InitialLinkSegmentCostPeriod;
import org.planit.cost.physical.initial.InitialPhysicalCost;
import org.planit.cost.virtual.AbstractVirtualCost;
import org.planit.cost.virtual.FixedConnectoidTravelTimeCost;
import org.planit.cost.virtual.SpeedConnectoidTravelTimeCost;
import org.planit.demands.Demands;
import org.planit.network.MacroscopicNetwork;
import org.planit.network.Network;
import org.planit.network.ServiceNetwork;
import org.planit.path.OdPathSets;
import org.planit.path.choice.PathChoice;
import org.planit.path.choice.logit.LogitChoiceModel;
import org.planit.path.choice.logit.MultinomialLogit;
import org.planit.sdinteraction.smoothing.MSASmoothing;
import org.planit.sdinteraction.smoothing.Smoothing;
import org.planit.service.routed.RoutedServices;
import org.planit.supply.fundamentaldiagram.FundamentalDiagram;
import org.planit.supply.fundamentaldiagram.NewellFundamentalDiagram;
import org.planit.supply.network.nodemodel.NodeModel;
import org.planit.supply.network.nodemodel.TampereNodeModel;
import org.planit.supply.networkloading.NetworkLoading;
import org.planit.utils.event.Event;
import org.planit.utils.event.EventListener;
import org.planit.utils.event.EventProducerImpl;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.reflection.ReflectionUtils;
import org.planit.zoning.Zoning;

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
    registeredPlanitComponents.put(FundamentalDiagram.class.getCanonicalName(), new TreeSet<>());
    registeredPlanitComponents.put(NodeModel.class.getCanonicalName(), new TreeSet<>());
    registeredPlanitComponents.put(PathChoice.class.getCanonicalName(), new TreeSet<>());
    registeredPlanitComponents.put(LogitChoiceModel.class.getCanonicalName(), new TreeSet<>());
    registeredPlanitComponents.put(OdPathSets.class.getCanonicalName(), new TreeSet<>());

    registerDefaultImplementations();
  }

  /**
   * register all implementations that are by default available as they are provided in the PLANit core packages
   */
  private static void registerDefaultImplementations() {
    registerPlanitComponentType(Zoning.class);
    registerPlanitComponentType(TraditionalStaticAssignment.class);
    registerPlanitComponentType(MSASmoothing.class);
    registerPlanitComponentType(Demands.class);
    registerPlanitComponentType(RoutedServices.class);
    registerPlanitComponentType(MacroscopicNetwork.class);
    registerPlanitComponentType(ServiceNetwork.class);
    registerPlanitComponentType(BPRLinkTravelTimeCost.class);
    registerPlanitComponentType(InitialLinkSegmentCost.class);
    registerPlanitComponentType(InitialLinkSegmentCostPeriod.class);
    registerPlanitComponentType(FixedConnectoidTravelTimeCost.class);
    registerPlanitComponentType(SpeedConnectoidTravelTimeCost.class);
    registerPlanitComponentType(NewellFundamentalDiagram.class);
    registerPlanitComponentType(TampereNodeModel.class);
    registerPlanitComponentType(MultinomialLogit.class);
    registerPlanitComponentType(OdPathSets.class);
  }

  /**
   * Create a PLANit component with no parameters
   *
   * @param planitComponentClassName the name of the PLANit component to be created
   * @param constructorParameters    parameters to pass to the constructor
   * @return the created component object
   * @throws PlanItException thrown if there is an error
   */
  @SuppressWarnings("unchecked")
  private T createTrafficComponent(final String planitComponentClassName, final Object[] constructorParameters) throws PlanItException {
    final TreeSet<String> eligibleComponentTypes = registeredPlanitComponents.get(componentSuperTypeCanonicalName);
    PlanItException.throwIf(eligibleComponentTypes == null || !eligibleComponentTypes.contains(planitComponentClassName),
        "provided PLANit Component class is not eligible for construction");

    Object instance = ReflectionUtils.createInstance(planitComponentClassName, constructorParameters);
    PlanItException.throwIf(!(instance instanceof PlanitComponent<?>), "provided factory class is not eligible for construction since it is not derived from PLANitComponent<?>");

    return (T) instance;
  }

  /**
   * Dispatch an event on creation of a PLANit component with variable parameters
   *
   * @param newPlanitComponent the PLANit component being created
   * @param parameters         parameter object array to be used by the event
   * @throws PlanItException thrown if there is an exception
   */
  private void dispatchPopulatePlanitComponentEvent(final T newPlanitComponent, final Object[] parameters) throws PlanItException {
    /* when possible use more specific event for user friendly access to event content on listeners */
    /* TODO: type check parameters and issue message when not correct */
    if (newPlanitComponent instanceof MacroscopicNetwork) {
      fireEvent(new PopulateNetworkEvent(this, (MacroscopicNetwork) newPlanitComponent));
    } else if (newPlanitComponent instanceof Zoning) {
      fireEvent(new PopulateZoningEvent(this, (Zoning) newPlanitComponent, (MacroscopicNetwork) parameters[0]));
    } else if (newPlanitComponent instanceof Demands) {
      fireEvent(new PopulateDemandsEvent(this, (Demands) newPlanitComponent, (Zoning) parameters[0], (MacroscopicNetwork) parameters[1]));
    } else if (newPlanitComponent instanceof ServiceNetwork) {
      fireEvent(new PopulateServiceNetworkEvent(this, (ServiceNetwork) newPlanitComponent));
    } else if (newPlanitComponent instanceof RoutedServices) {
      fireEvent(new PopulateRoutedServicesEvent(this, (RoutedServices) newPlanitComponent));
    } else if (newPlanitComponent instanceof InitialLinkSegmentCost) {
      // TODO: probably better if we generalise to initialCost event rather then specialise to link segment as we do now */
      fireEvent(new PopulateInitialLinkSegmentCostEvent(this, (InitialLinkSegmentCost) newPlanitComponent, (String) parameters[0], (MacroscopicNetwork) parameters[1]));
    } else if (newPlanitComponent instanceof AbstractPhysicalCost) {
      fireEvent(new PopulatePhysicalCostEvent(this, (AbstractPhysicalCost) newPlanitComponent));
    } else {
      /* fire generic populate component event, likely third party class, or one that is likely not meant for user listeners to do anything with */
      fireEvent(new PopulateComponentEvent(this, newPlanitComponent, parameters));
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
        final TreeSet<String> treeSet = registeredPlanitComponents.get(currentClass.getCanonicalName());
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
   * Create PLANit component
   *
   * @param planitComponentClassName the derived class name of the PLANit component (without packages)
   * @param constructorParameters    parameters to pass to the constructor
   * @return the created TrafficAssignmentComponent
   * @throws PlanItException thrown if there is an error
   */
  public T create(final String planitComponentClassName, final Object[] constructorParameters) throws PlanItException {
    final T newTrafficComponent = createTrafficComponent(planitComponentClassName, constructorParameters);
    dispatchPopulatePlanitComponentEvent(newTrafficComponent, constructorParameters);
    return newTrafficComponent;
  }

  /**
   * Create PLANit component
   *
   * @param planitComponentClassName the derived class name of the PLANit component (without packages)
   * @param constructorParameters    parameters to pass to the constructor
   * @param eventParameters          object array which contains any data required to create the component
   * @return the created component
   * @throws PlanItException thrown if there is an error
   */
  public T create(final String planitComponentClassName, final Object[] constructorParameters, final Object... eventParameters) throws PlanItException {
    final T newTrafficComponent = createTrafficComponent(planitComponentClassName, constructorParameters);
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
   * @param listener to register
   */
  public void addListener(PlanitComponentListener listener) {
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
