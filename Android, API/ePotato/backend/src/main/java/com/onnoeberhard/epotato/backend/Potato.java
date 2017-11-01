package com.onnoeberhard.epotato.backend;

import com.googlecode.objectify.annotation.Index;

import java.util.ArrayList;

@SuppressWarnings({"unused", "WeakerAccess"})
@com.googlecode.objectify.annotation.Entity
class Potato extends Entity {

    static final int TYPE_POTATO = 1;
    static final int TYPE_FEED_POTATO = 2;

    private String dt;
    private int form;
    private int n;
    private String message;
    @Index
    private String pid;
    @Index
    private String uid;

    Potato() {
    }

    Potato(ArrayList<String> properties, ArrayList<String> values) {
        if (!properties.contains(Endpoint.PO_DT)) {
            properties.add(Endpoint.PO_DT);
            values.add(Endpoint.getTimestamp());
        }
        updateProperties(properties, values);
    }

    void updateProperties(ArrayList<String> properties, ArrayList<String> values) {
        for (int i = 0; i < properties.size(); i++) {
            switch (properties.get(i)) {
                case Endpoint.ID:
                    setId(Long.parseLong(values.get(i)));
                    break;
                case Endpoint.PO_DT:
                    setDt(values.get(i));
                    break;
                case Endpoint.PO_FORM:
                    setForm(Integer.parseInt(values.get(i)));
                    break;
                case Endpoint.PO_N:
                    setN(Integer.parseInt(values.get(i)));
                    break;
                case Endpoint.PO_MESSAGE:
                    setMessage(values.get(i));
                    break;
                case Endpoint.PO_PID:
                    setPid(values.get(i));
                    break;
                case Endpoint.PO_UID:
                    setUid(values.get(i));
                    break;
            }
        }
    }

    public String getDt() {
        return dt;
    }

    public void setDt(String dt) {
        this.dt = dt;
    }

    public int getForm() {
        return form;
    }

    public void setForm(int form) {
        this.form = form;
    }

    public int getN() {
        return n;
    }

    public void setN(int n) {
        this.n = n;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

}
