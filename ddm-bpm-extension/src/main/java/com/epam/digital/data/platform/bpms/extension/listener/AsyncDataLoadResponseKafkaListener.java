/*
 * Copyright 2023 EPAM Systems.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.digital.data.platform.bpms.extension.listener;

import com.epam.digital.data.platform.bpms.extension.delegate.dto.AsyncDataLoadResponse;
import com.epam.digital.data.platform.bpms.extension.delegate.dto.Result;
import com.epam.digital.data.platform.starter.kafka.config.properties.KafkaProperties;
import com.google.common.base.CaseFormat;
import org.camunda.bpm.engine.RuntimeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka listener that receives messages from the Data Factory and notifies the business process
 * about the completion of data load.
 */
@Component
@ConditionalOnProperty(prefix = "data-platform", name = {"kafka.consumer.enabled"}, havingValue = "true")
public class AsyncDataLoadResponseKafkaListener {

  private static final String ACTION = "DataLoadCsv";
  @Autowired
  private RuntimeService runtimeService;
  @Autowired
  private KafkaProperties kafkaProperties;

  @KafkaListener(topics = "#{kafkaProperties.additionalTopics.get('data-load-csv-topic-outbound')}",
      groupId = "#{kafkaProperties.consumer.groupId}")
  public void processAsyncMessages(
      @Payload AsyncDataLoadResponse message,
      MessageHeaders headers) {
    var payload = message.getPayload();
    var requestContext = message.getRequestContext();
    var result = new Result(message.getStatus(), message.getDetails());

    runtimeService.createMessageCorrelation(createMessageName(message))
        .processInstanceId(requestContext.getBusinessProcessInstanceId())
        .setVariable(payload.getResultVariable(), result)
        .correlate();
  }

  private String createMessageName(AsyncDataLoadResponse message) {
    return message.getPayload().getEntityName() + ACTION + CaseFormat.LOWER_UNDERSCORE.to(
        CaseFormat.UPPER_CAMEL, message.getStatus());
  }
}
