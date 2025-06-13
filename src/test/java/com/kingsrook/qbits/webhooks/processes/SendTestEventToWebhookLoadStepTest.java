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
import java.util.List;
import com.kingsrook.qbits.webhooks.BaseTest;
import com.kingsrook.qbits.webhooks.actions.WebhookEventSender;
import com.kingsrook.qbits.webhooks.actions.WebhookEventSenderTest;
import com.kingsrook.qbits.webhooks.model.Webhook;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLine;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLineInterface;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.Status;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for SendTestEventToWebhookLoadStep 
 *******************************************************************************/
class SendTestEventToWebhookLoadStepTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSuccess() throws QException
   {
      ProcessSummaryLine processSummaryLine = runProcessForSender(new WebhookEventSenderTest.WebhookEventSenderThatSucceeds(), 2);
      assertEquals(Status.OK, processSummaryLine.getStatus());
      assertEquals("were delivered successfully", processSummaryLine.getMessage());
      assertEquals(2, processSummaryLine.getCount());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testErrorWithoutStatusCode() throws QException
   {
      ProcessSummaryLine processSummaryLine = runProcessForSender(new WebhookEventSenderTest.WebhookEventSenderThatFails());
      assertEquals(Status.ERROR, processSummaryLine.getStatus());
      assertEquals("failed to send with error message: Test failure", processSummaryLine.getMessage());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testErrorsRollup() throws QException
   {
      ProcessSummaryLine processSummaryLine = runProcessForSender(new WebhookEventSenderTest.WebhookEventSenderThatFails(), 3);
      assertEquals(Status.ERROR, processSummaryLine.getStatus());
      assertEquals("failed to send with error message: Test failure", processSummaryLine.getMessage());
      assertEquals(3, processSummaryLine.getCount());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testErrorWithStatusCode() throws QException
   {
      ProcessSummaryLine processSummaryLine = runProcessForSender(new WebhookEventSenderTest.WebhookEventSenderThatMocksHttp(500, "Server Error"));
      assertEquals(Status.ERROR, processSummaryLine.getStatus());
      assertEquals("failed to send with error message: Status Code: 500; Server Error", processSummaryLine.getMessage());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static ProcessSummaryLine runProcessForSender(WebhookEventSender webhookEventSender) throws QException
   {
      return runProcessForSender(webhookEventSender, 1);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static ProcessSummaryLine runProcessForSender(WebhookEventSender webhookEventSender, int nCopies) throws QException
   {
      SendTestEventToWebhookLoadStep step                 = getSendTestEventToWebhookLoadStep(webhookEventSender);
      RunBackendStepOutput           runBackendStepOutput = new RunBackendStepOutput();

      List<QRecord> records = Collections.nCopies(nCopies, new Webhook().withUrl("http://localhost:8000/").toQRecord());

      step.runOnePage(new RunBackendStepInput().withRecords(records), runBackendStepOutput);
      ArrayList<ProcessSummaryLineInterface> processSummary     = step.doGetProcessSummary(runBackendStepOutput, true);
      ProcessSummaryLine                     processSummaryLine = (ProcessSummaryLine) processSummary.get(0);
      return processSummaryLine;
   }



   /***************************************************************************
    *
    ***************************************************************************/
   private static SendTestEventToWebhookLoadStep getSendTestEventToWebhookLoadStep(WebhookEventSender webhookEventSender)
   {
      SendTestEventToWebhookLoadStep step = new SendTestEventToWebhookLoadStep()
      {
         /***************************************************************************
          **
          ***************************************************************************/
         @Override
         protected WebhookEventSender getWebhookEventSender()
         {
            return webhookEventSender;
         }
      };
      return step;
   }

}