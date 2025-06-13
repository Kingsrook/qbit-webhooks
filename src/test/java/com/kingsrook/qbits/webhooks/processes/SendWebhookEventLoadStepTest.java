/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.kingsrook.qbits.webhooks.processes;


import java.util.ArrayList;
import java.util.Collections;
import com.kingsrook.qbits.webhooks.BaseTest;
import com.kingsrook.qbits.webhooks.WebhooksTestApplication;
import com.kingsrook.qbits.webhooks.actions.WebhookEventSender;
import com.kingsrook.qbits.webhooks.actions.WebhookEventSenderTest;
import com.kingsrook.qbits.webhooks.model.Webhook;
import com.kingsrook.qbits.webhooks.model.WebhookEvent;
import com.kingsrook.qbits.webhooks.model.WebhookEventSendLog;
import com.kingsrook.qbits.webhooks.model.WebhookEventStatus;
import com.kingsrook.qbits.webhooks.model.WebhookHealthStatus;
import com.kingsrook.qbits.webhooks.model.WebhookSubscription;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLine;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLineInterface;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.Status;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for SendWebhookEventLoadStep 
 *******************************************************************************/
class SendWebhookEventLoadStepTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test() throws QException
   {
      String eventTypeName = WebhooksTestApplication.PERSON_INSERTED_EVENT_TYPE_NAME;

      Integer webhookId = insert(newWebhook("test"));
      Webhook webhook   = new Webhook(GetAction.execute(Webhook.TABLE_NAME, webhookId));

      Integer             subscriptionId      = insert(newWebhookSubscription(eventTypeName).withWebhookId(webhookId));
      WebhookSubscription webhookSubscription = new WebhookSubscription(GetAction.execute(WebhookSubscription.TABLE_NAME, subscriptionId));

      ///////////////////////////////////////////
      // insert 3 events, each with 4 failures //
      ///////////////////////////////////////////
      Integer eventId0 = insert(newWebhookEvent(webhookSubscription, eventTypeName).withEventStatusId(WebhookEventStatus.AWAITING_RETRY.getId()));
      Integer eventId1 = insert(newWebhookEvent(webhookSubscription, eventTypeName).withEventStatusId(WebhookEventStatus.AWAITING_RETRY.getId()));
      Integer eventId2 = insert(newWebhookEvent(webhookSubscription, eventTypeName).withEventStatusId(WebhookEventStatus.AWAITING_RETRY.getId()));

      new InsertAction().execute(new InsertInput(WebhookEventSendLog.TABLE_NAME).withRecordEntities(Collections.nCopies(4,
         new WebhookEventSendLog().withWebhookId(webhookId).withWebhookEventId(eventId0).withSuccessful(false))));

      new InsertAction().execute(new InsertInput(WebhookEventSendLog.TABLE_NAME).withRecordEntities(Collections.nCopies(4,
         new WebhookEventSendLog().withWebhookId(webhookId).withWebhookEventId(eventId1).withSuccessful(false))));

      new InsertAction().execute(new InsertInput(WebhookEventSendLog.TABLE_NAME).withRecordEntities(Collections.nCopies(4,
         new WebhookEventSendLog().withWebhookId(webhookId).withWebhookEventId(eventId2).withSuccessful(false))));

      SendWebhookEventLoadStep loadStep = getSendWebhookEventLoadStep(new WebhookEventSenderTest.WebhookEventSenderThatFails());
      loadStep.setTransformStep(new SendWebhookEventTransformStep().withWebhook(webhook));

      //////////////////////////////////
      // run process for these events //
      //////////////////////////////////
      RunBackendStepInput input = new RunBackendStepInput();
      input.withRecords(QueryAction.execute(WebhookEvent.TABLE_NAME, new QQueryFilter()
         .withCriteria(new QFilterCriteria("id", QCriteriaOperator.IN, eventId0, eventId1, eventId2))
         .withOrderBy(new QFilterOrderBy("id"))));
      RunBackendStepOutput output = new RunBackendStepOutput();
      loadStep.runOnePage(input, output);

      assertEquals(WebhookHealthStatus.UNHEALTHY.getId(), GetAction.execute(Webhook.TABLE_NAME, webhookId).getValue("healthStatusId"));

      ///////////////////////////////////////////////////////////////////////////////////////////////////////
      // only the first event should have been processed, because the webhook became unhealthy after that. //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////
      assertEquals(WebhookEventStatus.FAILED.getId(), GetAction.execute(WebhookEvent.TABLE_NAME, eventId0).getValue("eventStatusId"));
      assertEquals(WebhookEventStatus.AWAITING_RETRY.getId(), GetAction.execute(WebhookEvent.TABLE_NAME, eventId1).getValue("eventStatusId"));
      assertEquals(WebhookEventStatus.AWAITING_RETRY.getId(), GetAction.execute(WebhookEvent.TABLE_NAME, eventId2).getValue("eventStatusId"));

      ArrayList<ProcessSummaryLineInterface> processSummaryLines = loadStep.doGetProcessSummary(output, true);
      assertEquals(2, processSummaryLines.size());

      assertEquals(Status.ERROR, processSummaryLines.get(0).getStatus());
      assertEquals(1, ((ProcessSummaryLine) processSummaryLines.get(0)).getCount());
      assertThat(processSummaryLines.get(0).getMessage()).contains("failed to deliver");

      assertEquals(Status.WARNING, processSummaryLines.get(1).getStatus());
      assertEquals(2, ((ProcessSummaryLine) processSummaryLines.get(1)).getCount());
      assertThat(processSummaryLines.get(1).getMessage()).contains("were not sent because their associated webhook became unhealthy.");
   }



   /***************************************************************************
    *
    ***************************************************************************/
   private static SendWebhookEventLoadStep getSendWebhookEventLoadStep(WebhookEventSender webhookEventSender)
   {
      SendWebhookEventLoadStep loadStep = new SendWebhookEventLoadStep()
      {
         /***************************************************************************
          **
          ***************************************************************************/
         @Override
         protected WebhookEventSender newWebhookEventSender()
         {
            return webhookEventSender;
         }
      };
      return loadStep;
   }

}