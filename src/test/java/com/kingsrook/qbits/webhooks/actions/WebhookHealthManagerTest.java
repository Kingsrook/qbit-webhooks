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
import com.kingsrook.qbits.webhooks.BaseTest;
import com.kingsrook.qbits.webhooks.model.Webhook;
import com.kingsrook.qbits.webhooks.model.WebhookEventSendLog;
import com.kingsrook.qbits.webhooks.model.WebhookHealthStatus;
import com.kingsrook.qbits.webhooks.processes.SendWebhookEventLoadStep;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for WebhookHealthManager 
 *******************************************************************************/
class WebhookHealthManagerTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test() throws QException
   {
      Webhook webhook = newWebhook("test");
      webhook.setId(insert(webhook));

      assertHealthStatus(WebhookHealthStatus.HEALTHY, webhook, makeStack(1));
      assertHealthStatus(WebhookHealthStatus.HEALTHY, webhook, makeStack(1, 1));
      assertHealthStatus(WebhookHealthStatus.HEALTHY, webhook, makeStack(0, 1, 1));
      assertHealthStatus(WebhookHealthStatus.HEALTHY, webhook, makeStack(0, 0, 0, 0, 0, 0, 0, 0, 0, 1));

      Stack<WebhookEventSendLog> tenFails = makeStack(0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
      assertHealthStatus(WebhookHealthStatus.UNHEALTHY, webhook, tenFails);

      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // make sure that pushing another event onto a stack that was full of fails, that it then lets a success go out of probation //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      tenFails.push(new WebhookEventSendLog().withSuccessful(true));
      webhook.setHealthStatusId(WebhookHealthStatus.PROBATION.getId());
      assertHealthStatus(WebhookHealthStatus.HEALTHY, webhook, tenFails);

      webhook.setHealthStatusId(WebhookHealthStatus.PROBATION.getId());
      assertHealthStatus(WebhookHealthStatus.HEALTHY, webhook, makeStack(0, 1, 1, 1));

      ///////////////////////////////////////////////////////////////////
      // start webhook HEALTHy, and stack with 5 success, then 8 fails //
      ///////////////////////////////////////////////////////////////////
      webhook.setHealthStatusId(WebhookHealthStatus.HEALTHY.getId());
      Stack<WebhookEventSendLog> stack = makeStack(1);
      for(int i = 0; i < 4; i++)
      {
         stack.push(new WebhookEventSendLog().withSuccessful(true));
      }
      for(int i = 0; i < 8; i++)
      {
         stack.push(new WebhookEventSendLog().withSuccessful(false));
      }

      //////////////////
      // stay healthy //
      //////////////////
      assertHealthStatus(WebhookHealthStatus.HEALTHY, webhook, stack);

      /////////////////////////////////////////
      // another fail (now 9), stays healthy //
      /////////////////////////////////////////
      stack.push(new WebhookEventSendLog().withSuccessful(false));
      assertHealthStatus(WebhookHealthStatus.HEALTHY, webhook, stack);

      ///////////////////////////////////////////
      // another fail (now 10), goes unhealthy //
      ///////////////////////////////////////////
      stack.push(new WebhookEventSendLog().withSuccessful(false));
      assertHealthStatus(WebhookHealthStatus.UNHEALTHY, webhook, stack);

      /////////////////////////////////////////////////////////////////////
      // now if we got to probation, then a success, and we'd go healthy //
      /////////////////////////////////////////////////////////////////////
      webhook.setHealthStatusId(WebhookHealthStatus.PROBATION.getId());
      stack.push(new WebhookEventSendLog().withSuccessful(true));
      assertHealthStatus(WebhookHealthStatus.HEALTHY, webhook, stack);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private void assertHealthStatus(WebhookHealthStatus expected, Webhook webhook, Stack<WebhookEventSendLog> recentSendLogs) throws QException
   {
      Webhook updatedWebhook = new WebhookHealthManager().updateWebhookHealth(recentSendLogs, webhook);
      assertEquals(expected.getId(), updatedWebhook.getHealthStatusId());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static Stack<WebhookEventSendLog> makeStack(int... values)
   {
      Stack<WebhookEventSendLog> rs = new SendWebhookEventLoadStep().newSendLogStack(10);
      for(int value : values)
      {
         rs.add(new WebhookEventSendLog().withId(rs.size() + 1).withSuccessful(value == 1));
      }
      return (rs);
   }

}