package com.araguacaima.braas.api.wrapper;

import com.araguacaima.braas.core.google.model.Account;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.oauth.profile.google2.Google2Profile;

public class AccountWrapper {
    public static Account toAccount(UserProfile profile) {
        Account account = null;
        if (profile != null) {
            account = new Account();
            String displayName = ((Google2Profile) profile).getDisplayName();
            account.setLogin(displayName == null ? account.getLogin() : displayName);
            account.setEmail(((Google2Profile) profile).getEmail());
            String url = profile.getAttribute("image.url").toString();
        }
        return account;
    }
}
