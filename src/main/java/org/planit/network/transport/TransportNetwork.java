package org.planit.network.transport;

import java.util.Iterator;

import javax.annotation.Nonnull;

import org.planit.exceptions.PlanItException;
import org.planit.network.Edge;
import org.planit.network.EdgeSegment;
import org.planit.network.physical.Link;
import org.planit.network.physical.LinkSegment;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.network.physical.PhysicalNetwork.LinkSegments;
import org.planit.network.virtual.Connectoid;
import org.planit.network.virtual.ConnectoidSegment;
import org.planit.network.virtual.VirtualNetwork;
import org.planit.network.virtual.VirtualNetwork.ConnectoidSegments;
import org.planit.zoning.Zoning;
import org.planit.zoning.Zoning.Zones;

/**
 * Entire transport network that is being modeled including both the physical
 * and virtual aspects of it as well as the zoning. It acts as a wrapper
 * unifying the two components during the assignment stage
 * 
 * @author markr
 *
 */
public class TransportNetwork {

    /**
     * Custom iterator over all registered edge segments by combining the physical
     * and virtual edge segments
     * 
     * @author markr
     *
     */
    public class TransportSegmentIterator implements Iterator<EdgeSegment> {

        private final Iterator<LinkSegment> physicalIterator = physicalNetwork.linkSegments.iterator();
        private final Iterator<ConnectoidSegment> virtualIterator = zoning.getVirtualNetwork().connectoidSegments
                .iterator();

        private Iterator<?> currentIterator;

        TransportSegmentIterator() {
            currentIterator = physicalIterator.hasNext() ? physicalIterator : virtualIterator;
        }

        /**
         * Iterator through registered edge segments
         * 
         * @return true if there are more EdgeSegment objects to loop through, false
         *         otherwise
         */
        @Override
        public boolean hasNext() {
            if (currentIterator.hasNext()) {
                return true;
            } else if (!currentIterator.equals(virtualIterator)) {
                currentIterator = virtualIterator;
            }
            return currentIterator.hasNext();
        }

        /**
         * Return next EdgeSegment object
         * 
         * @return next EdgeSegment object
         */
        @Override
        public EdgeSegment next() {
            return (EdgeSegment) currentIterator.next();
        }
    }

    /**
     * Holds the physical road network that is being modeled
     */
    protected final PhysicalNetwork physicalNetwork;

    /**
     * Holds the zoning structure and virtual transport network interfacing with the
     * physical network
     */
    protected final Zoning zoning;

    /**
     * Add edge segment to the incoming or outgoing set of edge segments for the
     * related vertices
     * 
     * @param edgeSegment
     *            EdgeSegment to be added to upstream and downstream vertices
     * @throws PlanItException
     *             thrown if there is an error
     */
    protected void connectVerticesToEdgeSegment(EdgeSegment edgeSegment) throws PlanItException {
        edgeSegment.getUpstreamVertex().exitEdgeSegments.addEdgeSegment(edgeSegment);
        edgeSegment.getDownstreamVertex().entryEdgeSegments.addEdgeSegment(edgeSegment);
    }

    /**
     * Remove edge segment from the incoming or outgoing set of edge segments for
     * the related vertices
     * 
     * @param edgeSegment
     *            the EdgeSegment object to be removed from the network
     * @throws PlanItException
     *             thrown if there is an error
     */
    protected void disconnectVerticesFromEdgeSegment(EdgeSegment edgeSegment) throws PlanItException {
        edgeSegment.getUpstreamVertex().exitEdgeSegments.removeEdgeSegment(edgeSegment);
        edgeSegment.getDownstreamVertex().entryEdgeSegments.removeEdgeSegment(edgeSegment);
    }

    /**
     * Add Edge to both vertices
     * 
     * @param edge
     *            Edge to be added to upstream and downstream vertices
     * @throws PlanItException
     *             thrown if there is an error
     */
    protected void connectVerticesToEdge(Edge edge) throws PlanItException {
        edge.getVertexA().edges.addEdge(edge);
        edge.getVertexB().edges.addEdge(edge);
    }

    /**
     * Remove Edge from both vertices
     * 
     * @param edge
     *            Edge to be removed from upstream and downstream vertices
     * @throws PlanItException
     *             thrown if there is an error
     */
    protected void disconnectVerticesFromEdge(Edge edge) throws PlanItException {
        edge.getVertexA().edges.removeEdge(edge);
        edge.getVertexB().edges.removeEdge(edge);
    }

