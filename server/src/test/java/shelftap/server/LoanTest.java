package shelftap.server;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import shelftap.server.loan.LoanService;
import shelftap.server.loan.ReservationService;
import shelftap.server.member.Member;
import tools.jackson.databind.JsonNode;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoanTest extends IntegrationTest {
    @Autowired
    LoanService loanService;
    @Autowired
    ReservationService reservationService;

    Res checkout(Session who, String tagId) throws Exception {
        return post("/api/loans", Map.of("tagId", tagId), who.access());
    }

    UUID openLoanId(String tagId) {
        return jdbc.sql("select l.id from loan l join copy c on c.id = l.copy_id where c.tag_id = :t and l.returned_at is null")
                .param("t", tagId).query(UUID.class).single();
    }

    // --- self-checkout ---

    @Test
    void checkoutCreatesAFourteenDayLoan() throws Exception {
        Session member = login(newMember());
        TitleFixture book = newTitle(2);

        Res res = checkout(member, book.tagIds().get(0));
        assertEquals(201, res.status());
        JsonNode loan = res.body();
        assertEquals(book.titleId().toString(), loan.get("titleId").asString());
        assertEquals(book.copyIds().get(0).toString(), loan.get("copyId").asString());
        assertEquals(book.title(), loan.get("title").asString());
        assertEquals(Duration.ofDays(14), Duration.between(Instant.parse(loan.get("outAt").asString()), Instant.parse(loan.get("dueAt").asString())));
        assertTrue(loan.get("returnedAt").isNull());
        assertEquals(0, loan.get("renewals").asInt());
        assertTrue(loan.get("renewable").asBoolean());

        JsonNode mine = get("/api/me/loans", member.access()).body();
        assertEquals(1, mine.size());
        assertEquals(loan.get("loanId").asString(), mine.get(0).get("loanId").asString());
        assertEquals(1, get("/api/titles/" + book.titleId(), null).body().get("copiesAvailable").asInt());
    }

    @Test
    void unknownAndMalformedTagsAreRejected() throws Exception {
        Session member = login(newMember());
        assertEquals("NOT_FOUND", checkout(member, "FFFFFFFFFFFFFFFF").code());
        assertEquals("VALIDATION_FAILED", checkout(member, "abc").code());
        assertEquals("VALIDATION_FAILED", checkout(member, "0123456789abcdef").code());
    }

    @Test
    void aCopyOnLoanCannotBeTakenAgain() throws Exception {
        Session first = login(newMember());
        Session second = login(newMember());
        TitleFixture book = newTitle(1);
        assertEquals(201, checkout(first, book.tagIds().get(0)).status());

        assertEquals("COPY_NOT_AVAILABLE", checkout(second, book.tagIds().get(0)).code());
        assertEquals("COPY_NOT_AVAILABLE", checkout(first, book.tagIds().get(0)).code(), "double tap by the owner");
    }

    @Test
    void aWithdrawnCopyIsNotLendable() throws Exception {
        Session member = login(newMember());
        TitleFixture book = newTitle(1);
        jdbc.sql("update copy set status = 'WITHDRAWN' where id = :id").param("id", book.copyIds().get(0)).update();
        Res res = checkout(member, book.tagIds().get(0));
        assertEquals(409, res.status());
        assertEquals("COPY_NOT_AVAILABLE", res.code());
    }

    @Test
    void theSixthLoanHitsTheLimit() throws Exception {
        Session member = login(newMember());
        TitleFixture books = newTitle(6);
        for (int i = 0; i < 5; i++) {
            assertEquals(201, checkout(member, books.tagIds().get(i)).status());
        }
        Res sixth = checkout(member, books.tagIds().get(5));
        assertEquals(409, sixth.status());
        assertEquals("LOAN_LIMIT_REACHED", sixth.code());
    }

    @Test
    void anUnpaidFineBlocksNewLoansUntilPaid() throws Exception {
        Account account = newMember();
        Session member = login(account);
        TitleFixture book = newTitle(1);
        UUID fineId = UUID.randomUUID();
        jdbc.sql("insert into fine (id, loan_id, member_id, amount_vnd, assessed_at, paid_at) values (:id, null, :m, 5000, now(), null)")
                .param("id", fineId).param("m", account.id()).update();

        Res blocked = checkout(member, book.tagIds().get(0));
        assertEquals(409, blocked.status());
        assertEquals("UNPAID_FINE", blocked.code());
        JsonNode fines = get("/api/me/fines", member.access()).body();
        assertEquals(1, fines.size());
        assertEquals(5000, fines.get(0).get("amountVnd").asInt());
        assertTrue(fines.get(0).get("paidAt").isNull());

        jdbc.sql("update fine set paid_at = now() where id = :id").param("id", fineId).update();
        assertEquals(201, checkout(member, book.tagIds().get(0)).status());
    }

    @Test
    void aSuspendedMemberCannotBorrowOrReserve() throws Exception {
        Session member = login(newMember(false, Member.Status.SUSPENDED));
        TitleFixture book = newTitle(1);
        assertEquals("FORBIDDEN", checkout(member, book.tagIds().get(0)).code());
        assertEquals("FORBIDDEN", post("/api/reservations", Map.of("titleId", book.titleId().toString()), member.access()).code());
    }

    @Test
    void twoMembersTappingTheSameTagNeverBothWin() throws Exception {
        List<Session> pool = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            pool.add(login(newMember()));
        }
        for (int round = 0; round < 5; round++) {
            TitleFixture book = newTitle(1);
            String tag = book.tagIds().get(0);
            List<Res> results = race(List.of(() -> checkout(pool.get(0), tag), () -> checkout(pool.get(1), tag),
                    () -> checkout(pool.get(2), tag), () -> checkout(pool.get(3), tag)));
            assertEquals(1, results.stream().filter(r -> r.status() == 201).count(), "round " + round);
            assertEquals(3, results.stream().filter(r -> r.status() == 409 && r.code().equals("COPY_NOT_AVAILABLE")).count());
            assertEquals(1, jdbc.sql("select count(*) from loan where copy_id = :c and returned_at is null")
                    .param("c", book.copyIds().get(0)).query(Long.class).single());
        }
    }

    @Test
    void oneMemberTappingManyTagsAtOnceCannotBeatTheLimit() throws Exception {
        Session member = login(newMember());
        TitleFixture books = newTitle(9);
        List<Callable<Res>> taps = new ArrayList<>();
        for (String tag : books.tagIds()) {
            taps.add(() -> checkout(member, tag));
        }
        List<Res> results = race(taps);
        assertEquals(5, results.stream().filter(r -> r.status() == 201).count());
        assertEquals(4, results.stream().filter(r -> r.code().equals("LOAN_LIMIT_REACHED")).count());
        assertEquals(5, get("/api/me/loans", member.access()).body().size());
    }

    // --- renew ---

    @Test
    void renewExtendsOnceThenRefuses() throws Exception {
        Session member = login(newMember());
        TitleFixture book = newTitle(1);
        JsonNode loan = checkout(member, book.tagIds().get(0)).body();
        Instant due = Instant.parse(loan.get("dueAt").asString());

        Res renewed = post("/api/loans/" + loan.get("loanId").asString() + "/renew", null, member.access());
        assertEquals(200, renewed.status());
        assertEquals(due.plus(Duration.ofDays(14)), Instant.parse(renewed.body().get("dueAt").asString()));
        assertEquals(1, renewed.body().get("renewals").asInt());
        assertFalse(renewed.body().get("renewable").asBoolean());

        Res again = post("/api/loans/" + loan.get("loanId").asString() + "/renew", null, member.access());
        assertEquals(409, again.status());
        assertEquals("RENEWAL_NOT_ALLOWED", again.code());
    }

    @Test
    void anOverdueLoanCannotBeRenewed() throws Exception {
        Account account = newMember();
        JsonNode loan = checkout(login(account), newTitle(1).tagIds().get(0)).body();
        clock.advance(Duration.ofDays(15));
        Session later = login(account); // the first access token expired with the clock jump
        Res res = post("/api/loans/" + loan.get("loanId").asString() + "/renew", null, later.access());
        assertEquals(409, res.status());
        assertEquals("RENEWAL_NOT_ALLOWED", res.code());
        assertFalse(get("/api/me/loans", later.access()).body().get(0).get("renewable").asBoolean());
    }

    @Test
    void renewIsRefusedWhileAnotherMemberWaitsForTheTitle() throws Exception {
        Session holder = login(newMember());
        Session waiter = login(newMember());
        TitleFixture book = newTitle(1);
        JsonNode loan = checkout(holder, book.tagIds().get(0)).body();
        assertTrue(loan.get("renewable").asBoolean());

        assertEquals(201, post("/api/reservations", Map.of("titleId", book.titleId().toString()), waiter.access()).status());

        assertFalse(get("/api/me/loans", holder.access()).body().get(0).get("renewable").asBoolean());
        assertEquals("RENEWAL_NOT_ALLOWED", post("/api/loans/" + loan.get("loanId").asString() + "/renew", null, holder.access()).code());
    }

    @Test
    void anotherMembersLoanLooksLikeAnUnknownOne() throws Exception {
        Session owner = login(newMember());
        Session intruder = login(newMember());
        JsonNode loan = checkout(owner, newTitle(1).tagIds().get(0)).body();

        Res stolen = post("/api/loans/" + loan.get("loanId").asString() + "/renew", null, intruder.access());
        Res missing = post("/api/loans/" + UUID.randomUUID() + "/renew", null, intruder.access());
        assertEquals(404, stolen.status());
        assertEquals("NOT_FOUND", stolen.code());
        assertEquals(missing.status(), stolen.status());
        assertEquals(missing.body().get("detail").asString(), stolen.body().get("detail").asString());
        assertEquals(0, get("/api/me/loans", intruder.access()).body().size());
        assertEquals(0, get("/api/me/history", intruder.access()).body().size());
    }

    // --- reservations ---

    @Test
    void reserveAndCancel() throws Exception {
        Session member = login(newMember());
        Session other = login(newMember());
        TitleFixture book = newTitle(1);

        Res reserved = post("/api/reservations", Map.of("titleId", book.titleId().toString()), member.access());
        assertEquals(201, reserved.status());
        assertEquals("WAITING", reserved.body().get("status").asString());
        assertTrue(reserved.body().get("readyAt").isNull());
        assertEquals(book.title(), reserved.body().get("title").asString());
        String id = reserved.body().get("reservationId").asString();

        assertEquals("ALREADY_RESERVED", post("/api/reservations", Map.of("titleId", book.titleId().toString()), member.access()).code());
        assertEquals("NOT_FOUND", post("/api/reservations", Map.of("titleId", UUID.randomUUID().toString()), member.access()).code());
        assertEquals(1, get("/api/me/reservations", member.access()).body().size());
        assertEquals(0, get("/api/me/reservations", other.access()).body().size());

        assertEquals("NOT_FOUND", delete("/api/reservations/" + id, other.access()).code(), "someone else's reservation");
        assertEquals(204, delete("/api/reservations/" + id, member.access()).status());
        assertEquals(204, delete("/api/reservations/" + id, member.access()).status(), "cancelling twice is harmless");
        assertEquals("CANCELLED", get("/api/me/reservations", member.access()).body().get(0).get("status").asString());
        assertEquals(201, post("/api/reservations", Map.of("titleId", book.titleId().toString()), member.access()).status(), "can reserve again after cancelling");
    }

    @Test
    void aMemberCannotReserveATitleTheyAlreadyHaveOut() throws Exception {
        Session member = login(newMember());
        TitleFixture book = newTitle(2);
        assertEquals(201, checkout(member, book.tagIds().get(0)).status());
        assertEquals("ALREADY_RESERVED", post("/api/reservations", Map.of("titleId", book.titleId().toString()), member.access()).code());
    }

    @Test
    void aReturnedCopyIsHeldForTheFirstWaitingReservation() throws Exception {
        Session holder = login(newMember());
        Session first = login(newMember());
        Session second = login(newMember());
        Session stranger = login(newMember());
        TitleFixture book = newTitle(1);
        String tag = book.tagIds().get(0);
        assertEquals(201, checkout(holder, tag).status());
        String firstReservation = post("/api/reservations", Map.of("titleId", book.titleId().toString()), first.access()).body().get("reservationId").asString();
        post("/api/reservations", Map.of("titleId", book.titleId().toString()), second.access());

        loanService.checkIn(openLoanId(tag));

        JsonNode ready = get("/api/me/reservations", first.access()).body().get(0);
        assertEquals("READY", ready.get("status").asString());
        assertFalse(ready.get("readyAt").isNull());
        assertEquals("WAITING", get("/api/me/reservations", second.access()).body().get(0).get("status").asString());
        assertEquals(0, get("/api/titles/" + book.titleId(), null).body().get("copiesAvailable").asInt(), "a held copy is not available");

        Res blocked = checkout(stranger, tag);
        assertEquals(409, blocked.status());
        assertEquals("RESERVED_BY_OTHER", blocked.code());
        assertEquals("RESERVED_BY_OTHER", checkout(second, tag).code());

        assertEquals(201, checkout(first, tag).status());
        assertEquals("FULFILLED", get("/api/me/reservations", first.access()).body().get(0).get("status").asString());
        assertEquals(firstReservation, get("/api/me/reservations", first.access()).body().get(0).get("reservationId").asString());
    }

    @Test
    void anUnclaimedHoldExpiresAndPassesOn() throws Exception {
        Account first = newMember();
        Account second = newMember();
        TitleFixture book = newTitle(1);
        String tag = book.tagIds().get(0);
        checkout(login(newMember()), tag);
        post("/api/reservations", Map.of("titleId", book.titleId().toString()), login(first).access());
        post("/api/reservations", Map.of("titleId", book.titleId().toString()), login(second).access());
        loanService.checkIn(openLoanId(tag));

        clock.advance(Duration.ofDays(4));
        assertTrue(reservationService.expireHolds() >= 1, "other tests may leave holds of their own");

        assertEquals("EXPIRED", get("/api/me/reservations", login(first).access()).body().get(0).get("status").asString());
        assertEquals("READY", get("/api/me/reservations", login(second).access()).body().get(0).get("status").asString());
    }

    @Test
    void cancellingAReadyReservationPassesTheCopyOn() throws Exception {
        Session holder = login(newMember());
        Session first = login(newMember());
        Session second = login(newMember());
        TitleFixture book = newTitle(1);
        String tag = book.tagIds().get(0);
        checkout(holder, tag);
        String id = post("/api/reservations", Map.of("titleId", book.titleId().toString()), first.access()).body().get("reservationId").asString();
        post("/api/reservations", Map.of("titleId", book.titleId().toString()), second.access());
        loanService.checkIn(openLoanId(tag));

        assertEquals(204, delete("/api/reservations/" + id, first.access()).status());
        assertEquals("READY", get("/api/me/reservations", second.access()).body().get(0).get("status").asString());
    }

    // --- return and fines (domain only, no HTTP route yet) ---

    @Test
    void anOverdueReturnChargesADayRateFineThatBlocksTheNextLoan() throws Exception {
        Account account = newMember();
        Session member = login(account);
        TitleFixture book = newTitle(2);
        String tag = book.tagIds().get(0);
        checkout(member, tag);

        clock.advance(Duration.ofDays(14).plusHours(30));
        loanService.checkIn(openLoanId(tag));

        Session again = login(account);
        JsonNode fines = get("/api/me/fines", again.access()).body();
        assertEquals(1, fines.size());
        assertEquals(2000, fines.get(0).get("amountVnd").asInt(), "30 hours late is 2 started days at 1000 VND");
        assertEquals("UNPAID_FINE", checkout(again, book.tagIds().get(1)).code());
    }

    // --- gate ---

    @Test
    void theGateSeesLoanStatusWithItsDeviceKey() throws Exception {
        Session member = login(newMember());
        TitleFixture book = newTitle(1);
        String tag = book.tagIds().get(0);

        assertFalse(gate(tag, GATE_KEY).body().get("onLoan").asBoolean());
        checkout(member, tag);
        Res onLoan = gate(tag, GATE_KEY);
        assertEquals(200, onLoan.status());
        assertEquals(tag, onLoan.body().get("tagId").asString());
        assertTrue(onLoan.body().get("onLoan").asBoolean());

        assertEquals("FORBIDDEN", gate(tag, randomKey()).code());
        assertEquals("FORBIDDEN", gate(tag, null).code());
        assertEquals("FORBIDDEN", gate(tag, GATE_KEY.substring(0, GATE_KEY.length() - 1)).code());
        assertEquals("NOT_FOUND", gate("FFFFFFFFFFFFFFFE", GATE_KEY).code());
        assertEquals(403, call(withToken(MockMvcRequestBuilders.get("/api/gate/" + tag).header("Authorization", "Bearer " + member.access()), null)).status(),
                "a member token is not a gate key");
    }

    Res gate(String tag, String key) throws Exception {
        var request = MockMvcRequestBuilders.get("/api/gate/" + tag);
        if (key != null) {
            request.header("X-Gate-Key", key);
        }
        return call(request);
    }

    // --- audit ---

    @Test
    void everyCirculationActionAndDenialIsAudited() throws Exception {
        Account account = newMember();
        Session member = login(account);
        Session other = login(newMember());
        TitleFixture book = newTitle(1);
        String tag = book.tagIds().get(0);

        var ok = MockMvcRequestBuilders.post("/api/loans").contentType("application/json")
                .content("{\"tagId\":\"" + tag + "\"}").header("Authorization", "Bearer " + member.access())
                .header("X-Device-Integrity", "root, emulator, <script>");
        assertEquals(201, call(ok).status());
        assertEquals("COPY_NOT_AVAILABLE", checkout(other, tag).code());

        var rows = jdbc.sql("select outcome, actor_type, actor_id, ip, device_id, integrity from audit_log where action = 'LOAN_OUT' and (actor_id = :a or target = :t) order by id")
                .param("a", account.id().toString()).param("t", tag).query().listOfRows();
        assertEquals("OK", rows.get(0).get("outcome"));
        assertEquals("MEMBER", rows.get(0).get("actor_type"));
        assertEquals(account.id().toString(), rows.get(0).get("actor_id"));
        assertEquals(member.device().toString(), rows.get(0).get("device_id"));
        assertEquals("root,emulator", rows.get(0).get("integrity"), "only known flags are stored");
        assertTrue(rows.get(0).get("ip") != null);
        assertEquals("DENIED_COPY_NOT_AVAILABLE", rows.stream().filter(r -> !r.get("actor_id").equals(account.id().toString()))
                .findFirst().orElseThrow().get("outcome"));
    }

    // --- helpers ---

    /** Fires all calls at once from separate threads. */
    <T> List<T> race(List<Callable<T>> calls) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(calls.size());
        try {
            CountDownLatch ready = new CountDownLatch(calls.size());
            CountDownLatch go = new CountDownLatch(1);
            List<Future<T>> futures = new ArrayList<>();
            for (Callable<T> call : calls) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    go.await();
                    return call.call();
                }));
            }
            ready.await();
            go.countDown();
            List<T> out = new ArrayList<>();
            for (Future<T> f : futures) {
                out.add(f.get());
            }
            return out;
        } finally {
            pool.shutdownNow();
        }
    }

}
