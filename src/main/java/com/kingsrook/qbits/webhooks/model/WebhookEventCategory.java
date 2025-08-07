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
 ** WebhookEventCategory - possible value enum
 *******************************************************************************/
@QMetaDataProducingPossibleValueEnum()
public enum WebhookEventCategory implements PossibleValueEnum<String>
{
   INSERT("insert", "Record Inserted", Kind.INSERT, true, false, false),
   INSERT_WITH_FIELD("insert-with-field", "Record Inserted with Field", Kind.INSERT, true, true, false),
   INSERT_WITH_VALUE("insert-with-value", "Record Inserted with Specific Value", Kind.INSERT, true, true, true),

   UPDATE("update", "Record Updated", Kind.UPDATE, true, false, false),
   UPDATE_WITH_FIELD("update-with-field", "Record Updated with Field", Kind.UPDATE, true, true, false),
   UPDATE_WITH_VALUE("update-with-value", "Record Updated with Specific Value", Kind.UPDATE, true, true, true),

   STORE("store", "Record Stored", Kind.STORE, true, false, false),
   STORE_WITH_FIELD("store-with-field", "Record Stored with Field", Kind.STORE, true, true, false),
   STORE_WITH_VALUE("store-with-value", "Record Stored with Specific Value", Kind.STORE, true, true, true),

   AD_HOC("ad-hoc", "Ad Hoc", Kind.AD_HOC, false, false, false);

   private final String id;
   private final String label;
   private final Kind kind;
   private final boolean requiresTable;
   private final boolean requiresField;
   private final boolean requiresValue;

   public static final String NAME = "WebhookEventCategory";



   /***************************************************************************
    **
    ***************************************************************************/
   public enum Kind
   {
      INSERT,
      UPDATE,
      STORE,
      AD_HOC
   }


   /*******************************************************************************
    **
    *******************************************************************************/
   WebhookEventCategory(String id, String label, Kind kind, boolean requiresTable, boolean requiresField, boolean requiresValue)
   {
      this.id = id;
      this.label = label;
      this.kind = kind;
      this.requiresTable = requiresTable;
      this.requiresField = requiresField;
      this.requiresValue = requiresValue;
   }



   /*******************************************************************************
    ** Get instance by id
    **
    *******************************************************************************/
   public static WebhookEventCategory getById(String id)
   {
      if(id == null)
      {
         return (null);
      }

      for(WebhookEventCategory value : WebhookEventCategory.values())
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
   public String getId()
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
   public String getPossibleValueId()
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



   /*******************************************************************************
    ** Getter for requiresTable
    **
    *******************************************************************************/
   public boolean getRequiresTable()
   {
      return requiresTable;
   }



   /*******************************************************************************
    ** Getter for requiresField
    **
    *******************************************************************************/
   public boolean getRequiresField()
   {
      return requiresField;
   }



   /*******************************************************************************
    ** Getter for requiresValue
    **
    *******************************************************************************/
   public boolean getRequiresValue()
   {
      return requiresValue;
   }



   /*******************************************************************************
    ** Getter for kind
    **
    *******************************************************************************/
   public Kind getKind()
   {
      return kind;
   }
}
