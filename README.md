## QBit:  webhooks

### Overview
*Note:  This is one of the original QBit implementations - so, some of the mechanics of how
it is loaded and used by an application are not exactly fully defined at the time of its
creation... Please excuse any dust or not-quite-round wheels you find here!*

This QBit provides a basic implementation of webhooks to a QQQ Application.

### Usage

#### Pom dependency
```xml
<dependency>
    <groupId>com.kingsrook.qbits</groupId>
    <artifactId>qbit-webhooks</artifactId>
    <version>${TODO}</version>
</dependency>
```

#### Setup
##### Define the QBit
```java
WebhooksQBitProducer producer = new WebhooksQBitProducer()
   .withQBitConfig(new WebhooksQBitConfig());
MetaDataProducerMultiOutput allQBitMetaData = producer.produce(qInstance);
// add it to your QInstance.
```

##### Define webhook event types
```java
WebhooksRegistry webhooksRegistry = WebhooksRegistry.ofOrWithNew(qInstance);

webhooksRegistry.registerWebhookEventType(new WebhookEventType()
   .withName(ORDER_CREATED_EVENT_TYPE)
   .withLabel("Order Created")
   .withCategory(WebhookEventCategory.INSERT)
   .withTableName(Order.TABLE_NAME));
```

### Provides
#### Tables
- `webhook` - a connection to a partner system, e.g., URL to post events to.
- `webhookSubscription` - which event types should be sent to a webhook.
- `webhookEvent` - table-as-queue plus log of events for sending to a subscription.
- `webhookEventContent` - storage of JSON bodies to post.
- `webhookEventSendLog` - tracking of attempted sends.

#### Classes
- `WebhooksRegistry` - where application-defined event types must be registered.

### Dependencies
- `qqq-backend-module-api` - as the objects posted are api-versioned
- `QQQTablesMetaDataProvider` - for event record foreign keys

