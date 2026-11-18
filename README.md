# Namaz

Türkiye odaklı Kotlin / Jetpack Compose Android namaz vakitleri ve dini günler uygulaması.

## 0.2.0 deneme sürümü

- Ana Sayfa, kaydedilen şehrin pilot namaz vakitlerini, günlük özeti, rutinleri ve İyilik Çarkı’nı gösterir.
- İbadet’te kayan taneli, imameli tespih animasyonlu zikirmatik, Kur’an ezberi, dua kütüphanesi ve beş vaktin farzı için sesli prova bulunur. Prova, Android’in Türkçe ses motorunu kullanır.
- Dualar favorilere veya kişisel rutinlere eklenebilir. Takip geçmişi, rutinler için haftalık ve aylık kayıtları da gösterir.
- Hatırlatıcı’da rutin günleri ve saatleri düzenlenir. Dini gün detayları ilgili duaya ve bildirim tercihlerine açılır.
- Deneme APK’sı `:app:assembleDebug` göreviyle üretilir. Namaz vakitleri AlAdhan pilot kaynağından gelir.

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

## Namaz takip geçmişi

- Haftalık görünümde Pazartesi–Pazar için işaretlenen namaz sayıları.
- Aylık görünümde gün bazlı 0–5 işaret sayısı ve önceki aylara geçiş.
- Geçmiş gün seçerek beş vakit kaydını düzeltme; gelecek günler düzenlenemez.
- Son 30 günün toplam işaret sayısı; boş kayıtlar kılınmamış namaz olarak yorumlanmaz.
- Eksikler: takvim haftası hizalaması (şu an yedişerli gün grupları), tarih değişiminde ekran yenileme, geçmişten dönüşte ana ekranın tazelenmesi, erişilebilirlik, derleme ve cihaz testleri.

## İbadet takibi senkronizasyonu ve bildirimleri

- Günlük İbadet ve geçmiş ekranları SharedPreferences değişikliklerini dinleyerek kayıtları yeniden okur (cihaz testi yapılmadı).
- Ayarlar > İbadet takibi açıkken yaklaşık 21.00'de, yalnızca 5/5 işaretlenmemişse günlük kontrol bildirimi planlanır. İşaretlenmemiş namaz kılınmamış olarak yorumlanmaz.
- Boot ve saat değişikliklerinde bu alarm yeniden planlanır. Android bildirim izni ve pil tasarrufu kısıtları geçerlidir; tam zaman garantisi yoktur.
- Kalan işler: gece yarısında açık ekran yenileme, küçük ekran takvim yerleşimi, ayarlar/hatırlatıcılar arasında anlık kategori senkronizasyonu, gerçek cihaz ve derleme testleri.

## Tercih senkronizasyonu ve dar ekran iyileştirmesi

- Ayarlar ve Hatırlatıcılar ekranları aynı SharedPreferences dosyasını dinleyerek kategori, ses, namaz ve önceden bildirim tercihlerini yeniden okur.
- Namaz tercihi ve bildirim öncesi dakika değişiklikleri WorkManager üzerinden yeniden planlanır.
- Haftalık ve aylık takvim satırlarına yatay kaydırma ve sabit hücre genişliği eklendi; küçük ekran davranışı cihazda henüz doğrulanmadı.
- Kalan: gece yarısı açık ekran yenileme, aylık başlıklarla yatay kaydırma hizası, derleme ve cihaz testleri, resmî vakit karşılaştırması.

## 14 Kasım 2026 — arayüz düzeltmeleri

- [x] İbadet ve geçmiş ekranlarında İstanbul saatine göre gece yarısı tarih yenilemesi ve uygulama ön plana geldiğinde tarih kontrolü eklendi.
- [x] Aylık takvim başlığı ve tüm hafta satırları tek yatay kaydırma alanına alındı; hücreler aynı genişlikte.
- [ ] Gradle/Android derlemesi: bu çalışma ortamında Android SDK ve Gradle bulunmadığından çalıştırılamadı.
- [ ] Gerçek cihazda gece yarısı, küçük ekran ve bildirim testleri.
- [ ] Tüm ekranlar arasında anlık senkronizasyonun cihazda doğrulanması.

