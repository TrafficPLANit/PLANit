package org.planit.graph;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.opengis.geometry.DirectPosition;
import org.planit.utils.misc.IdGenerator;
import org.planit.utils.network.Edge;
import org.planit.utils.network.EdgeSegment;
import org.planit.utils.network.Vertex;

/**
 * Node representation connected to one or more entry and exit links
 * 
 * @author markr
 *
 */
public class VertexImpl implements Vertex {

    /**
     * edges of this vertex
     * 
     * @author markr
     */
	public class EdgesImpl implements Edges {

        protected Set<Edge> edges = new TreeSet<Edge>();

        /**
         * Add edge, do not invoke when parsing networks, this connection is
         * auto-populated before the assignment starts based on the edge and its two
         * vertices that have been registered.
         * 
         * @param edge Edge to be added
         * @return true when added, false when already present (and not added)
         */
        @Override
        public boolean addEdge(Edge edge) {
            return edges.add(edge);
        }

        /**
         * Remove edge
         * 
         * @param edge Edge to be removed
         * @return true when removed, false when not present (and not removed)
         */
        @Override
        public boolean removeEdge(Edge edge) {
            return edges.remove(edge);
        }
        
        /**
         * Returns a Set of Edge objects
         * 
         * @return Set of Edge objects
         */
        @Override
        public Set<Edge> getEdges() {
        	return edges;
        }
    }

    /**
     * EdgeSegment container
     * 
     * @author markr
     */
	public class EdgeSegmentsImpl implements EdgeSegments {

        /**
         * Edge segments which connect to this vertex
         */
        protected Set<EdgeSegment> edgeSegments = new TreeSet<EdgeSegment>();

        /**
         * Add edgeSegment, do not invoke when parsing networks, this connection is
         * auto-populated before the assignment starts based on the edge segment
         * vertices that have been registered.
         * 
         * @param edgeSegment EdgeSegment object to be added
         * @return true when added, false when already present (and not added)
         */
        @Override
        public boolean addEdgeSegment(EdgeSegment edgeSegment) {
            return edgeSegments.add(edgeSegment);
        }

        /**
         * Remove edgeSegment
         * 
         * @param edgeSegment EdgeSegment object to be removed
         * @return true when removed, false when not present (and not removed)
         */
        @Override
        public boolean removeEdgeSegment(EdgeSegment edgeSegment) {
            return edgeSegments.remove(edgeSegment);
        }

        /**
         * Test whether no edge segments have been registered
         * 
         * @return true if no edge segments have been registered, false otherwise
         */
        @Override
        public boolean isEmpty() {
            return edgeSegments.isEmpty();
        }
        
        /**
         * Return Set of EdgeSegment objects
         * 
         * @return Set of EdgeSegment objects
         */
        @Override
        public Set<EdgeSegment> getEdgeSegments() {
        	return edgeSegments;
        }
    }

    // Protected

    /**
     * Centre point geometry which is coordinate reference system aware
     */
    protected DirectPosition centrePointGeometry;

    /**
     * generate unique node id
     * 
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
     * edge container
     */
    protected final Edges edges = new EdgesImpl();
    
    /**
     * exitEdgeSegmentcontainer
     */
    protected final EdgeSegments exitEdgeSegments = new EdgeSegmentsImpl();
    
    /**
     * entryEdgeSegment container
     */
    protected final EdgeSegments entryEdgeSegments = new EdgeSegmentsImpl();
    
    /**
     * Constructor
     */
    protected VertexImpl() {
        this.id = generateVertexId();
    }    


    // Public

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

    /**
     * Add a property from the original input that is not part of the readily
     * available members
     * 
     * @param key key (name) of the input property
     * @param value value of the input property
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
     * @param key the key of the input property
     * @return value the value of the input property
     */
    public Object getInputProperty(String key) {
        return inputProperties.get(key);
    }

    /**
     * Compare vertices by their id
     * 
     * @param o Vertex object to be compared to this one
     * @return result of comparison
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(Vertex o) {
        return Long.valueOf(id).compareTo(o.getId());
    }


	/**
	 * collect entry edge segments
	 * @return entry edge segments
	 */
	@Override
	public EdgeSegments getEntryEdgeSegments() {
		return entryEdgeSegments;
	}
	
	/**
	 * collect exit edge segments
	 * @return exit edge segments
	 */
	@Override
	public EdgeSegments getExitEdgeSegments() {
		return entryEdgeSegments;
	}


	@Override
	public Edges getEdges() {
		return edges;
	}

}
