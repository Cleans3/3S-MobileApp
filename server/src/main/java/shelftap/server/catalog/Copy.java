package shelftap.server.catalog;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/** One physical book. Titles are only read through {@link Catalog}, so they have no entity. */
@Entity
@Table(name = "copy")
public class Copy {
    /** LENDABLE still means "may be lent"; whether it is out is decided by an open loan. */
    public enum Status { LENDABLE, WITHDRAWN, LOST }

    @Id
    public UUID id;
    public UUID titleId;
    public String barcode;
    public String tagId;
    public String shelf;
    @Enumerated(EnumType.STRING)
    public Status status;
}
