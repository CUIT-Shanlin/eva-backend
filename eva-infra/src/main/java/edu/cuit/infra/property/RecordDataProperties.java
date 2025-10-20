package edu.cuit.infra.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "eva.records-image")
@Data
public class RecordDataProperties {

    private String directory = "/data/eva/records";

}
