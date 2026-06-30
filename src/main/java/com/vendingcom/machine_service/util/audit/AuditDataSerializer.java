package com.vendingcom.machine_service.util.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vendingcom.machine_service.domain.model.Machine;

/**
 * Serializa datos de dominio a JSON para guardarlos en la auditoría (old_data / new_data).
 *
 * NUNCA incluir datos sensibles (tokens, secretos, etc.).
 */
public class AuditDataSerializer {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())                       // soporta OffsetDateTime / LocalDate
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);   // fechas como ISO-8601

    private AuditDataSerializer() {
    }

    /** Serializa cualquier objeto de dominio a JSON (o null si falla / es null). */
    public static String serialize(Object data) {
        if (data == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception exception) {
            return null;
        }
    }

    public static String serializeMachine(Machine machine) {
        return serialize(machine);
    }
}
