package org.planit.userclass;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.planit.utils.IdGenerator;

/**
 * Traveler type is a placeholder for all different types of traveler
 * characteristics that affect the user class in the route choice component of
 * traffic assignment. Together with the mode this largely defines each user
 * class TODO: Not used yet in UserClass class
 * 
 * @author markr
 *
 */
public class TravelerType {

    /**
     * Logger for this class
     */
    private static final Logger LOGGER = Logger.getLogger(TravelerType.class.getName());
        
    /**
     * Default name of a traveler type
     */
    public static final String NAME = "Default";
    
    /**
     * Default value of the external id
     */
    public static final long EXTERNAL_ID = -1;    

    /**
     * Unique feature id
     */
    private final long id;
    
    /**
     * Unique external id
     */
    private final long externalId;    

    /**
     * Name of this traveler type
     */
    private final String name;

    /**
     * Map to store registered traveller types
     */
    private static Map<Long, TravelerType> travelerTypes = new HashMap<Long, TravelerType>();
    
    /**
     * Constructor
     * 
     * @param id
     *            id of this traveller type
     * @param name
     *            name of this traveller type
     */
    public TravelerType() {
        this.id = IdGenerator.generateId(TravelerType.class);
        this.name = NAME;
        this.externalId = EXTERNAL_ID;
        travelerTypes.put(this.id, this);
    }    

    /**
     * Constructor
     * 
     * @param id
     *            id of this traveller type
     * @param name
     *            name of this traveller type
     */
    public TravelerType(long externalId, String name) {
        this.id = IdGenerator.generateId(TravelerType.class);
        this.externalId = externalId;
        this.name = name;
        travelerTypes.put(this.id, this);
    }

    /**
     * Constructor
     * 
     * @param name
     *            name of this traveller type
     */
    public TravelerType(String name) {
        this.id = IdGenerator.generateId(TravelerType.class);
        this.externalId = EXTERNAL_ID;
        this.name = name;        
        travelerTypes.put(this.id, this);
    }

    /**
     * Get traveller type by id
     * 
     * @param id
     *            id of this traveller type
     * @return retrieved TravellerType object
     */
    public static TravelerType getById(long id) {
        return travelerTypes.get(id);
    }

    /**
     * Store traveller type by its id
     * 
     * @param travellerType
     *            TravellerType object to store
     */
    public static void putById(TravelerType travellerType) {
        travelerTypes.put(travellerType.getId(), travellerType);
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public long getExternalId() {
        return externalId;
    }
}
