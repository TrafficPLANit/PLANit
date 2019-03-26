/**
 * 
 */
package org.planit.network.physical;

import java.util.HashSet;
import java.util.Set;

import org.planit.network.Vertex;
import org.planit.utils.IdGenerator;

//import java.awt.geom.Point2D;

/**
 * Node representation connected to one or more entry and exit links
 * @author markr
 *
 */
public class Node extends Vertex {
		
	// Protected
	
	/**
	 * Identifier defined in input files
	 */
	protected Set<Long> geometryIdSet = new HashSet<Long>();
	
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
	 * @param id
	 */
	public Node() {
		super();
		this.nodeId = generateNodeId();
	}	
	
	// Getters-Setters
	
	public long getNodeId() {
		return nodeId;
	}
	
	public void addGeometryId(Long geometryId) {
		geometryIdSet.add(geometryId);
	}
	
	public Set<Long> getGeometryIdSet() {
		return geometryIdSet;
	}

}
