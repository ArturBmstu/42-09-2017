package ru.mail.park.mechanics;

import ru.mail.park.domain.User;

public class Player {
    private User user;
    private boolean ready = false;
    private boolean finished = false;

    public Player(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    public void setFinished() {
        this.finished = true;
    }
}
