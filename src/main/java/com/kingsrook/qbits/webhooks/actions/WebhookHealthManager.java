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

package com.kingsrook.qbits.webhooks.actions;


import java.util.Stack;
import com.kingsrook.qbits.webhooks.WebhooksQBitConfig;
import com.kingsrook.qbits.webhooks.model.Webhook;
import com.kingsrook.qbits.webhooks.model.WebhookEventSendLog;
import com.kingsrook.qbits.webhooks.model.WebhookHealthStatus;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.scheduler.processes.AbstractRecordSyncToScheduledJobProcess;
import org.apache.commons.lang3.BooleanUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** class that manages webhook health status
 *******************************************************************************/
public class WebhookHealthManager
{
   private static final QLogger LOG = QLogger.getLogger(WebhookHealthManager.class);



   /***************************************************************************
    **
    ***************************************************************************/
   public Webhook updateWebhookHealth(Stack<WebhookEventSendLog> recentSendLogs, Webhook webhook) throws QException
   {
      Integer repeatedFailsToGoUnhealthy = WebhooksQBitConfig.getConfigValue(config -> config.getRepeatedFailsToGoUnhealthy());
      boolean lastSuccessful             = !recentSendLogs.isEmpty() && BooleanUtils.isTrue(recentSendLogs.peek().getSuccessful());
      if(lastSuccessful)
      {
         //////////////////////////////////////////////////////////////////
         // if the last send was successful, and the webhook had been on //
         // probation, then take it off probation and mark it as healthy //
         //////////////////////////////////////////////////////////////////
         if(WebhookHealthStatus.PROBATION.getId().equals(webhook.getHealthStatusId()))
         {
            LOG.info("Marking previously-probation webhook as healthy due to a success", logPair("id", webhook.getId()));
            webhook = updateWebhookHealthStatus(webhook, WebhookHealthStatus.HEALTHY);
         }
      }
      else
      {
         /////////////////////////////////////////
         // else the last send was a failure... //
         /////////////////////////////////////////
         if(WebhookHealthStatus.PROBATION.getId().equals(webhook.getHealthStatusId()))
         {
            ////////////////////////////////////////////////////////////////
            // if the webhook was on probation, put it back on probation. //
            ////////////////////////////////////////////////////////////////
            LOG.info("Marking previously-probation webhook as unhealthy due to a failure", logPair("id", webhook.getId()));
            webhook = updateWebhookHealthStatus(webhook, WebhookHealthStatus.UNHEALTHY);
         }
         else if(WebhookHealthStatus.HEALTHY.getId().equals(webhook.getHealthStatusId()))
         {
            ///////////////////////////////////////////////////////////////////////////////////////////////////
            // if the webhook was healthy - mark it as unhealthy if there have been enough failures in-a-row //
            ///////////////////////////////////////////////////////////////////////////////////////////////////
            if(repeatedFailsToGoUnhealthy != null && recentSendLogs.size() >= repeatedFailsToGoUnhealthy
               && recentSendLogs.stream().noneMatch(sendLog -> BooleanUtils.isTrue(sendLog.getSuccessful())))
            {
               LOG.info("Marking previously-healthy webhook as unhealthy due to repeated failures", logPair("id", webhook.getId()), logPair("repeatedFailLimit", repeatedFailsToGoUnhealthy));
               webhook = updateWebhookHealthStatus(webhook, WebhookHealthStatus.UNHEALTHY);
            }
         }
      }

      return (webhook);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private Webhook updateWebhookHealthStatus(Webhook webhook, WebhookHealthStatus status) throws QException
   {
      new UpdateAction().execute(new UpdateInput(Webhook.TABLE_NAME)
         .withFlag(AbstractRecordSyncToScheduledJobProcess.ActionFlags.DO_NOT_SYNC)
         .withRecord(new QRecord()
            .withValue("id", webhook.getId())
            .withValue("healthStatusId", status.getId())));

      webhook.setHealthStatusId(status.getId());
      return (webhook);
   }

}
