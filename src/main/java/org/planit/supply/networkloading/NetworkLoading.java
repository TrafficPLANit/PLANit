package org.planit.supply.networkloading;

import java.io.Serializable;

import org.planit.trafficassignment.TrafficAssignmentComponent;
import org.planit.utils.misc.IdGenerator;

/**
 * Network loading traffic component
 *
 * @author markr
 *
 */
public abstract class NetworkLoading extends TrafficAssignmentComponent<NetworkLoading> implements Serializable   {

	/** generated UID */
	private static final long serialVersionUID = 6213911562665516698L;

	/**
	 * Unique id
	 */
	protected final long id;

	/**
	 * Base contructor
	 */
	public NetworkLoading() {
		super();
		this.id = IdGenerator.generateId(NetworkLoading.class);
	}

	/**
	 * #{@inheritDoc}
	 */
	@Override
	public long getId() {
		return id;
	}

}
