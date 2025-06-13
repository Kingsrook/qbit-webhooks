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
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.kingsrook.qbits.webhooks.WebhooksQBitConfig;
import com.kingsrook.qbits.webhooks.model.Webhook;
import com.kingsrook.qbits.webhooks.model.WebhookActiveStatus;
import com.kingsrook.qbits.webhooks.model.WebhookEventCategory;
import com.kingsrook.qbits.webhooks.model.WebhookSubscription;
import com.kingsrook.qbits.webhooks.registry.WebhookEventType;
import com.kingsrook.qbits.webhooks.registry.WebhooksRegistry;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.ValidateRecordSecurityLockHelper;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.QBitConfig;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.memoization.Memoization;


/*******************************************************************************
 ** helper class for working with webhook subscriptions
 *******************************************************************************/
public class WebhookSubscriptionsHelper
{
   private static final Memoization<String, List<WebhookSubscription>> subscriptionsByEventTypeMemoization = new Memoization<String, List<WebhookSubscription>>()
      .withTimeout(Duration.of(5, ChronoUnit.MINUTES));

   private static final Memoization<Integer, QRecord> webhookBySubscriptionIdMemoization = new Memoization<Integer, QRecord>()
      .withTimeout(Duration.of(5, ChronoUnit.MINUTES));

   private static List<String> allAccessSecurityKeysToUse = null;



