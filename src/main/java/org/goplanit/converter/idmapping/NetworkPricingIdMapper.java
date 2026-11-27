package org.goplanit.converter.idmapping;

import java.util.function.Function;

import org.goplanit.pricing.NetworkPricing;
import org.goplanit.utils.id.IdMapperType;
import org.goplanit.utils.id.IdMappingUtils;
import org.goplanit.utils.pricing.PriceCap;
import org.goplanit.utils.pricing.PricingScheme;
import org.goplanit.utils.pricing.PricingSchemeGate;
import org.goplanit.utils.pricing.PricingSchemeSection;

/**
 * Id mappers for the entities of a network pricing: the pricing itself, its schemes, sections, gates and caps
 * <p>
 * A writer writes the XML ids, the external ids or the internal ids of what it writes, as chosen. The network pricing
 * writer asks this mapper for the chosen id of each entity, as the zoning writer asks the {@link ZoningIdMapper}. The
 * schemes of all three kinds share one mapper, as their ids are unique across kinds.
 * </p>
 *
 * @author markr
 */
public class NetworkPricingIdMapper extends PlanitComponentIdMapper {

  /**
   * Create id mappers per type based on a given id mapping type
   *
   * @param mappingType to apply
   */
  public NetworkPricingIdMapper(IdMapperType mappingType) {
    super(mappingType);
    add(NetworkPricing.class, IdMappingUtils.createIdMappingFunction(NetworkPricing.class, mappingType));
    add(PricingScheme.class, IdMappingUtils.createIdMappingFunction(PricingScheme.class, mappingType));
    add(PricingSchemeSection.class, IdMappingUtils.createIdMappingFunction(PricingSchemeSection.class, mappingType));
    add(PricingSchemeGate.class, IdMappingUtils.createIdMappingFunction(PricingSchemeGate.class, mappingType));
    add(PriceCap.class, IdMappingUtils.createIdMappingFunction(PriceCap.class, mappingType));
  }

  /**
   * Get the id mapper for the network pricing
   *
   * @return id mapper
   */
  public Function<NetworkPricing, String> getNetworkPricingIdMapper() {
    return get(NetworkPricing.class);
  }

  /**
   * Get the id mapper for schemes of every kind
   *
   * @return id mapper
   */
  public Function<PricingScheme, String> getSchemeIdMapper() {
    return get(PricingScheme.class);
  }

  /**
   * Get the id mapper for sections
   *
   * @return id mapper
   */
  public Function<PricingSchemeSection, String> getSectionIdMapper() {
    return get(PricingSchemeSection.class);
  }

  /**
   * Get the id mapper for gates
   *
   * @return id mapper
   */
  public Function<PricingSchemeGate, String> getGateIdMapper() {
    return get(PricingSchemeGate.class);
  }

  /**
   * Get the id mapper for caps
   *
   * @return id mapper
   */
  public Function<PriceCap, String> getCapIdMapper() {
    return get(PriceCap.class);
  }
}
