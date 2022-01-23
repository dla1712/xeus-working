package com.pinewoodbuilders.contracts.roblox.evaluations;

import com.pinewoodbuilders.time.Carbon;

public record EvaluationStatus(boolean passedQuiz, boolean passedPatrol, boolean passedCombat, boolean passedConsensus,
                               String lastEvaluator, Carbon lastEdit,
                               Carbon firstEvaluation) {

    public boolean isPassed() {
        return passedQuiz && passedPatrol && passedCombat && passedConsensus;
    }

    public String getLastEvaluator() {
        return lastEvaluator;
    }

    public Carbon getLastEdit() {
        return lastEdit;
    }

    public Carbon getFirstEvaluation() {
        return firstEvaluation;
    }
}
