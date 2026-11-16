package shelftap.server;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogTest extends IntegrationTest {

    @Test
    void searchMatchesTitleAuthorIsbnAndSubjectCaseInsensitively() throws Exception {
        String word = "Zephyr" + randomHex(3);
        TitleFixture book = newTitle("The " + word + " Atlas", 2);
        jdbc.sql("update title set authors = array['Ada " + word + "son'], subjects = array['Cartography " + word + "'] where id = :id")
                .param("id", book.titleId()).update();
        String isbn = jdbc.sql("select isbn from title where id = :id").param("id", book.titleId()).query(String.class).single();

        for (String q : new String[]{word.toLowerCase(), word.toUpperCase(), "ada " + word.toLowerCase() + "son", "cartography", isbn}) {
            JsonNode page = get("/api/titles?q=" + q, null).body();
            assertTrue(page.get("total").asLong() >= 1, q);
            assertTrue(contains(page, book.titleId()), q);
        }
        JsonNode hit = get("/api/titles?q=" + word, null).body().get("items").get(0);
        assertEquals(book.titleId().toString(), hit.get("titleId").asString());
        assertEquals(2, hit.get("copiesAvailable").asInt());
        assertTrue(hit.get("coverUrl").isNull());
        assertEquals(1, hit.get("authors").size());
    }

    @Test
    void pagingAndTheSizeCap() throws Exception {
        String word = "Pager" + randomHex(3);
        for (int i = 0; i < 5; i++) {
            newTitle(word + " " + i, 1);
        }
        JsonNode first = get("/api/titles?q=" + word + "&page=0&size=2", null).body();
        assertEquals(5, first.get("total").asLong());
        assertEquals(2, first.get("items").size());
        assertEquals(0, first.get("page").asInt());
        assertEquals(2, first.get("size").asInt());
        assertEquals(1, get("/api/titles?q=" + word + "&page=2&size=2", null).body().get("items").size());
        assertEquals(50, get("/api/titles?size=500", null).body().get("size").asInt());
        assertEquals("VALIDATION_FAILED", get("/api/titles?size=0", null).code());
        assertEquals("VALIDATION_FAILED", get("/api/titles?page=-1", null).code());
        assertEquals("VALIDATION_FAILED", get("/api/titles?q=" + "x".repeat(101), null).code());
        assertEquals("VALIDATION_FAILED", get("/api/titles?page=abc", null).code());
    }

    @Test
    void likeWildcardsAndSqlInjectionAreInert() throws Exception {
        String word = "Wild" + randomHex(3);
        TitleFixture percent = newTitle(word + " 100% done", 1);
        TitleFixture plain = newTitle(word + " plain", 1);

        // MockMvc percent-encodes the template, so a raw % reaches the server as the literal character.
        JsonNode withPercent = get("/api/titles?q=" + word + " 100%", null).body();
        assertTrue(contains(withPercent, percent.titleId()));
        assertTrue(!contains(withPercent, plain.titleId()), "% is a literal here, not a wildcard");
        JsonNode underscore = get("/api/titles?q=" + word + "_", null).body();
        assertEquals(0, underscore.get("items").size(), "_ is a literal here, not a wildcard");

        Res injected = get("/api/titles?q='; drop table title; --", null);
        assertEquals(200, injected.status());
        assertEquals(0, injected.body().get("total").asLong());
        assertEquals(200, get("/api/titles/" + plain.titleId(), null).status(), "table still there");
    }

    @Test
    void detailShowsCopyCounts() throws Exception {
        TitleFixture book = newTitle(3);
        Session member = login(newMember());
        post("/api/loans", java.util.Map.of("tagId", book.tagIds().get(0)), member.access());

        JsonNode detail = get("/api/titles/" + book.titleId(), null).body();
        assertEquals(3, detail.get("copiesTotal").asInt());
        assertEquals(2, detail.get("copiesAvailable").asInt());
        assertEquals(book.title(), detail.get("title").asString());

        assertEquals("NOT_FOUND", get("/api/titles/" + UUID.randomUUID(), null).code());
        assertEquals("VALIDATION_FAILED", get("/api/titles/not-a-uuid", null).code());
    }

    private static boolean contains(JsonNode page, UUID titleId) {
        for (JsonNode item : page.get("items")) {
            if (item.get("titleId").asString().equals(titleId.toString())) {
                return true;
            }
        }
        return false;
    }
}
