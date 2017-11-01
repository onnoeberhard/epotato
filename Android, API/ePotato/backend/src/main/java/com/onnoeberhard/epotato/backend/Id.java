package com.onnoeberhard.epotato.backend;

class Id {

    @com.googlecode.objectify.annotation.Id private Long id = 0L;

    Id(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
