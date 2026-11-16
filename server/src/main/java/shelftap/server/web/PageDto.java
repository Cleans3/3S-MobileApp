package shelftap.server.web;

import java.util.List;

/** `Page<T>` of API.md. */
public record PageDto<T>(List<T> items, int page, int size, long total) {
}
