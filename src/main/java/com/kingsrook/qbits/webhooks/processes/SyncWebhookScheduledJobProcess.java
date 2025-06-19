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


import com.kingsrook.qbits.webhooks.WebhooksQBitConfig;
import com.kingsrook.qbits.webhooks.model.Webhook;
import com.kingsrook.qbits.webhooks.model.WebhookActiveStatus;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.scheduledjobs.ScheduledJob;
import com.kingsrook.qqq.backend.core.scheduler.processes.AbstractRecordSyncToScheduledJobProcess;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 **
 *******************************************************************************/
public class SyncWebhookScheduledJobProcess extends AbstractRecordSyncToScheduledJobProcess
{

   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected ScheduledJob customizeScheduledJob(ScheduledJob scheduledJob, QRecord sourceRecord) throws QException
   {
      scheduledJob.setRepeatSeconds(WebhooksQBitConfig.getConfigValue(config -> config.getSendWebhookEventProcessRepeatSeconds()));

      boolean isActive = (WebhookActiveStatus.ACTIVE.getId().equals(sourceRecord.getValueInteger("activeStatusId")));
      scheduledJob.setIsActive(isActive);

      if(!StringUtils.hasContent(scheduledJob.getSchedulerName()))
      {
         scheduledJob.setSchedulerName(WebhooksQBitConfig.getConfigValue(config -> config.getSchedulerName()));
      }

      return (scheduledJob);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected String getScheduledJobForeignKeyType()
   {
      return (Webhook.TABLE_NAME);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected String getRecordForeignKeyFieldName()
   {
      return ("id");
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected String getRecordForeignKeyPossibleValueSourceName()
   {
      return (Webhook.TABLE_NAME);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected String getSourceTableName()
   {
      return (Webhook.TABLE_NAME);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected String getProcessNameScheduledJobParameter()
   {
      return (SendWebhookEventProcessMetaDataProducer.NAME);
   }
}
