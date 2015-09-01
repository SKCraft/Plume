package com.skcraft.plume.common.service.sql;

import com.google.common.collect.Lists;
import com.skcraft.plume.common.service.claim.Claim;
import com.skcraft.plume.common.util.WorldVector3i;
import org.junit.Before;
import org.junit.Test;
import uk.co.it.modular.hamcrest.date.DateMatchers;

import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;

public class DatabaseClaimsTest {

    private MockDatabase db;
    private final WorldVector3i SK_OWNED = new WorldVector3i("main", 20, 0, -30);
    private final WorldVector3i SK_OWNED2 = new WorldVector3i("second", 20, 0, -30);
    private final WorldVector3i VINCENT_OWNED = new WorldVector3i("main", 21, 0, -30);
    private final WorldVector3i VINCENT_OWNED2 = new WorldVector3i("second", 21, 0, -30);
    private final WorldVector3i UNOWNED = new WorldVector3i("main", 50, 0, -30);

    @Before
    public void setUp() throws Exception {
        db = MockDatabase.getInstance();
    }

    private DatabaseClaims createClaims(String server) {
        db.loadData();
        DatabaseManager manager = db.createDatabaseManager();
        DatabaseClaims claims = new DatabaseClaims(manager, server);
        claims.load();
        return claims;
    }

    private void verifySKOwned(DatabaseClaims claims) {
        Claim claim = claims.findClaimByPosition(SK_OWNED);
        assertThat(claim, notNullValue());
        assertThat(claim.getServer(), equalTo(MockDatabase.MOCK_SERVER));
        assertThat(claim.getWorld(), equalTo(SK_OWNED.getWorldId()));
        assertThat(claim.getOwner().getUuid(), equalTo(MockDatabase.SK_USER.getUuid()));
        assertThat(claim.getOwner().getName(), equalTo(MockDatabase.SK_USER.getName()));
        assertThat(claim.getParty(), equalTo("friends"));
        assertThat(claim.getX(), is(SK_OWNED.getX()));
        assertThat(claim.getZ(), is(SK_OWNED.getZ()));
        assertThat(claim.getIssueTime(), DateMatchers.sameSecond(MockDatabase.parseDate("2005-01-02 02:04:06")));
    }

    private void verifySKOwned2(DatabaseClaims claims) {
        Claim claim = claims.findClaimByPosition(SK_OWNED2);
        assertThat(claim, notNullValue());
        assertThat(claim.getServer(), equalTo(MockDatabase.MOCK_SERVER));
        assertThat(claim.getWorld(), equalTo(SK_OWNED2.getWorldId()));
        assertThat(claim.getOwner().getUuid(), equalTo(MockDatabase.SK_USER.getUuid()));
        assertThat(claim.getOwner().getName(), equalTo(MockDatabase.SK_USER.getName()));
        assertThat(claim.getParty(), nullValue());
        assertThat(claim.getX(), is(SK_OWNED2.getX()));
        assertThat(claim.getZ(), is(SK_OWNED2.getZ()));
        assertThat(claim.getIssueTime(), DateMatchers.sameSecond(MockDatabase.parseDate("2005-01-03 00:00:00")));
    }

    private void verifyVincentOwned(DatabaseClaims claims) {
        Claim claim = claims.findClaimByPosition(VINCENT_OWNED);
        assertThat(claim, notNullValue());
        assertThat(claim.getServer(), equalTo(MockDatabase.MOCK_SERVER));
        assertThat(claim.getWorld(), equalTo(VINCENT_OWNED.getWorldId()));
        assertThat(claim.getOwner().getUuid(), equalTo(MockDatabase.VINCENT_USER.getUuid()));
        assertThat(claim.getOwner().getName(), equalTo(MockDatabase.VINCENT_USER.getName()));
        assertThat(claim.getParty(), equalTo("guests"));
        assertThat(claim.getX(), is(VINCENT_OWNED.getX()));
        assertThat(claim.getZ(), is(VINCENT_OWNED.getZ()));
        assertThat(claim.getIssueTime(), DateMatchers.sameSecond(MockDatabase.parseDate("2005-02-03 00:10:00")));
    }

