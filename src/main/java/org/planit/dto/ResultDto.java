package org.planit.dto;

public class ResultDto implements Comparable<ResultDto> {
	
	private long startNodeId;
	private long endNodeId;
	private double linkFlow;
	private double linkCost;
	private Double totalCostToEndNode;  //use this to order results
	
	public ResultDto(long startNodeId, long endNodeId, double linkFlow, double linkCost, double  totalCostToEndNode) {
		this.startNodeId = startNodeId;
		this.endNodeId = endNodeId;
		this.linkFlow = linkFlow;
		this.linkCost = linkCost;
		this.totalCostToEndNode =  totalCostToEndNode;
	}
	public long getStartNodeId() {
		return startNodeId;
	}
	public void setStartNodeId(long startNodeId) {
		this.startNodeId = startNodeId;
	}
	public long getEndNodeId() {
		return endNodeId;
	}
	public void setEndNodeId(long endNodeId) {
		this.endNodeId = endNodeId;
	}
	public double getLinkFlow() {
		return linkFlow;
	}
	public void setLinkFlow(double linkFlow) {
		this.linkFlow = linkFlow;
	}
	public double getLinkCost() {
		return linkCost;
	}
	public void setLinkCost(double linkCost) {
		this.linkCost = linkCost;
	}
	public double getTotalCostToEndNode() {
		return  totalCostToEndNode;
	}
	public void setTotalCost(double totalCostToEndNode) {
		this.totalCostToEndNode =  totalCostToEndNode;
	}
	@Override
	public int compareTo(ResultDto other) {
		return totalCostToEndNode.compareTo(other.getTotalCostToEndNode());
	}
	
	public boolean equals(ResultDto other) {
		if (startNodeId != other.getStartNodeId())
			return false;
		if (endNodeId != other.getEndNodeId())
			return false;
		if (linkCost != other.getLinkCost())
			return false;
		if (linkFlow != other.getLinkFlow())
			return false;
		if (totalCostToEndNode != other.getTotalCostToEndNode())
			return false;
		return true;
	}
	
	public int hashCode() {
		double val = startNodeId + endNodeId * linkCost * linkFlow * totalCostToEndNode;
		return (int) Math.round(val);
	}
	
	public String toString() {
		return "startNodeId = " + startNodeId + " endNodeId = " + endNodeId + " linkCost = " + linkCost + " linkFlow = " + linkFlow + " totalCostToEndNode = " + totalCostToEndNode;
	}

}
