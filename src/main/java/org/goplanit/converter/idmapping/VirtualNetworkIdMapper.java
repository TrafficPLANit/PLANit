package org.goplanit.converter.idmapping;

import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.userclass.TravellerType;
import org.goplanit.userclass.UserClass;
import org.goplanit.utils.graph.Vertex;
import org.goplanit.utils.id.IdMapperType;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;
import org.goplanit.utils.network.layer.physical.Link;
import org.goplanit.utils.network.virtual.ConnectoidEdge;
import org.goplanit.utils.network.virtual.ConnectoidSegment;
import org.goplanit.utils.time.TimePeriod;

import java.util.function.Function;

/**
 * All virtual network id mappers in a convenience class
 */
public class VirtualNetworkIdMapper extends PlanitComponentIdMapper {

  /**
   * Constructor
   * @param type to use
   */
  public VirtualNetworkIdMapper(IdMapperType type){
    super(type);
    add(ConnectoidEdge.class, IdMapperFunctionFactory.createConnectoidEdgeIdMappingFunction(type));
    add(ConnectoidSegment.class, IdMapperFunctionFactory.createConnectoidSegmentIdMappingFunction(type));
    add(Vertex.class, IdMapperFunctionFactory.createVertexIdMappingFunction(type));
  }

  /**
   * get id mapper for nodes
   *
   * @return id mapper
   */
  public Function<Vertex, String> getVertexIdMapper(){
    return get(Vertex.class);
  }

  /** get id mapper for links
   * @return id mapper
   */
  public Function<ConnectoidEdge, String> getConnectoidEdgeIdMapper(){
    return get(ConnectoidEdge.class);
  }

  /** get id mapper for link segments
   * @return id mapper
   */
  public Function<ConnectoidSegment, String> getConnectoidSegmentIdMapper(){
    return get(ConnectoidSegment.class);
  }

}
