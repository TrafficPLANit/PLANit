/**
 * 
 */
package org.planit.network.physical;

import java.util.logging.Logger;

import org.planit.network.Vertex;
import org.planit.utils.IdGenerator;

/**
 * Node representation connected to one or more entry and exit links
 * @author markr
 *
 */
public class Node extends Vertex {
		
    /**
     * Logger for this class
     */
    private static final Logger LOGGER = Logger.getLogger(Node.class.getName());
        
	// Protected
	
	/** generate unique node id
	 * @return nodeId
	 */
	protected static int generateNodeId() {
		return IdGenerator.generateId(Node.class);
	}	

    /**
	 * Unique internal identifier 
	 */	
	protected final long nodeId;
	
	
	// Public
	
    /**
     * Node constructor
      */
	public Node() {
		super();
		this.nodeId = generateNodeId();
	}	
	
	// Getters-Setters
	
	public long getNodeId() {
		return nodeId;
	}
	
    public long getExternalId() {
        return getExternalId();
    }

    public void setExternalId(long externalId) {
        setExternalId(externalId);
    }
    
    

}
