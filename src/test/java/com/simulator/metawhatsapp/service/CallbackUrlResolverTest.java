package com.simulator.metawhatsapp.service;

import com.simulator.metawhatsapp.properties.SimulatorProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CallbackUrlResolverTest {

    private CallbackUrlResolver resolver;
    private SimulatorProperties properties;

    @BeforeEach
    void setUp() {
        SimulatorProperties.Webhook webhook = new SimulatorProperties.Webhook(
                "https://testqa.gtsstaging.com/api/whatsapp/call-back/c0ffee00-51m0-4a7a-9e01-000000000001",
                "102934821739482",
                5,
                new SimulatorProperties.Webhook.Retry(3, List.of(1, 2, 5))
        );

        properties = new SimulatorProperties(
                new SimulatorProperties.ApiVersion(List.of("v20.0", "v21.0", "v22.0", "v23.0"), "v23.0"),
                new SimulatorProperties.Auth(List.of("token"), "verify"),
                webhook,
                new SimulatorProperties.Delays(2, 5, 10),
                new SimulatorProperties.Events(true, true, false),
                new SimulatorProperties.Probability(98, 1, 1),
                new SimulatorProperties.PhoneNumber("15550001111", "109876543210987")
        );

        resolver = new CallbackUrlResolver(properties);
    }

    @Test
    void testResolveFromExplicitCallbackUrlHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Callback-Url", "https://tenant-alpha.gtsstaging.com/api/whatsapp/call-back/custom-sender-123");

        String result = resolver.resolveCallbackUrl(request, null, "c0ffee00-51m0-4a7a-9e01-000000000001");
        assertEquals("https://tenant-alpha.gtsstaging.com/api/whatsapp/call-back/custom-sender-123", result);
    }

    @Test
    void testResolveFromTenantDomainHeader_WithoutCom() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Domain", "testqa.gtsstaging");

        String result = resolver.resolveCallbackUrl(request, null, "c0ffee00-51m0-4a7a-9e01-000000000001");
        assertEquals("https://testqa.gtsstaging.com/api/whatsapp/call-back/c0ffee00-51m0-4a7a-9e01-000000000001", result);
    }

    @Test
    void testResolveFromTenantDomainHeader_WithCom() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Domain", "testqa.gtsstaging.com");

        String result = resolver.resolveCallbackUrl(request, null, "c0ffee00-51m0-4a7a-9e01-000000000001");
        assertEquals("https://testqa.gtsstaging.com/api/whatsapp/call-back/c0ffee00-51m0-4a7a-9e01-000000000001", result);
    }

    @Test
    void testResolveFromSeparateTenantAndInstanceHeaders() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant", "acme-corp");
        request.addHeader("X-Instance", "gtsprod.com");

        String result = resolver.resolveCallbackUrl(request, null, "sender-hash-456");
        assertEquals("https://acme-corp.gtsprod.com/api/whatsapp/call-back/sender-hash-456", result);
    }

    @Test
    void testResolveFromOriginHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Origin", "https://testqa.gtsstaging.com");

        String result = resolver.resolveCallbackUrl(request, null, "c0ffee00-51m0-4a7a-9e01-000000000001");
        assertEquals("https://testqa.gtsstaging.com/api/whatsapp/call-back/c0ffee00-51m0-4a7a-9e01-000000000001", result);
    }

    @Test
    void testResolveFromRefererHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Referer", "https://testqa.gtsstaging.com/campaigns/send");

        String result = resolver.resolveCallbackUrl(request, null, "c0ffee00-51m0-4a7a-9e01-000000000001");
        assertEquals("https://testqa.gtsstaging.com/api/whatsapp/call-back/c0ffee00-51m0-4a7a-9e01-000000000001", result);
    }

    @Test
    void testResolveFromHostHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Host", "testqa.gtsstaging.com");

        String result = resolver.resolveCallbackUrl(request, null, "c0ffee00-51m0-4a7a-9e01-000000000001");
        assertEquals("https://testqa.gtsstaging.com/api/whatsapp/call-back/c0ffee00-51m0-4a7a-9e01-000000000001", result);
    }

    @Test
    void testResolveFromPathTenantDomain() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        String result = resolver.resolveCallbackUrl(request, "testqa.gtsstaging", "c0ffee00-51m0-4a7a-9e01-000000000001");
        assertEquals("https://testqa.gtsstaging.com/api/whatsapp/call-back/c0ffee00-51m0-4a7a-9e01-000000000001", result);
    }

    @Test
    void testResolveFromLocalhostSubdomain() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Host", "testqa.localhost:8000");

        String result = resolver.resolveCallbackUrl(request, null, "1ce8726a-2fe9-41e1-aa5f-1de62039d8c8");
        assertEquals("http://testqa.localhost:8000/api/whatsapp/call-back/1ce8726a-2fe9-41e1-aa5f-1de62039d8c8", result);
    }

    @Test
    void testFallbackToDefaultConfig() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        String result = resolver.resolveCallbackUrl(request, null, "any-sender");
        assertEquals("https://testqa.gtsstaging.com/api/whatsapp/call-back/c0ffee00-51m0-4a7a-9e01-000000000001", result);
    }
}
