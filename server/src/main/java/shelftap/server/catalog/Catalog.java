package shelftap.server.catalog;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import shelftap.server.web.PageDto;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Read side of the catalog. Every value reaches SQL as a bind parameter; the search text is also LIKE-escaped. */
@Component
public class Catalog {
    public record TitleSummary(UUID titleId, String isbn, String title, List<String> authors, List<String> subjects,
                               String coverUrl, int copiesAvailable) {
    }

    public record TitleDetail(UUID titleId, String isbn, String title, List<String> authors, List<String> subjects,
                              String coverUrl, int copiesAvailable, int copiesTotal) {
    }

    /** Lendable, not out, and not held for a reservation. */
    private static final String AVAILABLE = """
            (select count(*) from copy c where c.title_id = t.id and c.status = 'LENDABLE'
               and not exists (select 1 from loan l where l.copy_id = c.id and l.returned_at is null)
               and not exists (select 1 from reservation r where r.held_copy_id = c.id and r.status = 'READY'))""";

    private final JdbcClient jdbc;

    Catalog(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public PageDto<TitleSummary> search(String q, int page, int size) {
        String pattern = "%" + escapeLike(q) + "%";
        String match = """
                (:q = '' or t.title ilike :p escape '\\' or t.isbn ilike :p escape '\\'
                   or array_to_string(t.authors, ' ') ilike :p escape '\\'
                   or array_to_string(t.subjects, ' ') ilike :p escape '\\')""";
        long total = jdbc.sql("select count(*) from title t where " + match)
                .param("q", q).param("p", pattern).query(Long.class).single();
        List<TitleSummary> items = jdbc.sql("select t.id, t.isbn, t.title, t.authors, t.subjects, t.cover_url, "
                        + AVAILABLE + " as available from title t where " + match
                        + " order by t.title, t.id limit :size offset :offset")
                .param("q", q).param("p", pattern).param("size", size).param("offset", (long) page * size)
                .query((rs, n) -> new TitleSummary(rs.getObject("id", UUID.class), rs.getString("isbn"),
                        rs.getString("title"), strings(rs, "authors"), strings(rs, "subjects"),
                        rs.getString("cover_url"), rs.getInt("available")))
                .list();
        return new PageDto<>(items, page, size, total);
    }

    public Optional<TitleDetail> find(UUID titleId) {
        return jdbc.sql("select t.id, t.isbn, t.title, t.authors, t.subjects, t.cover_url, "
                        + AVAILABLE + " as available, (select count(*) from copy c where c.title_id = t.id) as total "
                        + "from title t where t.id = :id")
                .param("id", titleId)
                .query((rs, n) -> new TitleDetail(rs.getObject("id", UUID.class), rs.getString("isbn"),
                        rs.getString("title"), strings(rs, "authors"), strings(rs, "subjects"),
                        rs.getString("cover_url"), rs.getInt("available"), rs.getInt("total")))
                .optional();
    }

    public boolean exists(UUID titleId) {
        return jdbc.sql("select count(*) from title where id = :id").param("id", titleId).query(Long.class).single() > 0;
    }

    private static String escapeLike(String q) {
        return q.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static List<String> strings(ResultSet rs, String column) throws SQLException {
        Array array = rs.getArray(column);
        return List.of((String[]) array.getArray());
    }
}
