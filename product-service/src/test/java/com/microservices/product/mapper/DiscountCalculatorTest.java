package com.microservices.product.mapper;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class DiscountCalculatorTest {

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    @Test
    void discountPercent_isNull_whenMrpIsNull() {
        assertThat(DiscountCalculator.discountPercent(null, bd("999.99"))).isNull();
    }

    @Test
    void discountPercent_isNull_whenPriceIsNull() {
        assertThat(DiscountCalculator.discountPercent(bd("1099.99"), null)).isNull();
    }

    @Test
    void discountPercent_isNull_whenMrpEqualsPrice() {
        assertThat(DiscountCalculator.discountPercent(bd("999.99"), bd("999.99"))).isNull();
        // scale must not matter: 100.0 and 100.00 are the same amount
        assertThat(DiscountCalculator.discountPercent(bd("100.0"), bd("100.00"))).isNull();
    }

    @Test
    void discountPercent_isNull_whenMrpIsBelowPrice() {
        assertThat(DiscountCalculator.discountPercent(bd("90.00"), bd("100.00"))).isNull();
    }

    @Test
    void discountPercent_isNull_whenMrpIsZero() {
        assertThat(DiscountCalculator.discountPercent(BigDecimal.ZERO, BigDecimal.ZERO)).isNull();
        assertThat(DiscountCalculator.discountPercent(BigDecimal.ZERO, bd("5.00"))).isNull();
    }

    @Test
    void discountPercent_isWholePercent_forExactDiscount() {
        assertThat(DiscountCalculator.discountPercent(bd("200.00"), bd("150.00"))).isEqualTo(25);
        assertThat(DiscountCalculator.discountPercent(bd("100.00"), bd("1.00"))).isEqualTo(99);
    }

    @Test
    void discountPercent_roundsDown_whenFractionBelowHalf() {
        // (1099.99 - 999.99) / 1099.99 = 9.09 percent
        assertThat(DiscountCalculator.discountPercent(bd("1099.99"), bd("999.99"))).isEqualTo(9);
        // (3 - 2) / 3 = 33.33 percent
        assertThat(DiscountCalculator.discountPercent(bd("3.00"), bd("2.00"))).isEqualTo(33);
    }

    @Test
    void discountPercent_roundsHalfUp_whenFractionIsExactlyHalf() {
        // (200 - 199) / 200 = 0.5 percent -> 1
        assertThat(DiscountCalculator.discountPercent(bd("200.00"), bd("199.00"))).isEqualTo(1);
        // (200 - 197) / 200 = 1.5 percent -> 2
        assertThat(DiscountCalculator.discountPercent(bd("200.00"), bd("197.00"))).isEqualTo(2);
    }

    @Test
    void discountPercent_roundsUp_whenFractionAboveHalf() {
        // (3 - 1) / 3 = 66.67 percent -> 67
        assertThat(DiscountCalculator.discountPercent(bd("3.00"), bd("1.00"))).isEqualTo(67);
    }

    @Test
    void discountPercent_isZero_whenDiscountRoundsBelowOnePercent() {
        // mrp is above price (so there is a discount) but it rounds to 0 percent
        assertThat(DiscountCalculator.discountPercent(bd("1000.00"), bd("999.99"))).isEqualTo(0);
    }
}
