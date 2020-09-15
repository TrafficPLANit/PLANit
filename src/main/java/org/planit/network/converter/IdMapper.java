package org.planit.network.converter;

/**
 * Indicates which PLANit id to use for extracting ids on the output format. ID: use the PLANit internal id, EXTERNAL_ID: use the planit external id, GENERATEd: use a generator to
 * generate ids independent of the PLANit ids
 * 
 * @author markr
 *
 */
public enum IdMapper {

  ID, EXTERNAL_ID, GENERATED,
}
