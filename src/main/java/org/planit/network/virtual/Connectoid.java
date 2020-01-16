package org.planit.network.virtual;

import java.math.BigInteger;

import javax.annotation.Nonnull;
import org.planit.exceptions.PlanItException;
import org.planit.network.Edge;
import org.planit.network.physical.Node;
import org.planit.utils.IdGenerator;

/**
 * connectoid connecting a zone to the physical road network, carrying two
 * connectoid segments in one or two directions which may carry additional
 * information for each particular direction of the connectoid.
 * 
 * @author markr
 *
 */
public class Connectoid extends Edge {

    /**
     * Default connectoid length
     */
    public static final double DEFAULT_LENGTH = 1.0;    
    
    // Protected

    /**
     * unique internal identifier
     */
    protected final long connectoidId;

    /**
     * External Id of the connectoid (can be null, in which case the external Id was not set in the input file
     */
    protected BigInteger externalId;

    /**
     * Generate connectoid id
     * 
     * @return id of connectoid
     */
    protected static int generateConnectoidId() {
        return IdGenerator.generateId(Connectoid.class);
    }
    
    // Public

    /**
     * Constructor
     * 
     * @param centroidA  the centroid at one end of the connectoid
     * @param nodeB the node at the other end of the connectoid
     * @param length  length of the current connectoid
     * @param externalId externalId of the connectoid (can be null, in which case this has not be set in the input files)
     * @throws PlanItException thrown if there is an error
     */
    public Connectoid(@Nonnull Centroid centroidA, @Nonnull Node nodeB, double length, BigInteger externalId) throws PlanItException {
        super(centroidA, nodeB, length);
        setExternalId(externalId);
        this.connectoidId = generateConnectoidId();
    }

	/**
     * Register connectoidSegment.
     * 
     * If there already exists a connectoidSegment for that direction it is replaced
     * and returned
     * 
     * @param connectoidSegment
     *            connectoid segment to be registered
     * @param directionAB
     *            direction of travel
     * @return replaced ConnectoidSegment
     * @throws PlanItException
     *             thrown if there is an error
     */
    public ConnectoidSegment registerConnectoidSegment(ConnectoidSegment connectoidSegment, boolean directionAB)
            throws PlanItException {
        return (ConnectoidSegment) registerEdgeSegment(connectoidSegment, directionAB);
    }

    // Getters-Setters

    /**
     * 
     * Return the id of this connectoid
     * 
     * @return id of this connectoid
     */
    public long getConnectoidId() {
        return connectoidId;
    }

    public BigInteger getExternalId() {
		return externalId;
	}

	public void setExternalId(BigInteger externalId) {
		this.externalId = externalId;
	}
}