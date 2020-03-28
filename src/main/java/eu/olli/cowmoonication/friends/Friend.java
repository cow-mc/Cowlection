package eu.olli.cowmoonication.friends;

import com.google.gson.InstanceCreator;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.UUID;

public class Friend {
    public static final Friend FRIEND_NOT_FOUND = new Friend(null, null, -1);
    private UUID id;
    private String name;
    private long lastChecked;

    /**
     * No-args constructor for GSON
     */
    private Friend() {
        this.lastChecked = System.currentTimeMillis();
    }

    private Friend(UUID uuid, String name, long lastChecked) {
        this.id = uuid;
        this.name = name;
        if (lastChecked > 0) {
            this.lastChecked = lastChecked;
        }
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

    public static class FriendCreator implements InstanceCreator {
        @Override
        public Friend createInstance(Type type) {
            return new Friend();
        }
    }
}
