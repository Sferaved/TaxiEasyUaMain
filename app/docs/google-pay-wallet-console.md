# Google Pay у WebView — реєстрація PAS у Google Pay & Wallet Console

## Навіщо

Починаючи з Android WebView 137+ Google Pay у WebView працює через **Payment Request API**.
У коді PAS уже додано:

- `androidx.webkit:webkit`
- `WebSettingsCompat.setPaymentRequestEnabled(true)` у `WfpWebViewHelper`
- `<queries>` для `org.chromium.intent.action.PAY` у `AndroidManifest.xml`

Без схвалення додатка в Console на частині пристроїв може з’являтися **OR_BIBED_11**.

## Дані для реєстрації (release keystore)

| Додаток | Package name | SHA-256 (release) |
|---------|--------------|-------------------|
| PAS_1 | `com.taxi.easy.ua` | `5D:DC:CC:ED:78:F5:1E:BB:C9:23:0F:5B:F4:CC:B5:8C:1D:51:0C:AC:9E:F3:52:93:5C:5B:FA:64:B2:B1:C6:B9` |
| PAS_2 | `com.taxieasyua.back4app` | `DE:0D:C2:3A:45:7B:1C:17:47:9E:9F:C5:D0:15:13:64:6D:AC:34:59:B0:B6:9B:D5:75:3A:96:89:ED:FE:C6:28` |
| PAS_3 | `com.taxi_pas3` | `46:7B:8B:88:4D:51:3D:05:CE:0C:FE:37:8C:DD:C9:40:F0:AA:BD:11:99:64:54:8D:79:09:B8:63:2A:D3:43:79` |
| PAS_4 | `com.taxi_pas_4` | `6C:C3:56:E7:21:88:7C:1D:8D:FE:43:CD:32:F8:A7:64:3A:74:D2:F1:92:2F:F3:97:66:81:C2:B7:38:60:5A:19` |

### Отримати SHA-256 вручну

```bat
keytool -list -v -keystore app\keystore\keystore.jks -alias key0
```

Скопіювати рядок **SHA256:** (формат з двокрапками).

Для PAS_3 пароль у `gradle.properties` або змінні CI: `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.

## Кроки в Google Pay & Wallet Console

1. Відкрити https://pay.google.com/business/console
2. Увійти під Google-акаунтом бізнесу (той самий, що для merchant WayForPay, якщо є доступ).
3. Для **кожного** PAS окремо:
   - **Google Pay API** → **Android** (або *Publish your integration* → Android app)
   - Додати **Package name** з таблиці вище
   - Додати **SHA-256 certificate fingerprint** (release)
   - Integration type: додаток як **контейнер WebView** для стороннього checkout (WayForPay)
4. Заповнити чекліст інтеграції (скріншоти, опис: таксі-додаток, оплата через WayForPay у WebView).
5. Надіслати на **Production access** / схвалення.
6. Дочекатися статусу **Approved** (може зайняти кілька днів).

## Документація Google

- https://developers.google.com/pay/api/android/guides/recipes/using-android-webview
- https://developer.chrome.com/docs/android/payments-in-webviews

## Перевірка після схвалення

1. Встановити release-збірку PAS на пристрій з WebView 137+ і Play Services 25.18.30+.
2. Прив’язка карти WFP (1 грн) → WayForPay у WebView → Google Pay.
3. Успіх: `WaitingAuthComplete` / `Approved` на сервері, карта в списку.
4. Якщо **OR_BIBED_11** — додаток ще не схвалено в Console.
5. Якщо **OR_PMCR_58** — відмова банку/G Pay; спробувати карту напряму на WayForPay.

## Важливо

- Реєструється **Android-додаток (PAS)**, не сайт WayForPay.
- Merchant WayForPay (`play_google_com_f183e` тощо) налаштовується в кабінеті WayForPay окремо.
- Custom Tabs / зовнішній Chrome для WFP **не використовуємо** — перевірено, гірший результат за WebView.
