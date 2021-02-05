package org.planit.converter;

/**
 * Indicates which PLANit id to use for extracting ids on the output format.
 * <ul>
 * <li>ID: use the PLANit internal id</li>
 * <li>EXTERNAL_ID: use the planit external id, otherwise use xml id, otherwise use the internal id</li>
 * <li>DEFAULT: use xml id if present, otherwise use the internal id</li>
 * </ul>
 * 
 * @author markr
 *
 */
public enum IdMapperType {

  DEFAULT, ID, EXTERNAL_ID
}
