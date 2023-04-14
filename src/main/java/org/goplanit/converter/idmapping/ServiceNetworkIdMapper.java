package org.goplanit.converter.idmapping;

import org.goplanit.network.ServiceNetwork;
import org.goplanit.utils.function.PlanitExceptionFunction;
import org.goplanit.utils.graph.Vertex;
import org.goplanit.utils.network.layer.ServiceNetworkLayer;
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
    add(ServiceNetwork.class,  IdMapperFunctionFactory.createServiceNetworkIdMappingFunction(type));
    add(ServiceNetworkLayer.class,  IdMapperFunctionFactory.createServiceNetworkLayerIdMappingFunction(type));
  }

  /** get id mapper for nodes
   * @return id mapper
   */
  public Function<Vertex, String> getServiceNodeIdMapper(){
    return get(Vertex.class);
  }

  /** get id mapper for ServiceLeg instances
   * @return id mapper
   */
  public Function<ServiceLeg, String> getServiceLegIdMapper(){
    return get(ServiceLeg.class);
  }

  /** get id mapper for ServiceLegSegment instances
   * @return id mapper
   */
  public Function<ServiceLegSegment, String> getServiceLegSegmentIdMapper(){
    return get(ServiceLegSegment.class);
  }

  /** get id mapper for ServiceNetwork instances
   * @return id mapper
   */
  public Function<ServiceNetwork, String> getServiceNetworkIdMapper() {
    return get(ServiceNetwork.class);
  }

  /** get id mapper for ServiceNetworkLayer instances
   * @return id mapper
   */
  public Function<ServiceNetworkLayer, String> getServiceNetworkLayerIdMapper() {
    return get(ServiceNetworkLayer.class);
  }
}
