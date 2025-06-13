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


import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import com.kingsrook.qbits.webhooks.model.WebhookEventCategory;
import com.kingsrook.qbits.webhooks.registry.WebhookEventType;
import com.kingsrook.qqq.backend.core.actions.customizers.OldRecordHelper;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** post-update table customizer to fire webhook events
 *******************************************************************************/
public class FirePostUpdateWebhookEvent implements TableCustomizerInterface
{
   private static final QLogger LOG = QLogger.getLogger(FirePostUpdateWebhookEvent.class);



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public List<QRecord> postUpdate(UpdateInput updateInput, List<QRecord> records, Optional<List<QRecord>> oldRecordList) throws QException
   {
      List<WebhookEventType> webhookEventTypes = WebhookSubscriptionsHelper.getWebhookEventTypesToConsiderFiringEventsFor(WebhookEventCategory.Kind.UPDATE, updateInput.getTableName());
      if(CollectionUtils.nullSafeIsEmpty(webhookEventTypes))
      {
         return (records);
      }

      OldRecordHelper oldRecordHelper = new OldRecordHelper(updateInput.getTableName(), oldRecordList);

      WebhookEventBuilder webhookEventBuilder = null;
      for(WebhookEventType webhookEventType : webhookEventTypes)
      {
         QFieldMetaData field = getQFieldMetaData(updateInput, webhookEventType);
         for(QRecord record : records)
         {
            if(CollectionUtils.nullSafeHasContents(record.getErrors()))
            {
               continue;
            }

            if(record.getTableName() == null)
            {
               record.setTableName(updateInput.getTableName());
            }

            Optional<QRecord> oldRecord = oldRecordHelper.getOldRecord(record);
            if(doesRecordMatchWebhookEventType(record, oldRecord, webhookEventType, field))
            {
               webhookEventBuilder = FirePostInsertOrUpdateWebhookEventUtil.processSubscriptions(updateInput.getTableName(), webhookEventType, record, webhookEventBuilder, updateInput.getTransaction());
            }
         }
      }

      if(webhookEventBuilder != null)
      {
         webhookEventBuilder.storeWebhookEvents(updateInput.getTransaction());
      }

      return records;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static QFieldMetaData getQFieldMetaData(UpdateInput updateInput, WebhookEventType webhookEventType)
   {
      QFieldMetaData field = null;
      if(StringUtils.hasContent(webhookEventType.getFieldName()))
      {
         field = QContext.getQInstance().getTable(webhookEventType.getTableName()).getFields().get(webhookEventType.getFieldName());
         if(field == null)
         {
            LOG.warn("No field found for webhook event type", logPair("webhookEventType", webhookEventType), logPair("tableName", updateInput.getTableName()), logPair("fieldName", webhookEventType.getFieldName()));
         }
      }
      return field;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   boolean doesRecordMatchWebhookEventType(QRecord record, Optional<QRecord> optionalOldRecord, WebhookEventType webhookEventType, QFieldMetaData field)
   {
      switch(webhookEventType.getCategory())
      {
         case UPDATE ->
         {
            return (true);
         }
         case UPDATE_WITH_FIELD ->
         {
            if(optionalOldRecord.isEmpty())
            {
               return (false);
            }

            Serializable newValue = getValue(record, field);
            Serializable oldValue = getValue(optionalOldRecord.get(), field);
            if(!Objects.equals(newValue, oldValue))
            {
               return (true);
            }
         }
         case UPDATE_WITH_VALUE ->
         {
            if(optionalOldRecord.isEmpty())
            {
               return (false);
            }

            Serializable newValue   = getValue(record, field);
            Serializable oldValue   = getValue(optionalOldRecord.get(), field);
            Serializable eventValue = ValueUtils.getValueAsFieldType(field == null ? QFieldType.STRING : field.getType(), webhookEventType.getValue());
            if(!Objects.equals(newValue, oldValue) && Objects.equals(newValue, eventValue))
            {
               return (true);
            }
         }
         default ->
         {
            return (false);
         }
      }

      return (false);
   }



   /***************************************************************************
    *
    ***************************************************************************/
   private Serializable getValue(QRecord record, QFieldMetaData field)
   {
      if(field == null)
      {
         return (null);
      }

      return ValueUtils.getValueAsFieldType(field.getType(), record.getValue(field.getName()));
   }

}
