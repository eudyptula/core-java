/*
 * This work is part of the Productive 4.0 innovation project, which receives grants from the
 * European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 * (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 * national funding authorities from involved countries.
 */

package eu.arrowhead.common.messages;

import eu.arrowhead.common.database.ArrowheadSystem;
import java.util.ArrayList;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;

public class IntraCloudAuthEntry {

  @Valid
  @NotNull
  private ArrowheadSystem consumer;
  @Valid
  @NotEmpty
  private List<ArrowheadSystem> providerList = new ArrayList<>();
  @Valid
  @NotEmpty
  private List<ArrowheadService> serviceList = new ArrayList<>();

  public IntraCloudAuthEntry() {
  }

  public IntraCloudAuthEntry(ArrowheadSystem consumer, List<ArrowheadSystem> providerList, List<ArrowheadService> serviceList) {
    this.consumer = consumer;
    this.providerList = providerList;
    this.serviceList = serviceList;
  }

  public ArrowheadSystem getConsumer() {
    return consumer;
  }

  public void setConsumer(ArrowheadSystem consumer) {
    this.consumer = consumer;
  }

  public List<ArrowheadSystem> getProviderList() {
    return providerList;
  }

  public void setProviderList(List<ArrowheadSystem> providerList) {
    this.providerList = providerList;
  }

  public List<ArrowheadService> getServiceList() {
    return serviceList;
  }

  public void setServiceList(List<ArrowheadService> serviceList) {
    this.serviceList = serviceList;
  }

}