Bu değişiklikler kod seviyesindedir; APK veya başarılı build iddiası değildir.

## Tasarım iyileştirmeleri (`design/ui-polish`)

- Renkler, şekiller ve yazı ağırlıkları tek bir `Theme.kt` dosyasında toplandı; ekranlardaki kopya renk tanımları kaldırıldı.
- Alt menüde ve oklarda Unicode semboller yerine Material ikonları kullanılıyor (`material-icons-core`).
- Durum çubuğu ve gezinme çubuğu uygulama renklerine uyumlu (edge-to-edge); açılışta beyaz flaş yerine krem arka plan.
- İç ekran başlıkları geri ikonu + başlık satırı oldu; sekme ve bölüm başlıkları ortak bileşenlerle tutarlı hâle geldi.
- Ana Sayfa'da sıradaki vakit vurgulanıyor, geçmiş vakitler soluk gösteriliyor.
- Derleme ve cihaz testi yapılmadı.

## Sade kullanım, sesli yardım ve tanıtım (`feature/easy-mode`)

Hedef: Uygulamanın yaşlı kullanıcılar tarafından da kolayca kullanılabilmesi. Yeni özellik eklemek yerine mevcut özellikler sade bir yüzle sunuluyor.

- **İlk açılış tanıtımı (5 adım, sesli):** hoş geldin → kullanım şekli (Sade / Tüm özellikler) → şehir → vakit bildirimi izni → kısa tur. Her adım sesli okunur; ses kapatılabilir. Ayarlar’dan veya sade ekrandaki “Nasıl kullanılır?” penceresinden tekrar izlenebilir. Güncelleme sonrası mevcut kullanıcılar da tanıtımı bir kez görür.
- **Sade kullanım ekranı:** sıradaki vakit çok büyük yazıyla; dokununca sesli okunur. Açılışta vakti otomatik söyler (kapatılabilir). Altında 80dp yüksekliğinde büyük düğmeler: Konuşarak sor, Bugünün vakitleri, Tespih çek, Dua dinle, Namazımı kıldım. Alt menü ve ayrıntılı ayarlar gizli; “Tüm özellikler” onay ister.
- **Sesli tespih:** her 33’te ve hedefe ulaşınca sesli bildirir; “Sayıyı söyle” düğmesi var.
- **Dua dinle:** her dua büyük kartta, Türkçe anlamı sesli okunur.
- **Namazımı kıldım:** girmiş olan son vakti kaydeder, sesli onaylar, “geri al” sunar.
- **Sesli komut (yapay zekâ yok):** Android’in konuşma tanıma ekranı sesi metne çevirir; `VoiceCommands.kt` metindeki anahtar kelimelere bakar. Desteklenenler: “namaza ne kadar kaldı”, “öğle/ikindi/… ne zaman”, “namazımı kıldım”, “tespih”, “dua”, “vakitleri oku”, “bugün günlerden ne”, “yardım”.
- Sesli okuma için telefonda Türkçe TTS (ör. Google konuşma hizmetleri) yüklü olmalı; sesli komut için Google uygulaması gerekir.

Derleme: `:app:assembleDebug` 17 Kasım 2026’da başarıyla derlendi (Kotlin uyarısı yok). Bilinen eksikler: cihaz testi yapılmadı; vakit bildiriminin kendisi henüz sesli okunmuyor; sade modda yazı boyutu sistem yazı boyutu ile birlikte büyür, çok büyük ayarda yerleşim kontrol edilmeli.

## Sesli ve tam zamanında vakit bildirimi

