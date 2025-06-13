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
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.producers.MetaDataCustomizerInterface;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.QBitConfig;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;


/*******************************************************************************
 ** Configuration data for this qbit.
 **
 *******************************************************************************/
public class WebhooksQBitConfig implements QBitConfig
{
   private MetaDataCustomizerInterface<QTableMetaData> tableMetaDataCustomizer;

   private String defaultBackendNameForTables;
   private String schedulerName;

   private String defaultApiNameForNewSubscriptions;
   private String defaultApiVersionForNewSubscriptions;

   private List<QFieldMetaData>     securityFields;
   private List<RecordSecurityLock> recordSecurityLocks;



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void validate(QInstance qInstance, List<String> errors)
   {
      ///////////////////////////////////////////////////////////////////////////////////////////////////////////
      // we don't feel the need to validate fields or locks, as they'll be added to tables and validated there //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////////

      if(this.schedulerName != null)
      {
         assertCondition(qInstance.getSchedulers().containsKey(this.schedulerName), "Unrecognized schedulerName: " + schedulerName, errors);
      }
   }



   /*******************************************************************************
    ** Getter for tableMetaDataCustomizer
    *******************************************************************************/
   public MetaDataCustomizerInterface<QTableMetaData> getTableMetaDataCustomizer()
   {
      return (this.tableMetaDataCustomizer);
   }



   /*******************************************************************************
    ** Setter for tableMetaDataCustomizer
    *******************************************************************************/
   public void setTableMetaDataCustomizer(MetaDataCustomizerInterface<QTableMetaData> tableMetaDataCustomizer)
   {
      this.tableMetaDataCustomizer = tableMetaDataCustomizer;
   }



   /*******************************************************************************
    ** Fluent setter for tableMetaDataCustomizer
    *******************************************************************************/
   public WebhooksQBitConfig withTableMetaDataCustomizer(MetaDataCustomizerInterface<QTableMetaData> tableMetaDataCustomizer)
   {
      this.tableMetaDataCustomizer = tableMetaDataCustomizer;
      return (this);
   }



   /*******************************************************************************
    ** Getter for defaultBackendNameForTables
    *******************************************************************************/
   @Override
   public String getDefaultBackendNameForTables()
   {
      return (this.defaultBackendNameForTables);
   }



   /*******************************************************************************
    ** Setter for defaultBackendNameForTables
    *******************************************************************************/
   public void setDefaultBackendNameForTables(String defaultBackendNameForTables)
   {
      this.defaultBackendNameForTables = defaultBackendNameForTables;
   }



   /*******************************************************************************
    ** Fluent setter for defaultBackendNameForTables
    *******************************************************************************/
   public WebhooksQBitConfig withDefaultBackendNameForTables(String defaultBackendNameForTables)
   {
      this.defaultBackendNameForTables = defaultBackendNameForTables;
      return (this);
   }



   /*******************************************************************************
    ** Getter for schedulerName
    *******************************************************************************/
   public String getSchedulerName()
   {
      return (this.schedulerName);
   }



   /*******************************************************************************
    ** Setter for schedulerName
    *******************************************************************************/
   public void setSchedulerName(String schedulerName)
   {
      this.schedulerName = schedulerName;
   }



   /*******************************************************************************
    ** Fluent setter for schedulerName
    *******************************************************************************/
   public WebhooksQBitConfig withSchedulerName(String schedulerName)
   {
      this.schedulerName = schedulerName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for defaultApiNameForNewSubscriptions
    *******************************************************************************/
   public String getDefaultApiNameForNewSubscriptions()
   {
      return (this.defaultApiNameForNewSubscriptions);
   }



   /*******************************************************************************
    ** Setter for defaultApiNameForNewSubscriptions
    *******************************************************************************/
   public void setDefaultApiNameForNewSubscriptions(String defaultApiNameForNewSubscriptions)
   {
      this.defaultApiNameForNewSubscriptions = defaultApiNameForNewSubscriptions;
   }



   /*******************************************************************************
    ** Fluent setter for defaultApiNameForNewSubscriptions
    *******************************************************************************/
   public WebhooksQBitConfig withDefaultApiNameForNewSubscriptions(String defaultApiNameForNewSubscriptions)
   {
      this.defaultApiNameForNewSubscriptions = defaultApiNameForNewSubscriptions;
      return (this);
   }



   /*******************************************************************************
    ** Getter for defaultApiVersionForNewSubscriptions
    *******************************************************************************/
   public String getDefaultApiVersionForNewSubscriptions()
   {
      return (this.defaultApiVersionForNewSubscriptions);
   }



   /*******************************************************************************
    ** Setter for defaultApiVersionForNewSubscriptions
    *******************************************************************************/
   public void setDefaultApiVersionForNewSubscriptions(String defaultApiVersionForNewSubscriptions)
   {
      this.defaultApiVersionForNewSubscriptions = defaultApiVersionForNewSubscriptions;
   }



   /*******************************************************************************
    ** Fluent setter for defaultApiVersionForNewSubscriptions
    *******************************************************************************/
   public WebhooksQBitConfig withDefaultApiVersionForNewSubscriptions(String defaultApiVersionForNewSubscriptions)
   {
      this.defaultApiVersionForNewSubscriptions = defaultApiVersionForNewSubscriptions;
      return (this);
   }



   /*******************************************************************************
    ** Getter for securityFields
    *******************************************************************************/
   public List<QFieldMetaData> getSecurityFields()
   {
      return (this.securityFields);
   }



   /*******************************************************************************
    ** Setter for securityFields
    *******************************************************************************/
   public void setSecurityFields(List<QFieldMetaData> securityFields)
   {
      this.securityFields = securityFields;
   }



   /*******************************************************************************
    ** Fluent setter for securityFields
    *******************************************************************************/
   public WebhooksQBitConfig withSecurityFields(List<QFieldMetaData> securityFields)
   {
      this.securityFields = securityFields;
      return (this);
   }



   /*******************************************************************************
    ** Getter for recordSecurityLocks
    *******************************************************************************/
   public List<RecordSecurityLock> getRecordSecurityLocks()
   {
      return (this.recordSecurityLocks);
   }



   /*******************************************************************************
    ** Setter for recordSecurityLocks
    *******************************************************************************/
   public void setRecordSecurityLocks(List<RecordSecurityLock> recordSecurityLocks)
   {
      this.recordSecurityLocks = recordSecurityLocks;
   }



   /*******************************************************************************
    ** Fluent setter for recordSecurityLocks
    *******************************************************************************/
   public WebhooksQBitConfig withRecordSecurityLocks(List<RecordSecurityLock> recordSecurityLocks)
   {
      this.recordSecurityLocks = recordSecurityLocks;
      return (this);
   }

}
