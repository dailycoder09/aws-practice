package com.microservices.inventory.controller;

import com.microservices.inventory.dto.response.ServerInfoResponse;
import com.microservices.inventory.service.ServerInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Server Info REST Controller
 *
 * Exposes which server answered the request (host, request path, runtime and, on EC2, instance
 * details) as JSON for the React UI. Lives under /api/inventory/ so the load balancer's
 * /api/inventory* path rule routes it to this service.
 *
 * @author Microservices Team
 * @version 1.0
 */
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Tag(name = "Server Info", description = "Details of the server instance that handled the request")
public class ServerInfoController {

    private final ServerInfoService serverInfoService;

    /**
     * Describe the server that handled this request
     *
     * @param request Current HTTP request (used to report how the request arrived)
     * @return Server info with HTTP 200; never cached, since behind a load balancer each call may
     *         be answered by a different instance
     */
    @GetMapping("/server-info")
    @Operation(summary = "Get server info",
            description = "Returns application, host, request and runtime details of the serving instance, plus AWS EC2 details when running on EC2")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Server info retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ServerInfoResponse.class)))
    })
    public ResponseEntity<ServerInfoResponse> getServerInfo(HttpServletRequest request) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(serverInfoService.describe(request));
    }
}
