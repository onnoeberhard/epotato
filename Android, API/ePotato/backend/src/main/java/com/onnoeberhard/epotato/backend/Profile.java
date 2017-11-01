package com.onnoeberhard.epotato.backend;

import com.googlecode.objectify.annotation.Index;

import java.util.ArrayList;
import java.util.Random;

@SuppressWarnings({"unused", "WeakerAccess"})
@com.googlecode.objectify.annotation.Entity
class Profile extends Entity {

    private ArrayList<String> contacts = new ArrayList<>();
    @Index private String epid;
    private ArrayList<String> fiids = new ArrayList<>();
    private ArrayList<String> fiids_ios = new ArrayList<>();
    private ArrayList<String> followers = new ArrayList<>();
    private ArrayList<String> following = new ArrayList<>();
    private String password;
    @Index private String phone;
    private ArrayList<String> potatoes = new ArrayList<>();
    @Index private Double rand;
    private Boolean strangers = true;

    Profile() {
    }

    Profile(ArrayList<String> properties, ArrayList<String> values) {
        this(properties, values, true);
    }

    Profile(ArrayList<String> properties, ArrayList<String> values, boolean rand) {
        if (rand && !properties.contains(Endpoint.PR_RAND)) {
            properties.add(Endpoint.PR_RAND);
            values.add(Double.toString(new Random().nextDouble()));
        }
        updateProperties(properties, values);
    }

    void updateProperties(ArrayList<String> properties, ArrayList<String> values) {
        for (int i = 0; i < properties.size(); i++) {
            switch (properties.get(i)) {
                case Endpoint.ID:
                    setId(Long.parseLong(values.get(i)));
                    break;
                case Endpoint.PR_CONTACTS:
                    setContacts(Endpoint.deserialize(values.get(i)));
                    break;
                case Endpoint.PR_EPID:
                    setEpid(values.get(i));
                    break;
                case Endpoint.PR_FIIDS:
                    setFiids(Endpoint.deserialize(values.get(i)));
                    break;
                case Endpoint.PR_FIIDS_IOS:
                    setFiidsIos(Endpoint.deserialize(values.get(i)));
                    break;
                case Endpoint.PR_FOLLOWERS:
                    setFollowers(Endpoint.deserialize(values.get(i)));
                    break;
                case Endpoint.PR_FOLLOWING:
                    setFollowing(Endpoint.deserialize(values.get(i)));
                    break;
                case Endpoint.PR_PASSWORD:
                    setPassword(values.get(i));
                    break;
                case Endpoint.PR_PHONE:
                    setPhone(values.get(i));
                    break;
                case Endpoint.PR_POTATOES:
                    setPotatoes(Endpoint.deserialize(values.get(i)));
                    break;
                case Endpoint.PR_RAND:
                    setRand(Double.valueOf(values.get(i)));
                    break;
                case Endpoint.PR_STRANGERS:
                    setStrangers(Boolean.valueOf(values.get(i)));
                    break;
            }
        }
    }

    public ArrayList<String> getContacts() {
        return contacts;
    }

    public void setContacts(ArrayList<String> contacts) {
        this.contacts = contacts;
    }

    public String getEpid() {
        return epid;
    }

    private void setEpid(String epid) {
        this.epid = epid;
    }

    public ArrayList<String> getFiids() {
        return fiids;
    }

    private void setFiids(ArrayList<String> fiids) {
        this.fiids = fiids;
    }

    public ArrayList<String> getFiidsIos() {
        return fiids_ios;
    }

    public void setFiidsIos(ArrayList<String> fiids_ios) {
        this.fiids_ios = fiids_ios;
    }

    public ArrayList<String> getFollowers() {
        return followers;
    }

    public void setFollowers(ArrayList<String> followers) {
        this.followers = followers;
    }

    public ArrayList<String> getFollowing() {
        return following;
    }

    public void setFollowing(ArrayList<String> following) {
        this.following = following;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhone() {
        return phone;
    }

    private void setPhone(String phone) {
        this.phone = phone;
    }

    public ArrayList<String> getPotatoes() {
        return potatoes;
    }

    public void setPotatoes(ArrayList<String> potatoes) {
        this.potatoes = potatoes;
    }

    public Double getRand() {
        return rand;
    }

    private void setRand(Double rand) {
        this.rand = rand;
    }

    public Boolean getStrangers() {
        return strangers;
    }

    private void setStrangers(Boolean strangers) {
        this.strangers = strangers;
    }
}
