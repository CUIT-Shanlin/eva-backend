package edu.cuit.infra;

import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.GenericConditionalQuery;
import edu.cuit.infra.util.QueryUtils;
import org.junit.jupiter.api.Test;

public class CacheTest {

    @Test
    public void testCreatePage() {
        PagingQuery<GenericConditionalQuery> query = new PagingQuery<>();
        query.setPage(2);
        query.setSize(15);
        var page = QueryUtils.createPage(query);
        org.junit.jupiter.api.Assertions.assertEquals(2, page.getCurrent());
        org.junit.jupiter.api.Assertions.assertEquals(15, page.getSize());
    }
}
