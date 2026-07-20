package com.simulator.metawhatsapp.dto.request.interactive;

import java.util.List;

public record SectionObject(
        String title,
        List<RowObject> rows,
        List<ProductItemObject> product_items
) {
}