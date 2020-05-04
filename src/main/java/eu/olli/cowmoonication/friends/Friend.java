package eu.olli.cowmoonication.friends;

import java.util.Objects;
import java.util.UUID;

public class Friend {
    public static final Friend FRIEND_NOT_FOUND = new Friend();
    private UUID id;
    private String name;
    private long lastChecked;

    static {
        // uuid & name are null
        FRIEND_NOT_FOUND.setLastChecked(0);
    }

    /**
     * No-args constructor for GSON
     */
    private Friend() {
        this.lastChecked = System.currentTimeMillis();
    }

    public UUID getUuid() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getLastChecked() {
        return lastChecked;
    }

    public void setLastChecked(long lastChecked) {
        this.lastChecked = lastChecked;
    }

    @Override
    public String toString() {
        return "Friend{" +
                "uuid=" + id +
                ", name='" + name + '\'' +
                ", lastChecked=" + lastChecked +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Friend friend = (Friend) o;
        return Objects.equals(id, friend.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
