package uk.ac.ed.inf.acpTutorial.dto;

import java.math.BigDecimal;


public record DroneFromService(
        String id,
        String name,

        BigDecimal costPer100Moves,
        DroneCapabilities capability) {

    public record DroneCapabilities(
            Boolean cooling,
            Boolean heating,
            BigDecimal capacity,
            Integer maxMoves,
            BigDecimal costPerMove,
            BigDecimal costInitial,
            BigDecimal costFinal) {
    }
}
