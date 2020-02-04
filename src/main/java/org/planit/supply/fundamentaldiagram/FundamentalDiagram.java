package org.planit.supply.fundamentaldiagram;

import java.io.Serializable;

import org.planit.trafficassignment.TrafficAssignmentComponent;
import org.planit.utils.misc.IdGenerator;

/**
 * Fundamental diagram traffic component
 *
 * @author markr
 *
 */
public abstract class FundamentalDiagram extends TrafficAssignmentComponent<FundamentalDiagram> implements Serializable {

    /** generated UID */
	private static final long serialVersionUID = 5815100111048623093L;

	/**
     * unique identifier
     */
    protected final long id;

	/**
     * Base constructor
     */
    public FundamentalDiagram() {
        super();
        this.id = IdGenerator.generateId(FundamentalDiagram.class);
    }

    /**
     * #{@inheritDoc}
     */
	@Override
	public long getId() {
		return this.id;
	}

}
