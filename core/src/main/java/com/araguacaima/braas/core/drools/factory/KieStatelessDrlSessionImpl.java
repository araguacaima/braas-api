package com.araguacaima.braas.core.drools.factory;

import com.araguacaima.commons.utils.ReflectionUtils;
import org.drools.core.ClassObjectFilter;
import org.drools.core.impl.StatelessKnowledgeSessionImpl;
import org.kie.api.event.rule.DebugAgendaEventListener;
import org.kie.api.event.rule.DebugRuleRuntimeEventListener;
import org.kie.api.runtime.ObjectFilter;
import org.kie.api.runtime.StatelessKieSession;
import org.kie.internal.runtime.StatefulKnowledgeSession;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * Created by Alejandro on 01/12/2014.
 */
public class KieStatelessDrlSessionImpl implements KieSessionImpl {
    private static final ReflectionUtils reflectionUtils = new ReflectionUtils(null);
    private final StatelessKnowledgeSessionImpl statelessSession;

    public KieStatelessDrlSessionImpl(StatelessKieSession statelessKieSession,
                                      boolean verbose,
                                      Map<String, Object> globals) {

        this.statelessSession = (StatelessKnowledgeSessionImpl) statelessKieSession;
        if (verbose) {
            this.statelessSession.addEventListener(new DebugAgendaEventListener());
            this.statelessSession.addEventListener(new DebugRuleRuntimeEventListener());
        } else {
            this.statelessSession.getAgendaEventListeners().clear();
        }
        if (globals != null && globals.size() > 0) {
            for (String identifier : globals.keySet()) {
                try {
                    this.statelessSession.setGlobal(identifier, globals.get(identifier));
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }
    }

    private void insertAssets(StatefulKnowledgeSession statefulKnowledgeSession, Object asset, boolean expandLists) {
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
        statelessSession.setGlobal(globalName, object);
    }

    @Override
    public Collection<Object> execute(Object asset, boolean expandLists) {
        Collection<Object> assets;
        ObjectFilter filter = new ClassObjectFilter(Object.class);
        Collection<Object> objects;
        if (asset != null) {
            if (asset instanceof Object[]) {
                objects = new ArrayList<>();
                Object[] assets_ = (Object[]) asset;
                Collections.addAll(objects, assets_);
            } else if (reflectionUtils.isCollectionImplementation(asset.getClass().getName())) {
                objects = (Collection<Object>) asset;
            } else {
                objects = new ArrayList<Object>() {{
                    add(asset);
                }};
            }
            assets = statelessSession.executeWithResults(objects, filter);
            return assets;
        }
        return null;
    }

    public StatelessKnowledgeSessionImpl getStatelessSession() {
        return statelessSession;
    }
}
