package org.example;

import soot.Local;
import soot.Value;

import java.util.LinkedHashSet;
import java.util.Set;

public class Variable {
	private Local local;
	private String name;
	private Member member; // local vars
	private Set<Integer> sourceId = new LinkedHashSet<>();

	public Variable(Value value, Member member) {
		name = value.toString();
		// if it is local
		if (value instanceof Local) {
			this.local = (Local)value;
		}
		this.member = member;
	}

	public static Variable getInstance(Value value) {
		Member m = new Member(value);
		return new Variable(value, m);
	}

	public void addAllocId(Integer id) {
		if (sourceId != null) {
			sourceId.add(id);
		}
	}

	public Variable copyDeep(boolean flag) {
		Variable var = new Variable(local, flag ? this.member.copyDeep() : this.member);
		var.sourceId.addAll(this.sourceId);
		return var;
	}

	public void assign(Variable var) {
		sourceId.clear();
		sourceId.addAll(var.sourceId);
		member = var.member;
	}

	public Local getLocal() {
		return local;
	}

	public Member getMember() {
		return member;
	}

	public Set<Integer> getSourceId() {
		return sourceId;
	}

	@Override
	public String toString() {
		return "Variable{ "
				+ name + " "
				+ System.identityHashCode(member) + " "
				+ sourceId + " "
				+ "}";
	}

//	@Override
//	public int hashCode() {
//		return super.hashCode();
//	}
//
//	@Override
//	public boolean equals(Object obj) {
//		return super.equals(obj);
//	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((member == null) ? 0 : member.hashCode());
		result = prime * result + ((sourceId == null) ? 0 : sourceId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		Variable other = (Variable) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (member == null) {
			if (other.member != null)
				return false;
		} else if (!member.equals(other.member))
			return false;
		if (sourceId == null) {
			if (other.sourceId != null)
				return false;
		} else if (!sourceId.equals(other.sourceId))
			return false;
		return true;
	}

}
