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
    public static final String DEFAULT_NAME = "Default";
    
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
     * Map to store registered traveler types
     */
    private static Map<Long, TravelerType> travelerTypes = new HashMap<Long, TravelerType>();
    
    /**
     * Constructor
     * 
     * @param id
     *            id of this traveler type
     * @param name
     *            name of this traveler type
     */
    public TravelerType() {
        this.id = IdGenerator.generateId(TravelerType.class);
        this.name = DEFAULT_NAME;
        this.externalId = EXTERNAL_ID;
        travelerTypes.put(this.externalId, this);
    }    

    /**
     * Constructor
     * 
     * @param id
     *            id of this traveler type
     * @param name
     *            name of this traveler type
     */
    public TravelerType(long externalId, String name) {
        this.id = IdGenerator.generateId(TravelerType.class);
        this.externalId = externalId;
        this.name = name;
        travelerTypes.put(this.externalId, this);
    }

    /**
     * Constructor
     * 
     * @param name
     *            name of this traveler type
     */
    public TravelerType(String name) {
        this.id = IdGenerator.generateId(TravelerType.class);
        this.externalId = EXTERNAL_ID;
        this.name = name;        
        travelerTypes.put(this.externalId, this);
    }

    /**
     * Get traveler type by externalId
     * 
     * @param externalId
     *            external id of this traveler type
     * @return retrieved TravellerType object
     */
    public static TravelerType getByExternalId(long externalId) {
        return travelerTypes.get(externalId);
    }

    /**
     * Store traveler type by its external id
     * 
     * @param travelerType
     *            TravelerType object to store
     */
    public static void putByExternalId(TravelerType travellerType) {
        travelerTypes.put(travellerType.getExternalId(), travellerType);
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
