package com.github.willrees23.reportx.paper.report;

import com.github.willrees23.reportx.core.config.CategoriesYaml;
import com.github.willrees23.reportx.core.config.ConfigYaml;
import com.github.willrees23.reportx.core.messaging.MessageBus;
import com.github.willrees23.reportx.core.messaging.events.ReportCreatedEvent;
import com.github.willrees23.reportx.core.model.Report;
import com.github.willrees23.reportx.core.storage.ReportRepository;
import com.github.willrees23.reportx.core.util.Clock;
import com.github.willrees23.reportx.paper.cases.CaseService;
import com.github.willrees23.solo.registry.CooldownRegistry;

import java.time.Instant;
import java.util.UUID;

public final class ReportSubmissionService {

    public static final String COOLDOWN_KEY = "reportx.report";

    private final ReportRepository reports;
    private final CaseService cases;
    private final MessageBus bus;
    private final CooldownRegistry cooldowns;
    private final Clock clock;

    public ReportSubmissionService(ReportRepository reports, CaseService cases, MessageBus bus,
                                   CooldownRegistry cooldowns, Clock clock) {
        this.reports = reports;
        this.cases = cases;
        this.bus = bus;
        this.cooldowns = cooldowns;
        this.clock = clock;
    }

    public ReportSubmissionResult submit(ReportSubmissionRequest request,
                                         ConfigYaml config,
                                         CategoriesYaml categoriesConfig) {
        if (request.reporterId().equals(request.targetId())) {
            return new ReportSubmissionResult.SelfReport();
        }
        if (!isKnownCategory(request.category(), categoriesConfig)) {
            return new ReportSubmissionResult.UnknownCategory(request.category());
        }
        if (config.reports().requireReason() && (request.detail() == null || request.detail().isBlank())) {
            return new ReportSubmissionResult.ReasonRequired();
        }
        if (cooldowns.isOnCooldown(COOLDOWN_KEY, request.reporterId())) {
            long remainingMs = cooldowns.getRemainingMillis(COOLDOWN_KEY, request.reporterId());
            return new ReportSubmissionResult.OnCooldown(Math.max(1, remainingMs / 1000));
        }

        CaseService.CaseAssignment assignment = cases.findOrCreateForReport(
                request.targetId(), request.category(), config.dedup());

        Instant now = clock.now();
        Report report = new Report(
                UUID.randomUUID(),
                assignment.value().id(),
                request.reporterId(),
                request.targetId(),
                request.category(),
                request.detail(),
                request.serverName(),
                request.reporterCoords(),
                now);
        reports.insert(report);

        bus.publish(new ReportCreatedEvent(report));
        if (!assignment.newlyCreated()) {
            cases.publishMerge(report.id(), assignment.value().id());
        }

        long cooldownMs = config.reports().cooldownSeconds() * 1000L;
        if (cooldownMs > 0) {
            cooldowns.setCooldown(COOLDOWN_KEY, request.reporterId(), cooldownMs);
        }

        return new ReportSubmissionResult.Success(report, assignment.value(), !assignment.newlyCreated());
    }

    private static boolean isKnownCategory(String category, CategoriesYaml categoriesConfig) {
        if (category == null || categoriesConfig == null || categoriesConfig.categories() == null) {
            return false;
        }
        for (CategoriesYaml.Category c : categoriesConfig.categories()) {
            if (c.id().equalsIgnoreCase(category)) {
                return true;
            }
        }
        return false;
    }
}
