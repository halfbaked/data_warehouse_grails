package com.erratick.datawarehouse.load.transformer

class CsvTransformer implements Transformer {

    List<Map<String, Object>> load(String csvString) {
        List<Map<String, Object>> output = []
        Map<String, Integer> colIndex = [:]
        csvString.eachLine { line, lineIdx ->

            List<String> lineValues = line.split(",")

            if(lineIdx == 0){
                // Process header to find what each col is
                lineValues.eachWithIndex{ String header, int i ->
                    colIndex[header] = i
                }
            } else {
                Map<String, Object> measurement = [:]
                colIndex.forEach { header, idx ->
                    measurement[header] = lineValues[idx].trim()
                }
                output << measurement
            }
        }
        return output
    }

}
