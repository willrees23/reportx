package com.github.willrees23.reportx.paper.evidence;

import com.github.willrees23.reportx.core.messaging.MessageBus;
import com.github.willrees23.reportx.core.messaging.events.EvidenceAddedEvent;
import com.github.willrees23.reportx.core.messaging.events.EvidenceDeletedEvent;
import com.github.willrees23.reportx.core.messaging.events.EvidenceEditedEvent;
import com.github.willrees23.reportx.core.model.Evidence;
import com.github.willrees23.reportx.core.storage.EvidenceRepository;
import com.github.willrees23.reportx.core.util.Clock;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

public final class EvidenceService {

    private static final Pattern URL_PATTERN = Pattern.compile(
            "\\bhttps?://[\\w./%?=&#:+\\-~,@!$'()*;\\[\\]]+",
            Pattern.CASE_INSENSITIVE);

    private final EvidenceRepository repo;
    private final MessageBus bus;
    private final Clock clock;

    public EvidenceService(EvidenceRepository repo, MessageBus bus, Clock clock) {
        this.repo = repo;
        this.bus = bus;
        this.clock = clock;
    }

    public EvidenceOutcome attach(UUID caseId, String label, String content, UUID authorId, boolean requireUrl) {
        if (content == null || content.isBlank()) {
            return new EvidenceOutcome.EmptyContent();
        }
        if (requireUrl && !containsUrl(content)) {
            return new EvidenceOutcome.UrlRequired();
        }
        Evidence evidence = new Evidence(
                UUID.randomUUID(),
                caseId,
                label == null ? "" : label,
                content,
                authorId,
                clock.now(),
                null);
        repo.insert(evidence);
        bus.publish(new EvidenceAddedEvent(evidence.id(), caseId, authorId));
        return new EvidenceOutcome.Success(evidence);
    }

    public EvidenceOutcome edit(UUID evidenceId, String newLabel, String newContent,
                                UUID actorId, boolean requireUrl, boolean adminOverride) {
        Optional<Evidence> existing = repo.findById(evidenceId);
        if (existing.isEmpty()) {
            return new EvidenceOutcome.NotFound();
        }
        Evidence current = existing.get();
        if (!adminOverride && !current.authorId().equals(actorId)) {
            return new EvidenceOutcome.NotAuthor();
        }
        if (newContent == null || newContent.isBlank()) {
            return new EvidenceOutcome.EmptyContent();
        }
        if (requireUrl && !containsUrl(newContent)) {
            return new EvidenceOutcome.UrlRequired();
        }
        Evidence updated = new Evidence(
                current.id(),
                current.caseId(),
                newLabel == null ? current.label() : newLabel,
                newContent,
                current.authorId(),
                current.createdAt(),
                clock.now());
        repo.update(updated);
        bus.publish(new EvidenceEditedEvent(current.id(), current.caseId(), actorId));
        return new EvidenceOutcome.Success(updated);
    }

    public EvidenceOutcome delete(UUID evidenceId, UUID actorId, boolean adminOverride) {
        Optional<Evidence> existing = repo.findById(evidenceId);
        if (existing.isEmpty()) {
            return new EvidenceOutcome.NotFound();
        }
        Evidence current = existing.get();
        if (!adminOverride && !current.authorId().equals(actorId)) {
            return new EvidenceOutcome.NotAuthor();
        }
        repo.delete(evidenceId);
        bus.publish(new EvidenceDeletedEvent(current.id(), current.caseId(), actorId));
        return new EvidenceOutcome.Success(current);
    }

    public List<Evidence> listForCase(UUID caseId) {
        return repo.findByCase(caseId);
    }

    private static boolean containsUrl(String text) {
        return text != null && URL_PATTERN.matcher(text).find();
    }
}
