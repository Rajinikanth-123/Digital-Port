package com.portshipping.controller;

import com.portshipping.repository.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final UserRepository users;
    private final PortRepository ports;
    private final VesselRepository vessels;
    private final ContainerRepository containers;
    private final ShipmentRepository shipments;
    private final CustomsClearanceRepository customs;
    private final BerthAllocationRepository berths;

    public DashboardController(
            UserRepository u,
            PortRepository p,
            VesselRepository v,
            ContainerRepository c,
            ShipmentRepository s,
            CustomsClearanceRepository cust,
            BerthAllocationRepository b
    ) {
        this.users = u;
        this.ports = p;
        this.vessels = v;
        this.containers = c;
        this.shipments = s;
        this.customs = cust;
        this.berths = b;
    }

    @GetMapping
    public Summary summary() {
        return new Summary(
                users.count(),
                users.countByEnabledTrue(),
                ports.count(),
                vessels.count(),
                containers.count(),
                shipments.count(),
                customs.countByStatusIgnoreCase("PENDING"),
                berths.count(),
                shipments.countByStatusIgnoreCase("IN_TRANSIT")
        );
    }

    public record Summary(
            long users,
            long activeUsers,
            long ports,
            long vessels,
            long containers,
            long shipments,
            long pendingCustoms,
            long berthAllocations,
            long shipmentsInTransit
    ) {}
}

