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
import com.kingsrook.qbits.webhooks.model.WebhookEventCategory;
import com.kingsrook.qbits.webhooks.model.WebhookEventCategory.Kind;
import com.kingsrook.qbits.webhooks.registry.WebhookEventType;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Unit test for IdentifyWebhookSubscriptions 
 *******************************************************************************/
class WebhookSubscriptionsHelperTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test() throws QException
   {
      String personTableName                      = "person";
      String placeTableName                       = "place";
      String insertedPersonEventName              = "inserted-person";
      String insertedPlaceEventName               = "inserted-place";
      String insertedPersonWithFirstNameEventName = "inserted-person-firstName";

      //////////////////////////////////////////////
      // nothing exists yet, so all null or empty //
      //////////////////////////////////////////////
      assertThat(getEventTypes(Kind.INSERT, "noSuchTable")).isNullOrEmpty();
      assertThat(getEventTypes(Kind.INSERT, "noSuchTable")).isNullOrEmpty();
      assertThat(getEventTypes(Kind.UPDATE, personTableName)).isNullOrEmpty();
      assertThat(getEventTypes(Kind.AD_HOC, personTableName)).isNullOrEmpty();

      ///////////////////////////////////////////////////////////////////////////////////////
      // register first event type, but no subscriptions to it, so still all empty results //
      ///////////////////////////////////////////////////////////////////////////////////////
      registerEventType(insertedPersonEventName, WebhookEventCategory.INSERT, personTableName);
      assertThat(getEventTypes(Kind.INSERT, personTableName)).isNullOrEmpty();
      assertThat(getEventTypes(Kind.UPDATE, personTableName)).isNullOrEmpty();
      assertThat(getEventTypes(Kind.AD_HOC, personTableName)).isNullOrEmpty();

      //////////////////////////////////////////////////////////////
      // add a second insert event with a sub on the person table //
      //////////////////////////////////////////////////////////////
      registerEventType(insertedPersonWithFirstNameEventName, WebhookEventCategory.INSERT, personTableName, "firstName");
      insert(newWebhookSubscription(insertedPersonWithFirstNameEventName));
      WebhookSubscriptionsHelper.clearMemoizations();
      assertThat(getEventTypes(Kind.INSERT, personTableName)).hasSize(1);
      assertThat(getEventTypes(Kind.UPDATE, personTableName)).isNullOrEmpty();
      assertThat(getEventTypes(Kind.AD_HOC, personTableName)).isNullOrEmpty();

      ////////////////////////////////////////
      // add insert event for another table //
      ////////////////////////////////////////
      registerEventType(insertedPlaceEventName, WebhookEventCategory.INSERT, placeTableName);
      insert(newWebhookSubscription(insertedPlaceEventName));
      WebhookSubscriptionsHelper.clearMemoizations();
      assertThat(getEventTypes(Kind.INSERT, personTableName)).hasSize(1);
      assertThat(getEventTypes(Kind.INSERT, placeTableName)).hasSize(1);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private List<WebhookEventType> getEventTypes(Kind kind, String tableName)
   {
      return WebhookSubscriptionsHelper.getWebhookEventTypesToConsiderFiringEventsFor(kind, tableName);
   }

}