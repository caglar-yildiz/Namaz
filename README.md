# Namaz

Türkiye odaklı Kotlin / Jetpack Compose Android namaz vakitleri ve dini günler uygulaması.

## Sekmeler ve durum (14 Kasım 2026)

| Sekme | Durum |
| --- | --- |
| Ana Sayfa | İlk iskelet korunuyor; namaz vakti entegrasyonu yapılmadı |
| Dini Günler | Takvim, ay geçişleri, yaklaşan gün kartı ve detay diyaloğu kodlandı; yalnızca ilk 2026 kaydı eklendi |
| Hatırlatıcılar | Kalıcı tercihler, bildirim izni, Android kanalları ve yaklaşık 1 dakikalık test bildirimi kodlandı; gerçek namaz vakti zamanlaması yok |
| İbadet | Yer tutucu ekran |

## Eksikler / takip listesi

- [ ] Dini Günler: 2026'nın kalan ve 2027'nin tüm kandil/bayram tarihlerini resmî Diyanet kaynağından doğrulayıp ekle; takvim verisini kapsamlı test et
- [ ] Hatırlatıcılar: namaz bazında aç/kapat, önceden bildirim süresi ve tercihlerin kalıcı saklanması
- [ ] Hatırlatıcılar: doğrulanmış namaz vakti sağlayıcısı, şehir/ilçe seçimi ve zaman dilimi
- [x] Hatırlatıcılar: Android 13+ POST_NOTIFICATIONS izni isteme, bildirim kanalları ve test bildirimi altyapısı kodlandı (cihaz testi bekliyor)
- [ ] Hatırlatıcılar: doğrulanmış vakit verisiyle gerçek zamanlama ve iptal/yeniden planlama
- [ ] Bildirim sesi: 'hafif kısa ton' için özgün kısa ses dosyası; şu an sistem varsayılan sesine eşleniyor
- [ ] Ayarlar ve Hatırlatıcılar sekmeleri arasında anlık tercih senkronizasyonu ve gerçek kategori bazlı gönderim
- [x] Hatırlatıcılar: günlük yaklaşık yenileme, tercih değişiminde yeniden planlama, BOOT_COMPLETED / saat değişimi receiver kodları eklendi (test edilmedi)
- [ ] Hatırlatıcılar: reboot receiver için güvenilir arka plan yürütmesi (goAsync/WorkManager), cihaz testi, pil optimizasyonu ve kesin alarm gereksinimleri
- [ ] Hatırlatıcılar: gün değişiminde sabit alarm kimliklerinin kaydırılması, gece yarısı ve ağ hatası senaryoları
- [ ] Hatırlatıcılar: iki ekranda tercihlerin anlık senkronizasyonu
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
