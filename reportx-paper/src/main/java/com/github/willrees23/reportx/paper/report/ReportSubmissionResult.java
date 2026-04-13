package com.github.willrees23.reportx.paper.report;

import com.github.willrees23.reportx.core.model.Case;
import com.github.willrees23.reportx.core.model.Report;

public sealed interface ReportSubmissionResult {

    record Success(Report report, Case caseValue, boolean mergedIntoExistingCase) implements ReportSubmissionResult {
    }

    record SelfReport() implements ReportSubmissionResult {
    }

    record OnCooldown(long remainingSeconds) implements ReportSubmissionResult {
    }

    record UnknownCategory(String category) implements ReportSubmissionResult {
    }

    record ReasonRequired() implements ReportSubmissionResult {
    }
}
