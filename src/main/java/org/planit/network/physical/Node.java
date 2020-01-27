/**
 * 
 */
package org.planit.network.physical;

import org.planit.network.VertexImpl;
import org.planit.utils.misc.IdGenerator;

/**
 * Node representation connected to one or more entry and exit links
 * 
 * @author markr
 *
 */
public class Node extends VertexImpl {

    // Protected

    /**
     * generate unique node id
     * 
     * @return nodeId
     */
    protected static int generateNodeId() {
        return IdGenerator.generateId(Node.class);
    }

    /**
     * External identifier used in input files
     */
    protected long externalId;

    // Public

    /**
     * Node constructor
     */
    public Node() {
        super();
    }

    // Getters-Setters

    public long getExternalId() {
        return externalId;
    }

    public void setExternalId(long externalId) {
        this.externalId = externalId;
    }

}
