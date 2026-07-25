# نسخه Android/Kotlin WebView دفترچه هزینه

این پروژه نسخه‌ی Android/Kotlin همان اپ PWA «دفترچه هزینه» است. برای بیشترین شباهت، رابط کاربری اصلی HTML/CSS/JS دقیقاً داخل WebView اجرا می‌شود.

## ویژگی‌ها

- ساخته‌شده با Kotlin و WebView
- فایل‌های وب داخل `app/src/main/assets/www` قرار گرفته‌اند و آفلاین اجرا می‌شوند
- فونت Vazirmatn داخل پروژه قرار داده شده تا بدون اینترنت هم ظاهر فارسی حفظ شود
- LocalStorage فعال است؛ داده‌ها روی خود گوشی ذخیره می‌شوند
- Import فایل JSON/CSV با File Picker اندروید کار می‌کند
- Export JSON/CSV از طریق AndroidBridge در مسیر Downloads/DaftarcheHazine ذخیره می‌شود
- Share Sheet اندروید برای اشتراک‌گذاری متن انتخاب‌ها فعال شده
- دکمه Back اندروید اول مودال‌های داخل اپ را می‌بندد

## ویجت اضافه‌شده از روی فایل خودت

ویجت Native اندروید هم به پروژه اضافه شده و از طرح `widget-mockup.html` الگوبرداری شده:

- ویجت Resizable با پنج حالت ظاهری برای نزدیک‌تر شدن به رفتار واقعی Resize:
  - `۵×۲` عریض با آیتم‌های بیشتر
  - `۴×۲` کامل: تاریخ، جمع دوره، دکمه `+` و ردیف آیتم‌های آخر
  - `۳×۲` فشرده با آیتم کمتر و شمارنده بیشتر
  - `۴×۱` باریک
  - `۲×۲` مربع
- فونت Vazirmatn برای ویجت Native هم اضافه شده (`res/font/vazirmatn_regular.ttf`)
- ردیف آیتم‌ها در ویجت ۴×۲ با متن Rich شبیه دمو ساخته شده: جداکننده کم‌رنگ، مبلغ بولد، شمارنده `⁺N`
- `previewImage` اختصاصی برای لیست ویجت‌ها ساخته شد، به جای اینکه فقط آیکون اپ نمایش داده شود
- پنجره‌های Native ثبت سریع و تنظیمات به بالای صفحه منتقل شدند تا به حس «لنگر به ویجت» نزدیک‌تر شوند
- دکمه ⚙ برای انتخاب دوره:
  - خرج کرد امروز
  - خرج کرد این هفته
  - خرج کرد این ماه
- دکمه `+` برای ثبت سریع هزینه با پنجره دیالوگی Native
- لمس بدنه ویجت، اپ را باز می‌کند و بر اساس دوره فعال، فیلتر همان دوره را اعمال می‌کند
- داده‌های اپ WebView و ویجت با `AndroidBridge` سینک می‌شوند
- بعد از ثبت/ویرایش/حذف داخل اپ، ویجت آپدیت می‌شود
- بعد از ثبت سریع از ویجت، داده در SharedPreferences ذخیره می‌شود و با باز شدن اپ وارد LocalStorage می‌شود

فایل‌های اصلی ویجت:

```text
app/src/main/java/ir/ramezani/expensenotebook/ExpenseWidgetProvider.kt
app/src/main/java/ir/ramezani/expensenotebook/ExpenseDataUtils.kt
app/src/main/java/ir/ramezani/expensenotebook/QuickAddActivity.kt
app/src/main/java/ir/ramezani/expensenotebook/WidgetSettingsActivity.kt
app/src/main/res/xml/expense_widget_info.xml
app/src/main/res/layout/widget_5x2.xml
app/src/main/res/layout/widget_4x2.xml
app/src/main/res/layout/widget_3x2.xml
app/src/main/res/layout/widget_4x1.xml
app/src/main/res/layout/widget_2x2.xml
```

## ساختار مهم پروژه

```text
ExpenseNotebookAndroid/
├── app/src/main/java/ir/ramezani/expensenotebook/MainActivity.kt
├── app/src/main/java/ir/ramezani/expensenotebook/ExpenseWidgetProvider.kt
├── app/src/main/java/ir/ramezani/expensenotebook/ExpenseDataUtils.kt
├── app/src/main/assets/www/index.html
├── app/src/main/assets/www/app.css
├── app/src/main/assets/www/app.js
├── app/src/main/assets/www/Vazirmatn.woff2
├── app/src/main/res/layout/widget_5x2.xml
├── app/src/main/res/layout/widget_4x2.xml
├── app/src/main/res/layout/widget_3x2.xml
├── app/src/main/res/layout/widget_4x1.xml
├── app/src/main/res/layout/widget_2x2.xml
├── app/src/main/AndroidManifest.xml
├── app/build.gradle.kts
├── build.gradle.kts
└── settings.gradle.kts
```

## اجرا در Android Studio

1. پوشه `ExpenseNotebookAndroid` را در Android Studio باز کن.
2. صبر کن Gradle Sync کامل شود.
3. یک گوشی یا Emulator انتخاب کن.
4. روی Run بزن.
5. بعد از نصب، روی Home Screen نگه دار → Widgets → ویجت «دفترچه هزینه» را اضافه کن.



## ساخت APK بدون Android Studio با GitHub Actions

این پروژه فایل workflow آماده دارد:

```text
.github/workflows/android-build.yml
```

بعد از اینکه پروژه را در GitHub آپلود کردی، از تب **Actions** می‌توانی APK بگیری. خروجی در بخش Artifacts با نام زیر قرار می‌گیرد:

```text
ExpenseNotebook-debug-apk
```

## ساخت APK با ترمینال

اگر Android SDK و JDK 17 روی سیستم نصب باشد:

> نسخه فعلی برای حفظ فونت Native ویجت، `minSdk = 26` دارد؛ یعنی Android 8 به بالا.

```bash
cd ExpenseNotebookAndroid
./gradlew assembleDebug
```

خروجی APK اینجا ساخته می‌شود:

```text
app/build/outputs/apk/debug/app-debug.apk
```

برای نسخه Release:

```bash
./gradlew assembleRelease
```

> نکته: برای امضای نسخه Release باید signing config اضافه شود.

## نکته درباره «صد درصد شبیه بودن»

چون خود HTML/CSS/JS اصلی داخل WebView اجرا می‌شود، ظاهر و رفتار اپ تا حد ممکن عین نسخه فعلی حفظ شده است. برای ویجت هم با محدودیت‌های واقعی Android RemoteViews نزدیک‌ترین نسخه Native ساخته شده؛ انیمیشن‌های HTML و Canvas داخل ویجت واقعی اندروید قابل استفاده نیستند، اما ساختار، رنگ، متن‌ها، دوره‌ها، دکمه `+` و ⚙ مطابق طرح خودت پیاده شده‌اند.
