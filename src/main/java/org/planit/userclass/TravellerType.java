package org.planit.userclass;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.planit.utils.IdGenerator;

/**
 * Traveler type is a placeholder for all different types of traveler characteristics that affect the user class in
 * the route choice component of traffic assignment. Together with the mode this largely defines each user class
 * TODO: Not used yet in UserClass class
 *  
 * @author markr
 *
 */
public class TravellerType {

    /**
     * Logger for this class
     */
    private static final Logger LOGGER = Logger.getLogger(TravellerType.class.getName());
        
	/**
	 * Unique feature id
	 */
	private final long id;
	
/**
 * Name of this traveller type
 */
	private final String name;
	
/**
 * Map to store registered traveller types
 */
	private static Map<Long, TravellerType> travellerTypes = new HashMap<Long, TravellerType>();
	
/**
 * Constructor
 * 
 * @param id               id of this traveller type
 * @param name        name of this traveller type
 */
	public TravellerType(long id, String name) {
		this.id = id;
		this.name = name;
		travellerTypes.put(this.id, this);
	}

/**
 * Constructor
 * 
 * @param name     name of this traveller type
 */
	public TravellerType(String name) {
		this.id = IdGenerator.generateId(TravellerType.class);
		this.name = name;
		travellerTypes.put(this.id, this);
	}
	
/**
 * Get traveller type by id
 * 
 * @param id        id of this traveller type
 * @return           retrieved TravellerType object
 */
	public static TravellerType getById(long id) {
		return travellerTypes.get(id);
	}
	
/**
 * Store traveller type by its id
 * 
 * @param travellerType       TravellerType object to store
 */
	public static void putById(TravellerType travellerType) {
		travellerTypes.put(travellerType.getId(), travellerType);
	}
	
	public long getId() {
		return id;
	}

	public String getName() {
		return name;
	}
}
