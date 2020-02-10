package org.planit.network.virtual;

import org.planit.graph.EdgeSegmentImpl;
import org.planit.utils.misc.IdGenerator;
import org.planit.utils.network.virtual.Connectoid;
import org.planit.utils.network.virtual.ConnectoidSegment;

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
public class ConnectoidSegmentImpl extends EdgeSegmentImpl implements ConnectoidSegment {

    /** generated UID */
	private static final long serialVersionUID = 6462304338451088764L;

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
    public ConnectoidSegmentImpl(final Connectoid parentConnectoid, final boolean directionAb) {
        super(parentConnectoid, directionAb);
        this.connectoidSegmentId = generateConnectoidSegmentId();
    }

    // Public getters - setters

    @Override
	public int getConnectoidSegmentId() {
        return connectoidSegmentId;
    }
}