package com.comet.opik.api.resources.v1.priv;

import com.codahale.metrics.annotation.Timed;
import com.comet.opik.api.AuthDetailsHolder;
import com.comet.opik.api.LoginRequest;
import com.comet.opik.api.LoginResponse;
import com.comet.opik.api.WorkspaceNameHolder;
import com.comet.opik.api.error.ErrorMessage;
import com.comet.opik.domain.ProjectService;
import com.comet.opik.infrastructure.auth.LocalAuthService;
import com.comet.opik.infrastructure.auth.RequestContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Path("/v1/private/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Timed
@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Inject)
@Tag(name = "Check", description = "Access check resources")
public class AuthenticationResource {

    public static final String SESSION_COOKIE_NAME = "opik_session";

    private final @NonNull Provider<RequestContext> requestContext;
    private final @NonNull LocalAuthService localAuthService;

    @POST
    @Path("/login")
    @Operation(operationId = "login", summary = "Login with username and password", description = "Login with username and password", responses = {
            @ApiResponse(responseCode = "200", description = "Login successful", content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content(schema = @Schema(implementation = ErrorMessage.class)))
    })
    public Response login(
            @RequestBody(content = @Content(schema = @Schema(implementation = LoginRequest.class))) @Valid LoginRequest loginRequest) {

        if (!localAuthService.isEnabled()) {
            log.warn("Local authentication is not enabled");
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(new ErrorMessage(List.of("Local authentication is not enabled")))
                    .build();
        }

        if (!localAuthService.validateCredentials(loginRequest.username(), loginRequest.password())) {
            log.warn("Invalid login attempt for user: {}", loginRequest.username());
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(new ErrorMessage(List.of("Invalid username or password")))
                    .build();
        }

        String sessionToken = localAuthService.generateSessionToken(loginRequest.username());

        NewCookie sessionCookie = new NewCookie.Builder(SESSION_COOKIE_NAME)
                .value(sessionToken)
                .maxAge(localAuthService.getSessionDurationSeconds())
                .path("/")
                .httpOnly(true)
                .build();

        log.info("User '{}' logged in successfully", loginRequest.username());

        return Response.ok()
                .cookie(sessionCookie)
                .entity(LoginResponse.builder()
                        .username(loginRequest.username())
                        .workspaceName(ProjectService.DEFAULT_WORKSPACE_NAME)
                        .build())
                .build();
    }

    @POST
    @Path("/logout")
    @Operation(operationId = "logout", summary = "Logout and clear session", description = "Logout and clear session", responses = {
            @ApiResponse(responseCode = "204", description = "Logout successful")
    })
    public Response logout() {
        NewCookie clearCookie = new NewCookie.Builder(SESSION_COOKIE_NAME)
                .value("")
                .maxAge(0)
                .path("/")
                .httpOnly(true)
                .build();

        log.info("User logged out");

        return Response.noContent()
                .cookie(clearCookie)
                .build();
    }

    @GET
    @Path("/check")
    @Operation(operationId = "checkSession", summary = "Check if session is valid", description = "Check if session is valid", responses = {
            @ApiResponse(responseCode = "200", description = "Session valid", content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "401", description = "Session invalid or expired", content = @Content(schema = @Schema(implementation = ErrorMessage.class)))
    })
    public Response checkSession(@CookieParam(SESSION_COOKIE_NAME) String sessionToken) {
        if (!localAuthService.isEnabled()) {
            // If local auth is not enabled, always allow access
            return Response.ok()
                    .entity(LoginResponse.builder()
                            .username("default")
                            .workspaceName(ProjectService.DEFAULT_WORKSPACE_NAME)
                            .build())
                    .build();
        }

        var username = localAuthService.validateSessionToken(sessionToken);

        if (username.isEmpty()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(new ErrorMessage(List.of("Session invalid or expired")))
                    .build();
        }

        return Response.ok()
                .entity(LoginResponse.builder()
                        .username(username.get())
                        .workspaceName(ProjectService.DEFAULT_WORKSPACE_NAME)
                        .build())
                .build();
    }

    @POST
    @Operation(operationId = "checkAccess", summary = "Check user access to workspace", description = "Check user access to workspace", responses = {
            @ApiResponse(responseCode = "204", description = "No Content"),
            @ApiResponse(responseCode = "401", description = "Bad Request", content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "403", description = "Access forbidden", content = @Content(schema = @Schema(implementation = ErrorMessage.class)))
    })
    public Response checkAccess(
            @RequestBody(content = @Content(schema = @Schema(implementation = AuthDetailsHolder.class))) @Valid AuthDetailsHolder authDetailsHolder) {
        String workspaceId = requestContext.get().getWorkspaceId();
        String userName = requestContext.get().getUserName();

        log.info("User '{}' has access to workspace_id '{}'", userName, workspaceId);

        return Response.noContent().build();
    }

    @GET
    @Path("workspace")
    @Operation(operationId = "getWorkspaceName", summary = "User's default workspace name", description = "User's default workspace name", responses = {
            @ApiResponse(responseCode = "200", description = "Authentication resource", content = @Content(schema = @Schema(implementation = WorkspaceNameHolder.class))),
            @ApiResponse(responseCode = "401", description = "Bad Request", content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "403", description = "Access forbidden", content = @Content(schema = @Schema(implementation = ErrorMessage.class)))
    })
    public Response getWorkspaceName() {
        String workspaceName = requestContext.get().getWorkspaceName();
        String userName = requestContext.get().getUserName();
        String workspaceId = requestContext.get().getWorkspaceId();

        log.info("User '{}' has workspaceName '{}', workspaceid '{}'", userName, workspaceName, workspaceId);

        return Response.ok().entity(WorkspaceNameHolder.builder()
                .workspaceName(workspaceName)
                .build())
                .build();
    }
}
