package com.skcraft.plume.common.util.pagination;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;

import java.util.Iterator;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class ListPaginationTest {

    private ListPagination<String> test;

    @Before
    public void setUp() throws Exception {
        List<String> data = Lists.newArrayList("a", "b", "c", "d", "e", "f", "g");
        test = new ListPagination<>(data, 2);
    }

    @Test
    public void testGetPerPage() throws Exception {
        assertThat(test.getPerPage(), is(2));
    }

    @Test
    public void testFirstPage() throws Exception {
        assertThat(test.firstPage(), is(1));
    }

    @Test
    public void testLastPage() throws Exception {
        assertThat(test.lastPage(), is(4));
    }

    @Test
    public void testFirst() throws Exception {
        assertThat(test.first(), equalTo(test.at(1)));
    }

    @Test
    public void testLast() throws Exception {
        assertThat(test.last(), equalTo(test.at(4)));
    }

    @Test
    public void testHas() throws Exception {
        assertThat(test.has(-1), is(false));
        assertThat(test.has(0), is(false));
        assertThat(test.has(1), is(true));
        assertThat(test.has(2), is(true));
        assertThat(test.has(3), is(true));
        assertThat(test.has(4), is(true));
        assertThat(test.has(5), is(false));
    }

    @Test
    public void testAt() throws Exception {
        assertThat(test.at(1).get(0), equalTo("a"));
        assertThat(test.at(1).get(1), equalTo("b"));
        assertThat(test.at(1).size(), is(2));
        assertThat(test.at(2).get(0), equalTo("c"));
        assertThat(test.at(2).get(1), equalTo("d"));
        assertThat(test.at(2).size(), is(2));
        assertThat(test.at(4).get(0), equalTo("g"));
        assertThat(test.at(4).size(), is(1));
        assertThat(test.at(5), nullValue());
    }

    @Test
    public void testIsEmpty() throws Exception {
        assertThat(test.isEmpty(), is(false));
    }

    @Test
    public void testItemCount() throws Exception {
        assertThat(test.itemCount(), is(7));
    }

    @Test
    public void testPageCount() throws Exception {
        assertThat(test.pageCount(), is(4));
    }

    @Test
    public void testIterator() throws Exception {
        Iterator<Page<String>> it = test.iterator();
        assertThat(it.next(), equalTo(test.at(1)));
        assertThat(it.next(), equalTo(test.at(2)));
        assertThat(it.next(), equalTo(test.at(3)));
        assertThat(it.next(), equalTo(test.at(4)));
        assertThat(it.hasNext(), is(false));
    }
}
