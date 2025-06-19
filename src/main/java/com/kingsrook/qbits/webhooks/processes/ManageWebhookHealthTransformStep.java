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


import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.kingsrook.qbits.webhooks.WebhooksQBitConfig;
import com.kingsrook.qbits.webhooks.model.Webhook;
import com.kingsrook.qbits.webhooks.model.WebhookEventSendLog;
import com.kingsrook.qbits.webhooks.model.WebhookHealthStatus;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLine;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLineInterface;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.Status;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.AbstractTransformStep;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 ** check if any unhealthy webhooks are ready to go into Probation
 *******************************************************************************/
public class ManageWebhookHealthTransformStep extends AbstractTransformStep
{
   private static final QLogger LOG = QLogger.getLogger(ManageWebhookHealthTransformStep.class);

   private ProcessSummaryLine willPutOnProbationLine = new ProcessSummaryLine(Status.OK)
      .withMessageSuffix(" updated from Unhealthy to Probation")
      .withSingularFutureMessage("will be")
      .withPluralFutureMessage("will be")
      .withSingularPastMessage("was")
      .withPluralPastMessage("were");

   private ProcessSummaryLine notUnhealthyLine = new ProcessSummaryLine(Status.INFO)
      .withMessageSuffix(" not currently Unhealthy")
      .withSingularFutureMessage("is")
      .withPluralFutureMessage("are")
      .withSingularPastMessage("was")
      .withPluralPastMessage("were");

   private ProcessSummaryLine notUnhealthyLongEnoughLine = new ProcessSummaryLine(Status.ERROR)
      .withMessageSuffix(" Unhealthy long enough to be put on Probation")
      .withSingularFutureMessage("has not been")
      .withPluralFutureMessage("have not been")
      .withSingularPastMessage("was not")
      .withPluralPastMessage("were not");



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public ArrayList<ProcessSummaryLineInterface> getProcessSummary(RunBackendStepOutput runBackendStepOutput, boolean isForResultScreen)
   {
      ArrayList<ProcessSummaryLineInterface> rs = new ArrayList<>();
      willPutOnProbationLine.addSelfToListIfAnyCount(rs);
      notUnhealthyLine.addSelfToListIfAnyCount(rs);
      notUnhealthyLongEnoughLine.addSelfToListIfAnyCount(rs);
      return (rs);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void runOnePage(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
   {
      Set<Webhook> unhealthy = new HashSet<>();
      for(Webhook webhook : runBackendStepInput.getRecordsAsEntities(Webhook.class))
      {
         if(WebhookHealthStatus.UNHEALTHY.getId().equals(webhook.getHealthStatusId()))
         {
            unhealthy.add(webhook);
         }
         else
         {
            notUnhealthyLine.incrementCountAndAddPrimaryKey(webhook.getId());
         }
      }

      if(CollectionUtils.nullSafeIsEmpty(unhealthy))
      {
         return;
      }

      Integer timeoutMinutes = WebhooksQBitConfig.getConfigValue(config -> config.getUnhealthyToProbationTimeoutMinutes());
      Instant limitTime = Instant.now().minus(timeoutMinutes, ChronoUnit.MINUTES);

      for(Webhook webhook : unhealthy)
      {
         List<WebhookEventSendLog> eventLogs = new QueryAction().execute(new QueryInput(WebhookEventSendLog.TABLE_NAME).withFilter(new QQueryFilter()
            .withCriteria(new QFilterCriteria("webhookId", QCriteriaOperator.EQUALS, webhook.getId()))
            .withOrderBy(new QFilterOrderBy("id", false))
            .withLimit(1))).getRecordEntities(WebhookEventSendLog.class);
         Instant lastEventTimestamp = Instant.now();
         if(!eventLogs.isEmpty() && eventLogs.get(0).getStartTimestamp() != null)
         {
            lastEventTimestamp = eventLogs.get(0).getStartTimestamp();
         }

         if(lastEventTimestamp.isBefore(limitTime))
         {
            willPutOnProbationLine.incrementCountAndAddPrimaryKey(webhook.getId());
            runBackendStepOutput.addRecord(new QRecord()
               .withValue("id", webhook.getId())
               .withValue("name", webhook.getName()) // just for preview screen
               .withValue("healthStatusId", WebhookHealthStatus.PROBATION.getId()));
         }
         else
         {
            notUnhealthyLongEnoughLine.incrementCountAndAddPrimaryKey(webhook.getId());
         }
      }
   }

}
