package org.planit.userclass;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.planit.utils.IdGenerator;

/** A Mode is a user class feature representing a single form of transport (car, truck etc.).
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
 * Each mode has a passenger car unit number indicating how many standard passenger cars a single unit of this mode represents
 */
	private final double pcu;
	
/**
 * Id value of this mode	
 */
	private final long id;
	
/**
 * Name of this mode
 */
	private final String name;
	
	private static Map<Long, Mode> modes = new HashMap<Long, Mode>();
	
/** 
 * Constructor
 * 
 * @param name      the name of this mode
 * @param pcu         the PCU value of this mode
 */
	public Mode(String name, double pcu) {
		this.id = IdGenerator.generateId(Mode.class);
		this.name = name;
		this.pcu = pcu;
		modes.put(this.id, this);
	}	
		
/**
 * Constructor
 * 
 * @param id            the id of this mode
 * @param name      the name of this mode
 * @param pcu         the PCU value of this mode
 */
	public Mode(long id, String name, double pcu) {
		this.id = id;
		this.name = name;
		this.pcu = pcu;
		modes.put(this.id, this);
	}	
	
/**
 * Get mode by its id
 * 
 * @param id     id of this mode
 * @return        retrieved mode value
 */
	public static Mode getById(long id) {
		return modes.get(id);
	}
		
/**
 * Register mode
 * 
 * @param mode       mode to be registered
 */
	public static void putById(Mode mode) {
		modes.put(mode.getId(), mode);
	}
	
	public static Collection<Mode> getAllModes() {
		return modes.values();
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

	/** Compare based on id
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Mode o) {
		return (int) (this.getId() - o.getId());
	}


}
