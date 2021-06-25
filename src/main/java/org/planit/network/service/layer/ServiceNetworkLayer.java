package org.planit.network.service.layer;

import org.planit.network.TopologicalLayer;
import org.planit.network.physical.PhysicalNetwork;

/**
 * A service network layer is built on top of a physical network (layer). Its nodes and links contain references to the underlying network resulting in an efficient use of memory
 * while allowing one to treat the service layer as a normal network. Service Nodes are one-to-one matches to the underlying network nodes and represent locations where services
 * exist. edges are represented by legs, while edge segments are represented by LegSegments. A leg comprises of one or more physical links in the underlying network and as long as
 * the underlying links differ multiple legs can exist between service nodes. Leg segments have a direction in which order they traverse the underlying links of the leg.
 *
 * @author markr
 *
 */
public class ServiceNetworkLayer extends PhysicalNetwork<ServiceNode, ServiceLeg, ServiceLegSegment> implements TopologicalLayer {

}
