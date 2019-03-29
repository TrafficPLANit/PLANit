package org.planit.dto;

public class BprResultDto extends ResultDto {
	
	private double capacity;
    private double length;
    private double speed;
    private double alpha;
    private double beta;
    
	public BprResultDto(long startNodeId, long endNodeId, double linkFlow, double linkCost, double totalCostToEndNode,
			                             double capacity, double length, double speed, double alpha, double beta) {
		super(startNodeId, endNodeId, linkFlow, linkCost, totalCostToEndNode);
		this.capacity = capacity;
		this.length = length;
		this.speed = speed;
		this.alpha = alpha;
		this.beta = beta;
	}

	public double getCapacity() {
		return capacity;
	}

	public void setCapacity(double capacity) {
		this.capacity = capacity;
	}

	public double getLength() {
		return length;
	}

	public void setLength(double length) {
		this.length = length;
	}

	public double getSpeed() {
		return speed;
	}

	public void setSpeed(double speed) {
		this.speed = speed;
	}

	public double getAlpha() {
		return alpha;
	}

	public void setAlpha(double alpha) {
		this.alpha = alpha;
	}

	public double getBeta() {
		return beta;
	}

	public void setBeta(double beta) {
		this.beta = beta;
	}
	
}
