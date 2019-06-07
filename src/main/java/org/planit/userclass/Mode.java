package org.planit.userclass;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.planit.utils.IdGenerator;

/**
 * A Mode is a user class feature representing a single form of transport (car,
 * truck etc.).
 * 
 * @author markr
 */
public class Mode implements Comparable<Mode> {

    /**
     * Logger for this class
     */
    private static final Logger LOGGER = Logger.getLogger(Mode.class.getName());

    // Protected

    /**
     * Each mode has a passenger car unit number indicating how many standard
     * passenger cars a single unit of this mode represents
     */
    private final double pcu;

    /**
     * Id value of this mode
     */
    private final long id;
    
    /**
     * External Id of this mode
     */
    private long externalId;

    /**
     * Name of this mode
     */
    private final String name;

    private static Map<Long, Mode> modesById = new HashMap<Long, Mode>();
    
    private static Map<Long, Mode> modesByExternalId = new HashMap<Long, Mode>();

    /**
     * Constructor
     * 
     * @param name
     *            the name of this mode
     * @param pcu
     *            the PCU value of this mode
     */
    public Mode(String name, double pcu) {
        this.id = IdGenerator.generateId(Mode.class);
        this.name = name;
        this.pcu = pcu;
        modesById.put(this.id, this);
    }

    /**
     * Constructor
     * 
     * @param id
     *            the id of this mode
     * @param name
     *            the name of this mode
     * @param pcu
     *            the PCU value of this mode
     */
    public Mode(long externalId, String name, double pcu) {
        this.id = IdGenerator.generateId(Mode.class);
        this.externalId = externalId;
        this.name = name;
        this.pcu = pcu;
        modesById.put(this.id, this);
        modesByExternalId.put(this.externalId, this);
    }
    
    /** Collect the external ids across all available modes
     * @return set of externalIds
     */
    public static Set<Long> getExternalIdSet() {
    	return modesByExternalId.keySet();
    }

    /**
     * Get mode by its id
     * 
     * @param id
     *            id of this mode
     * @return retrieved mode value
     */
    public static Mode getById(long id) {
        return modesById.get(id);
    }
    
    /**
     * Get mode by its external Id
     * 
     * @param externalId external Id of this mode
     * @return retrieved mode value
     */
    public static Mode getByExternalId(long externalId) {
    	return modesByExternalId.get(externalId);
    }

    /**
     * Register mode
     * 
     * @param mode
     *            mode to be registered
     */
    public static void putById(Mode mode) {
    	modesById.put(mode.getId(), mode);
    	modesByExternalId.put(mode.getExternalId(), mode);
    }

    public static Collection<Mode> getAllModes() {
        return modesById.values();
    }

    // getters-setters

    public double getPcu() {
        return pcu;
    }

    public String getName() {
        return name;
    }

    public long getId() {
        return id;
    }
    
    public long getExternalId() {
    	return externalId;
    }

    /**
     * Compare based on id
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(Mode o) {
        return (int) (this.getId() - o.getId());
    }

}
