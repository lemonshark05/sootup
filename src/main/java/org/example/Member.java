package org.example;

import soot.RefLikeType;
import soot.SootFieldRef;
import soot.Type;
import soot.Value;

import java.util.HashMap;
import java.util.Map;

public class Member {
	private final Value value;
	private Type type;
	private String typename;
	private Map<SootFieldRef, Variable> fieldMap;

	public Member(Value val) {
		value = val;
		type = value.getType();
		typename = type.toString();

		if (type instanceof RefLikeType) {
			fieldMap = new HashMap<>();
		}
	}

	public Member copyDeep() {
		Member m = new Member(value);
		if (fieldMap != null) {
			for (Map.Entry<SootFieldRef, Variable> entry : this.fieldMap.entrySet()) {
                m.fieldMap.put(entry.getKey(), entry.getValue().copyDeep(true));
            }
		}
		return m;
	}

	public Variable getVariable(SootFieldRef sfr) {
		return fieldMap.get(sfr);
	}

	public void addField(SootFieldRef sfr, Variable var) {
		Variable v = fieldMap.get(sfr);
		if (v == null) {
			fieldMap.put(sfr, var.copyDeep(false));
		} else {
			v.assign(var);
		}
	}

	public Value getValue() {
		return value;
	}

	public Type getType() {
		return type;
	}

	@Override
	public String toString() {
		return "Member{ "
				+ value.toString() + " "
				+ typename + " "
				+ fieldMap + " "
				+ "}";
	}
}