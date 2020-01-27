package org.planit.network.virtual;

import java.math.BigInteger;

import org.planit.network.EdgeSegmentImpl;
import org.planit.utils.misc.IdGenerator;

/**
 * The link segment that connects a zone to the physical network is not a
 * physical link segment. However in order to be able to efficiently conduct
 * path searches this connection needs to materialise in a similar form.
 * 
 * to do this we construct ConnectoidLinkSegment instances which are a link
 * segment, but do not have any physical characteristics apart from connecting a
 * zone (via its centroid) to a physical node.
 * 
 * these segments are NOT registered on the network because they are not part of
 * the physical network, instead they are registered on the adopted zoning. they
 * are however injected/connected to the connectoid reference nodes in the
 * network as link segments to provide the above mentioned interface
 * 
 * @author markr
 *
 */
public class ConnectoidSegment extends EdgeSegmentImpl {

    /**
     * unique internal identifier
     */
    protected final int connectoidSegmentId;

    /**
     * generate unique connectoid segment id
     * 
     * @return linkSegmentId
     */
    protected static int generateConnectoidSegmentId() {
        return IdGenerator.generateId(ConnectoidSegment.class);
    }

    // Public

    /**
     * Constructor
     * 
     * @param parentConnectoid  parent connectoid
     * @param directionAb direction of travel
     */
    public ConnectoidSegment(Connectoid parentConnectoid, boolean directionAb) {
        super(parentConnectoid, directionAb);
        BigInteger externalId = parentConnectoid.getExternalId();
        if (externalId != null) {
        	setExternalId(externalId.longValue());
        }
        this.connectoidSegmentId = generateConnectoidSegmentId();
    }

    // Public getters - setters

    public int getConnectoidSegmentId() {
        return connectoidSegmentId;
    }
}