package org.planit.trafficassignment;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.TreeSet;

import org.planit.cost.physical.BPRLinkTravelTimeCost;
import org.planit.cost.physical.PhysicalCost;
import org.planit.cost.physical.initial.InitialLinkSegmentCost;
import org.planit.cost.physical.initial.InitialPhysicalCost;
import org.planit.cost.virtual.FixedConnectoidTravelTimeCost;
import org.planit.cost.virtual.SpeedConnectoidTravelTimeCost;
import org.planit.cost.virtual.VirtualCost;
import org.planit.event.CreatedProjectComponentEvent;
import org.planit.event.Event;
import org.planit.event.management.EventHandler;
import org.planit.event.management.EventManager;
import org.planit.event.listener.InteractorListener;
import org.planit.exceptions.PlanItException;
import org.planit.logging.PlanItLogger;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.network.physical.macroscopic.MacroscopicNetwork;
import org.planit.demands.Demands;
import org.planit.sdinteraction.smoothing.MSASmoothing;
import org.planit.sdinteraction.smoothing.Smoothing;
import org.planit.supply.fundamentaldiagram.FundamentalDiagram;
import org.planit.supply.network.nodemodel.NodeModel;
import org.planit.supply.networkloading.NetworkLoading;
import org.planit.zoning.Zoning;

/**
 * Generic factory class for registered subclasses of predefined traffic
 * assignment components, so it does not create instances of T but of sublcasses
 * of T.
 * 
 * @author markr
 *
 * @param <T>
 *            generic type of a type traffic assignment component for which we
 *            construct the eligible derived classes by class name
 */
public class TrafficAssignmentComponentFactory<T extends TrafficAssignmentComponent<T>> implements EventHandler {

    /**
     * instance of the super component class this factory creates subclass instances
     * for
     */
    protected final Class<T> componentSuperType;

    /**
     * Event manager used to handle events
     */
    protected EventManager eventManager;

    /**
     * Register per traffic assignment component type the derived classes that are
     * supported
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
    }

    // Currently supported traffic assignment components
    // TODO: make this event based where we register with events rather than this
    // static approach
    // TODO: apply this same concept to local components such as the different types
    // of cost within generalized cost

    static {
        try {
            // Networks
            registerTrafficAssignmentComponentType(MacroscopicNetwork.class);
            // Zoning
            registerTrafficAssignmentComponentType(Zoning.class);
            // Smoothing
            registerTrafficAssignmentComponentType(MSASmoothing.class);
            // Demand
            registerTrafficAssignmentComponentType(Demands.class);
            // Traffic assignment/network loading
            registerTrafficAssignmentComponentType(TraditionalStaticAssignment.class);
            // Physical Cost
            registerTrafficAssignmentComponentType(BPRLinkTravelTimeCost.class);
            // Fixed Virtual Cost
            registerTrafficAssignmentComponentType(FixedConnectoidTravelTimeCost.class);
            // Speed Cost
            registerTrafficAssignmentComponentType(SpeedConnectoidTravelTimeCost.class);
            // Initial Link Segment Cost
            registerTrafficAssignmentComponentType(InitialLinkSegmentCost.class);
        } catch (PlanItException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Create a traffic component with no parameters
     * 
     * @param trafficAssignmentComponentClassName the name of the traffic component to be created
     * @return the created traffic component object
     * @throws PlanItException thrown if there is an error
     */
    @SuppressWarnings("unchecked")
	private T createTrafficComponent(String trafficAssignmentComponentClassName) throws PlanItException {
        TreeSet<String> eligibleComponentTypes = registeredTrafficAssignmentComponents.get(componentSuperType);
        try {
            if (eligibleComponentTypes.contains(trafficAssignmentComponentClassName)) {
                return (T) Class.forName(trafficAssignmentComponentClassName).getConstructor()
                        .newInstance();
            } else {
                throw new PlanItException(
                        "Provided Traffic Assignment Component class is not eligible for construction.");
            }
        } catch (Exception ex) {
            throw new PlanItException(ex);
        }
    }

    /**
     * Dispatch an event on creation of a traffic component with one parameter
     * 
     * @param newTrafficComponent the traffic component being created
     * @param parameter parameter object to be used by the event
     * @throws PlanItException thrown if there is an exception
     */
    private void dispatchTrafficComponentEvent(T newTrafficComponent, Object parameter) throws PlanItException {
        newTrafficComponent.setEventManager(eventManager);
        registerEligibleInteractorListener(newTrafficComponent);

        Event event = new CreatedProjectComponentEvent<T>(newTrafficComponent, parameter);
        eventManager.dispatchEvent(event);
    }

