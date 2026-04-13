package com.github.willrees23.reportx.paper.evidence;

import com.github.willrees23.reportx.core.messaging.events.EvidenceAddedEvent;
import com.github.willrees23.reportx.core.messaging.events.EvidenceDeletedEvent;
import com.github.willrees23.reportx.core.messaging.events.EvidenceEditedEvent;
import com.github.willrees23.reportx.core.messaging.events.ReportXEvent;
import com.github.willrees23.reportx.core.model.Evidence;
import com.github.willrees23.reportx.core.storage.EvidenceRepository;
import com.github.willrees23.reportx.core.util.Clock;
import com.github.willrees23.reportx.messaging.local.LocalMessageBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EvidenceServiceTest {

    private static final Instant NOW = Instant.parse("2026-04-13T12:00:00Z");

    private InMemoryEvidenceRepo repo;
    private LocalMessageBus bus;
    private List<ReportXEvent> received;
    private EvidenceService service;

    @BeforeEach
    void setUp() {
        repo = new InMemoryEvidenceRepo();
        bus = new LocalMessageBus();
        received = new ArrayList<>();
        bus.subscribe(ReportXEvent.class, received::add);
        service = new EvidenceService(repo, bus, Clock.fixed(NOW));
    }

    @Test
    void attach_storesEvidenceAndPublishesAddedEvent() {
        UUID caseId = UUID.randomUUID();
        UUID author = UUID.randomUUID();

        EvidenceOutcome outcome = service.attach(caseId, "Spam 1", "https://example.com/img.png", author, false);

        assertThat(outcome).isInstanceOf(EvidenceOutcome.Success.class);
        Evidence evidence = ((EvidenceOutcome.Success) outcome).evidence();
        assertThat(evidence.caseId()).isEqualTo(caseId);
        assertThat(evidence.label()).isEqualTo("Spam 1");
        assertThat(evidence.content()).isEqualTo("https://example.com/img.png");
        assertThat(evidence.authorId()).isEqualTo(author);
        assertThat(evidence.createdAt()).isEqualTo(NOW);
        assertThat(evidence.editedAt()).isNull();
        assertThat(repo.byId).containsKey(evidence.id());
        assertThat(received).singleElement().isInstanceOf(EvidenceAddedEvent.class);
    }

    @Test
    void attach_blankContentIsRejected() {
        UUID caseId = UUID.randomUUID();

        EvidenceOutcome outcome = service.attach(caseId, "label", "   ", UUID.randomUUID(), false);

        assertThat(outcome).isInstanceOf(EvidenceOutcome.EmptyContent.class);
        assertThat(repo.byId).isEmpty();
        assertThat(received).isEmpty();
    }

    @Test
    void attach_requireUrlBlocksContentWithoutUrl() {
        EvidenceOutcome outcome = service.attach(UUID.randomUUID(), "label",
                "no link here", UUID.randomUUID(), true);

        assertThat(outcome).isInstanceOf(EvidenceOutcome.UrlRequired.class);
        assertThat(repo.byId).isEmpty();
    }

    @Test
    void attach_requireUrlAcceptsContentContainingUrl() {
        EvidenceOutcome outcome = service.attach(UUID.randomUUID(), "label",
                "see http://example.com here", UUID.randomUUID(), true);

        assertThat(outcome).isInstanceOf(EvidenceOutcome.Success.class);
    }

    @Test
    void edit_authorCanUpdateOwnEvidence() {
        UUID caseId = UUID.randomUUID();
        UUID author = UUID.randomUUID();
        Evidence original = ((EvidenceOutcome.Success) service.attach(
                caseId, "label", "https://a", author, false)).evidence();
        received.clear();

        EvidenceOutcome outcome = service.edit(original.id(), "label v2",
                "https://b", author, false, false);

        assertThat(outcome).isInstanceOf(EvidenceOutcome.Success.class);
        Evidence updated = ((EvidenceOutcome.Success) outcome).evidence();
        assertThat(updated.label()).isEqualTo("label v2");
        assertThat(updated.content()).isEqualTo("https://b");
        assertThat(updated.editedAt()).isEqualTo(NOW);
        assertThat(received).singleElement().isInstanceOf(EvidenceEditedEvent.class);
    }

    @Test
    void edit_nonAuthorIsBlockedWithoutAdminOverride() {
        UUID author = UUID.randomUUID();
        Evidence original = ((EvidenceOutcome.Success) service.attach(
                UUID.randomUUID(), "label", "https://a", author, false)).evidence();

        EvidenceOutcome outcome = service.edit(original.id(), "x", "https://x",
                UUID.randomUUID(), false, false);

        assertThat(outcome).isInstanceOf(EvidenceOutcome.NotAuthor.class);
    }

    @Test
    void delete_unknownIdReturnsNotFound() {
        EvidenceOutcome outcome = service.delete(UUID.randomUUID(), UUID.randomUUID(), true);

        assertThat(outcome).isInstanceOf(EvidenceOutcome.NotFound.class);
    }

    @Test
    void delete_authorRemovesAndPublishesEvent() {
        UUID author = UUID.randomUUID();
        Evidence original = ((EvidenceOutcome.Success) service.attach(
                UUID.randomUUID(), "label", "https://a", author, false)).evidence();
        received.clear();

        EvidenceOutcome outcome = service.delete(original.id(), author, false);

        assertThat(outcome).isInstanceOf(EvidenceOutcome.Success.class);
        assertThat(repo.byId).doesNotContainKey(original.id());
        assertThat(received).singleElement().isInstanceOf(EvidenceDeletedEvent.class);
    }

    @Test
    void delete_adminOverrideAllowsRemovingOthersEvidence() {
        UUID author = UUID.randomUUID();
        Evidence original = ((EvidenceOutcome.Success) service.attach(
                UUID.randomUUID(), "label", "https://a", author, false)).evidence();
        received.clear();

        EvidenceOutcome outcome = service.delete(original.id(), UUID.randomUUID(), true);

        assertThat(outcome).isInstanceOf(EvidenceOutcome.Success.class);
        assertThat(repo.byId).doesNotContainKey(original.id());
    }

    @Test
    void listForCaseReturnsAttachments() {
        UUID caseId = UUID.randomUUID();
        UUID author = UUID.randomUUID();
        service.attach(caseId, "1", "https://a", author, false);
        service.attach(caseId, "2", "https://b", author, false);
        service.attach(UUID.randomUUID(), "elsewhere", "https://c", author, false);

        assertThat(service.listForCase(caseId)).hasSize(2);
    }

    private static final class InMemoryEvidenceRepo implements EvidenceRepository {

        final Map<UUID, Evidence> byId = new HashMap<>();

        @Override
        public void insert(Evidence evidence) {
            byId.put(evidence.id(), evidence);
        }

        @Override
        public void update(Evidence evidence) {
            byId.put(evidence.id(), evidence);
        }

        @Override
        public void delete(UUID id) {
            byId.remove(id);
        }

        @Override
        public Optional<Evidence> findById(UUID id) {
            return Optional.ofNullable(byId.get(id));
        }

        @Override
        public List<Evidence> findByCase(UUID caseId) {
            return byId.values().stream().filter(e -> e.caseId().equals(caseId)).toList();
        }
    }
}
