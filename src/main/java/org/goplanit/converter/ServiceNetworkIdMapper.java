package org.goplanit.converter;

import org.goplanit.utils.graph.Vertex;
import org.goplanit.utils.network.layer.service.ServiceLeg;
import org.goplanit.utils.network.layer.service.ServiceLegSegment;

import java.util.function.Function;

/**
 * All service network id mappers in a convenience class
 */
public class ServiceNetworkIdMapper extends PlanitComponentIdMapper{

  /**
   * Constructor
   * @param type to use
   */
  public ServiceNetworkIdMapper(IdMapperType type){
    super(type);
    add(Vertex.class, IdMapperFunctionFactory.createVertexIdMappingFunction(type));
    add(ServiceLeg.class, IdMapperFunctionFactory.createServiceLegIdMappingFunction(type));
    add(ServiceLegSegment.class,  IdMapperFunctionFactory.createServiceLegSegmentIdMappingFunction(type));
  }

  /** get id mapper for nodes
   * @return id mapper
   */
  public Function<Vertex, String> getServiceNodeIdMapper(){
    return get(Vertex.class);
  }

  /** get id mapper for service leg instances
   * @return id mapper
   */
  public Function<ServiceLeg, String> getServiceLegIdMapper(){
    return get(ServiceLeg.class);
  }

  /** get id mapper for service leg segment instances
   * @return id mapper
   */
  public Function<ServiceLegSegment, String> getServiceLegSegmentIdMapper(){
    return get(ServiceLegSegment.class);
  }

}
