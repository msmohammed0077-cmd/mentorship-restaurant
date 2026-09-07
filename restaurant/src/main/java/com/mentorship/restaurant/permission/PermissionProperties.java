package com.mentorship.restaurant.permission;

import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Binds {@code app.permissions.<userId>=<comma-separated codes>}. */
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class PermissionProperties {

  private Map<Long, List<String>> permissions = Map.of();
}
