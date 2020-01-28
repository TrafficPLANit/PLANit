/**
 * 
 */
package org.planit.network.physical;

import org.planit.graph.VertexImpl;
import org.planit.utils.misc.IdGenerator;
import org.planit.utils.network.physical.Node;

/**
 * Node representation connected to one or more entry and exit links
 * 
 * @author markr
 *
 */
public class NodeImpl extends VertexImpl implements Node {

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
    public NodeImpl() {
        super();
    }

    // Getters-Setters

    @Override
	public long getExternalId() {
        return externalId;
    }

    @Override
	public void setExternalId(long externalId) {
        this.externalId = externalId;
    }

}
