/*
 * This work is part of the Productive 4.0 innovation project, which receives grants from the
 * European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 * (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 * national funding authorities from involved countries.
 */

package eu.arrowhead.common.database;

import com.google.common.base.MoreObjects;
import eu.arrowhead.common.messages.ArrowheadService;
import java.util.Objects;
import javax.persistence.CascadeType;
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
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * JPA entity class for storing inter-cloud authorization rights in the database. The <i>consumer_cloud_id</i> and <i>arrowhead_service_id</i> columns
 * must be unique together. <p> The table contains foreign keys to {@link ArrowheadCloud} and {@link ArrowheadService}. A particular
 * <tt>ArrowheadCloud</tt> - <tt>ArrowheadService</tt> pair is authorized if there is a database entry for it in this table. The existence of the
 * database entry means the given cloud can consume the given service from an {@link ArrowheadSystem} inside the Local Cloud.
 *
 * @author Umlauf Zoltán
 */
@Entity
@Table(name = "inter_cloud_authorization", uniqueConstraints = {@UniqueConstraint(columnNames = {"consumer_cloud_id", "arrowhead_service_id"})})
public class InterCloudAuthorization {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;

  @Valid
  @NotNull
  @JoinColumn(name = "consumer_cloud_id")
  @ManyToOne(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  @OnDelete(action = OnDeleteAction.CASCADE)
  private ArrowheadCloud cloud;

  @Valid
  @NotNull
  @JoinColumn(name = "arrowhead_service_id")
  @ManyToOne(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  @OnDelete(action = OnDeleteAction.CASCADE)
  private ArrowheadService service;

  public InterCloudAuthorization() {
  }

  public InterCloudAuthorization(ArrowheadCloud cloud, ArrowheadService service) {
    this.cloud = cloud;
    this.service = service;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public ArrowheadCloud getCloud() {
    return cloud;
  }

  public void setCloud(ArrowheadCloud cloud) {
    this.cloud = cloud;
  }

  public ArrowheadService getService() {
    return service;
  }

  public void setService(ArrowheadService service) {
    this.service = service;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof InterCloudAuthorization)) {
      return false;
    }
    InterCloudAuthorization that = (InterCloudAuthorization) o;
    return Objects.equals(cloud, that.cloud) && Objects.equals(service, that.service);
  }

  @Override
  public int hashCode() {
    return Objects.hash(cloud, service);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("cloud", cloud).add("service", service).toString();
  }
}
