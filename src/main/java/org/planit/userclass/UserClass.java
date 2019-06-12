package org.planit.userclass;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.planit.utils.IdGenerator;

/**
 * A user class defines a combination of one or more characteristics of users in
 * an aggregate representation of traffic which partially dictate how they
 * behave in traffic assignment.
 * 
 * @author markr
 *
 */
public class UserClass {

    /**
     * Logger for this class
     */
    private static final Logger LOGGER = Logger.getLogger(UserClass.class.getName());
    
    public static final String  DEFAULT_NAME = "Default";
    public static final int      DEFAULT_EXTERNAL_ID = 1;   
    public static final int       DEFAULT_MODE_REF = 1;
    public static final int       DEFAULT_TRAVELLER_TYPE = 1;
    
    /**
     * id of this user class
     */
    private long id;

    /**
     * Name of this user class
     */
    private String name;

    /**
     * Mode of travel of this user class
     */
    private Mode mode;

    /**
     * Traveller type of this user class
     */
    private TravelerType travellerType;

    /**
     * External Id of mode of travel
     */
    private long modeExternalId = 0;

    /**
     * Id of this traveller type
     */
    private long travellerTypeId = 0;

    /**
     * Map to store registered user classes
     */
    private static Map<Long, UserClass> userClasses = new HashMap<Long, UserClass>();

    /**
     * Constructor of user class
     * 
     * @param name
     *            the name of this user class
     * @param mode
     *            the mode of travel
     * @param travellerType
     *            the traveller type
     */
    public UserClass(String name, Mode mode, TravelerType travellerType) {
        this.id = IdGenerator.generateId(UserClass.class);
        this.name = name;
        this.mode = mode;
        this.travellerType = travellerType;
        userClasses.put(this.id, this);
    }

    /**
     * Constructor of user class
     * 
     * @param id
     *            id of this user class
     * @param name
     *            the name of this user class
     * @param mode
     *            the mode of travel
     * @param travellerType
     *            the traveller type
     */
    public UserClass(long id, String name, Mode mode, TravelerType travellerType) {
        this.id = id;
        this.name = name;
        this.mode = mode;
        this.travellerType = travellerType;
        userClasses.put(this.id, this);
    }

    /**
     * Constructor of user class
     * 
     * @param id
     *            id of this user class
     * @param name
     *            name of this user class
     * @param modeExternalId
     *            external id of mode of travel
     * @param travellerTypeId
     *            if of traveller type
     */
    public UserClass(long id, String name, long modeExternalId, long travellerTypeId) {
        this.id = id;
        this.name = name;
        this.modeExternalId = modeExternalId;
        this.travellerTypeId = travellerTypeId;
        this.mode = Mode.getByExternalId(modeExternalId);
        this.travellerType = TravelerType.getByExternalId(travellerTypeId);
        userClasses.put(this.id, this);
    }

    /**
     * Retrieve user class by id
     * 
     * @param id
     *            id of user class to be retrieve
     * @return retrieved user class
     */
    public static UserClass getById(long id) {
        return userClasses.get(id);
    }

    /**
     * Store user class by its id
     * 
     * @param userClass
     *            the user class to be stored
     */
    public static void putById(UserClass userClass) {
        userClasses.put(userClass.getId(), userClass);
    }

    /**
     * Get the traveller type of this user class
     * 
     * @return TravellerType of this user class
     */
    public TravelerType getTravellerType() {
        return travellerType;
    }

    /**
     * Get the id of this user class
     * 
     * @return id of this user class
     */
    public long getId() {
        return id;
    }

    /**
     * Get the name of this user class
     * 
     * @return the name of this user class
     */
    public String getName() {
        return name;
    }

    /**
     * Return the mode of travel of this user class
     * 
     * @return Mode of this user class
     */
    public Mode getMode() {
        return mode;
    }

    /**
     * Return the id of the mode of this user class
     * 
     * @return id of the mode of this user class
     */
    public long getModeExternalId() {
        return modeExternalId;
    }

    /**
     * Return the id of the traveller type of this user class
     * 
     * @return id of the traveller type of this user class
     */
    public long getTravellerTypeId() {
        return travellerTypeId;
    }

}
