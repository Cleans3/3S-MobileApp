package shelftap.server.audit;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** One row per auth, card or circulation action. The table is append-only (V1__schema.sql trigger). */
@Entity
@Table(name = "audit_log")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public Instant at;
    public String actorType;
    public String actorId;
    public String action;
    public String target;
    public String outcome;
    public String ip;
    public String deviceId;
    public String integrity;
}
