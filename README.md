# Namaz

Türkiye odaklı Kotlin / Jetpack Compose Android namaz vakitleri ve dini günler uygulaması.

## Sekmeler ve durum (14 Kasım 2026)

| Sekme | Durum |
| --- | --- |
| Ana Sayfa | İlk iskelet korunuyor; namaz vakti entegrasyonu yapılmadı |
| Dini Günler | Takvim, ay geçişleri, yaklaşan gün kartı ve detay diyaloğu kodlandı; yalnızca ilk 2026 kaydı eklendi |
| Hatırlatıcılar | Geliştirme aşamasında; gerçek bildirim altyapısı henüz yok |
| İbadet | Yer tutucu ekran |

## Eksikler / takip listesi

- [ ] Dini Günler: 2026'nın kalan ve 2027'nin tüm kandil/bayram tarihlerini resmî Diyanet kaynağından doğrulayıp ekle; takvim verisini kapsamlı test et
- [ ] Hatırlatıcılar: namaz bazında aç/kapat, önceden bildirim süresi ve tercihlerin kalıcı saklanması
- [ ] Hatırlatıcılar: doğrulanmış namaz vakti sağlayıcısı, şehir/ilçe seçimi ve zaman dilimi
- [ ] Hatırlatıcılar: Android 13+ POST_NOTIFICATIONS izni, bildirim kanalı ve gerçek zamanlama (AlarmManager/WorkManager gereksinimlerine göre)
- [ ] Hatırlatıcılar: yeniden başlatma, saat dilimi değişimi, pil optimizasyonu ve kesin alarm izni senaryolarını test et
- [ ] İbadet: zikirmatik ve günlük namaz takip ekranları
- [ ] Ana Sayfa: diğer sekmelerden özet kartlar ve namaz vakitleri
- [ ] Widget ve erişilebilirlik / küçük ekran kontrolleri
- [ ] Android derlemesi ve cihaz testi
- [ ] En son: GitHub Actions APK build workflow'u

## Çalışma düzeni

Geliştirme branch'i: `feature/religious-days`. `main` henüz değiştirilmedi.
APK derlemesi ve GitHub Actions en son yapılacak. UI kodunun commitlenmesi, derlendiği veya cihazda test edildiği anlamına gelmez.

## Teknoloji

Kotlin, Jetpack Compose, Android SDK (minSdk 26, targetSdk 35).
