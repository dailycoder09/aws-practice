package com.microservices.inventory.dto.response;

import lombok.Builder;
import lombok.Value;

/**
 * AWS EC2 instance details read from the instance metadata service.
 * Any field can be null if the metadata service did not return it (for example, an instance without a public IP).
 */
@Value
@Builder
public class CloudMetadata {

    String instanceId;
    String instanceType;
    String availabilityZone;
    String region;
    String privateIp;
    String publicIp;
    String amiId;
}
