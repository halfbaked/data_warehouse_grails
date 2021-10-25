package com.erratick.datawarehouse.query.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

class QueryCreate {
    List<String> metrics = []
    Map<String, String> filterBy = [:]
    List<String> groupBy = ["date"]
    LocalDate rangeStart = LocalDate.now().minus(7, ChronoUnit.DAYS)
    LocalDate rangeEnd = LocalDate.now()
}
