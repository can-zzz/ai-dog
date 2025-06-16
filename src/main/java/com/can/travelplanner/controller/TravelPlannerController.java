package com.can.travelplanner.controller;

import com.can.travelplanner.dto.TravelPlanRequest;
import com.can.travelplanner.dto.TravelPlanResponse;
import com.can.travelplanner.service.TravelPlannerService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/travel")
public class TravelPlannerController {
    private final TravelPlannerService travelPlannerService;
    
    public TravelPlannerController(TravelPlannerService travelPlannerService) {
        this.travelPlannerService = travelPlannerService;
    }
    
    @PostMapping("/plan")
    public TravelPlanResponse generateTravelPlan(@Valid @RequestBody TravelPlanRequest request) {
        return travelPlannerService.generateTravelPlan(request);
    }
} 