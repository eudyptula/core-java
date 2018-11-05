/*
 * This work is part of the Productive 4.0 innovation project, which receives grants from the
 * European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 * (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 * national funding authorities from involved countries.
 */

package eu.arrowhead.common.messages;

import java.util.ArrayList;
import java.util.List;
import javax.validation.Valid;

public class ServiceQueryResult {

  @Valid
  private List<ServiceRegistryEntry> serviceQueryData = new ArrayList<>();

  public ServiceQueryResult() {
  }

  public ServiceQueryResult(List<ServiceRegistryEntry> serviceQueryData) {
    this.serviceQueryData = serviceQueryData;
  }

  public List<ServiceRegistryEntry> getServiceQueryData() {
    return serviceQueryData;
  }

  public void setServiceQueryData(List<ServiceRegistryEntry> serviceQueryData) {
    this.serviceQueryData = serviceQueryData;
  }

  public boolean isValid() {
    return serviceQueryData != null && !serviceQueryData.isEmpty();
  }

}
