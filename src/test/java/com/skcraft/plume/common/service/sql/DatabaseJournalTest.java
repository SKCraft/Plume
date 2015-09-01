package com.skcraft.plume.common.service.sql;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.skcraft.plume.common.service.journal.criteria.Criteria;
import com.skcraft.plume.common.service.journal.Record;
import com.skcraft.plume.common.util.Order;
import org.junit.Before;
import org.junit.Test;
import uk.co.it.modular.hamcrest.date.DateMatchers;

import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class DatabaseJournalTest {

    private MockDatabase db;

    @Before
    public void setUp() throws Exception {
        db = MockDatabase.getInstance();
    }

    private DatabaseJournal createJournal() {
        db.loadData();
        DatabaseManager manager = db.createDatabaseManager();
        DatabaseJournal journal = new DatabaseJournal(manager);
        journal.load();
        return journal;
    }

    private void verifyRecord1(Record record) {
        assertThat(record.getId(), is(1));
        assertThat(record.getTime(), DateMatchers.sameSecond(MockDatabase.parseDate("2014-05-01 00:01:00")));
        assertThat(record.getUserId(), equalTo(MockDatabase.SK_USER));
        assertThat(record.getLocation().getWorldId(), equalTo("world"));
        assertThat(record.getLocation().getX(), is(-20));
        assertThat(record.getLocation().getY(), is(150));
        assertThat(record.getLocation().getZ(), is(30));
        assertThat(record.getAction(), is((short) 1));
    }

    private void verifyRecord2(Record record) {
        assertThat(record.getId(), is(2));
        assertThat(record.getTime(), DateMatchers.sameSecond(MockDatabase.parseDate("2014-05-01 00:02:00")));
        assertThat(record.getUserId(), equalTo(MockDatabase.SK_USER));
        assertThat(record.getLocation().getWorldId(), equalTo("world"));
        assertThat(record.getLocation().getX(), is(50));
        assertThat(record.getLocation().getY(), is(50));
        assertThat(record.getLocation().getZ(), is(30));
        assertThat(record.getAction(), is((short) 1));
    }

    private void verifyRecord3(Record record) {
        assertThat(record.getId(), is(3));
        assertThat(record.getTime(), DateMatchers.sameSecond(MockDatabase.parseDate("2014-05-01 00:03:00")));
        assertThat(record.getUserId(), equalTo(MockDatabase.VINCENT_USER));
        assertThat(record.getLocation().getWorldId(), equalTo("world"));
        assertThat(record.getLocation().getX(), is(-20));
        assertThat(record.getLocation().getY(), is(70));
        assertThat(record.getLocation().getZ(), is(30));
        assertThat(record.getAction(), is((short) 2));
    }

    private void verifyRecord4(Record record) {
        assertThat(record.getId(), is(4));
        assertThat(record.getTime(), DateMatchers.sameSecond(MockDatabase.parseDate("2014-05-01 00:04:00")));
        assertThat(record.getUserId(), equalTo(MockDatabase.SK_USER));
        assertThat(record.getLocation().getWorldId(), equalTo("nether"));
        assertThat(record.getLocation().getX(), is(50));
        assertThat(record.getLocation().getY(), is(50));
        assertThat(record.getLocation().getZ(), is(50));
        assertThat(record.getAction(), is((short) 2));
    }

    private void verifyRecord5(Record record) {
        assertThat(record.getId(), is(5));
        assertThat(record.getTime(), DateMatchers.sameSecond(MockDatabase.parseDate("2014-05-01 00:05:00")));
        assertThat(record.getUserId(), equalTo(MockDatabase.VINCENT_USER));
        assertThat(record.getLocation().getWorldId(), equalTo("end"));
        assertThat(record.getLocation().getX(), is(-20));
        assertThat(record.getLocation().getY(), is(40));
        assertThat(record.getLocation().getZ(), is(70));
        assertThat(record.getAction(), is((short) 1));
    }

    @Test
    public void testLoad() throws Exception {
        DatabaseJournal journal = createJournal();
        Map<String, Short> worldIds = journal.getWorldIds();
        assertThat(worldIds.get("world"), is((short) 1));
        assertThat(worldIds.get("nether"), is((short) 2));
        assertThat(worldIds.get("end"), is((short) 3));
    }

    @Test
    public void testQueryRecords_ASC() throws Exception {
        DatabaseJournal journal = createJournal();
        List<Record> records = journal.findRecords(new Criteria.Builder().build(), Order.ASC, 1000);
        assertThat(records.size(), is(5));
        verifyRecord1(records.get(0));
        verifyRecord2(records.get(1));
        verifyRecord3(records.get(2));
        verifyRecord4(records.get(3));
        verifyRecord5(records.get(4));
    }

    @Test
    public void testQueryRecords_DESC() throws Exception {
        DatabaseJournal journal = createJournal();
        List<Record> records = journal.findRecords(new Criteria.Builder().build(), Order.DESC, 1000);
        assertThat(records.size(), is(5));
        verifyRecord1(records.get(4));
        verifyRecord2(records.get(3));
        verifyRecord3(records.get(2));
        verifyRecord4(records.get(1));
        verifyRecord5(records.get(0));
    }

    @Test
    public void testQueryRecords_LimitASC() throws Exception {
        DatabaseJournal journal = createJournal();
        List<Record> records = journal.findRecords(new Criteria.Builder().build(), Order.ASC, 3);
        assertThat(records.size(), is(3));
        verifyRecord1(records.get(0));
        verifyRecord2(records.get(1));
        verifyRecord3(records.get(2));
    }

    @Test
    public void testQueryRecords_LimitDESC() throws Exception {
        DatabaseJournal journal = createJournal();
        List<Record> records = journal.findRecords(new Criteria.Builder().build(), Order.DESC, 3);
        assertThat(records.size(), is(3));
        verifyRecord5(records.get(0));
        verifyRecord4(records.get(1));
        verifyRecord3(records.get(2));
    }

    @Test
    public void testQueryRecords_Before() throws Exception {
        DatabaseJournal journal = createJournal();
        Criteria criteria = new Criteria.Builder()
                .setBefore(MockDatabase.parseDate("2014-05-01 00:03:00"))
                .build();
        List<Record> records = journal.findRecords(criteria, Order.ASC, 1000);
        assertThat(records.size(), is(2));
        verifyRecord1(records.get(0));
        verifyRecord2(records.get(1));
    }

    @Test
    public void testQueryRecords_Since() throws Exception {
        DatabaseJournal journal = createJournal();
        Criteria criteria = new Criteria.Builder()
                .setSince(MockDatabase.parseDate("2014-05-01 00:03:00"))
                .build();
        List<Record> records = journal.findRecords(criteria, Order.ASC, 1000);
        assertThat(records.size(), is(2));
        verifyRecord4(records.get(0));
        verifyRecord5(records.get(1));
    }

    @Test
    public void testQueryRecords_Region() throws Exception {
        DatabaseJournal journal = createJournal();
        Criteria criteria = new Criteria.Builder()
                .setContainedWithin(new CuboidRegion(new Vector(-20, 50, 10), new Vector(50, 80, 55)))
                .build();
        List<Record> records = journal.findRecords(criteria, Order.ASC, 1000);
        assertThat(records.size(), is(3));
        verifyRecord2(records.get(0));
        verifyRecord3(records.get(1));
        verifyRecord4(records.get(2));
    }

    @Test
    public void testQueryRecords_UserId() throws Exception {
        DatabaseJournal journal = createJournal();
        Criteria criteria = new Criteria.Builder()
                .setUserId(MockDatabase.VINCENT_USER)
                .build();
        List<Record> records = journal.findRecords(criteria, Order.ASC, 1000);
        assertThat(records.size(), is(2));
        verifyRecord3(records.get(0));
        verifyRecord5(records.get(1));
    }

    @Test
    public void testQueryRecords_World() throws Exception {
        DatabaseJournal journal = createJournal();
        Criteria criteria = new Criteria.Builder()
                .setWorldId("end")
                .build();
        List<Record> records = journal.findRecords(criteria, Order.ASC, 1000);
        assertThat(records.size(), is(1));
        verifyRecord5(records.get(0));
    }

}
