package org.example;

import org.jetbrains.annotations.NotNull;
import soot.Unit;
import soot.Value;
import soot.toolkits.scalar.FlowSet;

import java.util.*;
import java.util.stream.Collectors;

public class TaintStore implements FlowSet<Map.Entry<Value, Set<Unit>>> {
    private LinkedHashMap<Value, Set<Unit>> store;

    public TaintStore() {
        this.store = new LinkedHashMap<>();
    }

    private LinkedHashMap<Value, Set<Unit>> iteratorToMap(Iterator<Map.Entry<Value, Set<Unit>>> iterator) {
        LinkedHashMap<Value, Set<Unit>> map = new LinkedHashMap<>();
        while (iterator.hasNext()) {
            Map.Entry<Value, Set<Unit>> entry = iterator.next();
            map.put(entry.getKey(), entry.getValue());
        }

        return map;
    }


    @Override
    public FlowSet<Map.Entry<Value, Set<Unit>>> clone() {
        // why do i have to call super.clone if i'm unsure it does a deep copy here
        TaintStore clonedStore = new TaintStore();
        for (Map.Entry<Value, Set<Unit>> entry : this.store.entrySet()) {
            clonedStore.store.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }
        return clonedStore;
    }

    @Override
    public FlowSet<Map.Entry<Value, Set<Unit>>> emptySet() {
        return new TaintStore();
    }

    @Override
    public void copy(FlowSet<Map.Entry<Value, Set<Unit>>> flowSet) {
        flowSet.clear();

        this.iterator().forEachRemaining(flowSet::add);
    }

    @Override
    public void clear() {
        this.store = new LinkedHashMap<>();
    }

    @Override
    public void union(FlowSet<Map.Entry<Value, Set<Unit>>> flowSet) {
        for (Map.Entry<Value, Set<Unit>> entry : flowSet) {
            if (!store.containsKey(entry.getKey())) {
                store.put(entry.getKey(), entry.getValue());
            } else {
                Set<Unit> thisSet = store.get(entry.getKey());
                thisSet.addAll(entry.getValue());
            }
        }
    }

    @Override
    public void union(FlowSet<Map.Entry<Value, Set<Unit>>> flowSet, FlowSet<Map.Entry<Value, Set<Unit>>> flowSet1) {
        flowSet.copy(flowSet1);
        flowSet1.union(flowSet);
    }

    @Override
    public void intersection(FlowSet<Map.Entry<Value, Set<Unit>>> flowSet) {
        for (Map.Entry<Value, Set<Unit>> entry : flowSet) {
            if (!store.containsKey(entry.getKey())) {
                store.remove(entry.getKey());
            } else {
                Set<Unit> thisSet = store.get(entry.getKey());
                thisSet.retainAll(entry.getValue());
            }
        }
    }

    @Override
    public void intersection(FlowSet<Map.Entry<Value, Set<Unit>>> flowSet, FlowSet<Map.Entry<Value, Set<Unit>>> flowSet1) {
        flowSet.copy(flowSet1);
        flowSet1.intersection(flowSet);
    }

    @Override
    public void difference(FlowSet<Map.Entry<Value, Set<Unit>>> flowSet) {
        for (Map.Entry<Value, Set<Unit>> entry : flowSet) {
            if (store.containsKey(entry.getKey())) {
                Set<Unit> thisSet = store.get(entry.getKey());
                thisSet.removeAll(entry.getValue());
            }
        }
    }

    @Override
    public void difference(FlowSet<Map.Entry<Value, Set<Unit>>> flowSet, FlowSet<Map.Entry<Value, Set<Unit>>> flowSet1) {
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
    public void add(Map.Entry<Value, Set<Unit>> valueSetEntry) {
        store.put(valueSetEntry.getKey(), valueSetEntry.getValue());
    }

    @Override
    public void add(Map.Entry<Value, Set<Unit>> valueSetEntry, FlowSet<Map.Entry<Value, Set<Unit>>> flowSet) {
        this.copy(flowSet);
        flowSet.add(valueSetEntry);
    }

    @Override
    public void remove(Map.Entry<Value, Set<Unit>> valueSetEntry) {
        store.remove(valueSetEntry.getKey(), valueSetEntry.getValue());
    }

    @Override
    public void remove(Map.Entry<Value, Set<Unit>> valueSetEntry, FlowSet<Map.Entry<Value, Set<Unit>>> flowSet) {
        this.copy(flowSet);
        flowSet.remove(valueSetEntry);
    }

    // checks exactly for key AND value
    @Override
    public boolean contains(Map.Entry<Value, Set<Unit>> valueSetEntry) {
        Set<Unit> checkSet = store.get(valueSetEntry.getKey());
        if (checkSet == null) {
            return false;
        }

        return checkSet.equals(valueSetEntry.getValue());
    }

    @Override
    public boolean isSubSet(FlowSet<Map.Entry<Value, Set<Unit>>> flowSet) {
        return false;
    }

    @Override
    public @NotNull Iterator<Map.Entry<Value, Set<Unit>>> iterator() {
        return store.entrySet().iterator();
    }

    @Override
    public List<Map.Entry<Value, Set<Unit>>> toList() {
        return new ArrayList<>(store.entrySet());
    }
}
