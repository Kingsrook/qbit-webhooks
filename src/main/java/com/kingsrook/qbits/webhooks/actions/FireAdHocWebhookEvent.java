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


import java.util.List;
import com.kingsrook.qbits.webhooks.model.WebhookEventCategory;
import com.kingsrook.qbits.webhooks.registry.WebhookEventType;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 * fire an ad-hoc webhook event - e.g., one not tied to a basic table insert/
 * update/delete
 *******************************************************************************/
public class FireAdHocWebhookEvent
{
   private static final QLogger LOG = QLogger.getLogger(FireAdHocWebhookEvent.class);



   /***************************************************************************
    * fire the event - creating webhookEvent records for any non-disabled
    * subscriptions that match it.
    *
    * @param eventTypeName must be a registered event type name in the instance.
    * @param tableName table that the key-record is from, which will be used as
    *                  part of the subscription-matching
    * @param keyRecord record that the event is related to - again may be used
    *                  in subscription-matching (e.g., based on security fields
    * @param transaction in case an update to the key record was made on a
    *                    transaction, the events will be created in the same one.
    ***************************************************************************/
   public void fire(String eventTypeName, String tableName, QRecord keyRecord, QBackendTransaction transaction) throws QException
   {
      List<WebhookEventType> webhookEventTypes = WebhookSubscriptionsHelper.getWebhookEventTypesToConsiderFiringEventsFor(WebhookEventCategory.Kind.AD_HOC, tableName);
      if(CollectionUtils.nullSafeIsEmpty(webhookEventTypes))
      {
         return;
      }

      WebhookEventBuilder webhookEventBuilder = null;
      for(WebhookEventType webhookEventType : webhookEventTypes)
      {
         //////////////////////////////
         // filter by the event type //
         //////////////////////////////
         if(!webhookEventType.getName().equals(eventTypeName))
         {
            continue;
         }

         if(keyRecord.getTableName() == null)
         {
            keyRecord.setTableName(tableName);
         }

         webhookEventBuilder = FirePostInsertOrUpdateWebhookEventUtil.processSubscriptions(tableName, webhookEventType, keyRecord, webhookEventBuilder, transaction);
      }

      if(webhookEventBuilder != null)
      {
         webhookEventBuilder.storeWebhookEvents(transaction);
      }
   }

}
