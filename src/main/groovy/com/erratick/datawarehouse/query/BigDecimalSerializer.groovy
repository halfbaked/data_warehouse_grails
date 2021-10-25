package com.erratick.datawarehouse.query

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider

import java.math.RoundingMode

/**
 * Used to get the partial numbers (BigDecimals) in the format we want in the api output
 */
class BigDecimalSerializer extends JsonSerializer<BigDecimal> {
    @Override
    void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider serializerProvider) throws IOException {
        if (value != null && value != 0) {
            BigDecimal formattedValue = value
                .setScale(6, RoundingMode.HALF_UP)
                .stripTrailingZeros().with {
                    if(scale() < 0) setScale(0) else it
                }
            gen.writeNumber(formattedValue);
        } else {
            gen.writeNumber(value);
        }
    }
}