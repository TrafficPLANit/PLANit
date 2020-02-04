package org.planit.trafficassignment;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.TreeSet;

import org.djutils.event.EventProducer;
import org.djutils.event.EventType;
import org.planit.cost.physical.PhysicalCost;
import org.planit.cost.physical.initial.InitialPhysicalCost;
import org.planit.cost.virtual.VirtualCost;
import org.planit.demands.Demands;
import org.planit.exceptions.PlanItException;
import org.planit.logging.PlanItLogger;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.network.virtual.Zoning;
import org.planit.route.RouteChoice;
import org.planit.sdinteraction.smoothing.Smoothing;
import org.planit.supply.fundamentaldiagram.FundamentalDiagram;
import org.planit.supply.network.nodemodel.NodeModel;
import org.planit.supply.networkloading.NetworkLoading;

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
public class TrafficAssignmentComponentFactory<T extends TrafficAssignmentComponent<T> & Serializable> extends EventProducer implements Serializable {

	/** generated UID */
	private static final long serialVersionUID = -4507287133047792042L;

	/** event type fired off when a new traffic assignment component is created */
	public static final EventType TRAFFICCOMPONENT_CREATE = new EventType("TRAFFICCOMPONENT.CREATE");

    /** instance of the super component class this factory creates subclass instances for */
    protected final Class<T> componentSuperType;

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
        registeredTrafficAssignmentComponents.put(RouteChoice.class, new TreeSet<>());
    }

    /**
     * Create a traffic component with no parameters
     *
     * @param trafficAssignmentComponentClassName the name of the traffic component to be created
     * @return the created traffic component object
     * @throws PlanItException thrown if there is an error
     */
    @SuppressWarnings("unchecked")
	private T createTrafficComponent(final String trafficAssignmentComponentClassName) throws PlanItException {
        final TreeSet<String> eligibleComponentTypes = registeredTrafficAssignmentComponents.get(componentSuperType);
        try {
            if (eligibleComponentTypes.contains(trafficAssignmentComponentClassName)) {
                return (T) Class.forName(trafficAssignmentComponentClassName).getConstructor().newInstance();
            } else {
                throw new PlanItException("Provided Traffic Assignment Component class is not eligible for construction.");
            }
        } catch (final Exception ex) {
            throw new PlanItException(ex);
        }
    }

    /**
     * Dispatch an event on creation of a traffic component with variable parameters
     *
     * @param newTrafficComponent the traffic component being created
     * @param parameters parameter object array to be used by the event
     * @throws PlanItException thrown if there is an exception
     */
    private void dispatchTrafficComponentEvent(final T newTrafficComponent, final Object[] parameters) throws PlanItException {        fireEvent(new org.djutils.event.Event(TRAFFICCOMPONENT_CREATE, this, new Object[] {newTrafficComponent, parameters}));
    }

    // PUBLIC

    /**
     * Constructor
     *
     * @param componentSuperType
     *            super type for this factory
     */
    public TrafficAssignmentComponentFactory(final Class<T> componentSuperType) {
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
    @SuppressWarnings("unchecked")
	public static void registerTrafficAssignmentComponentType(final Class<? extends TrafficAssignmentComponent<?>> trafficAssignmentComponent)
            throws PlanItException {
        Class<? extends TrafficAssignmentComponent<?>> currentClass = trafficAssignmentComponent;
        while (currentClass != null) {
            final Type currentSuperClass = currentClass.getGenericSuperclass();
            if (currentSuperClass instanceof ParameterizedType
                    && ((ParameterizedType) currentSuperClass).getRawType() == TrafficAssignmentComponent.class) {
                // superclass is a trafficAssignmentComponent class, so the current class is the
                // class that we need
                // register by collecting the component entry and placing the component
                final TreeSet<String> treeSet = registeredTrafficAssignmentComponents.get(currentClass);
                treeSet.add(trafficAssignmentComponent.getCanonicalName());
                registeredTrafficAssignmentComponents.get(currentClass).add(trafficAssignmentComponent.getCanonicalName());
                return;
            } else {
            	currentClass = (Class<? extends TrafficAssignmentComponent<?>>) currentClass.getSuperclass(); // move up the hierarchy
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
    public T create(final String trafficAssignmentComponentClassName) throws PlanItException {
    	return create(trafficAssignmentComponentClassName, null);
    }

    /**
     * Create traffic assignment component
     *
     * @param trafficAssignmentComponentClassName
     *            the derived class name of the traffic assignment component
     *            (without packages)
     * @param parameters object array which contains any data required to create the component
     * @return the created TrafficAssignmentComponent
     * @throws PlanItException
     *             thrown if there is an error
     */
    public T create(final String trafficAssignmentComponentClassName, final Object[] parameters) throws PlanItException {
    	final T newTrafficComponent = createTrafficComponent(trafficAssignmentComponentClassName);
    	PlanItLogger.info("Created " + trafficAssignmentComponentClassName + ", dispatching creation event" );
    	dispatchTrafficComponentEvent(newTrafficComponent, parameters);
        return newTrafficComponent;
    }

	/**
	 * sourceId provides information of the source of an event fired from this class instance
	 *
	 * @reutn this class instance
	 */
	@Override
	public Serializable getSourceId() {
		return this;
	}

}