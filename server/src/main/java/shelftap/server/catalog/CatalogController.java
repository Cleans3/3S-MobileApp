package shelftap.server.catalog;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import shelftap.server.web.ApiException;
import shelftap.server.web.ErrorCode;
import shelftap.server.web.PageDto;

import java.util.UUID;

@RestController
@RequestMapping("/api/titles")
class CatalogController {
    private static final int MAX_SIZE = 50;
    private static final int MAX_QUERY = 100;

    private final Catalog catalog;

    CatalogController(Catalog catalog) {
        this.catalog = catalog;
    }

    @GetMapping
    PageDto<Catalog.TitleSummary> search(@RequestParam(defaultValue = "") String q,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || q.length() > MAX_QUERY) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "page, size or q out of range");
        }
        return catalog.search(q.strip(), page, Math.min(size, MAX_SIZE));
    }

    @GetMapping("/{titleId}")
    Catalog.TitleDetail detail(@PathVariable UUID titleId) {
        return catalog.find(titleId).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "title not found"));
    }
}
