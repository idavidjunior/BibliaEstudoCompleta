package com.biblia.estudo.model;

import java.io.Serializable;

public class ReferenceEdge implements Serializable {
    public static final String TYPE_TEMATICA = "tematica";
    public static final String TYPE_PROFECIA = "profecia";
    public static final String TYPE_CUMPRIMENTO = "cumprimento";
    public static final String TYPE_PARALELO = "paralelo";
    public static final String TYPE_EXPLICACAO = "explicacao";
    public static final String TYPE_COMPLEMENTO = "complemento";
    public static final String TYPE_CONTRASTE = "contraste";
    public static final String TYPE_CONTEXTO = "contexto";
    public static final String TYPE_DOUTRINA = "doutrina";
    public static final String TYPE_PERSONAGEM = "personagem";
    public static final String TYPE_EVENTO = "evento";
    public static final String TYPE_PALAVRA_CHAVE = "palavra_chave";
    public static final String TYPE_PERSONALIZADA = "personalizada";

    private long id;
    private long mapId;
    private long sourceNodeId;
    private long targetNodeId;
    private String relationType;
    private String note;
    private long createdAt;
    private long updatedAt;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getMapId() { return mapId; }
    public void setMapId(long mapId) { this.mapId = mapId; }

    public long getSourceNodeId() { return sourceNodeId; }
    public void setSourceNodeId(long sourceNodeId) { this.sourceNodeId = sourceNodeId; }

    public long getTargetNodeId() { return targetNodeId; }
    public void setTargetNodeId(long targetNodeId) { this.targetNodeId = targetNodeId; }

    public String getRelationType() { return relationType; }
    public void setRelationType(String relationType) { this.relationType = relationType; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public String getRelationTypeLabel() {
        switch (relationType) {
            case TYPE_TEMATICA: return "Relação temática";
            case TYPE_PROFECIA: return "Profecia";
            case TYPE_CUMPRIMENTO: return "Cumprimento";
            case TYPE_PARALELO: return "Paralelo";
            case TYPE_EXPLICACAO: return "Explicação";
            case TYPE_COMPLEMENTO: return "Complemento";
            case TYPE_CONTRASTE: return "Contraste";
            case TYPE_CONTEXTO: return "Contexto";
            case TYPE_DOUTRINA: return "Doutrina";
            case TYPE_PERSONAGEM: return "Personagem";
            case TYPE_EVENTO: return "Evento";
            case TYPE_PALAVRA_CHAVE: return "Palavra-chave";
            case TYPE_PERSONALIZADA: return "Personalizada";
            default: return relationType;
        }
    }
}