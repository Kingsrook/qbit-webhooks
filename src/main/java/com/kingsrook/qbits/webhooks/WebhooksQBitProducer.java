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

package com.kingsrook.qbits.webhooks;


import java.util.List;
import com.kingsrook.qbits.webhooks.actions.FirePostInsertWebhookEvent;
import com.kingsrook.qbits.webhooks.actions.FirePostUpdateWebhookEvent;
import com.kingsrook.qbits.webhooks.model.Webhook;
import com.kingsrook.qbits.webhooks.model.WebhookEvent;
import com.kingsrook.qbits.webhooks.model.WebhookEventContent;
import com.kingsrook.qbits.webhooks.model.WebhookEventSendLog;
import com.kingsrook.qbits.webhooks.model.WebhookSubscription;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerMultiOutput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppSection;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.QBitMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.QBitMetaDataProducer;


/*******************************************************************************
 ** meta-data producer for the webhooks qbit
 *******************************************************************************/
public class WebhooksQBitProducer implements QBitMetaDataProducer<WebhooksQBitConfig>
{
   private WebhooksQBitConfig qBitConfig;



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public QBitMetaData getQBitMetaData()
   {
      QBitMetaData qBitMetaData = new QBitMetaData()
         .withGroupId("com.kingsrook.qbits")
         .withArtifactId("webhooks")
         .withVersion("0.1.4")
         .withNamespace(getNamespace())
         .withConfig(getQBitConfig());
      return qBitMetaData;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static QAppSection produceAppSection()
   {
      return new QAppSection()
         .withName("webhooks")
         .withTables(List.of(
            Webhook.TABLE_NAME,
            WebhookSubscription.TABLE_NAME,
            WebhookEvent.TABLE_NAME,
            WebhookEventContent.TABLE_NAME,
            WebhookEventSendLog.TABLE_NAME
         ));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void postProduceActions(MetaDataProducerMultiOutput metaDataProducerMultiOutput, QInstance qinstance)
   {
      registerTableActionsInInstance(qinstance);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public void registerTableActionsInInstance(QInstance qInstance)
   {
      qInstance.withTableCustomizer(TableCustomizers.POST_INSERT_RECORD, new QCodeReference(FirePostInsertWebhookEvent.class));
      qInstance.withTableCustomizer(TableCustomizers.POST_UPDATE_RECORD, new QCodeReference(FirePostUpdateWebhookEvent.class));
   }



   /*******************************************************************************
    ** Setter for qBitConfig
    *******************************************************************************/
   public void setQBitConfig(WebhooksQBitConfig qBitConfig)
   {
      this.qBitConfig = qBitConfig;
   }



   /*******************************************************************************
    ** Fluent setter for qBitConfig
    *******************************************************************************/
   public WebhooksQBitProducer withQBitConfig(WebhooksQBitConfig qBitConfig)
   {
      this.qBitConfig = qBitConfig;
      return (this);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public WebhooksQBitConfig getQBitConfig()
   {
      return (qBitConfig);
   }

}
