package ru.rbt.barsgl.gwt.core.datafields;

import java.io.Serializable;

public class Param2 implements Serializable {
	private static final long serialVersionUID = -8564559482872749526L;
	
	public enum Direction {IN, OUT, INOUT}
	
	private Direction direction;
	private String name;
	private Field value;
	private Column.Type type;
	
	@SuppressWarnings("unused")
	private Param2() {}
	
	public Param2(String name, Field value, Column.Type type, Direction direction) {
		this.value = value;
		this.direction = direction;
		this.type = type;
		this.name = name;
	}
	
	public Param2(String name, Field value, Column.Type type) {
		this(name, value, type, Direction.IN);
	}
	
	public Param2(String name, Serializable value, Column.Type type, Direction direction) {
		this(name, new Field(value), type, direction);
	}

	public Param2(String name, Serializable value, Column.Type type) {
		this(name, value, type, Direction.IN);
	}

	public String getName() {
		return name;
	}
	
	public Direction getDirection() {
		return direction;
	}
	
	public Field getValue() {
		return value;
	}
	
	public Column.Type getType() {
		return type;
	}
}
