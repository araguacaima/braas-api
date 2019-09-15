package com.araguacaima.braas.core.google.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.Serializable;
import java.util.UUID;

public class Role implements Serializable, Comparable {

    public static final String FIND_BY_PRIORITY = "Role.findByOrder";
    public static final String FIND_BY_ID = "Role.findById";
    public static final String FIND_BY_NAME = "Role.findByName";
    public static final String GET_ROLES_COUNT = "Role.getRolesCount";
    public static final String GET_ALL_ROLES = "Role.getAllRoles";
    public static final String PARAM_PRIORITY = "priority";
    public static final String PARAM_ID = "id";
    public static final String PARAM_NAME = "name";
    public static final String ROLE_CHANGE_PASSWORD = "ROLE_CHANGE_PASSWORD";
    public static final String ROLE_ANONYMOUS = "ROLE_ANONYMOUS";
    public static final String ROLE_CHANGING_PASSWORD = "ROLE_CHANGING_PASSWORD";
    private static final long serialVersionUID = 3104672099472914869L;


    private String id;

    private String name;


    private int priority;

    public Role() {
        this.id = UUID.randomUUID().toString();
    }

    public Role(String id, String name, int priority) {
        this();
        this.id = id;
        this.name = name;
        this.priority = priority;
    }

    public Role(String name, int priority) {
        this();
        this.name = name;
        this.priority = priority;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int oreder) {
        this.priority = oreder;
    }

    @Override
    public int compareTo(Object o) {
        Role role = (Role) o;
        return this.getPriority() - role.getPriority();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        Role role = (Role) o;

        return new EqualsBuilder()
                .append(priority, role.priority)
                .append(id, role.id)
                .append(name, role.name)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(id)
                .append(name)
                .append(priority)
                .toHashCode();
    }
}
