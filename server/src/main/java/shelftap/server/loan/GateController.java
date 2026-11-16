package shelftap.server.loan;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import shelftap.server.audit.AuditService;
import shelftap.server.audit.AuditService.Actor;
import shelftap.server.catalog.Copy;
import shelftap.server.catalog.CopyRepository;
import shelftap.server.config.ShelfTapProperties;
import shelftap.server.security.Hashing;
import shelftap.server.web.ApiException;
import shelftap.server.web.ErrorCode;

import java.util.regex.Pattern;

/** Anti-theft gate: asks whether a tag's copy is on loan. Authenticated by a device key, not a user token. */
@RestController
@RequestMapping("/api/gate")
class GateController {
    private static final Pattern TAG_ID = Pattern.compile("[0-9A-F]{16}");

    record GateAnswer(String tagId, boolean onLoan) {
    }

    private final CopyRepository copies;
    private final LoanRepository loans;
    private final AuditService audit;
    private final String gateKey;

    GateController(CopyRepository copies, LoanRepository loans, AuditService audit, ShelfTapProperties props) {
        this.copies = copies;
        this.loans = loans;
        this.audit = audit;
        this.gateKey = props.gate().key();
    }

    @GetMapping("/{tagId}")
    GateAnswer check(@PathVariable String tagId, @RequestHeader(name = "X-Gate-Key", required = false) String key) {
        // Constant-time compare; a missing key and a wrong key look the same.
        if (key == null || !Hashing.constantTimeEquals(key, gateKey)) {
            audit.log(Actor.GATE, null, "GATE_CHECK", null, "BAD_KEY");
            throw new ApiException(ErrorCode.FORBIDDEN, "gate key rejected");
        }
        if (!TAG_ID.matcher(tagId).matches()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "tagId must be 16 uppercase hex characters");
        }
        Copy copy = copies.findByTagId(tagId).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "no book with this tag"));
        boolean onLoan = loans.existsByCopyIdAndReturnedAtIsNull(copy.id);
        audit.log(Actor.GATE, null, "GATE_CHECK", tagId, onLoan ? "ON_LOAN" : "NOT_ON_LOAN");
        return new GateAnswer(tagId, onLoan);
    }
}
