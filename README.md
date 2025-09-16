# QBit: Webhooks

[![Build Status](https://circleci.com/gh/Kingsrook/qbit-webhooks.svg?style=svg)](https://circleci.com/gh/Kingsrook/qbit-webhooks)
[![Version](https://img.shields.io/badge/version-0.1.0-blue.svg)](https://github.com/Kingsrook/qbit-webhooks)
[![License](https://img.shields.io/badge/license-GNU%20Affero%20GPL%20v3-green.svg)](https://www.gnu.org/licenses/agpl-3.0.en.html)
[![Java](https://img.shields.io/badge/java-17+-blue.svg)](https://adoptium.net/)

> **Webhook Implementation for QQQ Applications - Event-Driven Integration**

This QBit provides a comprehensive webhook system for QQQ applications, enabling real-time event notifications to external systems and partners.

## 🚀 What Makes QBit Webhooks Different?

**QBit Webhooks gives you enterprise-grade webhook capabilities** - you define your event types, configure webhook endpoints, and QBit Webhooks handles the reliable delivery, retry logic, and comprehensive logging. No more building webhook infrastructure from scratch.

### ✨ Core Capabilities

- **🔗 Webhook Management**: Complete webhook endpoint and subscription management
- **📡 Event Types**: Flexible event type registration and categorization
- **🔄 Reliable Delivery**: Built-in retry logic and delivery tracking
- **📊 Comprehensive Logging**: Full audit trail of webhook events and delivery attempts
- **🎯 Event Filtering**: Subscribe to specific event types and categories
- **📦 JSON Payloads**: Structured event content with API versioning support

## 🔓 Open Source & Full Control

**QBit Webhooks is 100% open source** - you have complete ownership and control:

- **💻 Your Code**: Full access to QBit Webhooks source code
- **🗄️ Your Data**: All webhook data stays in your systems
- **🏗️ Your System**: Deploy anywhere - on-premises, cloud, or hybrid
- **🔒 No Vendor Lock-in**: No external webhook services required
- **⚡ Full Customization**: Modify and extend webhook behavior to your needs

### Overview
*Note:  This is one of the original QBit implementations - so, some of the mechanics of how
it is loaded and used by an application are not exactly fully defined at the time of its
creation... Please excuse any dust or not-quite-round wheels you find here!*

This QBit provides a basic implementation of webhooks to a QQQ Application.

## 🏗️ Architecture

### Technology Stack

- **Java**: Java 17+ with UTF-8 encoding
- **QQQ Framework**: Built on QQQ backend modules
- **Database**: RDBMS support through QQQ tables
- **JSON**: Structured event payloads with API versioning

### What This QBit Contains

- **Webhook Management**: Complete webhook endpoint and subscription system
- **Event Processing**: Event type registration and categorization
- **Delivery System**: Reliable webhook delivery with retry logic
- **Audit Trail**: Comprehensive logging of all webhook activities
- **Database Schema**: Five core tables for webhook management

### What This QBit Does NOT Contain

- **HTTP Client**: Uses QQQ's built-in HTTP capabilities
- **Authentication**: Relies on QQQ's security framework
- **Scheduling**: Uses QQQ's job scheduling system
- **External Services**: No third-party webhook services

## 🚀 Getting Started

### Prerequisites

- **Java 17+** (required for QQQ features)
- **Maven 3.8+** (for build system)
- **QQQ Application** (this is a QBit, not a standalone application)

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

## 📚 Documentation

**📖 [Complete Documentation Wiki](https://github.com/Kingsrook/qqq/wiki)** - Start here for comprehensive guides

- **[🏠 Home](https://github.com/Kingsrook/qqq/wiki/Home)** - Project overview and quick start
- **[🏗️ Architecture](https://github.com/Kingsrook/qqq/wiki/High-Level-Architecture)** - System design and principles
- **[🔧 Development](https://github.com/Kingsrook/qqq/wiki/Developer-Onboarding)** - Setup and contribution guide
- **[📦 Modules](https://github.com/Kingsrook/qqq/wiki/Core-Modules)** - Available components and usage
- **[🚀 Building](https://github.com/Kingsrook/qqq/wiki/Building-Locally)** - Local development workflow
- **[🔌 QBits](https://github.com/Kingsrook/qqq/wiki/QBit-Development)** - QBit development guide

## 🤝 Contributing

QBit Webhooks is **open source** and welcomes contributions! 

- **🐛 [Report Issues](https://github.com/Kingsrook/qqq/issues)** - Bug reports and feature requests
- **📝 [Contribution Guide](https://github.com/Kingsrook/qqq/wiki/Contribution-Guidelines)** - How to contribute code and documentation
- **🔍 [Code Standards](https://github.com/Kingsrook/qqq/wiki/Code-Review-Standards)** - QQQ's coding standards and review process

**First time contributing?** Start with our [Developer Onboarding Guide](https://github.com/Kingsrook/qqq/wiki/Developer-Onboarding) to get your environment set up.

## 🏢 About Kingsrook

QBit Webhooks is built by **[Kingsrook](https://qrun.io)** - making engineers more productive through intelligent automation and developer tools.

- **Website**: [https://qrun.io](https://qrun.io)
- **Contact**: [contact@kingsrook.com](mailto:contact@kingsrook.com)
- **GitHub**: [https://github.com/Kingsrook](https://github.com/Kingsrook)

## 📄 License

This project is licensed under the **GNU Affero General Public License v3.0** - see the [LICENSE.txt](LICENSE.txt) file for details.

## 🆘 Support & Community

### ⚠️ Important: Use Main QQQ Repository

**All support, issues, discussions, and community interactions should go through the main QQQ repository:**

- **Main Repository**: https://github.com/Kingsrook/qqq
- **Issues**: https://github.com/Kingsrook/qqq/issues
- **Discussions**: https://github.com/Kingsrook/qqq/discussions
- **Wiki**: https://github.com/Kingsrook/qqq/wiki

### Why This Repository Exists

This repository is maintained separately from the main QQQ repository to:
- **Enable independent QBit development** and versioning
- **Allow QBit-specific CI/CD** and deployment pipelines
- **Provide clear separation** between QBit components and core framework concerns
- **Support different release cycles** for QBits vs. core framework

### Getting Help:

- **Documentation**: Check the [QQQ Wiki](https://github.com/Kingsrook/qqq/wiki)
- **Issues**: Report bugs and feature requests on [Main QQQ Issues](https://github.com/Kingsrook/qqq/issues)
- **Discussions**: Join community discussions on [Main QQQ Discussions](https://github.com/Kingsrook/qqq/discussions)
- **Questions**: Ask questions in the main QQQ repository

---

**Ready to integrate webhooks into your QQQ application?** [Get started with QBit Webhooks today!](https://github.com/Kingsrook/qqq/wiki/QBit-Development)

