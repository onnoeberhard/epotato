package com.onnoeberhard.epotato.backend;

import com.googlecode.objectify.annotation.Id;

class Entity {

    @Id private Long id;

    public Long getId() {
        return id;
    }

    void setId(Long id) {
        this.id = id;
    }

    com.onnoeberhard.epotato.backend.Id toId() {
        return new com.onnoeberhard.epotato.backend.Id(id);
    }
}
