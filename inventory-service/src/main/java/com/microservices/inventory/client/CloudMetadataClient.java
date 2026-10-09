package com.microservices.inventory.client;

import com.microservices.inventory.dto.response.CloudMetadata;

import java.util.Optional;

/**
 * Reads details about the AWS EC2 instance the app is running on.
 */
public interface CloudMetadataClient {

    /**
     * @return the instance details, or empty when not running on EC2 or the metadata service cannot be reached - never throws
     */
    Optional<CloudMetadata> fetch();
}
