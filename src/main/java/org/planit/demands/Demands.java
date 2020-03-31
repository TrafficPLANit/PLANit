package org.planit.demands;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.planit.exceptions.PlanItException;
import org.planit.od.odmatrix.demand.ODDemandMatrix;
import org.planit.time.TimePeriod;
import org.planit.trafficassignment.TrafficAssignmentComponent;
import org.planit.trafficassignment.TrafficAssignmentComponentFactory;
import org.planit.utils.misc.IdGenerator;
import org.planit.utils.network.physical.Mode;
import org.planit.userclass.TravelerType;
import org.planit.userclass.UserClass;

/**
 * Container class for all demands registered on the project. In PlanIt we
 * assume that all traffic flows between an origin and destination. Hence all
 * demand for a given time period and mode is provided between an origin and
 * destination via ODDemand.
 *
 * Further, unlike other components, we let anyone register ODDemand compatible
 * instances on this class to provide maximum flexibility in the underlying
 * container since depending on the od data different containers might be
 * preferred for optimizing memory usage. Also not all ODDemand instances on the
 * same Demands instance might utilize the same data structure, hence the need
 * to avoid a general approach across all entries within a Demands instance
 *
 * @author markr
 *
 */
public class Demands extends TrafficAssignmentComponent<Demands> implements Serializable {

	/** the logger */
	private static final Logger LOGGER =  Logger.getLogger(Demands.class.getCanonicalName());
	
    // Protected

    /** generated UID */
	private static final long serialVersionUID = 144798248371260732L;
	
	/**
	 * Map of registered User Classes
	 */
	private Map<Long, UserClass> userClassMap;
	
	/**
	 * Map of registered Traveler Types
	 */
	private Map<Long,TravelerType> travelerTypeMap;

	// register to be eligible in PLANit
    static {
        try {
            TrafficAssignmentComponentFactory.registerTrafficAssignmentComponentType(Demands.class);
        } catch (final PlanItException e) {
            e.printStackTrace();
        }
    }

	/**
     * unique identifier for this demand set
     */
    protected long id;

    /**
     * Trip demand matrices
     */
    protected final TreeMap<Long, TreeMap<Mode, ODDemandMatrix>> odDemands;

    /**
     * Constructor
     */
    public Demands() {
        super();
        this.id = IdGenerator.generateId(Demands.class);
        odDemands = new TreeMap<Long, TreeMap<Mode, ODDemandMatrix>>();
        userClassMap = new HashMap<Long, UserClass>();
    }

    /**
     * Register provided odDemand
     *
     * @param timePeriod  the time period for this origin-demand object
     * @param mode the mode for this origin-demand object
     * @param odDemandMatrix  the origin-demand object to be registered
     * @return oldODDemand if there already existed an odDemand for the given mode
     *         and time period, the overwritten entry is returned
     */
    public ODDemandMatrix registerODDemand(final TimePeriod timePeriod, final Mode mode, final ODDemandMatrix odDemandMatrix) {
     	if (!odDemands.containsKey(timePeriod.getId())) {
    		odDemands.put(timePeriod.getId(),  new TreeMap<Mode, ODDemandMatrix>());
    	}
    	return odDemands.get(timePeriod.getId()).put(mode, odDemandMatrix);
    }

    /**
     * Get an ODDemand by mode and time period
     *
     * @param mode
     *            the mode for which the ODDemand object is required
     * @param timePeriod
     *            the time period for which the ODDemand object is required
     * @return ODDemand object if found, otherwise null
     */

    public ODDemandMatrix get(final Mode mode, final TimePeriod timePeriod) throws PlanItException {
    	if (!odDemands.containsKey(timePeriod.getId())) {
    		throw new PlanItException("No demands matrix for time period " + timePeriod.getId());
    	}
        if (odDemands.containsKey(timePeriod.getId()) && odDemands.get(timePeriod.getId()).containsKey(mode)) {
            return odDemands.get(timePeriod.getId()).get(mode);
        } else {
            return null;
        }
    }

    /**
     * Get the set of time periods registered
     *
     * @return Set of registered time periods
     */
    public SortedSet<TimePeriod> getRegisteredTimePeriods() {
    	final Set<Long> keys = odDemands.keySet();
    	final SortedSet<TimePeriod> timePeriods = new TreeSet<TimePeriod>();
    	for (final Long id : keys) {
    		timePeriods.add(TimePeriod.getById(id));
    	}
    	return timePeriods;
    }

    /**
     * Get modes registered for the given time period
     *
     * @param timePeriod
     *            the specified time period
     * @return Set of modes for this time period
     */
    public Set<Mode> getRegisteredModesForTimePeriod(final TimePeriod timePeriod) {
        if (odDemands.containsKey(timePeriod.getId())) {
            return odDemands.get(timePeriod.getId()).keySet();
        } else {
            return null;
        }
    }

    /**
     * Return the id of this demand object
     *
     * @return id of this demand object
     */
    public long getId() {
        return this.id;
    }
    
    /**
     * Register UserClass
     * 
     * @param userClass the UserClass to be registered
     */
    public void registerUserClass(UserClass userClass) {
    	userClassMap.put(userClass.getId(), userClass);
    }
    
    /**
     * Retrieve UserClass by its Id
     * 
     * @param id the Id of the UserClass
     * @return the retrieved UserClass
     */
    public UserClass getUserClassById(long id) {
    	return userClassMap.get(id);
    }
    
    /**
     * Register TravelerType
     * 
     * @param travelerType the TravelerType to be registered
     */
    public void registerTravelerType(TravelerType travelerType) {
    	travelerTypeMap.put(travelerType.getId(), travelerType);
    }
    
    /**
     * Retrieve TravelerType by its Id
     * 
     * @param id the Id of the TravelerType
     * @return the retrieved TravelerType
     */
    public TravelerType getTravelerTypeById(long id) {
    	return travelerTypeMap.get(id);
    }
    
}