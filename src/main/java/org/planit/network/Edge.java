package org.planit.network;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import org.opengis.referencing.operation.TransformException;
import org.planit.exceptions.PlanItException;
import org.planit.geo.utils.PlanitGeoUtils;
import org.planit.utils.IdGenerator;

/** 
 * Edge class connecting two vertices via some geometry. Each edge has one or two underlying edge segments in a particular direction
 * which may carry additional information for each particular direction of the edge.
 * @author markr
 *
 */
public class Edge implements Comparable<Edge>{
	
		
	// Protected
	
	/**
	 * unique internal identifier 
	 */
	protected final long id;	
	/**
	 * generic input property storage
	 */
	protected Map<String, Object> inputProperties = null;	
	/**
	 * Name of the edge
	 */
	protected String name = null;
	/**
	 * vertex A
	 */
	protected Vertex vertexA = null;
	/**
	 * vertex B
	 */
	protected Vertex vertexB = null;
	
	//protected final double length;
	protected double length;
		
	/**
	 * edge segment a->b direction
	 */
	protected EdgeSegment edgeSegmentAB = null;
	/**
	 * edge segment b->a direction
	 */	
	protected EdgeSegment edgeSegmentBA = null;
	
	/** generate unique link id
	 * @return linkId
	 */
	protected static long generateEdgeId() {
		return IdGenerator.generateId(Edge.class);
	}	
	
	// Public
	
	/**
	 * Constructor which uses GeoTools to generate link lengths
	 * @throws PlanItException 
	 * @throws TransformException 
	 */
	protected Edge(@Nonnull Vertex vertexA, @Nonnull Vertex vertexB, PlanitGeoUtils planitGeoUtils) throws TransformException, PlanItException
	{
		this.id = generateEdgeId();
		this.vertexA = vertexA;
		this.vertexB = vertexB;
		this.length = planitGeoUtils.getDistanceInMeters(vertexA.getCentrePointGeometry(), vertexB.getCentrePointGeometry());
	}
	
	/**
	 * Constructor which injects link lengths directly
	 * @throws PlanItException 
	 * @throws TransformException 
	 */
	protected Edge(@Nonnull Vertex vertexA, @Nonnull Vertex vertexB, double length) throws TransformException, PlanItException
	{
		this.id = generateEdgeId();
		this.vertexA = vertexA;
		this.vertexB = vertexB;
		this.length = length;
	}
		
	/** Register edgeSegment. If there already exists an edgeSegment for that direction it is replaced and returned
	 * @param edgeSegment
	 * @param directionAB
	 * @return replacedLinkSegment
	 * @throws PlanItException 
	 */
	protected EdgeSegment registerEdgeSegment(EdgeSegment edgeSegment, boolean directionAB) throws PlanItException {
		if(edgeSegment.getParentEdge().getId() != getId()){
			throw new PlanItException("Inconsistency between link segment parent link and link it is being registered on");
		}
		EdgeSegment currentEdgeSegment = directionAB ? edgeSegmentAB : edgeSegmentBA;		
		if (directionAB) {
			this.edgeSegmentAB = edgeSegment;	
		} else {
			this.edgeSegmentBA = edgeSegment;			
		}		
		return currentEdgeSegment;
	}
		
	/**
	 * Add a property from the original input that is not part of the readily available link members
	 * @param key
	 * @param value
	 */
	public void addInputProperty(String key, Object value) {
		if(inputProperties == null) {
			inputProperties = new HashMap<String, Object>();
		}
		inputProperties.put(key, value);
	}
	
	/** Get input property by its key
	 * @param key
	 * @return value
	 */
	public Object getInputProperty(String key) {
		return inputProperties.get(key);
	}	
	
	public double getLength() {
		return length;
	}
	
	
	// Getters-Setters
	
	public long getId() {
		return id;
	}
	public Vertex getVertexA() {
		return vertexA;
	}
	public Vertex getVertexB() {
		return vertexB;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public EdgeSegment getEdgeSegmentAB() {
		return edgeSegmentAB;
	}
	public EdgeSegment getEdgeSegmentBA() {
		return edgeSegmentBA;
	}

	@Override
	public int compareTo(Edge o) {
		return Long.valueOf(id).compareTo(o.getId());
	}
}
