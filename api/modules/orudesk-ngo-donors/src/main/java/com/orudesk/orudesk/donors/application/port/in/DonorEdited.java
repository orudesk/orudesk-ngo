package com.orudesk.orudesk.donors.application.port.in;

import com.orudesk.orudesk.shared.AuditableEvent;
import com.orudesk.orudesk.shared.FieldChange;

import java.util.List;
import java.util.UUID;

/**
 * The NGO's details of a donor changed (Day 49). The PAN is in the list only by name, never its values: the
 * audit log is read by more people than may see a PAN. The person's own details are recorded by the core as
 * party.person.edited, on the person.
 */
public record DonorEdited(UUID donorId, String fullName, List<FieldChange> changes) implements AuditableEvent {

    public DonorEdited {
        changes = List.copyOf(changes);
    }

    @Override public String action() { return "donors.donor.edited"; }
    @Override public String subjectType() { return "donor"; }
    @Override public UUID subjectId() { return donorId; }

    @Override
    public String summary() {
        return "Changed " + String.join(", ", changes.stream().map(FieldChange::field).toList()) + " of donor " + fullName;
    }
}
