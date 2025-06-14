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

package com.kingsrook.qbits.webhooks.registry;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qbits.webhooks.actions.WebhookEventTypeCustomizerInterface;
import com.kingsrook.qbits.webhooks.model.WebhookEventCategory;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.QSupplementalInstanceMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** class that manages webhook types (meta-data like)
 *******************************************************************************/
public class WebhooksRegistry implements QSupplementalInstanceMetaData
{
   private static final QLogger LOG = QLogger.getLogger(WebhooksRegistry.class);

   private static String NAME = "com.kingsrook.qbits.webhooks.registry.WebhooksRegistry";

   private final Map<String, WebhookEventType> eventTypes = new LinkedHashMap<>();



   /*******************************************************************************
    ** Singleton constructor
    *******************************************************************************/
   private WebhooksRegistry()
   {
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static WebhooksRegistry of(QInstance qInstance)
   {
      return QSupplementalInstanceMetaData.of(qInstance, NAME);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static WebhooksRegistry ofOrWithNew(QInstance qInstance)
   {
      return QSupplementalInstanceMetaData.ofOrWithNew(qInstance, NAME, WebhooksRegistry::new);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public void clear()
   {
      eventTypes.clear();
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public WebhookEventType getWebhookEventType(String name)
   {
      return (eventTypes.get(name));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public List<WebhookEventType> getAllWebhookEventTypes()
   {
      return (new ArrayList<>(eventTypes.values()));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public void registerWebhookEventType(WebhookEventType eventType)
   {
      if(eventTypes.containsKey(eventType.getName()))
      {
         LOG.info("Replacing existing webhook event type", logPair("name", eventType.getName()));
      }

      eventTypes.put(eventType.getName(), eventType);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void validate(QInstance qInstance, QInstanceValidator validator)
   {
      for(WebhookEventType eventType : CollectionUtils.nonNullList(WebhooksRegistry.ofOrWithNew(qInstance).getAllWebhookEventTypes()))
      {
         String prefix = "Webhook Event Type: " + eventType.getName() + " ";

         validator.assertCondition(StringUtils.hasContent(eventType.getName()), prefix + "is missing a name'");

         WebhookEventCategory category = eventType.getCategory();
         if(validator.assertCondition(category != null, prefix + "is missing a category"))
         {
            if(category.getRequiresTable())
            {
               validator.assertCondition(StringUtils.hasContent(eventType.getTableName()), prefix + "requires a tableName, but does not have one");
            }

            if(category.getRequiresField())
            {
               validator.assertCondition(StringUtils.hasContent(eventType.getFieldName()), prefix + "requires a fieldName, but does not have one");
            }

            if(category.getRequiresValue())
            {
               validator.assertCondition(eventType.getValue() != null, prefix + "requires a value, but does not have one");
            }
         }

         if(StringUtils.hasContent(eventType.getTableName()))
         {
            QTableMetaData table = qInstance.getTable(eventType.getTableName());
            validator.assertCondition(table != null, prefix + "references a non-existing tableName: " + eventType.getTableName());

            if(table != null && StringUtils.hasContent(eventType.getFieldName()))
            {
               validator.assertCondition(table.getFields().containsKey(eventType.getFieldName()), prefix + "references a non-existing fieldName: " + eventType.getFieldName() + " for table " + eventType.getTableName());
            }
         }

         if(eventType.getCustomizer() != null)
         {
            validator.validateSimpleCodeReference(prefix + "customizer codeReference: ", eventType.getCustomizer(), WebhookEventTypeCustomizerInterface.class);
         }
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void enrich(QInstance qInstance)
   {
      for(WebhookEventType webhookEventType : CollectionUtils.nonNullList(WebhooksRegistry.ofOrWithNew(qInstance).getAllWebhookEventTypes()))
      {
         if(!StringUtils.hasContent(webhookEventType.getLabel()))
         {
            webhookEventType.setLabel(QInstanceEnricher.nameToLabel(webhookEventType.getName()));
         }
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public String getName()
   {
      return NAME;
   }

}
