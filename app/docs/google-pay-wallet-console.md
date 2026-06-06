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
| PAS_1 | `com.taxi.easy.ua` | `42:E6:CF:13:98:13:92:B1:88:BD:2A:F7:1F:25:0E:FF:DA:D1:C8:D8:7D:C8:43:C3:8B:41:F4:B5:58:5B:B4:31` (App signing, Play) |
| PAS_2 | `com.taxieasyua.back4app` | `DA:8C:BC:E9:5F:79:10:3C:6D:ED:D4:61:40:59:6F:C3:03:10:CB:1C:E2:48:E3:C5:80:CF:D6:8F:BE:41:E8:F6` (App signing, Play) |
| PAS_3 | `com.taxi_pas3` | `38:86:B1:5A:A6:59:AC:73:14:08:D4:8D:F6:10:83:C0:6C:78:DE:04:06:72:4A:36:F8:D8:3F:AA:71:76:25:1D` (App signing, Play) |
| PAS_4 | `com.taxi_pas_4` | `0B:FC:7D:49:C5:40:D9:72:55:76:17:FD:2A:F3:30:53:F0:3E:DA:E3:2B:0B:53:E7:0A:C7:97:8B:70:B7:80:29` (App signing, Play) |

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
