# طريقة تحويل مشروع ELITE X إلى APK

## الطريقة 1 — Android Studio

1. فك ضغط `EliteX-Android-Source.zip`.
2. افتح **Android Studio** ثم **Open** واختر مجلد `EliteX`.
3. ثبّت Android SDK Platform 35 و JDK 17 من SDK Manager عند الحاجة. الإصدار المطلوب من Gradle هو 8.9.
4. انتظر مزامنة Gradle وتحميل Shizuku API و AndroidX DocumentFile (يتطلب إنترنت أول مرة).
5. اختر **Build → Build APK(s)** أو `gradle :app:assembleDebug` إذا كان Gradle 8.9 مثبتًا.
6. ملف التثبيت الناتج: `app/build/outputs/apk/debug/app-debug.apk`.
7. أرسل ملف APK لهاتفك وافتحه للتثبيت (يلزم السماح بالتثبيت من هذا المصدر).

## الطريقة 2 — GitHub Actions بدون Android Studio

1. أنشئ مستودعًا جديدًا على GitHub.
2. ارفع **محتويات** مجلد `EliteX` إلى جذر المستودع؛ لا ترفع ملف ZIP كما هو.
3. من تبويب **Actions** افتح **Build ELITE X APK** ثم **Run workflow**، أو ارفع الملفات على فرع `main` ليبدأ البناء تلقائيًا.
4. بعد انتهاء عملية البناء افتح الـworkflow، وحمّل artifact باسم `EliteX-debug-apk`.
5. فك الضغط وستجد `app-debug.apk`.

## إعداد الصلاحيات

- تثبيت Shizuku: https://shizuku.rikka.app/download/
- تشغيل Shizuku عبر Wireless debugging / ADB، ومنح ELITE X صلاحية الوصول.
- منح إذن **Display over other apps** لتفعيل HUD و **Notifications** لخدمة الـOverlay.
- اختَر لعبة من Games، ثم فعّل أوضاع اللعبة من Boost.
- مدير الملفات يستخدم نافذة Android الرسمية SAF: بعض مجلدات ملفات الألعاب المحمية محظورة من النظام، وليس بالإمكان تجاوز القيود بدون صلاحية مناسبة.

## معلومات مهمة

- المشروع مصدر Android أصلي، وليس ملف APK جاهزًا من هذه المحادثة.
- لم يُبنَ التطبيق أو يُختبر على هاتف فعلي ضمن بيئة التنفيذ الحالية، لذا قد يحتاج تعديلات توافقية مع جهازك/Android ROM.
- التغييرات لا تضمن زيادة FPS أو خفض Ping؛ التطبيق يعرض فقط قيم القياس التي يسمح النظام بقراءتها.
