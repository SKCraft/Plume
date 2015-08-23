/**
 * This class is generated by jOOQ
 */
package com.skcraft.plume.common.service.sql.model.data.tables.records;


import com.skcraft.plume.common.service.sql.model.data.tables.Party;

import java.sql.Timestamp;

import javax.annotation.Generated;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record2;
import org.jooq.Row2;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * This class is generated by jOOQ.
 */
@Generated(
	value = {
		"http://www.jooq.org",
		"jOOQ version:3.6.2"
	},
	comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class PartyRecord extends UpdatableRecordImpl<PartyRecord> implements Record2<String, Timestamp> {

	private static final long serialVersionUID = -22048125;

	/**
	 * Setter for <code>data.party.name</code>.
	 */
	public void setName(String value) {
		setValue(0, value);
	}

	/**
	 * Getter for <code>data.party.name</code>.
	 */
	public String getName() {
		return (String) getValue(0);
	}

	/**
	 * Setter for <code>data.party.create_time</code>.
	 */
	public void setCreateTime(Timestamp value) {
		setValue(1, value);
	}

	/**
	 * Getter for <code>data.party.create_time</code>.
	 */
	public Timestamp getCreateTime() {
		return (Timestamp) getValue(1);
	}

	// -------------------------------------------------------------------------
	// Primary key information
	// -------------------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Record1<String> key() {
		return (Record1) super.key();
	}

	// -------------------------------------------------------------------------
	// Record2 type implementation
	// -------------------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Row2<String, Timestamp> fieldsRow() {
		return (Row2) super.fieldsRow();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Row2<String, Timestamp> valuesRow() {
		return (Row2) super.valuesRow();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Field<String> field1() {
		return Party.PARTY.NAME;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Field<Timestamp> field2() {
		return Party.PARTY.CREATE_TIME;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String value1() {
		return getName();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Timestamp value2() {
		return getCreateTime();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PartyRecord value1(String value) {
		setName(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PartyRecord value2(Timestamp value) {
		setCreateTime(value);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PartyRecord values(String value1, Timestamp value2) {
		value1(value1);
		value2(value2);
		return this;
	}

	// -------------------------------------------------------------------------
	// Constructors
	// -------------------------------------------------------------------------

	/**
	 * Create a detached PartyRecord
	 */
	public PartyRecord() {
		super(Party.PARTY);
	}

	/**
	 * Create a detached, initialised PartyRecord
	 */
	public PartyRecord(String name, Timestamp createTime) {
		super(Party.PARTY);

		setValue(0, name);
		setValue(1, createTime);
	}
}
