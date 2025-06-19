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
import com.kingsrook.qbits.webhooks.BaseTest;
import com.kingsrook.qbits.webhooks.WebhooksTestApplication;
import com.kingsrook.qbits.webhooks.model.WebhookEvent;
import com.kingsrook.qbits.webhooks.model.WebhookEventCategory;
import com.kingsrook.qbits.webhooks.registry.WebhookEventType;
import com.kingsrook.qbits.webhooks.registry.WebhooksRegistry;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QCollectingLogger;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for FireAdHocWebhookEvent 
 *******************************************************************************/
class FireAdHocWebhookEventTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test() throws QException
   {
      QCollectingLogger collectingLogger = QLogger.activateCollectingLoggerForClass(FirePostInsertOrUpdateWebhookEventUtil.class);

      WebhooksRegistry webhooksRegistry = WebhooksRegistry.of(QContext.getQInstance());

      String typeA = "typeA";
      webhooksRegistry.registerWebhookEventType(new WebhookEventType()
         .withName(typeA)
         .withCategory(WebhookEventCategory.AD_HOC));

      String typeB = "typeB";
      webhooksRegistry.registerWebhookEventType(new WebhookEventType()
         .withName(typeB)
         .withCategory(WebhookEventCategory.AD_HOC));

      ////////////////////////////////////////////////
      // fire without any webhook - assert no log //
      ////////////////////////////////////////////////
      FireAdHocWebhookEvent fireAdHocWebhookEvent = new FireAdHocWebhookEvent();
      fireAdHocWebhookEvent.fire(typeA, WebhooksTestApplication.TABLE_NAME_PERSON, new QRecord().withValue("id", 1).withValue("firstName", "James T."), null);
      assertThat(collectingLogger.getCollectedMessages())
         .noneMatch(m -> m.getMessage().matches(".*Building webhook event.*"));

      /////////////////////////////////////////////////////
      // make a sub - then fire, and assert positive log //
      /////////////////////////////////////////////////////
      insert(newWebhookSubscription(typeA));
      WebhookSubscriptionsHelper.clearMemoizations();

      fireAdHocWebhookEvent.fire(typeA, WebhooksTestApplication.TABLE_NAME_PERSON, new QRecord().withValue("id", 2).withValue("firstName", "Jean Luc"), null);
      assertThat(collectingLogger.getCollectedMessages())
         .anyMatch(m -> m.getMessage().matches(".*Building webhook event.*"));
      collectingLogger.clear();

      ///////////////////////////////////////////////
      // query to make sure event was inserted too //
      ///////////////////////////////////////////////
      List<QRecord> insertedEvents = new QueryAction().execute(new QueryInput(WebhookEvent.TABLE_NAME).withIncludeAssociations(true)).getRecords();
      assertEquals(1, insertedEvents.size());
      assertEquals(2, insertedEvents.get(0).getValueInteger("eventSourceRecordId"));

      ////////////////////////////////////
      // assert about the contents too! //
      ////////////////////////////////////
      List<QRecord> contents = insertedEvents.get(0).getAssociatedRecords().get(WebhookEvent.CONTENT_ASSOCIATION_NAME);
      assertEquals(1, contents.size());
      String postBody = contents.get(0).getValueString("postBody");
      assertNotNull(postBody);
      JSONObject postBodyJson = new JSONObject(postBody);
      assertTrue(postBodyJson.has("record"));
      assertEquals("Jean Luc", postBodyJson.getJSONObject("record").getString("firstName"));
      assertEquals(2, postBodyJson.getJSONObject("record").getInt("id"));

      /////////////////////////////////////////////////////////////////
      // fire for the other ad-hoc with no subs - and should NOT log //
      /////////////////////////////////////////////////////////////////
      fireAdHocWebhookEvent.fire(typeB, WebhooksTestApplication.TABLE_NAME_PERSON, new QRecord().withValue("id", 1).withValue("firstName", "James T."), null);
      assertThat(collectingLogger.getCollectedMessages())
         .noneMatch(m -> m.getMessage().matches(".*Building webhook event.*"));
   }

}