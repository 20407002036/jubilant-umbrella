# Financial Assistant App - Android Architecture (MVP)

This is an offline-first Android application that automatically intercepts incoming financial SMS notifications, parses structural details, updates a secure local database, and provides users with a premium, slate-dark dashboard for searching, sorting, and analyzing transactions.

---

## Technical Stack
* **Language:** Kotlin
* **Architecture:** MVVM
* **Local Database:** Room Database
* **Dependency Injection:** Hilt
* **Background Tasks:** WorkManager
* **UI:** Jetpack Compose with modern glassmorphism
* **Secure Storage:** Android Keystore + EncryptedSharedPreferences
* **Logging:** Timber
* **Testing:** JUnit

---

## Directory & File Structure

Here is a breakdown of the structural components implemented in the repository:

* **Build Configurations:**
  * [settings.gradle.kts](file:///home/kyo/Developer/jubilant-umbrella/settings.gradle.kts)
  * [build.gradle.kts](file:///home/kyo/Developer/jubilant-umbrella/build.gradle.kts)
  * [gradle.properties](file:///home/kyo/Developer/jubilant-umbrella/gradle.properties)
  * [gradle-wrapper.properties](file:///home/kyo/Developer/jubilant-umbrella/gradle/wrapper/gradle-wrapper.properties)
  * [app/build.gradle.kts](file:///home/kyo/Developer/jubilant-umbrella/app/build.gradle.kts)
  * [AndroidManifest.xml](file:///home/kyo/Developer/jubilant-umbrella/app/src/main/AndroidManifest.xml)

* **App Initialization & DI:**
  * [App.kt](file:///home/kyo/Developer/jubilant-umbrella/app/src/main/java/com/financialassistant/App.kt) (Hilt application)
  * [DatabaseModule.kt](file:///home/kyo/Developer/jubilant-umbrella/app/src/main/java/com/financialassistant/di/DatabaseModule.kt)
  * [RepositoryModule.kt](file:///home/kyo/Developer/jubilant-umbrella/app/src/main/java/com/financialassistant/di/RepositoryModule.kt)

* **Security:**
  * [EncryptionManager.kt](file:///home/kyo/Developer/jubilant-umbrella/app/src/main/java/com/financialassistant/security/EncryptionManager.kt) (Keystore configuration)

* **Room Database & Schema:**
  * [AppDatabase.kt](file:///home/kyo/Developer/jubilant-umbrella/app/src/main/java/com/financialassistant/data/database/AppDatabase.kt) (Prepopulates standard categories on creation)
  * Entities: [TransactionEntity.kt](file:///home/kyo/Developer/jubilant-umbrella/app/src/main/java/com/financialassistant/data/entities/TransactionEntity.kt), [CategoryEntity.kt](file:///home/kyo/Developer/jubilant-umbrella/app/src/main/java/com/financialassistant/data/entities/CategoryEntity.kt), [RecipientEntity.kt](file:///home/kyo/Developer/jubilant-umbrella/app/src/main/java/com/financialassistant/data/entities/RecipientEntity.kt)
  * DAOs: [TransactionDao.kt](file:///home/kyo/Developer/jubilant-umbrella/app/src/main/java/com/financialassistant/data/dao/TransactionDao.kt), [CategoryDao.kt](file:///home/kyo/Developer/jubilant-umbrella/app/src/main/java/com/financialassistant/data/dao/CategoryDao.kt), [RecipientDao.kt](file:///home/kyo/Developer/jubilant-umbrella/app/src/main/java/com/financialassistant/data/dao/RecipientDao.kt)
  * Repository: [TransactionRepository.kt](file:///home/kyo/Developer/jubilant-umbrella/app/src/main/java/com/financialassistant/data/repository/TransactionRepository.kt), [TransactionRepositoryImpl.kt](file:///home/kyo/Developer/jubilant-umbrella/app/src/main/java/com/financialassistant/data/repository/TransactionRepositoryImpl.kt)

* **SMS Pipelines (Broadcast & Parsing):**
  * Domain: [Transaction.kt](file:///home/kyo/Developer/jubilant-umbrella/app/src/main/java/com/financialassistant/sms/models/Transaction.kt), [TransactionType.kt](file:///home/kyo/Developer/jubilant-umbrella/app/src/main/java/com/financialassistant/sms/models/TransactionType.kt)
  * Filter: [SenderFilter.kt](file:///home/kyo/Developer/jubilant-umbrella/app/src/main/java/com/financialassistant/sms/filters/SenderFilter.kt)
  * Parsers: [SmsParser.kt](file:///home/kyo/Developer/jubilant-umbrella/app/src/main/java/com/financialassistant/sms/parsers/SmsParser.kt), [MpesaParser.kt](file:///home/kyo/Developer/jubilant-umbrella/app/src/main/java/com/financialassistant/sms/parsers/MpesaParser.kt), [KcbParser.kt](file:///home/kyo/Developer/jubilant-umbrella/app/src/main/java/com/financialassistant/sms/parsers/KcbParser.kt), [EquityParser.kt](file:///home/kyo/Developer/jubilant-umbrella/app/src/main/java/com/financialassistant/sms/parsers/EquityParser.kt), [CoopParser.kt](file:///home/kyo/Developer/jubilant-umbrella/app/src/main/java/com/financialassistant/sms/parsers/CoopParser.kt), [AbsaParser.kt](file:///home/kyo/Developer/jubilant-umbrella/app/src/main/java/com/financialassistant/sms/parsers/AbsaParser.kt), [ParserFactory.kt](file:///home/kyo/Developer/jubilant-umbrella/app/src/main/java/com/financialassistant/sms/parsers/ParserFactory.kt)
  * Receiver: [SmsReceiver.kt](file:///home/kyo/Developer/jubilant-umbrella/app/src/main/java/com/financialassistant/sms/receivers/SmsReceiver.kt)

* **Background Task Worker:**
  * [SmsImportWorker.kt](file:///home/kyo/Developer/jubilant-umbrella/app/src/main/java/com/financialassistant/workers/SmsImportWorker.kt) (Scans historical inbox content providers)

* **Compose UI & Theme:**
  * Theme: [Theme.kt](file:///home/kyo/Developer/jubilant-umbrella/app/src/main/java/com/financialassistant/ui/theme/Theme.kt), [Color.kt](file:///home/kyo/Developer/jubilant-umbrella/app/src/main/java/com/financialassistant/ui/theme/Color.kt), [Type.kt](file:///home/kyo/Developer/jubilant-umbrella/app/src/main/java/com/financialassistant/ui/theme/Type.kt)
  * Navigation: [Navigation.kt](file:///home/kyo/Developer/jubilant-umbrella/app/src/main/java/com/financialassistant/ui/navigation/Navigation.kt), [MainActivity.kt](file:///home/kyo/Developer/jubilant-umbrella/app/src/main/java/com/financialassistant/ui/MainActivity.kt)
  * Views: [HomeScreen.kt](file:///home/kyo/Developer/jubilant-umbrella/app/src/main/java/com/financialassistant/ui/home/HomeScreen.kt), [TransactionsScreen.kt](file:///home/kyo/Developer/jubilant-umbrella/app/src/main/java/com/financialassistant/ui/transactions/TransactionsScreen.kt), [DetailsScreen.kt](file:///home/kyo/Developer/jubilant-umbrella/app/src/main/java/com/financialassistant/ui/details/DetailsScreen.kt), [SettingsScreen.kt](file:///home/kyo/Developer/jubilant-umbrella/app/src/main/java/com/financialassistant/ui/settings/SettingsScreen.kt)

* **Unit Testing:**
  * [SmsParserTest.kt](file:///home/kyo/Developer/jubilant-umbrella/app/src/test/java/com/financialassistant/SmsParserTest.kt) (Validates regex logic for bank and mobile money messages)

---

## Getting Started in Android Studio

1. **Clone/Open the project:**
   Open Android Studio, select **Open**, and navigate to this workspace folder.
2. **Build Sync:**
   Android Studio will automatically detect the Gradle files (`settings.gradle.kts`, `build.gradle.kts`) and run a project sync.
3. **Run Unit Tests:**
   Open the [SmsParserTest.kt](file:///home/kyo/Developer/jubilant-umbrella/app/src/test/java/com/financialassistant/SmsParserTest.kt) file and click the green play icon to run parser validation tests.
4. **Deploying on a Device/Emulator:**
   * Run the app.
   * Go to **Settings** in the app.
   * Click **Grant SMS Permissions** to allow the app to listen for incoming messages.
   * Click **Scan Inbox Now** to import all past financial SMS history from M-PESA, KCB, Equity, ABSA, and Co-op Bank.
