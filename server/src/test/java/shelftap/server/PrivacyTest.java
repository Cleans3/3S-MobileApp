package shelftap.server;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import shelftap.server.loan.LoanService;
import shelftap.server.loan.Retention;
import tools.jackson.databind.JsonNode;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** Decree 13/2023: history opt-out and retention (SPEC.md §7). */
class PrivacyTest extends IntegrationTest {
    @Autowired
    LoanService loanService;
    @Autowired
    Retention retention;

    /** Borrows a fresh book and hands it back at the desk. Returns the loan id. */
    UUID borrowAndReturn(Session member) throws Exception {
        TitleFixture book = newTitle(1);
        assertEquals(201, post("/api/loans", Map.of("tagId", book.tagIds().get(0)), member.access()).status());
        UUID loanId = jdbc.sql("select l.id from loan l where l.copy_id = :c").param("c", book.copyIds().get(0)).query(UUID.class).single();
        loanService.checkIn(loanId);
        return loanId;
    }

    UUID linkedMember(UUID loanId) {
        return jdbc.sql("select member_id from loan where id = :id").param("id", loanId).query(UUID.class).optional().orElse(null);
    }

    @Test
    void historyListsReturnedLoansOnly() throws Exception {
        Session member = login(newMember());
        borrowAndReturn(member);
        TitleFixture stillOut = newTitle(1);
        post("/api/loans", Map.of("tagId", stillOut.tagIds().get(0)), member.access());

        JsonNode history = get("/api/me/history", member.access()).body();
        assertEquals(1, history.size());
        assertFalse(history.get(0).get("returnedAt").isNull());
        assertEquals(1, get("/api/me/loans", member.access()).body().size());
    }

    @Test
    void turningHistoryOffHidesItAndUnlinksWhatIsStored() throws Exception {
        Account account = newMember();
        Session member = login(account);
        UUID returned = borrowAndReturn(member);
        TitleFixture stillOut = newTitle(1);
        post("/api/loans", Map.of("tagId", stillOut.tagIds().get(0)), member.access());

        Res off = put("/api/me/privacy", Map.of("historyEnabled", false), member.access());
        assertEquals(200, off.status());
        assertEquals(false, off.body().get("historyEnabled").asBoolean());

        assertEquals(0, get("/api/me/history", member.access()).body().size());
        assertEquals(null, linkedMember(returned), "the returned loan lost its member link");
        assertEquals(1, get("/api/me/loans", member.access()).body().size(), "a loan still out keeps its link");

        put("/api/me/privacy", Map.of("historyEnabled", true), member.access());
        assertEquals(0, get("/api/me/history", member.access()).body().size(), "what was unlinked does not come back");
    }

    @Test
    void withHistoryOffAReturnNeverLinksTheLoanToTheMember() throws Exception {
        Session member = login(newMember());
        put("/api/me/privacy", Map.of("historyEnabled", false), member.access());
        UUID returned = borrowAndReturn(member);
        assertEquals(null, linkedMember(returned));
    }

    @Test
    void retentionUnlinksReturnedLoansAfterTheRetentionPeriod() throws Exception {
        Account account = newMember();
        UUID returned = borrowAndReturn(login(account));

        clock.advance(Duration.ofDays(364));
        retention.run();
        assertEquals(account.id(), linkedMember(returned), "inside the retention period");

        clock.advance(Duration.ofDays(2));
        retention.run();
        assertEquals(null, linkedMember(returned), "past the retention period");
    }

    @Test
    void anUnpaidFineKeepsTheLinkUntilItIsPaidThenTheFineLosesTheBook() throws Exception {
        Account account = newMember();
        Session member = login(account);
        TitleFixture book = newTitle(1);
        post("/api/loans", Map.of("tagId", book.tagIds().get(0)), member.access());
        UUID loanId = jdbc.sql("select id from loan where copy_id = :c").param("c", book.copyIds().get(0)).query(UUID.class).single();
        clock.advance(Duration.ofDays(20));
        loanService.checkIn(loanId);
        UUID fineId = jdbc.sql("select id from fine where member_id = :m").param("m", account.id()).query(UUID.class).single();

        clock.advance(Duration.ofDays(400));
        retention.run();
        assertEquals(account.id(), linkedMember(loanId), "unpaid fine still points at the loan");

        jdbc.sql("update fine set paid_at = now() where id = :id").param("id", fineId).update();
        retention.run();
        assertEquals(null, linkedMember(loanId));
        assertEquals(null, jdbc.sql("select loan_id from fine where id = :id").param("id", fineId).query(UUID.class).optional().orElse(null),
                "a paid fine no longer names the book");
    }
}