    // Public

    /**
     * Reference to virtual edge segments
     */
    public final ConnectoidSegments connectoidSegments;

    /**
     * Reference to physical edge segments
     */
    public final LinkSegments linkSegments;

    /**
     * Reference to zones
     */
    public final Zones zones;

    /**
     * Constructor
     * 
     * @param physicalNetwork
     *            the PhysicalNetwork used to generate this TransportNetwork
     * @param zoning
     *            the Zoning used to generate this TransportNetwork
     */
    public TransportNetwork(@Nonnull PhysicalNetwork physicalNetwork, @Nonnull Zoning zoning) {
        this.physicalNetwork = physicalNetwork;
        this.zoning = zoning;
        this.connectoidSegments = zoning.getVirtualNetwork().connectoidSegments;
        this.linkSegments = physicalNetwork.linkSegments;
        this.zones = zoning.zones;
    }

    /**
     * Returns the total number of edge segments available in this traffic
     * assignment by combining the physical and non-physical link segments
     * 
     * @return total number of physical and virtual edge segments
     */
    public int getTotalNumberOfEdgeSegments() {
        return getTotalNumberOfLinkSegments() + getTotalNumberOfConnectoidSegments();
    }

    /**
     * Returns the total number of link segments available in this transport network
     * 
     * @return the number of physical link segments in this network
     */
    public int getTotalNumberOfLinkSegments() {
        return physicalNetwork.linkSegments.getNumberOfLinkSegments();
    }

    /**
     * Returns the total number of connectoid segments available in this transport
     * network
     * 
     * @return the number of connectoid segments in this network
     */
    public int getTotalNumberOfConnectoidSegments() {
        return zoning.getVirtualNetwork().connectoidSegments.getNumberOfConnectoidSegments();
    }

    /**
     * Returns the total number of virtual and physical vertices in this transport
     * network
     * 
     * @return the total number of physical and virtual vertices in this network
     */
    public int getTotalNumberOfVertices() {
        return zoning.getVirtualNetwork().centroids.getNumberOfCentroids() + physicalNetwork.nodes.getNumberOfNodes();
    }

    /**
     * Returns an iterator that iterates over all edge segments both virtual and
     * physical
     * 
     * @return iterator over TransportSegment objects
     */
    public TransportSegmentIterator getTransportSegmentIterator() {
        return new TransportSegmentIterator();
    }

    /**
     * Integrate physical and virtual links
     * 
     * @throws PlanItException
     *             thrown if there is an error
     */
    public void integrateConnectoidsAndLinks() throws PlanItException {
        VirtualNetwork virtualNetwork = zoning.getVirtualNetwork();
        for (Connectoid connectoid : virtualNetwork.connectoids) {
            virtualNetwork.connectoidSegments.createAndRegisterConnectoidSegment(connectoid, true);
            virtualNetwork.connectoidSegments.createAndRegisterConnectoidSegment(connectoid, false);
            connectVerticesToEdge(connectoid);
        }
        for (ConnectoidSegment connectoidSegment : virtualNetwork.connectoidSegments) {
            connectVerticesToEdgeSegment(connectoidSegment);
        }
        for (Link link : physicalNetwork.links) {
            connectVerticesToEdge(link);
        }
        for (LinkSegment linkSegment : physicalNetwork.linkSegments) {
            connectVerticesToEdgeSegment(linkSegment);
        }
    }

    /**
     * Remove the edges and edge segments on the vertices of both virtual and
     * physical networks
     * 
     * @throws PlanItException
     *             thrown if there is an error
     */
    public void removeVirtualNetworkFromPhysicalNetwork() throws PlanItException {
        for (Connectoid connectoid : zoning.getVirtualNetwork().connectoids) {
            disconnectVerticesFromEdge(connectoid);
        }
        for (ConnectoidSegment connectoidSegment : zoning.getVirtualNetwork().connectoidSegments) {
            disconnectVerticesFromEdgeSegment(connectoidSegment);
        }
        for (Link link : physicalNetwork.links) {
            disconnectVerticesFromEdge(link);
        }
        for (LinkSegment linkSegment : physicalNetwork.linkSegments) {
            disconnectVerticesFromEdgeSegment(linkSegment);
        }
    }

}