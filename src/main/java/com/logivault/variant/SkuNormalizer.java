package com.logivault.variant;

public final class SkuNormalizer {

    private SkuNormalizer() {
    }

    public static String normalize(String sku) {
        return sku == null ? null : sku.trim().toUpperCase();
    }
}
