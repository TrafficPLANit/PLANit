package org.planit.userclass;

import java.util.HashMap;
import java.util.Map;

import org.planit.utils.IdGenerator;

/** A Mode is a user class feature representing a single form of transport (car, truck etc.).
 * 
 * @author markr
 */
public class Mode implements Comparable<Mode> {
	
	// Protected
	
	/**
	 * Each mode has a passenger car unit number indicating how many standard passenger cars a single unit of this mode represents
	 */
	private final double pcu;
	private final long id;
	private final String name;
	
	private static Map<Long, Mode> modes = new HashMap<Long, Mode>();
	
	/** Constructor
	 * @param name
	 * @param pcu
	 */
	public Mode(String name, double pcu) {
		this.id = IdGenerator.generateId(Mode.class);
		this.name = name;
		this.pcu = pcu;
		modes.put(this.id, this);
	}	
		
	public Mode(long id, String name, double pcu) {
		this.id = id;
		this.name = name;
		this.pcu = pcu;
		modes.put(this.id, this);
	}	
	
	public static Mode getById(long id) {
		return modes.get(id);
	}
		
	public static void putById(Mode mode) {
		modes.put(mode.getId(), mode);
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
