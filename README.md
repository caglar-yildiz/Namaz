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

## Güvenilirlik notları (14 Kasım 2026)

- Tarihe bağlı alarm kimlikleri eklendi; gece yarısı kimlik çakışması önlenmeye çalışıldı.
- BroadcastReceiver, asenkron işlem için `goAsync()` kullanıyor. Ancak API çağrısı iki güne yayıldığında Android'in yayın süresi sınırını aşabilir: üretim için WorkManager veya önbellekli kısa iş tercih edilmeli.
- Ağ hatalarında mevcut alarmlar korunuyor; fakat yeniden başlatma sonrası ağ yoksa alarmlar kurtarılamayabilir.
- Yaklaşık alarmlar Doze/pil kısıtları nedeniyle geç gelebilir. Dakika hassasiyetinin ürün gereksinimi ayrıca kararlaştırılmalı.
- Derleme, cihaz testi ve resmî Diyanet vakit doğrulaması yapılmadı. Bu özellik tamamlandı sayılmıyor.

## İbadet sekmesi (MVP)

- Günlük beş namaz için yerel işaretleme ve 0–5 ilerleme göstergesi.
- Sübhanallah, Elhamdülillah, Allahu ekber ve Salavat için birbirinden bağımsız günlük zikirmatik.
- 33, 99 ve 100 hedefleri; onaylı sıfırlama; SharedPreferences ile cihazda saklama.
- Takip kayıtları otomatik olarak bildirim göndermez; ibadet takibi bildirim kategorisi henüz işlevsel değildir.
- Eksikler: tarih geçmişi ve düzeltme, cihazlar arası yedekleme, veri dışa aktarma, gece yarısında açık ekranın yenilenmesi, erişilebilirlik ve cihaz/derleme testleri.
