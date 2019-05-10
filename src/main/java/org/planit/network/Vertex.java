package org.planit.network;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.opengis.geometry.DirectPosition;
import org.planit.utils.IdGenerator;

/**
 * Node representation connected to one or more entry and exit links
 * @author markr
 *
 */
/**
 * @author mraa2518
 *
 */
public class Vertex implements Comparable<Vertex> {
	
    /**
     * Logger for this class
     */
    private static final Logger LOGGER = Logger.getLogger(Vertex.class.getName());
        
	/** edges of this vertex
	 * @author markr
	 */
	public class Edges implements Iterable<Edge> {
		
		protected Set<Edge> edges = new TreeSet<Edge>();		
		
		@Override
		public Iterator<Edge> iterator() {
			return edges.iterator();
		}
		
        /** 
         * Add edge, do not invoke when parsing networks, this connection is auto-populated before the assignment starts based on the edge and its two vertices that have been registered.
         * 
         * @param edge       Edge to be added
         * @return                true when added, false when already present (and not added)
         */
		public boolean addEdge(Edge edge) {
			return edges.add(edge);
		}
		
        /** 
         * Remove edge
         * 
         * @param edge     Edge to be removed
         * @return              true when removed, false when not present (and not removed)
         */
		public boolean removeEdge(Edge edge) {
			return edges.remove(edge);
		}		
	}		
	
    /** 
     * EdgeSegment container
     * @author markr
     */
	public class EdgeSegments implements Iterable<EdgeSegment> {
	
        /**
         * Edge segments which connect to this vertex
         */
		protected Set<EdgeSegment> edgeSegments = new TreeSet<EdgeSegment>();
		
		@Override
		public Iterator<EdgeSegment> iterator() {
			return edgeSegments.iterator();
		}
		
        /** 
         * Add edgeSegment, do not invoke when parsing networks, this connection is auto-populated before the assignment starts based on the edge segment vertices that have been registered.
         * 
         * @param edgeSegment            EdgeSegment object to be added
         * @return                                    true when added, false when already present (and not added)
         */
		public boolean addEdgeSegment(EdgeSegment edgeSegment) {
			return edgeSegments.add(edgeSegment);
		}
		
        /** 
         * Remove edgeSegment
         * 
         * @param edgeSegment    EdgeSegment object to be removed        
         * @return                           true when removed, false when not present (and not removed)
         */
		public boolean removeEdgeSegment(EdgeSegment edgeSegment) {
			return edgeSegments.remove(edgeSegment);
		}		
		
        /**
         * Test whether no edge segments have been registered
         * 
         * @return         true if no edge segments have been registered, false otherwise
         */
		public boolean isEmpty() {
			return edgeSegments.isEmpty();
		}
	}	
			
	// Protected
	
    /**
     *  Centre point geometry which is coordinate reference system aware
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
		
    /**
     * External identifier for this vertex
     */
	protected long externalId;
		
	/**
	 * edge container
	 */	
    protected final Edges edges = new Edges();
	
	/**
	 * exitEdgeSegmentcontainer
	 */
    protected final EdgeSegments exitEdgeSegments = new EdgeSegments();
    
    // protected getters-setters
	
	/**
	 * entryEdgeSegment container
	 */	
    protected final EdgeSegments entryEdgeSegments = new EdgeSegments();
    
    protected long getExternalId() {
        return externalId;
    }

    protected void setExternalId(long externalId) {
        this.externalId = externalId;
    }    
	
	// Public
	
    /**
     * Constructor
     */
	protected Vertex() {
		this.id = generateVertexId();
	}
	
    /**
     * Add a property from the original input that is not part of the readily available members
     * 
     * @param key       key (name) of the input property
     * @param value    value of the input property
     */
    public void addInputProperty(String key, Object value) {
        if (inputProperties == null) {
            inputProperties = new HashMap<String, Object>();
        }
        inputProperties.put(key, value);
    }
    
    /** 
     * Get input property by its key
     * 
     * @param key        the key of the input property
     * @return value     the value of the input property
     */
    public Object getInputProperty(String key) {
        return inputProperties.get(key);
    }
    
    /** Verify if exit edge segments are present
     * @return true if present, false otherwise
     */
    public boolean hasExitEdgeSegments() {
        return !exitEdgeSegments.isEmpty();
    }
    
    
    /** Verify if entry edge segments are present
     * @return true if present, false otherwise
     */
    public boolean hasEntryEdgeSegments() {
        return !entryEdgeSegments.isEmpty();
    }  
    
    public void addExitEdgeSegment(EdgeSegment edgeSegment) {
        exitEdgeSegments.addEdgeSegment(edgeSegment);        
    }

    public void addEntryEdgeSegment(EdgeSegment edgeSegment) {
        exitEdgeSegments.addEdgeSegment(edgeSegment);        
    }    
    
    public void removeExitEdgeSegment(EdgeSegment edgeSegment) {
        exitEdgeSegments.removeEdgeSegment(edgeSegment);        
    }

    public void removeEntryEdgeSegment(EdgeSegment edgeSegment) {
        exitEdgeSegments.removeEdgeSegment(edgeSegment);        
    }   
    
    public void addEdge(Edge edge) {
        edges.addEdge(edge);
    }    
    
    public void removeEdge(Edge edge) {
        edges.removeEdge(edge);        
    }    
    
    /** 
     * Compare vertices by their id
     * 
     * @param o         Vertex object to be compared to this one
     * @return            result of comparison
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
   @Override
    public int compareTo(Vertex o) {
        return Long.valueOf(id).compareTo(o.getId());
    }   	
	
	// Getters-Setters

	public long getId() {
		return id;
	}	
	    
    public DirectPosition getCentrePointGeometry() {
        return centrePointGeometry;
    }
    
    public void setCentrePointGeometry(DirectPosition centrePointGeometry) {
        this.centrePointGeometry = centrePointGeometry;
    }  
    
    public EdgeSegments getExitEdgeSegments(){
        return exitEdgeSegments;
    }
    
    public EdgeSegments getEntryEdgeSegments(){
        return entryEdgeSegments;
    }    

}