    private void verifyVincentOwned2(DatabaseClaims claims) {
        Claim claim = claims.findClaimByPosition(VINCENT_OWNED2);
        assertThat(claim, notNullValue());
        assertThat(claim.getServer(), equalTo(MockDatabase.MOCK_SERVER));
        assertThat(claim.getWorld(), equalTo(VINCENT_OWNED2.getWorldId()));
        assertThat(claim.getOwner().getUuid(), equalTo(MockDatabase.VINCENT_USER.getUuid()));
        assertThat(claim.getOwner().getName(), equalTo(MockDatabase.VINCENT_USER.getName()));
        assertThat(claim.getParty(), equalTo("friends"));
        assertThat(claim.getX(), is(VINCENT_OWNED2.getX()));
        assertThat(claim.getZ(), is(VINCENT_OWNED2.getZ()));
        assertThat(claim.getIssueTime(), DateMatchers.sameSecond(MockDatabase.parseDate("2015-04-03 00:00:00")));
    }

    @Test
    public void testFindClaimByPosition_Missing() throws Exception {
        DatabaseClaims claims = createClaims(MockDatabase.MOCK_SERVER + "_MISSING");
        Claim claim;

        claim = claims.findClaimByPosition(SK_OWNED);
        assertThat(claim, nullValue());
    }

    @Test
    public void testFindClaimByPosition() throws Exception {
        DatabaseClaims claims = createClaims(MockDatabase.MOCK_SERVER);
        Claim claim;

        claim = claims.findClaimByPosition(SK_OWNED);
        assertThat(claim, notNullValue());
        assertThat(claim.getServer(), equalTo(MockDatabase.MOCK_SERVER));
        assertThat(claim.getWorld(), equalTo("main"));
        assertThat(claim.getOwner().getUuid(), equalTo(MockDatabase.SK_USER.getUuid()));
        assertThat(claim.getOwner().getName(), equalTo(MockDatabase.SK_USER.getName()));
        assertThat(claim.getParty(), equalTo("friends"));
        assertThat(claim.getX(), is(SK_OWNED.getX()));
        assertThat(claim.getZ(), is(SK_OWNED.getZ()));
        assertThat(claim.getIssueTime(), DateMatchers.sameSecond(MockDatabase.parseDate("2005-01-02 02:04:06")));
    }

    @Test
    public void testGetClaimCount() throws Exception {
        DatabaseClaims claims = createClaims(MockDatabase.MOCK_SERVER);
        assertThat(claims.getClaimCount(MockDatabase.SK_USER), is(2));
        assertThat(claims.getClaimCount(MockDatabase.VINCENT_USER), is(2));
    }

    @Test
    public void testSaveClaim() throws Exception {
        DatabaseClaims claims = createClaims(MockDatabase.MOCK_SERVER);
        Claim claim;

        List<Claim> returned = claims.saveClaim(Lists.newArrayList(SK_OWNED, SK_OWNED2), MockDatabase.VINCENT_USER, "guests");

        assertThat(returned.size(), is(2));
        assertThat(returned, containsInAnyOrder(
                new Claim(MockDatabase.MOCK_SERVER, SK_OWNED),
                new Claim(MockDatabase.MOCK_SERVER, SK_OWNED2)));

        claim = claims.findClaimByPosition(SK_OWNED);
        assertThat(claim, notNullValue());
        assertThat(claim.getServer(), equalTo(MockDatabase.MOCK_SERVER));
        assertThat(claim.getWorld(), equalTo(SK_OWNED.getWorldId()));
        assertThat(claim.getOwner().getUuid(), equalTo(MockDatabase.VINCENT_USER.getUuid()));
        assertThat(claim.getOwner().getName(), equalTo(MockDatabase.VINCENT_USER.getName()));
        assertThat(claim.getParty(), equalTo("guests"));
        assertThat(claim.getX(), is(SK_OWNED.getX()));
        assertThat(claim.getZ(), is(SK_OWNED.getZ()));

        claim = claims.findClaimByPosition(SK_OWNED2);
        assertThat(claim, notNullValue());
        assertThat(claim.getServer(), equalTo(MockDatabase.MOCK_SERVER));
        assertThat(claim.getWorld(), equalTo(SK_OWNED2.getWorldId()));
        assertThat(claim.getOwner().getUuid(), equalTo(MockDatabase.VINCENT_USER.getUuid()));
        assertThat(claim.getOwner().getName(), equalTo(MockDatabase.VINCENT_USER.getName()));
        assertThat(claim.getParty(), equalTo("guests"));
        assertThat(claim.getX(), is(SK_OWNED2.getX()));
        assertThat(claim.getZ(), is(SK_OWNED2.getZ()));

        verifyVincentOwned(claims);
        verifyVincentOwned2(claims);
    }

