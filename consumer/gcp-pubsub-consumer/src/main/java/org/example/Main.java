package org.example;

import service.PubSubConsumerService;

public class Main {
  public static void main(String[] args) {
    String projectId = "serjava-demo";
    String subscriptionId = "sub-grupo4";

    System.out.println("Iniciando o Consumer do GCP...");

    PubSubConsumerService pubSubService = new PubSubConsumerService();

    pubSubService.startListening(projectId, subscriptionId);
  }
}