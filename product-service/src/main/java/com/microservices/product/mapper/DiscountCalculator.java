package com.microservices.product.mapper;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Discount Calculator
 *
 * The single place where the derived discount percentage is computed. The discount is
 * never stored: it is recalculated from mrp and price on every read.
 *
 * @author Microservices Team
 * @version 1.0
 */
public final class DiscountCalculator {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private DiscountCalculator() {
    }

    /**
     * round((mrp - price) / mrp * 100), rounding half up to a whole percent
     *
     * @param mrp   maximum retail price, may be null
     * @param price selling price, may be null
     * @return discount percent, or null when mrp or price is null, mrp is not positive,
     *         or mrp is not above price (no discount to show)
     */
    public static Integer discountPercent(BigDecimal mrp, BigDecimal price) {
        if (mrp == null || price == null || mrp.signum() <= 0 || mrp.compareTo(price) <= 0) {
            return null;
        }
        return mrp.subtract(price)
                .multiply(HUNDRED)
                .divide(mrp, 0, RoundingMode.HALF_UP)
                .intValue();
    }
}
