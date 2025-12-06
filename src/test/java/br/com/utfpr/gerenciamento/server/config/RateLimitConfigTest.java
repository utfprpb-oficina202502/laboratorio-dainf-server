package br.com.utfpr.gerenciamento.server.config;

import static org.junit.jupiter.api.Assertions.*;

import br.com.utfpr.gerenciamento.server.config.RateLimitConfig.RateLimitedEndpoint;
import com.github.benmanes.caffeine.cache.Cache;
import io.github.bucket4j.Bucket;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RateLimitConfigTest {

  private RateLimitProperties properties;
  private RateLimitConfig config;

  @BeforeEach
  void setUp() {
    properties = new RateLimitProperties();
    config = new RateLimitConfig(properties);
  }

  @Test
  void shouldCreateCacheWithCorrectConfiguration() {
    Cache<String, Bucket> cache = config.rateLimitCache();

    assertNotNull(cache);
    // Cache deve estar vazio inicialmente
    assertEquals(0, cache.estimatedSize());
  }

  @Test
  void shouldCreateEndpointLimitsMapWithAllEndpoints() {
    Map<RateLimitedEndpoint, RateLimitProperties.EndpointLimit> limits = config.endpointLimits();

    assertNotNull(limits);
    assertEquals(12, limits.size());
    // Endpoints publicos
    assertTrue(limits.containsKey(RateLimitedEndpoint.LOGIN));
    assertTrue(limits.containsKey(RateLimitedEndpoint.AUTH));
    assertTrue(limits.containsKey(RateLimitedEndpoint.PASSWORD_RESET_REQUEST));
    assertTrue(limits.containsKey(RateLimitedEndpoint.NEW_USER));
    assertTrue(limits.containsKey(RateLimitedEndpoint.PASSWORD_RESET));
    assertTrue(limits.containsKey(RateLimitedEndpoint.RESEND_EMAIL));
    assertTrue(limits.containsKey(RateLimitedEndpoint.CONFIRM_EMAIL));
    // Endpoints do dashboard pessoal
    assertTrue(limits.containsKey(RateLimitedEndpoint.DASHBOARD_MY_STATS));
    assertTrue(limits.containsKey(RateLimitedEndpoint.DASHBOARD_MY_FREQUENT_ITEMS));
    assertTrue(limits.containsKey(RateLimitedEndpoint.DASHBOARD_MY_USAGE_HISTORY));
    assertTrue(limits.containsKey(RateLimitedEndpoint.DASHBOARD_MY_ACTIVITY));
    assertTrue(limits.containsKey(RateLimitedEndpoint.DASHBOARD_MY_CALENDAR_EVENTS));
  }

  @Test
  void shouldCreateBucketWithCorrectCapacity() {
    RateLimitProperties.EndpointLimit limit = new RateLimitProperties.EndpointLimit(5, 15);

    Bucket bucket = config.createBucket(limit);

    assertNotNull(bucket);
    // Deve consumir 5 tokens com sucesso
    for (int i = 0; i < 5; i++) {
      assertTrue(bucket.tryConsume(1), "Deveria consumir token " + (i + 1));
    }
    // 6o token deve falhar
    assertFalse(bucket.tryConsume(1), "Nao deveria consumir 6o token");
  }

  @Test
  void shouldResolveSameBucketForSameIpAndEndpoint() {
    Cache<String, Bucket> cache = config.rateLimitCache();
    Map<RateLimitedEndpoint, RateLimitProperties.EndpointLimit> limits = config.endpointLimits();

    Bucket bucket1 = config.resolveBucket(cache, "192.168.1.1", RateLimitedEndpoint.LOGIN, limits);
    Bucket bucket2 = config.resolveBucket(cache, "192.168.1.1", RateLimitedEndpoint.LOGIN, limits);

    assertSame(bucket1, bucket2);
  }

  @Test
  void shouldResolveDifferentBucketsForDifferentIps() {
    Cache<String, Bucket> cache = config.rateLimitCache();
    Map<RateLimitedEndpoint, RateLimitProperties.EndpointLimit> limits = config.endpointLimits();

    Bucket bucket1 = config.resolveBucket(cache, "192.168.1.1", RateLimitedEndpoint.LOGIN, limits);
    Bucket bucket2 = config.resolveBucket(cache, "192.168.1.2", RateLimitedEndpoint.LOGIN, limits);

    assertNotSame(bucket1, bucket2);
  }

  @Test
  void shouldResolveDifferentBucketsForDifferentEndpoints() {
    Cache<String, Bucket> cache = config.rateLimitCache();
    Map<RateLimitedEndpoint, RateLimitProperties.EndpointLimit> limits = config.endpointLimits();

    Bucket bucket1 = config.resolveBucket(cache, "192.168.1.1", RateLimitedEndpoint.LOGIN, limits);
    Bucket bucket2 = config.resolveBucket(cache, "192.168.1.1", RateLimitedEndpoint.AUTH, limits);

    assertNotSame(bucket1, bucket2);
  }

  @Test
  void rateLimitedEndpointShouldReturnCorrectPath() {
    // Endpoints publicos
    assertEquals("/login", RateLimitedEndpoint.LOGIN.getPath());
    assertEquals("/auth", RateLimitedEndpoint.AUTH.getPath());
    assertEquals("/usuario/new-user", RateLimitedEndpoint.NEW_USER.getPath());
    assertEquals(
        "/usuario/request-code-reset-password",
        RateLimitedEndpoint.PASSWORD_RESET_REQUEST.getPath());
    assertEquals("/usuario/reset-password", RateLimitedEndpoint.PASSWORD_RESET.getPath());
    assertEquals("/usuario/resend-confirm-email", RateLimitedEndpoint.RESEND_EMAIL.getPath());
    assertEquals("/usuario/confirm-email", RateLimitedEndpoint.CONFIRM_EMAIL.getPath());
    // Endpoints do dashboard pessoal
    assertEquals("/dashboard/my-stats", RateLimitedEndpoint.DASHBOARD_MY_STATS.getPath());
    assertEquals(
        "/dashboard/my-frequent-items", RateLimitedEndpoint.DASHBOARD_MY_FREQUENT_ITEMS.getPath());
    assertEquals(
        "/dashboard/my-usage-history", RateLimitedEndpoint.DASHBOARD_MY_USAGE_HISTORY.getPath());
    assertEquals("/dashboard/my-activity", RateLimitedEndpoint.DASHBOARD_MY_ACTIVITY.getPath());
    assertEquals(
        "/dashboard/my-calendar-events",
        RateLimitedEndpoint.DASHBOARD_MY_CALENDAR_EVENTS.getPath());
  }

  @Test
  void rateLimitedEndpointFromPathShouldHandleAllEndpoints() {
    // Endpoints publicos
    assertEquals(RateLimitedEndpoint.LOGIN, RateLimitedEndpoint.fromPath("/login"));
    assertEquals(RateLimitedEndpoint.AUTH, RateLimitedEndpoint.fromPath("/auth"));
    assertEquals(RateLimitedEndpoint.NEW_USER, RateLimitedEndpoint.fromPath("/usuario/new-user"));
    assertEquals(
        RateLimitedEndpoint.PASSWORD_RESET_REQUEST,
        RateLimitedEndpoint.fromPath("/usuario/request-code-reset-password"));
    assertEquals(
        RateLimitedEndpoint.PASSWORD_RESET,
        RateLimitedEndpoint.fromPath("/usuario/reset-password"));
    assertEquals(
        RateLimitedEndpoint.RESEND_EMAIL,
        RateLimitedEndpoint.fromPath("/usuario/resend-confirm-email"));
    assertEquals(
        RateLimitedEndpoint.CONFIRM_EMAIL, RateLimitedEndpoint.fromPath("/usuario/confirm-email"));
    // Endpoints do dashboard pessoal
    assertEquals(
        RateLimitedEndpoint.DASHBOARD_MY_STATS,
        RateLimitedEndpoint.fromPath("/dashboard/my-stats"));
    assertEquals(
        RateLimitedEndpoint.DASHBOARD_MY_FREQUENT_ITEMS,
        RateLimitedEndpoint.fromPath("/dashboard/my-frequent-items"));
    assertEquals(
        RateLimitedEndpoint.DASHBOARD_MY_USAGE_HISTORY,
        RateLimitedEndpoint.fromPath("/dashboard/my-usage-history"));
    assertEquals(
        RateLimitedEndpoint.DASHBOARD_MY_ACTIVITY,
        RateLimitedEndpoint.fromPath("/dashboard/my-activity"));
    assertEquals(
        RateLimitedEndpoint.DASHBOARD_MY_CALENDAR_EVENTS,
        RateLimitedEndpoint.fromPath("/dashboard/my-calendar-events"));
  }

  @Test
  void rateLimitedEndpointShouldReturnCorrectHttpMethodType() {
    // Endpoints publicos sao POST
    assertFalse(RateLimitedEndpoint.LOGIN.isGetEndpoint());
    assertFalse(RateLimitedEndpoint.AUTH.isGetEndpoint());
    assertFalse(RateLimitedEndpoint.NEW_USER.isGetEndpoint());
    assertFalse(RateLimitedEndpoint.PASSWORD_RESET_REQUEST.isGetEndpoint());
    assertFalse(RateLimitedEndpoint.PASSWORD_RESET.isGetEndpoint());
    assertFalse(RateLimitedEndpoint.RESEND_EMAIL.isGetEndpoint());
    assertFalse(RateLimitedEndpoint.CONFIRM_EMAIL.isGetEndpoint());
    // Endpoints do dashboard pessoal sao GET
    assertTrue(RateLimitedEndpoint.DASHBOARD_MY_STATS.isGetEndpoint());
    assertTrue(RateLimitedEndpoint.DASHBOARD_MY_FREQUENT_ITEMS.isGetEndpoint());
    assertTrue(RateLimitedEndpoint.DASHBOARD_MY_USAGE_HISTORY.isGetEndpoint());
    assertTrue(RateLimitedEndpoint.DASHBOARD_MY_ACTIVITY.isGetEndpoint());
    assertTrue(RateLimitedEndpoint.DASHBOARD_MY_CALENDAR_EVENTS.isGetEndpoint());
  }

  @Test
  void rateLimitedEndpointFromPathShouldReturnNullForUnknownPaths() {
    assertNull(RateLimitedEndpoint.fromPath("/unknown"));
    assertNull(RateLimitedEndpoint.fromPath("/usuario"));
    assertNull(RateLimitedEndpoint.fromPath("/item"));
    assertNull(RateLimitedEndpoint.fromPath(null));
    assertNull(RateLimitedEndpoint.fromPath(""));
  }
}