    @Test
    public void testUpdateClaim_ExistingUser() throws Exception {
        DatabaseClaims claims = createClaims(MockDatabase.MOCK_SERVER);
        Claim claim;

        List<Claim> returned = claims.updateClaim(Lists.newArrayList(SK_OWNED, VINCENT_OWNED, UNOWNED), MockDatabase.VINCENT_USER, "guests", MockDatabase.VINCENT_USER);

        assertThat(returned.size(), is(2));
        assertThat(returned, containsInAnyOrder(
                new Claim(MockDatabase.MOCK_SERVER, VINCENT_OWNED),
                new Claim(MockDatabase.MOCK_SERVER, UNOWNED)));

        verifySKOwned(claims);

        claim = claims.findClaimByPosition(VINCENT_OWNED);
        assertThat(claim, notNullValue());
        assertThat(claim.getServer(), equalTo(MockDatabase.MOCK_SERVER));
        assertThat(claim.getWorld(), equalTo(VINCENT_OWNED.getWorldId()));
        assertThat(claim.getOwner().getUuid(), equalTo(MockDatabase.VINCENT_USER.getUuid()));
        assertThat(claim.getOwner().getName(), equalTo(MockDatabase.VINCENT_USER.getName()));
        assertThat(claim.getParty(), equalTo("guests"));
        assertThat(claim.getX(), is(VINCENT_OWNED.getX()));
        assertThat(claim.getZ(), is(VINCENT_OWNED.getZ()));

        verifyVincentOwned2(claims);

        claim = claims.findClaimByPosition(UNOWNED);
        assertThat(claim, notNullValue());
        assertThat(claim.getServer(), equalTo(MockDatabase.MOCK_SERVER));
        assertThat(claim.getWorld(), equalTo(UNOWNED.getWorldId()));
        assertThat(claim.getOwner().getUuid(), equalTo(MockDatabase.VINCENT_USER.getUuid()));
        assertThat(claim.getOwner().getName(), equalTo(MockDatabase.VINCENT_USER.getName()));
        assertThat(claim.getParty(), equalTo("guests"));
        assertThat(claim.getX(), is(UNOWNED.getX()));
        assertThat(claim.getZ(), is(UNOWNED.getZ()));
    }

    @Test
    public void testUpdateClaim_Unowned() throws Exception {
        DatabaseClaims claims = createClaims(MockDatabase.MOCK_SERVER);
        Claim claim;

        List<Claim> returned = claims.updateClaim(Lists.newArrayList(SK_OWNED, VINCENT_OWNED, UNOWNED), MockDatabase.VINCENT_USER, "guests", null);

        assertThat(returned.size(), is(1));
        assertThat(returned, containsInAnyOrder(
                new Claim(MockDatabase.MOCK_SERVER, UNOWNED)));

        verifySKOwned(claims);
        verifyVincentOwned(claims);
        verifyVincentOwned2(claims);

        claim = claims.findClaimByPosition(UNOWNED);
        assertThat(claim, notNullValue());
        assertThat(claim.getServer(), equalTo(MockDatabase.MOCK_SERVER));
        assertThat(claim.getWorld(), equalTo(UNOWNED.getWorldId()));
        assertThat(claim.getOwner().getUuid(), equalTo(MockDatabase.VINCENT_USER.getUuid()));
        assertThat(claim.getOwner().getName(), equalTo(MockDatabase.VINCENT_USER.getName()));
        assertThat(claim.getParty(), equalTo("guests"));
        assertThat(claim.getX(), is(UNOWNED.getX()));
        assertThat(claim.getZ(), is(UNOWNED.getZ()));
    }

}