- **Tam zaman:** Vakit alarmları `setExactAndAllowWhileIdle` ile kuruluyor (Doze modunda da dakikasında çalışır). Android 12+ için `SCHEDULE_EXACT_ALARM` gerekir; Android 14’te yeni kurulumlarda bu izin varsayılan olarak kapalıdır. İzin yoksa uygulama `setAndAllowWhileIdle` yöntemine düşer (birkaç dakika gecikebilir) ve tanıtımda, sade ekranda ve Hatırlatıcılar’da “Alarmlar ve hatırlatıcılar” iznini açan bir kart gösterir. İzin verildiğinde alarmlar kendiliğinden yeniden kurulur.
- **Sesli:** Alarm gelince kısa ömürlü bir ön plan servisi (`PrayerAnnouncementService`, tür: mediaPlayback) bildirimi gösterir ve “Öğle namazı vakti girdi. Saat 12:41.” cümlesini alarm ses seviyesinde, araya kısa bir sessizlik koyarak iki kez okur. Bildirimde “Sustur” düğmesi vardır; servis en geç 45 saniyede kapanır, bildirim ekranda kalır.
- Telefon sessiz/titreşimdeyse veya görüşme sürüyorsa konuşmaz; seçili bildirim sesiyle normal bildirim gelir. Hatırlatıcılar > Namaz vakitleri’ndeki “Vakti sesli söyle” anahtarıyla kapatılabilir.
- “Önceden hatırlat” seçiliyse “Öğle namazı vaktine 10 dakika kaldı” der. Yatsı alarmı iki günlük planlama penceresini yeniler.
- “Test bildirimi planla” artık aynı yolu kullanır: bir dakika sonra sesli test bildirimi gelir.
- Derleme ve `lintDebug` başarılı (yeni kodda uyarı yok). Gerçek cihazda Doze, pil optimizasyonu ve üretici kısıtları (Xiaomi, Huawei vb.) henüz test edilmedi.

## Xiaomi, Huawei, Oppo, Vivo, Samsung: bildirim güvenilirliği

Bu markalar standart Android’in üzerine kendi pil yöneticilerini ekler ve uygulamanın alarmlarını tamamen durdurabilir. Bu ayarlar uygulamadan okunamaz. Çözüm üç katmanlı (`DeviceReliability.kt`):

1. **Kontrol edilebilenler:** bildirim izni, “Alarmlar ve hatırlatıcılar” (kesin alarm) izni ve Android pil optimizasyonu (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`). Her biri ✓ / ! olarak gösterilir ve tek dokunuşla ilgili sistem ekranı açılır.
2. **Markaya özel:** `Build.MANUFACTURER` ile marka algılanır, markanın kendi ekranı açılır (Xiaomi Otomatik başlatma ve pil tasarrufu, Huawei/Honor Uygulama başlatma, Oppo/Realme/OnePlus Otomatik başlatma, Vivo Arka planda başlatma, Samsung Arka plan kullanım sınırları). Ekran o sürümde yoksa sıradaki aday, en son uygulama bilgisi sayfası açılır. Ne yapılacağı Türkçe adımlarla yazılır; kullanıcı “Bunları yaptım” ile onaylar.
3. **Gerçek ölçüm (`AlarmHealth`):** her vakit alarmının planlanan ve gerçekte çalıştığı zaman kaydedilir. Bugün ve dün için gelmeyen (10 dk tolerans) veya 3 dakikadan geç gelen bildirim varsa uyarı gösterilir. Böylece 1. ve 2. katmanda kaçan bir sorun da ortaya çıkar.

Gösterildiği yerler: tanıtımın bildirim adımı (sorunlar giderilmeden ilerlemez, “Devam” ile atlanabilir), sade ana ekranda “Bildirimleri düzelt” kartı ve ayrı ekran (sesli deneme düğmesiyle), Hatırlatıcılar > Namaz vakitleri.

Notlar: Markaların ekran adresleri yazılım sürümüne göre değişebilir; cihazda doğrulanmadı. `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` Google Play’de gerekçe ister (çekirdek işlev: zamanlı namaz hatırlatması). Kaynak: dontkillmyapp.com.
