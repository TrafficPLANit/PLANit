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
 * Entire transport network that is being modeled including both the physical and virtual aspects of it as well
 * as the zoning. It acts as a wrapper unifying the two components during the assignment stage 
 * 
 * @author markr
 *
 */
public class TransportNetwork {
	
	/** Custom iterator over all registered edge segments by combining the physical and virtual edge segments
	 * @author markr
	 *
	 */
	public class TransportSegmentIterator implements Iterator<EdgeSegment> {
		
		private final Iterator<LinkSegment> physicalIterator = physicalNetwork.linkSegments.iterator();
		private final Iterator<ConnectoidSegment> virtualIterator = zoning.getVirtualNetwork().connectoidSegments.iterator();
		
		private Iterator<?> currentIterator;
		
		TransportSegmentIterator(){
			currentIterator = physicalIterator.hasNext() ? physicalIterator : virtualIterator;
		}

		@Override
		public boolean hasNext() {
			if(currentIterator.hasNext()) {
				return true;
			}else if(!currentIterator.equals(virtualIterator)) {
				currentIterator = virtualIterator;
			}
			return currentIterator.hasNext();
		}

		@Override
		public EdgeSegment next() {
			return (EdgeSegment) currentIterator.next();
		}				
	}

	/**
	 * Holds the physical road network that is being modelled
	 */
	protected final PhysicalNetwork physicalNetwork;
	
	/**
	 * Holds the zoning structure and virtual transport network interfacing with the physical network
	 */
	protected final Zoning zoning;
	
	/** Add edge segment to the incoming or outgoing set of edge segments for the related vertices
	 * @param edgeSegment
	 * @throws PlanItException
	 */
	protected void connectVerticesToEdgeSegment(EdgeSegment edgeSegment) throws PlanItException {
		edgeSegment.getUpstreamVertex().exitEdgeSegments.addEdgeSegment(edgeSegment);
		edgeSegment.getDownstreamVertex().entryEdgeSegments.addEdgeSegment(edgeSegment);	
	}	
	
	/** remove edge segment from the incoming or outgoing set of edge segments for the related vertices
	 * @param edgeSegment
	 * @throws PlanItException
	 */
	protected void disconnectVerticesFromEdgeSegment(EdgeSegment edgeSegment) throws PlanItException {
		edgeSegment.getUpstreamVertex().exitEdgeSegments.removeEdgeSegment(edgeSegment);
		edgeSegment.getDownstreamVertex().entryEdgeSegments.removeEdgeSegment(edgeSegment);	
	}		
	
	/** Add connectoid to both vertices
	 * @param connectoid
	 * @throws PlanItException
	 */
	protected void connectVerticesToEdge(Edge edge) throws PlanItException {
		edge.getVertexA().edges.addEdge(edge);
		edge.getVertexB().edges.addEdge(edge);
	}		
	
	/** Add connectoid to both vertices
	 * @param connectoid
	 * @throws PlanItException
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
	
	/** Constructor
	 * 
	 * @param physicalNetwork
	 * @param zoning
	 */
	public TransportNetwork(@Nonnull PhysicalNetwork physicalNetwork,@Nonnull Zoning zoning){
		this.physicalNetwork = physicalNetwork;
		this.zoning = zoning;
		this.connectoidSegments = zoning.getVirtualNetwork().connectoidSegments;
		this.linkSegments = physicalNetwork.linkSegments;
		this.zones = zoning.zones;
	}
	
	/** Collect the total number of edge segments available in this traffic assignment by combining the physical and non-physical link segments
	 * @return totalNumberOfEdgekSegments
	 */
	public int getTotalNumberOfEdgeSegments() {
		return getTotalNumberOfLinkSegments() + getTotalNumberOfConnectoidSegments();
	}
	
	/** Collect the total number of link segments available in this transport network 
	 * @return totalNumberOfLinkSegments
	 */
	public int getTotalNumberOfLinkSegments() {
		return physicalNetwork.linkSegments.getNumberOfLinkSegments();
	}		
	
	/** Collect the total number of connectoid segments available in this transport network 
	 * @return totalNumberOfLinkSegments
	 */
	public int getTotalNumberOfConnectoidSegments() {
		return zoning.getVirtualNetwork().connectoidSegments.getNumberOfConnectoidSegments();
	}
	
	/** Collect the total number of virtual and physical vertices in this transport network
	 * @return totalNumberOfVertices
	 */
	public int getTotalNumberOfVertices() {
		return zoning.getVirtualNetwork().centroids.getNumberOfCentroids() + physicalNetwork.nodes.getNumberOfNodes();
	}		
	
	/** collect an iterator that iterates over all edge segments both virtual and physical
	 * @return
	 */
	public TransportSegmentIterator getTransportSegmentIterator() {
		return new TransportSegmentIterator();
	}

	public void integrateConnectoidsAndLinks(VirtualNetwork virtualNetwork) throws PlanItException {
		for (Connectoid connectoid: virtualNetwork.connectoids) {
			virtualNetwork.connectoidSegments.createAndRegisterConnectoidSegment(connectoid, true);
			virtualNetwork.connectoidSegments.createAndRegisterConnectoidSegment(connectoid, false);
			connectVerticesToEdge(connectoid);
		}
		for (ConnectoidSegment connectoidSegment: virtualNetwork.connectoidSegments) {
			connectVerticesToEdgeSegment(connectoidSegment);
		}
		for (Link link: physicalNetwork.links) {
			connectVerticesToEdge(link);
		}
		for (LinkSegment linkSegment: physicalNetwork.linkSegments) {
			connectVerticesToEdgeSegment(linkSegment);
		}
	}
	
	/**
	 * de-register the edges and (incoming/outgoing) and edge segments on the vertices of both virtual and physical networks
	 * @throws PlanItException 
	 */
	public void removeVirtualNetworkFromPhysicalNetwork() throws PlanItException {
		for (Connectoid connectoid: zoning.getVirtualNetwork().connectoids) {
			disconnectVerticesFromEdge(connectoid);
		}
		for (ConnectoidSegment connectoidSegment: zoning.getVirtualNetwork().connectoidSegments) {
			disconnectVerticesFromEdgeSegment(connectoidSegment);
		}
		for (Link link: physicalNetwork.links) {
			disconnectVerticesFromEdge(link);
		}
		for (LinkSegment linkSegment: physicalNetwork.linkSegments) {
			disconnectVerticesFromEdgeSegment(linkSegment);
		}
	}
	
}