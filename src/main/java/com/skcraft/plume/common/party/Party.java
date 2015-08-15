package com.skcraft.plume.common.party;

import com.google.common.collect.Sets;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.Set;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;

@Data
@EqualsAndHashCode(of = "name")
public class Party {

    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z0-9_\\-\\.]{1,20}$");

    private String name;
    private Date createTime = new Date();
    private Set<Member> members = Sets.newConcurrentHashSet();

    public Party() {
    }

    public Party(String name) {
        checkNotNull(name, "name");
        setName(name);
    }

    /**
     * Set the name, which must follow the pattern described in {@link #NAME_PATTERN}.
     *
     * @param name The name
     */
    public void setName(String name) {
        checkNotNull(name, "name");
        if (!NAME_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException("Name does match the pattern " + NAME_PATTERN);
        }
        this.name = name;
    }

}
