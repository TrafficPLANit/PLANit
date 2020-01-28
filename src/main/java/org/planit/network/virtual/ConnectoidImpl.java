package org.planit.network.virtual;

import javax.annotation.Nonnull;
import org.planit.exceptions.PlanItException;
import org.planit.graph.EdgeImpl;
import org.planit.utils.misc.IdGenerator;
import org.planit.utils.network.physical.Node;
import org.planit.utils.network.virtual.Centroid;
import org.planit.utils.network.virtual.Connectoid;
import org.planit.utils.network.virtual.ConnectoidSegment;

/**
 * connectoid connecting a zone to the physical road network, carrying two
 * connectoid segments in one or two directions which may carry additional
 * information for each particular direction of the connectoid.
 * 
 * @author markr
 *
 */
public class ConnectoidImpl extends EdgeImpl implements Connectoid {
      
    
    // Protected

    /**
     * unique internal identifier
     */
    protected final long connectoidId;

    /**
     * External Id of the connectoid
     */
    protected long externalId = Long.MIN_VALUE;

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
    public ConnectoidImpl(@Nonnull Centroid centroidA, @Nonnull Node nodeB, double length, long externalId) throws PlanItException {
        super(centroidA, nodeB, length);
        this.connectoidId = generateConnectoidId();
        setExternalId(externalId);
    }
    
    /**
     * Constructor
     * 
     * @param centroidA  the centroid at one end of the connectoid
     * @param nodeB the node at the other end of the connectoid
     * @param length  length of the current connectoid
     * @throws PlanItException thrown if there is an error
     */
    public ConnectoidImpl(@Nonnull Centroid centroidA, @Nonnull Node nodeB, double length) throws PlanItException {
        super(centroidA, nodeB, length);
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
    @Override
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
    @Override
	public long getConnectoidId() {
        return connectoidId;
    }

    @Override
	public long getExternalId() {
		return externalId;
	}

	@Override
	public void setExternalId(long externalId) {
		this.externalId = externalId;
	}
}