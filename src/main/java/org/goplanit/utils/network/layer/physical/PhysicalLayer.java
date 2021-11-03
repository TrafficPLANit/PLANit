package org.goplanit.utils.network.layer.physical;

import org.goplanit.utils.network.layer.physical.Link;
import org.goplanit.utils.network.layer.physical.LinkSegment;
import org.goplanit.utils.network.layer.physical.Node;
import org.goplanit.utils.network.layer.physical.UntypedPhysicalLayer;

/**
 * Physical topological Network consisting of nodes, links and link segments.
 *
 * @author markr
 */
public interface PhysicalLayer extends UntypedPhysicalLayer<Node, Link, LinkSegment> {

}
