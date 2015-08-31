package com.skcraft.plume.common.util.pagination;

import com.google.common.collect.ForwardingList;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class ListPagination<T> implements Pagination<T> {

    @Getter
    private final List<T> data;
    private final int perPage;

    public ListPagination(List<T> data, int perPage) {
        checkNotNull(data, "data");
        checkArgument(perPage >= 1, "perPage >= 1");
        this.data = data;
        this.perPage = perPage;
    }

    @Override
    public int getPerPage() {
        return perPage;
    }

    @Override
    public int firstPage() {
        return 1;
    }

    @Override
    public int lastPage() {
        return (int) Math.ceil(data.size() / (double) perPage);
    }

    @Nullable
    @Override
    public Page<T> first() {
        return isEmpty() ? null : at(firstPage());
    }

    @Nullable
    @Override
    public Page<T> last() {
        return isEmpty() ? null : at(lastPage());
    }

    @Override
    public boolean has(int page) {
        return !isEmpty() && (page >= firstPage() && page <= lastPage());
    }

    @Nullable
    @Override
    public Page<T> at(int page) {
        if (isEmpty()) {
            return null;
        } else if (page >= 1) {
            if (page <= lastPage()) {
                return new ListPage(page, data.subList((page - 1) * perPage, Math.min(page * perPage, data.size())));
            } else {
                return null;
            }
        } else {
            throw new IllegalArgumentException("page >= 1");
        }
    }

    @Override
    public boolean isEmpty() {
        return data.isEmpty();
    }

    @Override
    public int itemCount() {
        return data.size();
    }

    @Override
    public int pageCount() {
        return (int) Math.ceil(data.size() / (double) perPage);
    }

    @Override
    public Iterator<Page<T>> iterator() {
        return new Iterator<Page<T>>() {
            private int index = 1;

            @Override
            public boolean hasNext() {
                return has(index);
            }

            @Override
            public Page<T> next() {
                return at(index++);
            }
        };
    }

    @EqualsAndHashCode(callSuper = false)
    private class ListPage extends ForwardingList<T> implements Page<T> {
        private final int index;
        private final List<T> subList;

        private ListPage(int index, List<T> subList) {
            this.index = index;
            this.subList = subList;
        }

        @Override
        protected List<T> delegate() {
            return subList;
        }

        @Override
        public Pagination<T> getPagination() {
            return ListPagination.this;
        }

        @Override
        public int getAbsoluteIndex(int index) {
            return (page() - 1) * getPagination().getPerPage() + index;
        }

        @Override
        public int page() {
            return index;
        }

        @Nullable
        @Override
        public Page<T> previous() {
            return ListPagination.this.at(index - 1);
        }

        @Nullable
        @Override
        public Page<T> next() {
            return ListPagination.this.at(index + 1);
        }
    }
}
