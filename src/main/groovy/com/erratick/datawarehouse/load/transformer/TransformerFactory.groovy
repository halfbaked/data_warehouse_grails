package com.erratick.datawarehouse.load.transformer
/**
 * Builds loaders
 */
class TransformerFactory {

    /**
     * Builds a loader depending on the request format provided
     */
    static Transformer buildLoader(String requestFormat) {
        switch(requestFormat) {
            case "text/csv":
                return new CsvTransformer()
            default:
                throw new TransformerNotFoundException("No transformer found for format $requestFormat")
        }
    }
}
