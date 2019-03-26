package org.planit.network;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.opengis.geometry.DirectPosition;
import org.planit.utils.IdGenerator;

/**
 * Node representation connected to one or more entry and exit links
 * @author markr
 *
 */
public class Vertex implements Comparable<Vertex> {
	
	/** edges of this vertex
	 * @author markr
	 */
	public class Edges implements Iterable<Edge> {
		
		protected Set<Edge> edges = new TreeSet<Edge>();		
		
		@Override
		public Iterator<Edge> iterator() {
			return edges.iterator();
		}
		
		/** add edge, do not invoke when parsing networks, this connection is auto-populated before the assignment
		 * starts based on the edge and its two vertices that have been registered.
		 * @param exitEdge
		 * @return true, when added, false when already present (and not added)
		 */
		public boolean addEdge(Edge edge) {
			return edges.add(edge);
		}
		
		/** remove edge
		 * @param exitEdge
		 * @return true, when removed, false when not present (and not removed)
		 */
		public boolean removeEdge(Edge edge) {
			return edges.remove(edge);
		}		
	}		
	
	/** EdgeSegment container
	 * @author markr
	 */
	public class EdgeSegments implements Iterable<EdgeSegment> {
		
		protected Set<EdgeSegment> edgeSegments = new TreeSet<EdgeSegment>();
		
		@Override
		public Iterator<EdgeSegment> iterator() {
			return edgeSegments.iterator();
		}
		
		/** add edgeSegment, do not invoke when parsing networks, this connection is auto-populated before the assignment
		 * starts based on the edge segment vertices that have been registered.
		 * @param edge
		 * @return true, when added, false when already present (and not added)
		 */
		public boolean addEdgeSegment(EdgeSegment edgeSegment) {
			return edgeSegments.add(edgeSegment);
		}
		
		/** remove edgeSegment
		 * @param edge
		 * @return true, when removed, false when not present (and not removed)
		 */
		public boolean removeEdgeSegment(EdgeSegment edgeSegment) {
			return edgeSegments.remove(edgeSegment);
		}		
		
		public boolean isEmpty() {
			return edgeSegments.isEmpty();
		}
	}	
		
	
	// Protected
	
	/**
	 * Centre point geometry which is coordinate reference system aware
	 */
	protected DirectPosition centrePointGeometry;
			
	
	/** generate unique node id
	 * @return nodeId
	 */
	protected static int generateVertexId() {
		return IdGenerator.generateId(Vertex.class);
	}	
	

	/**
	 * Unique internal identifier 
	 */	
	protected final long id;
	
	/**
	 * generic input property storage
	 */
	protected Map<String, Object> inputProperties = null;		
		

	protected long externalId;
	/**
	 * edge container
	 */	
	public final Edges edges = new Edges();		
	/**
	 * exitEdgeSegmentcontainer
	 */
	public final EdgeSegments exitEdgeSegments = new EdgeSegments();
	/**
	 * entryEdgeSegment container
	 */	
	public final EdgeSegments entryEdgeSegments = new EdgeSegments();	
	
	// Public
	
	/**
	 * Node constructor
	 * @param id
	 */
	protected Vertex() {
		this.id = generateVertexId();
	}	
	
	public DirectPosition getCentrePointGeometry() {
		return centrePointGeometry;
	}
	public void setCentrePointGeometry(DirectPosition centrePointGeometry) {
		this.centrePointGeometry = centrePointGeometry;
	}

	// Getters-Setters

	public long getId() {
		return id;
	}	

	public long getExternalId() {
		return externalId;
	}

	public void setExternalId(long externalId) {
		this.externalId = externalId;
	}

	/**
	 * Add a property from the original input that is not part of the readily available members
	 * @param key
	 * @param value
	 */
	public void addInputProperty(String key, Object value) {
		if(inputProperties == null) {
			inputProperties = new HashMap<String, Object>();
		}
		inputProperties.put(key, value);
	}
	
	/** Get input property by its key
	 * @param key
	 * @return value
	 */
	public Object getInputProperty(String key) {
		return inputProperties.get(key);
	}		
	
	/** compare vertices by their id
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Vertex o) {
		return Long.valueOf(id).compareTo(o.getId());
	}	

}
