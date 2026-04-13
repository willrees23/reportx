package com.github.willrees23.reportx.paper.cases;

import com.github.willrees23.reportx.core.model.Case;

import java.util.UUID;

public sealed interface CaseLifecycleOutcome {

    record Success(Case caseValue) implements CaseLifecycleOutcome {
    }

    record NotFound() implements CaseLifecycleOutcome {
    }

    record AlreadyClaimed(UUID currentClaimer) implements CaseLifecycleOutcome {
    }

    record NotClaimed() implements CaseLifecycleOutcome {
    }

    record NotClaimer(UUID currentClaimer) implements CaseLifecycleOutcome {
    }

    record AlreadyResolved() implements CaseLifecycleOutcome {
    }

    record NotResolved() implements CaseLifecycleOutcome {
    }
}
