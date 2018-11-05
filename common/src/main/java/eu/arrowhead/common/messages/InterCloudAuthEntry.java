/*
 * This work is part of the Productive 4.0 innovation project, which receives grants from the
 * European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 * (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 * national funding authorities from involved countries.
 */

package eu.arrowhead.common.messages;

import eu.arrowhead.common.database.ArrowheadCloud;
import java.util.ArrayList;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;

public class InterCloudAuthEntry {

  @Valid
  @NotNull
  private ArrowheadCloud cloud;
  @Valid
  @NotEmpty
  private List<ArrowheadService> serviceList = new ArrayList<>();

  public InterCloudAuthEntry() {
  }

  public InterCloudAuthEntry(ArrowheadCloud cloud, List<ArrowheadService> serviceList) {
    this.cloud = cloud;
    this.serviceList = serviceList;
  }

  public ArrowheadCloud getCloud() {
    return cloud;
  }

  public void setCloud(ArrowheadCloud cloud) {
    this.cloud = cloud;
  }

  public List<ArrowheadService> getServiceList() {
    return serviceList;
  }

  public void setServiceList(List<ArrowheadService> serviceList) {
    this.serviceList = serviceList;
  }

}
