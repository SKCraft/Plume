package com.skcraft.plume.event;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import com.skcraft.plume.common.UserId;
import com.skcraft.plume.util.profile.Profiles;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityThrowable;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An instance of this object describes the actors that played a role in
 * causing an event, with the ability to describe a situation where one actor
 * controls several other actors to create the event.
 *
 * <p>For example, if a EntityPlayerMP fires an arrow that hits an item frame, the EntityPlayerMP
 * is the initiator, while the arrow is merely controlled by the EntityPlayerMP to
 * hit the item frame.</p>
 */
public final class Cause {

    private static final Cause UNKNOWN = new Cause(Collections.emptyList(), false);

    private final List<Object> causes;
    private final boolean indirect;

    /**
     * Create a new instance.
     *
     * @param causes a list of causes
     * @param indirect whether the cause is indirect
     */
    private Cause(List<Object> causes, boolean indirect) {
        checkNotNull(causes);
        this.causes = causes;
        this.indirect = indirect;
    }

    /**
     * Test whether the traced cause is indirect.
     *
     * <p>If the cause is indirect, then the root cause may not be notified,
     * for example.</p>
     *
     * @return True if the cause is indirect
     */
    public boolean isIndirect() {
        return indirect;
    }

    /**
     * Return whether a cause is known. This method will return true if
     * the list of causes is empty or the list of causes only contains
     * objects that really are not root causes (i.e primed TNT).
     *
     * @return True if known
     */
    public boolean isKnown() {
        if (causes.isEmpty()) {
            return false;
        }

        boolean found = false;
        for (Object object : causes) {
            if (!(object instanceof EntityTNTPrimed) && !(object instanceof Entity && ((Entity) object).riddenByEntity != null)) {
                found = true;
                break;
            }
        }

        return found;
    }

    @Nullable
    public Object getRootCause() {
        if (!causes.isEmpty()) {
            return causes.get(0);
        }

        return null;
    }

    @Nullable
    public UserId getFirstUserId() {
        for (Object object : causes) {
            if (object instanceof UserId) {
                return (UserId) object;
            }
        }

        return null;
    }

    @Nullable
    public EntityPlayer getFirstPlayer() {
        for (Object object : causes) {
            if (object instanceof EntityPlayer) {
                return (EntityPlayer) object;
            }
        }

        return null;
    }

    @Nullable
    public Entity getFirstEntity() {
        for (Object object : causes) {
            if (object instanceof Entity) {
                return (Entity) object;
            }
        }

        return null;
    }

    @Nullable
    public Entity getFirstNonPlayerEntity() {
        for (Object object : causes) {
            if (object instanceof Entity && !(object instanceof EntityPlayerMP)) {
                return (Entity) object;
            }
        }

        return null;
    }

    @Override
    public String toString() {
        return Joiner.on(" | ").join(causes);
    }

    /**
     * Create a new instance with the given objects as the cause,
     * where the first-most object is the initial initiator and those
     * following it are controlled by the previous entry.
     *
     * @param cause An array of causing objects
     * @return A cause
     */
    public static Cause create(@Nullable Object... cause) {
        if (cause != null) {
            Builder builder = new Builder(cause.length);
            builder.addAll(cause);
            return builder.build();
        } else {
            return UNKNOWN;
        }
    }

    /**
     * Create a new instance that indicates that the cause is not known.
     *
     * @return A cause
     */
    public static Cause unknown() {
        return UNKNOWN;
    }

    /**
     * Builds causes.
     */
    private static final class Builder {
        private final List<Object> causes;
        private final Set<Object> seen = Sets.newHashSet();
        private boolean indirect;

        private Builder(int expectedSize) {
            this.causes = new ArrayList<Object>(expectedSize);
        }

        private void addAll(@Nullable Object... element) {
            if (element != null) {
                for (Object o : element) {
                    if (o == null || seen.contains(o)) {
                        continue;
                    }

                    seen.add(o);

                    if (o instanceof EntityPlayer) {
                        addAll(Profiles.fromPlayer((EntityPlayer) o));
                    } else if (o instanceof EntityTNTPrimed) {
                        addAll(((EntityTNTPrimed) o).getTntPlacedBy());
                    } else if (o instanceof EntityArrow) {
                        addAll(((EntityArrow) o).shootingEntity);
                    } else if (o instanceof EntityThrowable) {
                        addAll(((EntityThrowable) o).getThrower());
                    } else if (o instanceof Entity && ((Entity) o).riddenByEntity != null) {
                        indirect = true;
                        addAll(((Entity) o).riddenByEntity);
                    } else if (o instanceof EntityAnimal && ((EntityAnimal) o).func_146083_cb() != null) {
                        indirect = true;
                        addAll(((EntityAnimal) o).func_146083_cb());
                    }

                    causes.add(o);
                }
            }
        }

        public Cause build() {
            return new Cause(causes, indirect);
        }
    }

}
