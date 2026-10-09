package com.microservices.product.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Product Images Request DTO
 *
 * Replaces the WHOLE image list of a product (0-10 images; an empty list removes all images).
 * Only URLs are accepted - there is no upload and no file storage.
 *
 * @author Microservices Team
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductImagesRequest {

    /**
     * New image list in display order; the first image becomes the product's primary image
     */
    @NotNull(message = "Images are required (use an empty list to remove all images)")
    @Size(max = 10, message = "A product can have at most 10 images")
    @Valid
    private List<Image> images;

    /**
     * One image: a http(s) URL plus optional alt text
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Image {

        /**
         * http/https only - stored URLs end up in an img src attribute, so other schemes
         * (javascript:, data:, ftp:, ...) are never accepted
         */
        @NotBlank(message = "Image URL is required")
        @Size(max = 500, message = "Image URL must not exceed 500 characters")
        @Pattern(regexp = "^https?://\\S+$", message = "Image URL must be an http or https URL")
        private String url;

        @Size(max = 255, message = "Image alt text must not exceed 255 characters")
        private String alt;
    }
}
