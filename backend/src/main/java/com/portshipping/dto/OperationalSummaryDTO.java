package com.portshipping.dto;

public class OperationalSummaryDTO {
    private long totalVessels;
    private long vesselsInPort;
    private long totalContainers;
    private long containersInHold;
    private long totalShipments;
    private long activeShipments;
    private long deliveredShipments;
    private long pendingCustoms;
    private long approvedCustoms;
    private long allocatedBerths;

    public OperationalSummaryDTO() {}

    public OperationalSummaryDTO(long totalVessels, long vesselsInPort, long totalContainers, long containersInHold,
                                 long totalShipments, long activeShipments, long deliveredShipments,
                                 long pendingCustoms, long approvedCustoms, long allocatedBerths) {
        this.totalVessels = totalVessels;
        this.vesselsInPort = vesselsInPort;
        this.totalContainers = totalContainers;
        this.containersInHold = containersInHold;
        this.totalShipments = totalShipments;
        this.activeShipments = activeShipments;
        this.deliveredShipments = deliveredShipments;
        this.pendingCustoms = pendingCustoms;
        this.approvedCustoms = approvedCustoms;
        this.allocatedBerths = allocatedBerths;
    }

    public long getTotalVessels() { return totalVessels; }
    public void setTotalVessels(long totalVessels) { this.totalVessels = totalVessels; }

    public long getVesselsInPort() { return vesselsInPort; }
    public void setVesselsInPort(long vesselsInPort) { this.vesselsInPort = vesselsInPort; }

    public long getTotalContainers() { return totalContainers; }
    public void setTotalContainers(long totalContainers) { this.totalContainers = totalContainers; }

    public long getContainersInHold() { return containersInHold; }
    public void setContainersInHold(long containersInHold) { this.containersInHold = containersInHold; }

    public long getTotalShipments() { return totalShipments; }
    public void setTotalShipments(long totalShipments) { this.totalShipments = totalShipments; }

    public long getActiveShipments() { return activeShipments; }
    public void setActiveShipments(long activeShipments) { this.activeShipments = activeShipments; }

    public long getDeliveredShipments() { return deliveredShipments; }
    public void setDeliveredShipments(long deliveredShipments) { this.deliveredShipments = deliveredShipments; }

    public long getPendingCustoms() { return pendingCustoms; }
    public void setPendingCustoms(long pendingCustoms) { this.pendingCustoms = pendingCustoms; }

    public long getApprovedCustoms() { return approvedCustoms; }
    public void setApprovedCustoms(long approvedCustoms) { this.approvedCustoms = approvedCustoms; }

    public long getAllocatedBerths() { return allocatedBerths; }
    public void setAllocatedBerths(long allocatedBerths) { this.allocatedBerths = allocatedBerths; }
}
