# FinSplit — Android Expense Splitter

Split expenses with your partner. Real-time sync, FCM push notifications, weekly reports.

---

## Setup Steps

### a) Firebase Console

1. Go to [console.firebase.google.com](https://console.firebase.google.com) and create a project named **finsplit** (or any name).
2. Add an Android app with package name `com.finsplit.app`.
3. Download `google-services.json` and place it at `app/google-services.json` (replacing the placeholder).
4. In **Authentication** → Sign-in method, enable:
   - Google
   - Email/Password
5. Copy the **Web client ID** from Authentication → Sign-in method → Google → Web SDK configuration.
   Open `app/src/main/res/values/strings.xml` and replace `REPLACE_WITH_REAL_WEB_CLIENT_ID` with it.
6. In **Firestore Database** → Create database → Start in **test mode**.
7. In **Cloud Messaging**, FCM is enabled by default for all Firebase projects.

### b) Run on emulator or device

Requirements: API 24+ emulator or physical device.

```bash
# Debug APK
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"   # Windows / Git Bash
./gradlew assembleDebug
# Install via adb:
adb install app/build/outputs/apk/debug/app-debug.apk
```

Or run directly from Android Studio: **Run → Run 'app'**.

### c) Generate a signed release APK

**Step 1 — Create the keystore** (one-time):
```bash
keytool -genkey -v \
  -keystore app/keystore/finsplit.keystore \
  -alias finsplit \
  -keyalg RSA -keysize 2048 -validity 10000
```

**Step 2 — Set passwords** (use env vars to keep secrets out of source):
```bash
export KEYSTORE_PASSWORD="your_keystore_pass"
export KEY_ALIAS="finsplit"
export KEY_PASSWORD="your_key_pass"
```

**Step 3 — Build**:
```bash
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk
```

Or via Android Studio: **Build → Generate Signed Bundle / APK → APK → use keystore**.

---

## Architecture

| Layer | Package | Responsibility |
|-------|---------|----------------|
| Activities | `activities/` | UI, lifecycle, navigation |
| Fragments | `fragments/` | Bottom sheets |
| Adapters | `adapters/` | RecyclerView binding |
| Models | `models/` | Firestore POJOs |
| Repositories | `repositories/` | Firestore CRUD |
| Utils | `utils/` | Business logic (OOP) |
| Services | `services/` | FCM push notifications |
| Workers | `workers/` | WorkManager background jobs |

## OOP Patterns Demonstrated

| Pattern | Where |
|---------|-------|
| **Abstract class / Inheritance** | `BaseExpenseSplit` → `EqualSplit`, `PercentageSplit`, `ExactSplit` |
| **Interface** | `AuthCallback`, `ExpenseCallback` |
| **Polymorphism** | `BalanceCalculator.calculateNetBalances(…, BaseExpenseSplit)` |
| **Encapsulation** | All models: private fields + public getters/setters |
| **Singleton** | `UserRepository`, `ExpenseRepository` |
| **Strategy pattern** | Split-calculation strategy injected into `BalanceCalculator` |

## Firestore Schema

```
/users/{uid}
  displayName, email, profilePicUrl, defaultCurrency, fcmToken, createdAt
  /weeklyReports/{reportId}
    totalSpentPKR, topCategory, totalOwed, totalOwedToUser, weekRange, generatedAt

/groups/{groupId}
  groupName, members[], balances{uid: Double}
  /expenses/{expenseId}
    title, amountPKR, paidByUid, category, note, createdAt, groupId
```

## Notification Testing

Send a test push from Firebase Console → Cloud Messaging → Send test message.
Target the device registration token (printed to Logcat on first launch).
Tap the notification to open MainActivity.