    /**
     * Dispatch an event on creation of a traffic component with two parameters
     * 
     * @param newTrafficComponent the traffic component being created
     * @param parameter1 first parameter object to be used by the event
     * @param parameter2 second parameter object to be used by the event
     * @throws PlanItException thrown if there is an error
     */
    private void dispatchTrafficComponentEvent(T newTrafficComponent, Object parameter1, Object parameter2) throws PlanItException {
        newTrafficComponent.setEventManager(eventManager);
        registerEligibleInteractorListener(newTrafficComponent);
        Event event = new CreatedProjectComponentEvent<T>(newTrafficComponent, parameter1, parameter2);
        eventManager.dispatchEvent(event);
    }

    /**
     * If the provided traffic component is an interactive listener, it will now be
     * registered as such
     * 
     * @param newTrafficComponent
     *            TrafficComponent to be registered as a listener
     */
    protected void registerEligibleInteractorListener(T newTrafficComponent) {
        if (newTrafficComponent instanceof InteractorListener) {
            eventManager.addEventListener((InteractorListener) newTrafficComponent);
        }
    }

    // PUBLIC

    /**
     * Constructor
     * 
     * @param componentSuperType
     *            super type for this factory
     */
    public TrafficAssignmentComponentFactory(Class<T> componentSuperType) {
        this.componentSuperType = componentSuperType;
    }

    /**
     * Register a component type that one can choose for the given traffic component
     * 
     * @param trafficAssignmentComponent
     *            TrafficAssignmentComponent to be registered
     * @throws PlanItException
     *             thrown if there is an error
     */
    public static void registerTrafficAssignmentComponentType(final Class<?> trafficAssignmentComponent)
            throws PlanItException {
        Class<?> currentClass = trafficAssignmentComponent;
        while (currentClass != null) {
            Type currentSuperClass = currentClass.getGenericSuperclass();
            if (currentSuperClass instanceof ParameterizedType
                    && ((ParameterizedType) currentSuperClass).getRawType() == TrafficAssignmentComponent.class) {
                // superclass is a trafficAssignmentComponent class, so the current class is the
                // class that we need
                // register by collecting the component entry and placing the component
                TreeSet<String> treeSet = registeredTrafficAssignmentComponents.get(currentClass);
                treeSet.add(trafficAssignmentComponent.getCanonicalName());
                registeredTrafficAssignmentComponents.get(currentClass)
                        .add(trafficAssignmentComponent.getCanonicalName());
                return;
            } else {
                currentClass = currentClass.getSuperclass(); // move up the hierarchy
            }
        }
        throw new PlanItException("trafficAssignmentComponent not eligible for registration");
    }

    /**
     * Create traffic assignment component
     * 
     * @param trafficAssignmentComponentClassName
     *            the derived class name of the traffic assignment component
     *            (without packages)
     * @return the created TrafficAssignmentComponent
     * @throws PlanItException
     *             thrown if there is an error
     */
    public T create(String trafficAssignmentComponentClassName) throws PlanItException {
    	return create(trafficAssignmentComponentClassName, null);
    }
    
    /**
     * Create traffic assignment component
     * 
     * @param trafficAssignmentComponentClassName
     *            the derived class name of the traffic assignment component
     *            (without packages)
     * @param parameter object which contains any data required to create the component
     * @return the created TrafficAssignmentComponent
     * @throws PlanItException
     *             thrown if there is an error
     */
    public T create(String trafficAssignmentComponentClassName, Object parameter) throws PlanItException {
    	T newTrafficComponent = createTrafficComponent(trafficAssignmentComponentClassName);
    	PlanItLogger.info("Created " + trafficAssignmentComponentClassName + ", dispatching creation event" );
    	dispatchTrafficComponentEvent(newTrafficComponent, parameter);
        return newTrafficComponent;
    }

    /**
     * Create traffic assignment component
     * 
     * @param trafficAssignmentComponentClassName
     *            the derived class name of the traffic assignment component
     *            (without packages)
     * @param parameter1 object which contains any data required to create the component
     * @param parameter2 object which contains any data required to create the component
     * @return the created TrafficAssignmentComponent
     * @throws PlanItException
     *             thrown if there is an error
     */
   public T create(String trafficAssignmentComponentClassName, Object parameter1, Object parameter2) throws PlanItException {
    	T newTrafficComponent = createTrafficComponent(trafficAssignmentComponentClassName);
    	PlanItLogger.info("Created " + trafficAssignmentComponentClassName + ", dispatching creation event" );
    	dispatchTrafficComponentEvent(newTrafficComponent, parameter1, parameter2);
        return newTrafficComponent;
    }

   /**
     * Set the EventManager for this factory
     * 
     * The EventManager must be a singleton for each PlanItProject application.
     * 
     * @param eventManager
     *            EventManager to be used to create traffic assignment
     */
    @Override
    public void setEventManager(EventManager eventManager) {
        this.eventManager = eventManager;
    }

}