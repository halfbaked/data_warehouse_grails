package com.erratick.datawarehouse.load.transformer

interface Transformer {
    List<Map<String, Object>> load(String data)
}