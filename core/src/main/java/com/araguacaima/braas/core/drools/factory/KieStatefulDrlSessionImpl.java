package com.araguacaima.braas.core.drools.factory;

import org.kie.api.event.rule.DebugAgendaEventListener;
import org.kie.api.event.rule.DebugRuleRuntimeEventListener;
import org.kie.api.runtime.KieSession;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * Created by Alejandro on 01/12/2014.
 */
public class KieStatefulDrlSessionImpl implements KieSessionImpl {
    private final KieSession statefullSession;

    public KieStatefulDrlSessionImpl(KieSession kieSession, boolean verbose, Map<String, Object> globals) {
        this.statefullSession = kieSession;
        if (verbose) {
            this.statefullSession.addEventListener(new DebugAgendaEventListener());
            this.statefullSession.addEventListener(new DebugRuleRuntimeEventListener());
        }
        if (globals != null && globals.size() > 0) {
            for (String identifier : globals.keySet()) {
                try {
                    this.statefullSession.setGlobal(identifier, globals.get(identifier));
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }
    }

    private void insertAssets(KieSession statefulKnowledgeSession, Object asset, boolean expandLists) {
        if (Collection.class.isAssignableFrom(asset.getClass())) {
            for (Object element : (Collection) asset) {
                if (expandLists) {
                    insertAssets(statefulKnowledgeSession, element, true);
                } else {
                    statefulKnowledgeSession.insert(element);
                }
            }
        } else if (asset instanceof Object[]) {
            for (Object element : (Object[]) asset) {
                if (expandLists) {
                    insertAssets(statefulKnowledgeSession, element, true);
                } else {
                    statefulKnowledgeSession.insert(element);
                }
            }
        } else if (asset.getClass().isArray()) {
            final Object[] list = (Object[]) asset;
            for (Object element : list) {
                if (expandLists) {
                    insertAssets(statefulKnowledgeSession, element, true);
                } else {
                    statefulKnowledgeSession.insert(element);
                }
            }
        } else {
            statefulKnowledgeSession.insert(asset);
        }
    }

    @Override
    public void setGlobal(String globalName, Object object) {
        statefullSession.setGlobal(globalName, object);
    }

    @Override
    public Collection<Object> execute(Object asset, boolean expandLists) {
        Collection<Object> assets = new ArrayList<>();
        try {
            if (asset != null) {
                insertAssets(statefullSession, asset, expandLists);
                statefullSession.fireAllRules();
                assets.addAll(statefullSession.getObjects());
            }
        } finally {
            statefullSession.dispose();
        }
        return assets;
    }
}
