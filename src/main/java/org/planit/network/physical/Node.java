/**
 * 
 */
package org.planit.network.physical;

import java.util.HashSet;
import java.util.Set;

import org.planit.network.Vertex;
import org.planit.utils.IdGenerator;

/**
 * Node representation connected to one or more entry and exit links
 * @author markr
 *
 */
public class Node extends Vertex {
		
	// Protected
	
	/**
	 * Set of external Ids of links which connect to this node
	 */
	protected Set<Long> externalLinkIdSet = new HashSet<Long>();
	
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
	
/**
 * Add external Id of a link which connects to this node
 * 
 * @param externalLinkId         external Id of link which connects to this node
 */
	public void addExternalLinkId(Long externalLinkId) {
		externalLinkIdSet.add(externalLinkId);
	}
	
	public Set<Long> getExternalLinkIdSet() {
		return externalLinkIdSet;
	}

}
