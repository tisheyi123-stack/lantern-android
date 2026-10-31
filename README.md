# Lantern Android Clone (Target: Android 16 / API 36)

پروژه کلاینت اندروید شبیه به نرم‌افزار **Lantern** پیاده‌سازی شده با **Kotlin** و **Jetpack Compose** برای اندروید ۱۶ (API 36).

## ویژگی‌های پیاده‌سازی شده:

1. **رابط کاربری دقیق به سبک Lantern:**
   - دکمه پاور بزرگ نئونی با افکت Glow و انیمیشن تپنده (Pulse animation) در حالت اتصال.
   - وضعیت اتصال (ON/OFF Indicator).
   - کارت انتخاب موقعیت سرور (Server Location Selector).
   - مانیتور ترافیک و سرعت لحظه‌ای (Upload / Download Stats).
   - نمایشگر سهمیه حجم رایگان (Free High-Speed Data Quota bar) دقیقا مشابه Lantern.
   - آیکون فانوس طلایی و دکمه ارتقا به نسخه Pro.

2. **هسته شبکه (VpnService):**
   - پیاده‌سازی `LanternVpnService` به صورت Foreground Service سازگار با استانداردهای اندروید ۱۶.
   - ثبت اعلان دائمی (Persistent Notification) در زمان اتصال.
   - درخواست مجوز استاندارد سیستم‌عامل `VpnService.prepare`.
   - ایجاد تانل محلی VPN با MTU 1500 و سرورهای DNS ابری.

## ساختار فایل‌های پروژه:

- `settings.gradle.kts`
- `build.gradle.kts`
- `gradle/libs.versions.toml`
- `app/build.gradle.kts`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/lantern/clone/Color.kt`
- `app/src/main/java/com/lantern/clone/LanternVpnService.kt`
- `app/src/main/java/com/lantern/clone/MainActivity.kt`

## نحوه بیلد و خروجی APK:
این پروژه را مستقیماً در **Android Studio** باز کنید یا با دستور Gradle بیلد بگیرید:
```bash
./gradlew assembleDebug
```
فایل APK خروجی در مسیر `app/build/outputs/apk/debug/app-debug.apk` ساخته خواهد شد.
