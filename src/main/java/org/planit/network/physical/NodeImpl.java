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

    /** generated UID */
	private static final long serialVersionUID = 8237965522827691852L;

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
    protected Object externalId;

    // Public

    /**
     * Node constructor
     */
    public NodeImpl() {
        super();
    }

    // Getters-Setters

    @Override
    public Object getExternalId() {
      return externalId;
    }

    @Override
    public void setExternalId(final Object externalId) {
      this.externalId = externalId;
    }
    
    @Override
    public boolean hasExternalId() {
      return (externalId != null);
    }

}