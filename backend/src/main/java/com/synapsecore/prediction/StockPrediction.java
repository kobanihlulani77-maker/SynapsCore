package com.synapsecore.prediction;

public record StockPrediction(
    long recentUnitsOrdered,
    double unitsPerHour,
    Double hoursToStockout,
    boolean depletionRisk,
    boolean urgentRisk,
    boolean rapidConsumption
) {
}
