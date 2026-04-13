package com.github.willrees23.reportx.paper.evidence;

import com.github.willrees23.reportx.core.model.Evidence;

public sealed interface EvidenceOutcome {

    record Success(Evidence evidence) implements EvidenceOutcome {
    }

    record NotFound() implements EvidenceOutcome {
    }

    record NotAuthor() implements EvidenceOutcome {
    }

    record UrlRequired() implements EvidenceOutcome {
    }

    record EmptyContent() implements EvidenceOutcome {
    }
}