   /***************************************************************************
    **
    ***************************************************************************/
   public static List<WebhookEventType> getWebhookEventTypesToConsiderFiringEventsFor(WebhookEventCategory.Kind kind, String tableName)
   {
      List<WebhookEventType> eventTypes = null;
      for(WebhookEventType webhookEventType : WebhooksRegistry.ofOrWithNew(QContext.getQInstance()).getAllWebhookEventTypes())
      {
         if(webhookEventType.getCategory().getKind().equals(kind) && Objects.equals(webhookEventType.getTableName(), tableName))
         {
            if(CollectionUtils.nullSafeHasContents(getSubscriptionsForEventType(webhookEventType)))
            {
               eventTypes = Objects.requireNonNullElseGet(eventTypes, ArrayList::new);
               eventTypes.add(webhookEventType);
            }
         }
      }

      return (eventTypes);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static List<WebhookSubscription> getSubscriptionsForEventType(WebhookEventType webhookEventType)
   {
      return subscriptionsByEventTypeMemoization.getResultThrowing(webhookEventType.getName(), name ->
      {
         try
         {
            return QueryAction.execute(WebhookSubscription.TABLE_NAME, WebhookSubscription.class, new QQueryFilter()
               .withCriteria(new QFilterCriteria("webhookEventTypeName", QCriteriaOperator.EQUALS, name))
               .withCriteria(new QFilterCriteria("activeStatusId", QCriteriaOperator.NOT_EQUALS, WebhookActiveStatus.DISABLED.getId()))
            );
         }
         catch(QException e)
         {
            throw new RuntimeException(e);
         }
      }).orElse(Collections.emptyList());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static void clearMemoizations()
   {
      subscriptionsByEventTypeMemoization.clear();
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static QRecord getWebhookForSubscription(WebhookSubscription webhookSubscription)
   {
      return webhookBySubscriptionIdMemoization.getResultThrowing(webhookSubscription.getId(), subscriptionId ->
      {
         try
         {
            List<QRecord> webhooks = new QueryAction().execute(new QueryInput(Webhook.TABLE_NAME)
                  .withFilter(new QQueryFilter(new QFilterCriteria(WebhookSubscription.TABLE_NAME + ".id", QCriteriaOperator.EQUALS, subscriptionId))))
               .getRecords();
            if(webhooks.isEmpty())
            {
               return (null);
            }
            else
            {
               return (webhooks.get(0));
            }
         }
         catch(QException e)
         {
            throw new RuntimeException(e);
         }
      }).orElse(null);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static List<String> getAllAccessSecurityKeysToUse()
   {
      if(allAccessSecurityKeysToUse == null)
      {
         List<String> list = new ArrayList<>();

         Map<String, String> securityKeyTypeToAllAccessKeyNameMap = new HashMap<>();
         for(QSecurityKeyType securityKeyType : QContext.getQInstance().getSecurityKeyTypes().values())
         {
            if(StringUtils.hasContent(securityKeyType.getAllAccessKeyName()))
            {
               securityKeyTypeToAllAccessKeyNameMap.put(securityKeyType.getName(), securityKeyType.getAllAccessKeyName());
            }
         }

         QTableMetaData webhookTable = QContext.getQInstance().getTable(Webhook.TABLE_NAME);
         QBitConfig     qbitConfig   = webhookTable.getSourceQBitConfig();
         if(qbitConfig instanceof WebhooksQBitConfig webhooksQBitConfig)
         {
            for(RecordSecurityLock securityLock : CollectionUtils.nonNullList(webhooksQBitConfig.getRecordSecurityLocks()))
            {
               securityKeyTypeToAllAccessKeyNameMap.remove(securityLock.getSecurityKeyType());
            }

            list.addAll(securityKeyTypeToAllAccessKeyNameMap.values());
         }

         allAccessSecurityKeysToUse = list;
      }

      return (allAccessSecurityKeysToUse);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static boolean doesRecordMatchSubscription(WebhookEventType webhookEventType, WebhookSubscription webhookSubscription, QRecord record, QBackendTransaction transaction) throws QException
   {
      String         tableName = webhookEventType.getTableName();
      QTableMetaData table     = QContext.getQInstance().getTable(tableName);

      List<RecordSecurityLock> recordSecurityLocks = table.getRecordSecurityLocks();
      if(CollectionUtils.nullSafeIsEmpty(recordSecurityLocks))
      {
         ////////////////////////////////////////////
         // if no locks on the table, always allow //
         ////////////////////////////////////////////
         return (true);
      }

      QRecord        webhookRecord = getWebhookForSubscription(webhookSubscription);
      QTableMetaData webhookTable  = QContext.getQInstance().getTable(Webhook.TABLE_NAME);
      QBitConfig     qbitConfig    = webhookTable.getSourceQBitConfig();
      QSession       pseudoSession = new QSession();

      ////////////////////////////////////////////////////////////////////////////////////////////////////
      // ask the qbit config what security locks are in place - e.g., values to copy from the webhook   //
      // record into a pseudo-session, to use for checking if the webhook is allowed to see the record. //
      ////////////////////////////////////////////////////////////////////////////////////////////////////
      if(qbitConfig instanceof WebhooksQBitConfig webhooksQBitConfig)
      {
         for(RecordSecurityLock securityLock : CollectionUtils.nonNullList(webhooksQBitConfig.getRecordSecurityLocks()))
         {
            Serializable securityFieldValue = webhookRecord.getValue(securityLock.getFieldName());
            pseudoSession.withSecurityKeyValue(securityLock.getSecurityKeyType(), securityFieldValue);
         }
      }

      //////////////////////////////////////////////////////////////
      // if there are no security locks on the qbit, always allow //
      //////////////////////////////////////////////////////////////
      if(pseudoSession.getSecurityKeyValues().isEmpty())
      {
         return (true);
      }

      //////////////////////////////////////////////////////////////////////////////////////////////////
      // any keys in the instance that aren't used by the webhook qbit, if there's an all-access key, //
      // then use it - to avoid records being blocked by a non-applicable lock                        //
      //////////////////////////////////////////////////////////////////////////////////////////////////
      for(String allAccessKey : getAllAccessSecurityKeysToUse())
      {
         pseudoSession.withSecurityKeyValue(allAccessKey, true);
      }

      return (ValidateRecordSecurityLockHelper.allowedToReadRecord(table, record, pseudoSession, transaction));
   }
}
