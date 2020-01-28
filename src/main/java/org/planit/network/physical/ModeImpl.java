package org.planit.network.physical;

import java.util.HashMap;
import java.util.Map;
import org.planit.utils.misc.IdGenerator;
import org.planit.utils.network.physical.Mode;

/**
 * A Mode is a user class feature representing a single form of transport (car,
 * truck etc.).
 * 
 * @author markr
 */
public class ModeImpl implements Mode {
	
	private final long DEFAULT_EXTERNAL_ID = 1; 
	
	private static Map<Long, Mode> modesById = new HashMap<Long, Mode>();

	private static Map<Long, Mode> modesByExternalId = new HashMap<Long, Mode>();

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

	/**
	 * Constructor
	 * 
	 * @param name the name of this mode
	 * @param pcu  the PCU value of this mode
	 */

	public ModeImpl(String name, double pcu) {
		this.id = IdGenerator.generateId(Mode.class);
		this.externalId = DEFAULT_EXTERNAL_ID;
		this.name = name;
		this.pcu = pcu;
		modesById.put(this.id, this);
	}

	/**
	 * Constructor
	 * 
	 * @param externalId the externalId of this mode
	 * @param name       the name of this mode
	 * @param pcu        the PCU value of this mode
	 */
	public ModeImpl(long externalId, String name, double pcu) {
		this.id = IdGenerator.generateId(ModeImpl.class);
		this.externalId = externalId;
		this.name = name;
		this.pcu = pcu;
		modesById.put(this.id, this);
		modesByExternalId.put(this.externalId, this);
	}
	
	// getters-setters

	@Override
	public double getPcu() {
		return pcu;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public long getId() {
		return id;
	}

	@Override
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