package com.mrrg.backend.controller;

import com.mrrg.backend.dto.ActivateAccountRequest;
import com.mrrg.backend.dto.LoginRequest;
import com.mrrg.backend.dto.LoginResponse;
import com.mrrg.backend.dto.RegisterRequest;
import com.mrrg.backend.model.UserRole;
import com.mrrg.backend.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Test
    void registerEndpointEnabled_inDevelopmentProfile() {
        AuthController.RegisterEndpointProperty property = new AuthController.RegisterEndpointProperty("dev");
        assertThat(property.isEnabled()).isTrue();
    }

    @Test
    void registerEndpointEnabled_inDevelopmentProfileExplicit() {
        AuthController.RegisterEndpointProperty property = new AuthController.RegisterEndpointProperty("development");
        assertThat(property.isEnabled()).isTrue();
    }

    @Test
    void registerEndpointEnabled_inLocalProfile() {
        AuthController.RegisterEndpointProperty property = new AuthController.RegisterEndpointProperty("local");
        assertThat(property.isEnabled()).isTrue();
    }

    @Test
    void registerEndpointDisabled_inProductionProfile() {
        AuthController.RegisterEndpointProperty property = new AuthController.RegisterEndpointProperty("prod");
        assertThat(property.isEnabled()).isFalse();
    }

    @Test
    void registerEndpointDisabled_inProductionProfileExplicit() {
        AuthController.RegisterEndpointProperty property = new AuthController.RegisterEndpointProperty("production");
        assertThat(property.isEnabled()).isFalse();
    }

    @Test
    void register_throwsForbidden_whenEndpointDisabledInProduction() {
        AuthController.RegisterEndpointProperty disabledProperty = new AuthController.RegisterEndpointProperty("prod");
        AuthController controller = new AuthController(authService, disabledProperty);

        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("password");
        request.setName("Test User");

        assertThatThrownBy(() -> controller.register(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403")
                .hasMessageContaining("disabled");

        verify(authService, never()).register(any(RegisterRequest.class));
    }

    @Test
    void register_callsAuthService_whenEndpointEnabledInDevelopment() {
        AuthController.RegisterEndpointProperty enabledProperty = new AuthController.RegisterEndpointProperty("dev");
        AuthController controller = new AuthController(authService, enabledProperty);

        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("password");
        request.setName("Test User");

        LoginResponse expectedResponse = new LoginResponse();
        expectedResponse.setToken("jwt-token");
        expectedResponse.setUserId(1L);
        expectedResponse.setEmail("test@example.com");
        expectedResponse.setRole(UserRole.EMPLOYEE);

        when(authService.register(request)).thenReturn(expectedResponse);

        var response = controller.register(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expectedResponse);
        verify(authService).register(request);
    }

    @Test
    void login_callsAuthServiceAndReturnsResponse() {
        AuthController.RegisterEndpointProperty property = new AuthController.RegisterEndpointProperty("dev");
        AuthController controller = new AuthController(authService, property);

        LoginRequest request = new LoginRequest("test@example.com", "password");
        LoginResponse expectedResponse = new LoginResponse();
        expectedResponse.setToken("jwt-token");
        expectedResponse.setUserId(1L);
        expectedResponse.setEmail("test@example.com");
        expectedResponse.setRole(UserRole.EMPLOYEE);

        when(authService.login(request)).thenReturn(expectedResponse);

        var response = controller.login(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expectedResponse);
        verify(authService).login(request);
    }

    @Test
    void activateAccount_callsAuthServiceAndReturnsResponse() {
        AuthController.RegisterEndpointProperty property = new AuthController.RegisterEndpointProperty("dev");
        AuthController controller = new AuthController(authService, property);

        ActivateAccountRequest request = new ActivateAccountRequest();
        request.setToken("activation-token-12345");
        request.setPassword("newpassword");

        LoginResponse expectedResponse = new LoginResponse();
        expectedResponse.setToken("jwt-token");
        expectedResponse.setUserId(1L);
        expectedResponse.setEmail("test@example.com");
        expectedResponse.setRole(UserRole.EMPLOYEE);

        when(authService.activateAccount("activation-token-12345", "newpassword")).thenReturn(expectedResponse);

        var response = controller.activateAccount(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expectedResponse);
        verify(authService).activateAccount("activation-token-12345", "newpassword");
    }
}
