/*
 * This work is part of the Productive 4.0 innovation project, which receives grants from the
 * European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 * (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 * national funding authorities from involved countries.
 */

package eu.arrowhead.common.messages;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.MoreObjects;
import eu.arrowhead.common.database.ArrowheadSystem;
import eu.arrowhead.common.json.constraint.LDTInFuture;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "service_registry", uniqueConstraints = {@UniqueConstraint(columnNames = {"arrowhead_service_id", "provider_system_id"})})
public class ServiceRegistryEntry {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;

  @Valid
  @NotNull(message = "Provided ArrowheadService cannot be null")
  @JoinColumn(name = "arrowhead_service_id")
  @ManyToOne(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  @OnDelete(action = OnDeleteAction.CASCADE)
  private ArrowheadService providedService;

  @Valid
  @NotNull(message = "Provider ArrowheadSystem cannot be null")
  @JoinColumn(name = "provider_system_id")
  @ManyToOne(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  @OnDelete(action = OnDeleteAction.CASCADE)
  private ArrowheadSystem provider;

  @Column(name = "service_uri")
  @Size(max = 255, message = "Service URI must be 255 character at max")
  private String serviceUri;

  @Type(type = "yes_no")
  private Boolean udp = false;

  @Column(name = "end_of_validity")
  @LDTInFuture(message = "End of validity date must be in the future")
  private LocalDateTime endOfValidity;

  private Integer version = 1;

  //Takes the providedService metadata map
  @JsonIgnore
  private String metadata;

  public ServiceRegistryEntry() {
  }

  public ServiceRegistryEntry(ArrowheadService providedService, ArrowheadSystem provider, String serviceUri) {
    this.providedService = providedService;
    this.provider = provider;
    this.serviceUri = serviceUri;
  }

  public ServiceRegistryEntry(ArrowheadService providedService, ArrowheadSystem provider, String serviceUri, boolean udp, LocalDateTime endOfValidity,
                              int version) {
    this.providedService = providedService;
    this.provider = provider;
    this.serviceUri = serviceUri;
    this.udp = udp;
    this.endOfValidity = endOfValidity;
    this.version = version;
  }

  public ServiceRegistryEntry(ArrowheadService providedService, ArrowheadSystem provider, String serviceUri, Boolean udp, LocalDateTime endOfValidity,
                              Integer version, String metadata) {
    this.providedService = providedService;
    this.provider = provider;
    this.serviceUri = serviceUri;
    this.udp = udp;
    this.endOfValidity = endOfValidity;
    this.version = version;
    this.metadata = metadata;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public ArrowheadService getProvidedService() {
    return providedService;
  }

  public void setProvidedService(ArrowheadService providedService) {
    this.providedService = providedService;
  }

  public ArrowheadSystem getProvider() {
    return provider;
  }

  public void setProvider(ArrowheadSystem provider) {
    this.provider = provider;
  }

  public String getServiceUri() {
    return serviceUri;
  }

  public void setServiceUri(String serviceUri) {
    this.serviceUri = serviceUri;
  }

  public Boolean isUdp() {
    return udp;
  }

  public void setUdp(Boolean udp) {
    this.udp = udp;
  }

  public LocalDateTime getEndOfValidity() {
    return endOfValidity;
  }

  public void setEndOfValidity(LocalDateTime endOfValidity) {
    this.endOfValidity = endOfValidity;
  }

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ServiceRegistryEntry)) {
      return false;
    }
    ServiceRegistryEntry that = (ServiceRegistryEntry) o;
    return Objects.equals(providedService, that.providedService) && Objects.equals(provider, that.provider) && Objects
        .equals(serviceUri, that.serviceUri) && Objects.equals(version, that.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(providedService, provider, serviceUri, version);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("providedService", providedService).add("provider", provider).add("serviceUri", serviceUri)
                      .add("version", version).toString();
  }

  public void toDatabase() {
    if (providedService.getServiceMetadata() != null && !providedService.getServiceMetadata().isEmpty()) {
      StringBuilder sb = new StringBuilder();
      for (Map.Entry<String, String> entry : providedService.getServiceMetadata().entrySet()) {
        sb.append(entry.getKey()).append("=").append(entry.getValue()).append(",");
      }
      metadata = sb.toString().substring(0, sb.length() - 1);
    }
  }

  public void fromDatabase() {
    ArrowheadService temp = providedService;
    providedService = new ArrowheadService();
    providedService.setServiceDefinition(temp.getServiceDefinition());
    providedService.setInterfaces(temp.getInterfaces());

    if (metadata != null) {
      String[] parts = metadata.split(",");
      providedService.getServiceMetadata().clear();
      for (String part : parts) {
        String[] pair = part.split("=");
        providedService.getServiceMetadata().put(pair[0], pair[1]);
      }
    }
  }

}