package org.goplanit.converter.idmapping;

import org.goplanit.utils.graph.Vertex;
import org.goplanit.utils.id.IdMapperType;
import org.goplanit.utils.network.virtual.physical.ConnectoidLink;
import org.goplanit.utils.network.virtual.physical.ConnectoidSegment;

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
    add(ConnectoidLink.class, IdMapperFunctionFactory.createConnectoidEdgeIdMappingFunction(type));
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
  public Function<ConnectoidLink, String> getConnectoidLinkIdMapper(){
    return get(ConnectoidLink.class);
  }

  /** get id mapper for link segments
   * @return id mapper
   */
  public Function<ConnectoidSegment, String> getConnectoidSegmentIdMapper(){
    return get(ConnectoidSegment.class);
  }

}
