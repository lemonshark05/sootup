package org.example;

import org.jetbrains.annotations.NotNull;
import soot.toolkits.scalar.FlowSet;

import java.util.*;

public class TaintStore<K, V> implements FlowSet<Map.Entry<K, Set<V>>> {
    private LinkedHashMap<K, Set<V>> store;

    public TaintStore() {
        this.store = new LinkedHashMap<>();
    }

    private LinkedHashMap<K, Set<V>> iteratorToMap(Iterator<Map.Entry<K, Set<V>>> iterator) {
        LinkedHashMap<K, Set<V>> map = new LinkedHashMap<>();
        while (iterator.hasNext()) {
            Map.Entry<K, Set<V>> entry = iterator.next();
            map.put(entry.getKey(), entry.getValue());
        }

        return map;
    }

    @Override
    public FlowSet<Map.Entry<K, Set<V>>> clone() {
        // why do I have to call super.clone if I'm unsure it does a deep copy here
        TaintStore<K, V> clonedStore = new TaintStore<K, V>();
        for (Map.Entry<K, Set<V>> entry : this.store.entrySet()) {
            clonedStore.store.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }
        return clonedStore;
    }

    @Override
    public FlowSet<Map.Entry<K, Set<V>>> emptySet() {
        return new TaintStore<K, V>();
    }

    @Override
    public void copy(FlowSet<Map.Entry<K, Set<V>>> flowSet) {
        flowSet.clear();

        this.iterator().forEachRemaining(flowSet::add);
    }

    @Override
    public void clear() {
        this.store = new LinkedHashMap<>();
    }

    @Override
    public void union(FlowSet<Map.Entry<K, Set<V>>> flowSet) {
        for (Map.Entry<K, Set<V>> entry : flowSet) {
            if (!store.containsKey(entry.getKey())) {
                store.put(entry.getKey(), entry.getValue());
            } else {
                Set<V> thisSet = store.get(entry.getKey());
                thisSet.addAll(entry.getValue());
            }
        }
    }

    @Override
    public void union(FlowSet<Map.Entry<K, Set<V>>> flowSet, FlowSet<Map.Entry<K, Set<V>>> flowSet1) {
        flowSet.copy(flowSet1);
        flowSet1.union(flowSet);
    }

    @Override
    public void intersection(FlowSet<Map.Entry<K, Set<V>>> flowSet) {
        for (Map.Entry<K, Set<V>> entry : flowSet) {
            if (!store.containsKey(entry.getKey())) {
                store.remove(entry.getKey());
            } else {
                Set<V> thisSet = store.get(entry.getKey());
                thisSet.retainAll(entry.getValue());
            }
        }
    }

    @Override
    public void intersection(FlowSet<Map.Entry<K, Set<V>>> flowSet, FlowSet<Map.Entry<K, Set<V>>> flowSet1) {
        flowSet.copy(flowSet1);
        flowSet1.intersection(flowSet);
    }

    @Override
    public void difference(FlowSet<Map.Entry<K, Set<V>>> flowSet) {
        for (Map.Entry<K, Set<V>> entry : flowSet) {
            if (store.containsKey(entry.getKey())) {
                Set<V> thisSet = store.get(entry.getKey());
                thisSet.removeAll(entry.getValue());
            }
        }
    }

    @Override
    public void difference(FlowSet<Map.Entry<K, Set<V>>> flowSet, FlowSet<Map.Entry<K, Set<V>>> flowSet1) {
        flowSet.copy(flowSet1);
        flowSet1.difference(flowSet);
    }

    @Override
    public boolean isEmpty() {
        return store.isEmpty();
    }

    @Override
    public int size() {
        return store.size();
    }

    @Override
    public void add(Map.Entry<K, Set<V>> valueSetEntry) {
        store.put(valueSetEntry.getKey(), valueSetEntry.getValue());
    }

    @Override
    public void add(Map.Entry<K, Set<V>> valueSetEntry, FlowSet<Map.Entry<K, Set<V>>> flowSet) {
        this.copy(flowSet);
        flowSet.add(valueSetEntry);
    }

    @Override
    public void remove(Map.Entry<K, Set<V>> valueSetEntry) {
        store.remove(valueSetEntry.getKey(), valueSetEntry.getValue());
    }

    @Override
    public void remove(Map.Entry<K, Set<V>> valueSetEntry, FlowSet<Map.Entry<K, Set<V>>> flowSet) {
        this.copy(flowSet);
        flowSet.remove(valueSetEntry);
    }

    // checks exactly for key AND value
    @Override
    public boolean contains(Map.Entry<K, Set<V>> valueSetEntry) {
        Set<V> checkSet = store.get(valueSetEntry.getKey());
        if (checkSet == null) {
            return false;
        }

        return checkSet.equals(valueSetEntry.getValue());
    }

    @Override
    public boolean isSubSet(FlowSet<Map.Entry<K, Set<V>>> flowSet) {
        return false;
    }

    @Override
    public @NotNull Iterator<Map.Entry<K, Set<V>>> iterator() {
        return store.entrySet().iterator();
    }

    @Override
    public List<Map.Entry<K, Set<V>>> toList() {
        return new ArrayList<>(store.entrySet());
    }
}
