package com.araguacaima.braas.api.wrapper;

import com.araguacaima.braas.core.google.model.Role;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public class RolesWrapper {

    public static Collection<? extends Role> toRoles(Set<String> incomingRoles) {
        Collection<Role> roles = new LinkedHashSet<>();
        if (CollectionUtils.isNotEmpty(incomingRoles)) {
            incomingRoles.forEach(incomingRole -> roles.add(buildRole(incomingRole)));
        }
        return roles;
    }

    public static Role buildRole(String name) {
        Role role = new Role();
        role.setName(name);
        return role;
    }

    public static Collection<String> fromRoles(Set<Role> incomingRoles) {
        Collection<String> roles = new LinkedHashSet<>();

        if (CollectionUtils.isNotEmpty(incomingRoles)) {
            incomingRoles.forEach(incomingRole -> roles.add(incomingRole.getName()));
        }
        return roles;
    }
}
