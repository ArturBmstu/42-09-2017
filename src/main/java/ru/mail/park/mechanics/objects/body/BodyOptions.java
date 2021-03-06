package ru.mail.park.mechanics.objects.body;

import com.fasterxml.jackson.annotation.JsonCreator;
import ru.mail.park.info.constants.MessageConstants;

import javax.validation.constraints.NotNull;

public class BodyOptions {
    private boolean sensor;
    @NotNull(message = MessageConstants.REQUIRED_FIELD_EMPTY)
    private Float density;
    @NotNull(message = MessageConstants.REQUIRED_FIELD_EMPTY)
    private Float friction;
    @NotNull(message = MessageConstants.REQUIRED_FIELD_EMPTY)
    private Float restitution;
    @NotNull(message = MessageConstants.REQUIRED_FIELD_EMPTY)
    private Integer keyBodyID;

    @JsonCreator
    public BodyOptions() {

    }

    public boolean isSensor() {
        return sensor;
    }

    public void setSensor(boolean sensor) {
        this.sensor = sensor;
    }

    public Float getDensity() {
        return density;
    }

    public void setDensity(Float density) {
        this.density = density;
    }

    public Float getFriction() {
        return friction;
    }

    public void setFriction(Float friction) {
        this.friction = friction;
    }

    public Float getRestitution() {
        return restitution;
    }

    public void setRestitution(Float restitution) {
        this.restitution = restitution;
    }

    public Integer getKeyBodyID() {
        return keyBodyID;
    }

    public void setKeyBodyID(Integer keyBodyID) {
        this.keyBodyID = keyBodyID;
    }
}
