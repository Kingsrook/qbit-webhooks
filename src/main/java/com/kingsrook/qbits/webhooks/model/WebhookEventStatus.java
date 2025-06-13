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

package com.kingsrook.qbits.webhooks.model;


import java.util.Objects;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.PossibleValueEnum;
import com.kingsrook.qqq.backend.core.model.metadata.producers.annotations.QMetaDataProducingPossibleValueEnum;


/*******************************************************************************
 **
 *******************************************************************************/
@QMetaDataProducingPossibleValueEnum()
public enum WebhookEventStatus implements PossibleValueEnum<Integer>
{
   NEW(1, "New"),
   SENDING(2, "Sending"),
   DELIVERED(3, "Delivered"),
   AWAITING_RETRY(4, "Awaiting Retry"),
   FAILED(5, "Failed");

   private final Integer id;
   private final String  label;

   public static final String NAME = "WebhookEventStatus";

   public static final String DEFAULT_VALUE = "1";

   static
   {
      Objects.requireNonNull(getById(Integer.parseInt(DEFAULT_VALUE)), "Default value '" + DEFAULT_VALUE + " in WebhookEventStatus is not a defined enum value");
   }

   /*******************************************************************************
    **
    *******************************************************************************/
   WebhookEventStatus(Integer id, String label)
   {
      this.id = id;
      this.label = label;
   }



   /*******************************************************************************
    ** Get instance by id
    **
    *******************************************************************************/
   public static WebhookEventStatus getById(Integer id)
   {
      if(id == null)
      {
         return (null);
      }

      for(WebhookEventStatus value : WebhookEventStatus.values())
      {
         if(Objects.equals(value.id, id))
         {
            return (value);
         }
      }

      return (null);
   }



   /*******************************************************************************
    ** Getter for id
    **
    *******************************************************************************/
   public Integer getId()
   {
      return id;
   }



   /*******************************************************************************
    ** Getter for label
    **
    *******************************************************************************/
   public String getLabel()
   {
      return label;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Integer getPossibleValueId()
   {
      return (getId());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String getPossibleValueLabel()
   {
      return (getLabel());
   }
}
