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


import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import com.kingsrook.qbits.webhooks.model.Webhook;
import com.kingsrook.qbits.webhooks.model.WebhookEvent;
import com.kingsrook.qbits.webhooks.model.WebhookEventContent;
import com.kingsrook.qbits.webhooks.model.WebhookEventSendLog;
import com.kingsrook.qbits.webhooks.model.WebhookEventStatus;
import com.kingsrook.qbits.webhooks.model.WebhookHealthStatus;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.audits.AuditAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** class that send webhook events to a webhook
 *******************************************************************************/
public class WebhookEventSender
{
   private static final QLogger LOG = QLogger.getLogger(WebhookEventSender.class);

   private static final Integer MAX_ALLOWED_ATTEMPTS = 5;

   /////////////////////////////////////////////////////////////////////
   // given 5 allowed attempts, we only need 4 backoff-minute entries //
   // but the last one in the array here is what'll be used for the   //
   // failed-while-on-probation use-case, which is a longer delay.    //
   /////////////////////////////////////////////////////////////////////
   private static final long[] BACKOFF_MINUTES = { 1, 5, 15, 60, 4 * 60 };



   /***************************************************************************
    **
    ***************************************************************************/
   public boolean handleEvent(WebhookEvent webhookEvent, Webhook webhook, List<WebhookEventSendLog> sendLogs) throws QException
   {
      /////////////////////
      // mark as sending //
      /////////////////////
      Instant nextAttemptTimestamp = Instant.now().plus(10 * 60, ChronoUnit.SECONDS);
      updateWebhookEvent(webhookEvent.getId(), WebhookEventStatus.SENDING, nextAttemptTimestamp, null);

      /////////////////
      // try to send //
      /////////////////
      WebhookEventSendLog sendLog = post(webhookEvent, webhook);
      sendLog.setAttemptNo(sendLogs.size() + 1);

      InsertInput         insertSendLogInput = new InsertInput(WebhookEventSendLog.TABLE_NAME).withRecordEntity(sendLog);
      QBackendTransaction transaction        = QBackendTransaction.openFor(insertSendLogInput);
      insertSendLogInput.setTransaction(transaction);

      if(sendLog.getSuccessful())
      {
         ///////////////////////
         // mark as delivered //
         ///////////////////////
         new InsertAction().execute(insertSendLogInput);
         updateWebhookEvent(webhookEvent.getId(), WebhookEventStatus.DELIVERED, null, transaction);
         transaction.commit();
         return (true);
      }
      else
      {
         //////////////////////////////////////////////////////////////////////////////////////////
         // consider if this event is now failed, or if it is retryable (based on # of failures) //
         //////////////////////////////////////////////////////////////////////////////////////////
         WebhookEventStatus eventStatus = sendLog.getAttemptNo() >= MAX_ALLOWED_ATTEMPTS ? WebhookEventStatus.FAILED : WebhookEventStatus.AWAITING_RETRY;
         if(eventStatus.equals(WebhookEventStatus.FAILED) && WebhookHealthStatus.PROBATION.getId().equals(webhook.getHealthStatusId()))
         {
            String message = "This Webhook Event has had too many failures, but since its Webhook's health status is Probation, its status will be Awaiting Retry instead of Failure.";
            AuditAction.execute(WebhookEvent.TABLE_NAME, webhookEvent.getId(), null, message);
            eventStatus = WebhookEventStatus.AWAITING_RETRY;
         }

         ///////////////////////////////////////////////////////////////////////
         // set the next-send time based on how many attempts there have been //
         ///////////////////////////////////////////////////////////////////////
         nextAttemptTimestamp = calculateNextAttemptBackoff(sendLog.getAttemptNo());
         if(eventStatus.equals(WebhookEventStatus.FAILED))
         {
            nextAttemptTimestamp = null;
         }

         new InsertAction().execute(insertSendLogInput);
         updateWebhookEvent(webhookEvent.getId(), eventStatus, nextAttemptTimestamp, transaction);
         transaction.commit();
         return (false);
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static Instant calculateNextAttemptBackoff(int attemptNumber)
   {
      int index = Math.min(attemptNumber - 1, BACKOFF_MINUTES.length - 1);
      return Instant.now().plusSeconds(BACKOFF_MINUTES[index] * 60);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public WebhookEventSendLog post(WebhookEvent webhookEvent, Webhook webhook)
   {
      WebhookEventSendLog sendLog = new WebhookEventSendLog();
      sendLog.setWebhookEventId(webhookEvent.getId());
      sendLog.setWebhookId(webhook.getId());
      sendLog.setStartTimestamp(Instant.now());

      try
      {
         doPost(webhookEvent, webhook, sendLog);

         //////////////////////////////////////////////////
         // interpret no exception as meaning successful //
         //////////////////////////////////////////////////
         sendLog.setSuccessful(true);
      }
      catch(WebhookPostException e)
      {
         ///////////////////////////////////////////////////////////////////////////
         // any exception thrown means not successful - capture its error message //
         ///////////////////////////////////////////////////////////////////////////
         sendLog.setSuccessful(false);
         sendLog.setErrorMessage(e.getMessage());
      }

      sendLog.setEndTimestamp(Instant.now());
      return (sendLog);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   protected void doPost(WebhookEvent webhookEvent, Webhook webhook, WebhookEventSendLog sendLog) throws WebhookPostException
   {
      try(CloseableHttpClient httpClient = buildHttpClient())
      {
         HttpPost request = new HttpPost(webhook.getUrl());

         String postBody = getPostBody(webhookEvent);
         request.setEntity(new StringEntity(postBody, StandardCharsets.UTF_8));

         try(CloseableHttpResponse response = executeHttpRequest(httpClient, request))
         {
            int statusCode = response.getStatusLine().getStatusCode();
            sendLog.setHttpStatusCode(statusCode);

            if(statusCode < 200 || statusCode >= 300)
            {
               String responseString = EntityUtils.toString(response.getEntity());
               if(!StringUtils.hasContent(responseString))
               {
                  LOG.warn("Unsuccessful http status code, but no response body returned", logPair("statusCode", statusCode), logPair("webhookId", webhook.getId()));
                  responseString = "No response body returned";
               }
               throw new WebhookPostException(responseString);
            }
         }
         catch(WebhookPostException wpe)
         {
            throw (wpe);
         }
         catch(Exception e)
         {
            LOG.warn("Exception executing http request", e, logPair("webhookId", webhook.getId()));
            throw (new WebhookPostException(e.getMessage(), e));
         }
      }
      catch(WebhookPostException wpe)
      {
         throw (wpe);
      }
      catch(Exception e)
      {
         LOG.warn("Exception building http client", e, logPair("webhookId", webhook.getId()));
         throw (new WebhookPostException(e.getMessage(), e));
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   protected String getPostBody(WebhookEvent webhookEvent) throws QException
   {
      String postBody = null;
      if(CollectionUtils.nullSafeIsEmpty(webhookEvent.getContent()))
      {
         List<QRecord> contentRecords = QueryAction.execute(WebhookEventContent.TABLE_NAME, new QQueryFilter(new QFilterCriteria("webhookEventId", QCriteriaOperator.EQUALS, webhookEvent.getId())));
         if(CollectionUtils.nullSafeHasContents(contentRecords))
         {
            postBody = contentRecords.get(0).getValueString("postBody");
         }
      }
      else
      {
         postBody = webhookEvent.getContent().get(0).getPostBody();
      }

      if(!StringUtils.hasContent(postBody))
      {
         throw (new QException("Missing content/postBody for webhook event: " + webhookEvent.getId()));
      }

      return postBody;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   protected CloseableHttpResponse executeHttpRequest(CloseableHttpClient httpClient, HttpPost request) throws IOException
   {
      return httpClient.execute(request);
   }



   /*******************************************************************************
    ** Build the default HttpClient used by the makeRequest method
    *******************************************************************************/
   protected CloseableHttpClient buildHttpClient()
   {
      ///////////////////////////////////////////////////////////////////////////////////////
      // do we want this?? .setConnectionManager(new PoolingHttpClientConnectionManager()) //
      // needs some good scrutiny.                                                         //
      ///////////////////////////////////////////////////////////////////////////////////////
      return HttpClientBuilder.create()
         .setDefaultRequestConfig(RequestConfig.custom()
            .setConnectTimeout(5000)
            .setConnectionRequestTimeout(5000)
            .setSocketTimeout(5000).build())
         .build();
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private void updateWebhookEvent(Integer id, WebhookEventStatus status, Instant nextAttemptTimestamp, QBackendTransaction transaction) throws QException
   {
      new UpdateAction().execute(new UpdateInput(WebhookEvent.TABLE_NAME)
         .withTransaction(transaction)
         .withRecord(new QRecord()
            .withValue("id", id)
            .withValue("nextAttemptTimestamp", nextAttemptTimestamp)
            .withValue("eventStatusId", status.getId())));
   }

}
