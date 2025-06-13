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


import java.util.ArrayDeque;
import java.util.Queue;
import com.kingsrook.qbits.webhooks.BaseTest;
import com.kingsrook.qbits.webhooks.model.Webhook;
import com.kingsrook.qbits.webhooks.model.WebhookHealthStatus;
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

      assertHealthStatus(WebhookHealthStatus.HEALTHY, webhook, q(1));
      assertHealthStatus(WebhookHealthStatus.HEALTHY, webhook, q(1, 1));
      assertHealthStatus(WebhookHealthStatus.HEALTHY, webhook, q(0, 1, 1));
      assertHealthStatus(WebhookHealthStatus.HEALTHY, webhook, q(0, 0, 0, 0, 0, 0, 0, 0, 0, 1));
      assertHealthStatus(WebhookHealthStatus.UNHEALTHY, webhook, q(0, 0, 0, 0, 0, 0, 0, 0, 0, 0));

      webhook.setHealthStatusId(WebhookHealthStatus.PROBATION.getId());
      assertHealthStatus(WebhookHealthStatus.UNHEALTHY, webhook, q(0, 1, 1, 1));

      webhook.setHealthStatusId(WebhookHealthStatus.PROBATION.getId());
      assertHealthStatus(WebhookHealthStatus.HEALTHY, webhook, q(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private void assertHealthStatus(WebhookHealthStatus expected, Webhook webhook, Queue<Boolean> recentSuccesses) throws QException
   {
      Webhook updatedWebhook = new WebhookHealthManager().updateWebhookHealth(recentSuccesses, webhook);
      assertEquals(expected.getId(), updatedWebhook.getHealthStatusId());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static Queue<Boolean> q(int... values)
   {
      Queue<Boolean> rs = new ArrayDeque<>(values.length);
      for(int value : values)
      {
         rs.add(value == 1);
      }
      return (rs);
   }

}