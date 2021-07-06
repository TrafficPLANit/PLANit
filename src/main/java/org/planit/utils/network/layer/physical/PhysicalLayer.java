package org.planit.utils.network.layer.physical;

import org.planit.utils.network.layer.TopologicalLayer;

/**
 * Physical topological Network consisting of nodes, links and link segments.
 *
 * @author markr
 */
public interface PhysicalLayer extends TopologicalLayer, UntypedPhysicalLayer<Nodes, Links, LinkSegments> {

}
