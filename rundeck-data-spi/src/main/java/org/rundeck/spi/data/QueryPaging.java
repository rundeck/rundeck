package org.rundeck.spi.data;

public interface QueryPaging {
    int getOffset();

    int getTotal();

    int getPageSize();

    static QueryPaging with(int offset, int total, int pageSize) {
        return new QueryPaging() {
            @Override
            public int getOffset() {
                return offset;
            }

            @Override
            public int getTotal() {
                return total;
            }

            @Override
            public int getPageSize() {
                return pageSize;
            }
        };
    }
}
