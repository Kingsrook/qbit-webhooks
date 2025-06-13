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
import java.util.Optional;
import com.kingsrook.qbits.webhooks.BaseTest;
import com.kingsrook.qbits.webhooks.WebhooksTestApplication;
import com.kingsrook.qbits.webhooks.model.WebhookEvent;
import com.kingsrook.qbits.webhooks.model.WebhookEventCategory;
import com.kingsrook.qbits.webhooks.registry.WebhookEventType;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QCollectingLogger;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for FirePostInsertWebhookEvent 
 *******************************************************************************/
class FirePostUpdateWebhookEventTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test() throws QException
   {
      try
      {
         Integer personId = new InsertAction().execute(new InsertInput(WebhooksTestApplication.TABLE_NAME_PERSON).withRecord(new QRecord())).getRecords().get(0).getValueInteger("id");

         QCollectingLogger collectingLogger = QLogger.activateCollectingLoggerForClass(FirePostInsertOrUpdateWebhookEventUtil.class);

         ////////////////////////////////////////////////
         // update without any webhook - assert no log //
         ////////////////////////////////////////////////
         new UpdateAction().execute(new UpdateInput(WebhooksTestApplication.TABLE_NAME_PERSON).withRecord(new QRecord().withValue("id", personId).withValue("firstName", "Bubba")));
         assertThat(collectingLogger.getCollectedMessages())
            .noneMatch(m -> m.getMessage().matches(".*Building webhook event.*"));

         ///////////////////////////////////////////////////////////////////
         // register event type and make a sub - then assert positive log //
         ///////////////////////////////////////////////////////////////////
         String eventTypeName = "updatedPerson";
         registerEventType(eventTypeName, WebhookEventCategory.UPDATE, WebhooksTestApplication.TABLE_NAME_PERSON);
         insert(newWebhookSubscription(eventTypeName));
         WebhookSubscriptionsHelper.clearMemoizations();

         new UpdateAction().execute(new UpdateInput(WebhooksTestApplication.TABLE_NAME_PERSON)
            .withRecord(new QRecord().withValue("id", personId).withValue("firstName", "Bobby")));
         assertThat(collectingLogger.getCollectedMessages())
            .anyMatch(m -> m.getMessage().matches(".*Building webhook event.*"));

         ///////////////////////////////////////////////
         // query to make sure event was inserted too //
         ///////////////////////////////////////////////
         List<QRecord> insertedEvents = new QueryAction().execute(new QueryInput(WebhookEvent.TABLE_NAME).withIncludeAssociations(true)).getRecords();
         assertEquals(1, insertedEvents.size());
         assertEquals(personId, insertedEvents.get(0).getValueInteger("eventSourceRecordId"));

         ////////////////////////////////////
         // assert about the contents too! //
         ////////////////////////////////////
         List<QRecord> contents = insertedEvents.get(0).getAssociatedRecords().get(WebhookEvent.CONTENT_ASSOCIATION_NAME);
         assertEquals(1, contents.size());
         String postBody = contents.get(0).getValueString("postBody");
         assertNotNull(postBody);
         JSONObject postBodyJson = new JSONObject(postBody);
         assertTrue(postBodyJson.has("record"));
         assertEquals("Bobby", postBodyJson.getJSONObject("record").getString("firstName"));
         assertEquals(personId, postBodyJson.getJSONObject("record").getInt("id"));
      }
      finally
      {
         QLogger.deactivateCollectingLoggerForClass(FirePostInsertOrUpdateWebhookEventUtil.class);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testDoesRecordMatchWebhookEventType()
   {
      WebhookEventType update              = new WebhookEventType().withTableName(WebhooksTestApplication.TABLE_NAME_PERSON).withCategory(WebhookEventCategory.UPDATE);
      WebhookEventType updateFirstName     = new WebhookEventType().withTableName(WebhooksTestApplication.TABLE_NAME_PERSON).withCategory(WebhookEventCategory.UPDATE_WITH_FIELD).withFieldName("firstName");
      WebhookEventType updateFirstNameJohn = new WebhookEventType().withTableName(WebhooksTestApplication.TABLE_NAME_PERSON).withCategory(WebhookEventCategory.UPDATE_WITH_VALUE).withFieldName("firstName").withValue("John");

      ////////////////////////////////////////////////
      // w/ no values, only the generic rule passes //
      ////////////////////////////////////////////////
      QRecord blankRecord = new QRecord();
      assertTrue(doesMatch(blankRecord, blankRecord, update));
      assertFalse(doesMatch(blankRecord, blankRecord, updateFirstName));
      assertFalse(doesMatch(blankRecord, blankRecord, updateFirstNameJohn));

      /////////////////////////////////////////////////////////////////////////
      // w/ firstname set to Joe, the generic and "any firstname" rules pass //
      /////////////////////////////////////////////////////////////////////////
      QRecord firstNameJoeRecord = new QRecord().withValue("firstName", "Joe");
      assertTrue(doesMatch(firstNameJoeRecord, blankRecord, update));
      assertTrue(doesMatch(firstNameJoeRecord, blankRecord, updateFirstName));
      assertFalse(doesMatch(firstNameJoeRecord, blankRecord, updateFirstNameJohn));

      /////////////////////////////////////////////////////////////////////////////////
      // an update that doesn't change firstName won't fire the "any firstname" rule //
      /////////////////////////////////////////////////////////////////////////////////
      QRecord firstNameJoeLastNameSmithRecord = new QRecord().withValue("firstName", "Joe").withValue("lastName", "Smith");
      QRecord firstNameJoeLastNameJonesRecord = new QRecord().withValue("firstName", "Joe").withValue("lastName", "Jones");
      assertTrue(doesMatch(firstNameJoeLastNameSmithRecord, firstNameJoeLastNameJonesRecord, update));
      assertFalse(doesMatch(firstNameJoeLastNameSmithRecord, firstNameJoeLastNameJonesRecord, updateFirstName));
      assertFalse(doesMatch(firstNameJoeLastNameSmithRecord, firstNameJoeLastNameJonesRecord, updateFirstNameJohn));

      ///////////////////////////////////////////////////////////////////////
      // an update that only changes last name only fires the generic rule //
      ///////////////////////////////////////////////////////////////////////
      QRecord lastNameJonesRecord = new QRecord().withValue("lastName", "Jones");
      assertTrue(doesMatch(lastNameJonesRecord, blankRecord, update));
      assertFalse(doesMatch(lastNameJonesRecord, blankRecord, updateFirstNameJohn));
      assertFalse(doesMatch(lastNameJonesRecord, blankRecord, updateFirstNameJohn));

      ////////////////////////////////////////////////////////////////
      // changing first name to the target value passes all 3 rules //
      ////////////////////////////////////////////////////////////////
      QRecord firstNameJohnRecord = new QRecord().withValue("firstName", "John");
      assertTrue(doesMatch(firstNameJohnRecord, blankRecord, update));
      assertTrue(doesMatch(firstNameJohnRecord, blankRecord, updateFirstName));
      assertTrue(doesMatch(firstNameJohnRecord, blankRecord, updateFirstNameJohn));

      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // an update that doesn't change firstName (even if it has the target value) won't fire the "firstname John" rule //
      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      QRecord firstNameJohnRecordLastNameSmithRecord = new QRecord().withValue("firstName", "John").withValue("lastName", "Smith");
      QRecord firstNameJohnRecordLastNameJonesRecord = new QRecord().withValue("firstName", "John").withValue("lastName", "Jones");
      assertTrue(doesMatch(firstNameJohnRecordLastNameSmithRecord, firstNameJohnRecordLastNameJonesRecord, update));
      assertFalse(doesMatch(firstNameJohnRecordLastNameSmithRecord, firstNameJohnRecordLastNameJonesRecord, updateFirstName));
      assertFalse(doesMatch(firstNameJohnRecordLastNameSmithRecord, firstNameJohnRecordLastNameJonesRecord, updateFirstNameJohn));

      //////////////////////////////////////////////////////////////
      // without old-record, always fail the value-checking rules //
      //////////////////////////////////////////////////////////////
      assertTrue(doesMatch(firstNameJohnRecord, null, update));
      assertFalse(doesMatch(firstNameJohnRecord, null, updateFirstName));
      assertFalse(doesMatch(firstNameJohnRecord, null, updateFirstNameJohn));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private boolean doesMatch(QRecord record, QRecord oldRecord, WebhookEventType webhookEventType)
   {
      FirePostUpdateWebhookEvent firePostUpdateWebhookEvent = new FirePostUpdateWebhookEvent();
      QFieldMetaData field = QContext.getQInstance().getTable(webhookEventType.getTableName()).getFields().get(webhookEventType.getFieldName());
      return firePostUpdateWebhookEvent.doesRecordMatchWebhookEventType(record, Optional.ofNullable(oldRecord), webhookEventType, field);
   }
}