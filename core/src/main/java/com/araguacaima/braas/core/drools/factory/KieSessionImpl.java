package com.araguacaima.braas.core.drools.factory;

import java.util.Collection;

/**
 * Created by Alejandro on 01/12/2014.
 */
public interface KieSessionImpl {

    void setGlobal(String globalName, Object object);

    Collection<Object> execute(Object asset, boolean expandLists);
}
