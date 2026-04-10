package com.synapsecore.api.controller;

import com.synapsecore.access.AccessControlService;
import com.synapsecore.domain.dto.FulfillmentOverviewResponse;
import com.synapsecore.domain.dto.FulfillmentStatusResponse;
import com.synapsecore.domain.dto.FulfillmentUpdateRequest;
import com.synapsecore.fulfillment.FulfillmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fulfillment")
@RequiredArgsConstructor
public class FulfillmentController {

    private final AccessControlService accessControlService;
    private final FulfillmentService fulfillmentService;

    @GetMapping
    public FulfillmentOverviewResponse getOverview() {
        accessControlService.requireWorkspaceAccess("view fulfillment operations");
        return fulfillmentService.getOverview();
    }

    @PostMapping("/updates")
    @ResponseStatus(HttpStatus.OK)
    public FulfillmentStatusResponse recordUpdate(@Valid @RequestBody FulfillmentUpdateRequest request) {
        accessControlService.requireWorkspaceAccess("record fulfillment updates");
        return fulfillmentService.recordUpdate(request);
    }
}
