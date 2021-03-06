/**
 * This class is generated by jOOQ
 */
package com.skcraft.plume.common.service.sql.model.data.tables;


import com.skcraft.plume.common.service.sql.model.data.Data;
import com.skcraft.plume.common.service.sql.model.data.Keys;
import com.skcraft.plume.common.service.sql.model.data.tables.records.GroupParentRecord;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Generated;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.UniqueKey;
import org.jooq.impl.TableImpl;


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
public class GroupParent extends TableImpl<GroupParentRecord> {

	private static final long serialVersionUID = -530349303;

	/**
	 * The reference instance of <code>data.group_parent</code>
	 */
	public static final GroupParent GROUP_PARENT = new GroupParent();

	/**
	 * The class holding records for this type
	 */
	@Override
	public Class<GroupParentRecord> getRecordType() {
		return GroupParentRecord.class;
	}

	/**
	 * The column <code>data.group_parent.parent_id</code>.
	 */
	public final TableField<GroupParentRecord, Integer> PARENT_ID = createField("parent_id", org.jooq.impl.SQLDataType.INTEGER.nullable(false), this, "");

	/**
	 * The column <code>data.group_parent.group_id</code>.
	 */
	public final TableField<GroupParentRecord, Integer> GROUP_ID = createField("group_id", org.jooq.impl.SQLDataType.INTEGER.nullable(false), this, "");

	/**
	 * Create a <code>data.group_parent</code> table reference
	 */
	public GroupParent() {
		this("group_parent", null);
	}

	/**
	 * Create an aliased <code>data.group_parent</code> table reference
	 */
	public GroupParent(String alias) {
		this(alias, GROUP_PARENT);
	}

	private GroupParent(String alias, Table<GroupParentRecord> aliased) {
		this(alias, aliased, null);
	}

	private GroupParent(String alias, Table<GroupParentRecord> aliased, Field<?>[] parameters) {
		super(alias, Data.DATA, aliased, parameters, "");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public UniqueKey<GroupParentRecord> getPrimaryKey() {
		return Keys.KEY_GROUP_PARENT_PRIMARY;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<UniqueKey<GroupParentRecord>> getKeys() {
		return Arrays.<UniqueKey<GroupParentRecord>>asList(Keys.KEY_GROUP_PARENT_PRIMARY);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<ForeignKey<GroupParentRecord, ?>> getReferences() {
		return Arrays.<ForeignKey<GroupParentRecord, ?>>asList(Keys.FK_GROUP_PARENT_PARENT_ID, Keys.FK_GROUP_PARENT_GROUP_ID);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public GroupParent as(String alias) {
		return new GroupParent(alias, this);
	}

	/**
	 * Rename this table
	 */
	public GroupParent rename(String name) {
		return new GroupParent(name, null);
	}
}
