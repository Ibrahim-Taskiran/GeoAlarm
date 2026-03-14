<p align="center">
  <img src="https://img.icons8.com/3d-fluency/94/marker.png" width="80" alt="GeoAlarm Logo"/>
</p>

<h1 align="center">GeoAlarm</h1>

<p align="center">
  <b>Konum Bazlı Akıllı Alarm Uygulaması</b><br/>
  Hedefe yaklaştığınızda sizi uyandıran, modern ve kullanıcı dostu Android uygulaması.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white" alt="Platform"/>
  <img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white" alt="Language"/>
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white" alt="UI"/>
  <img src="https://img.shields.io/badge/Maps-Google%20Maps-4285F4?logo=googlemaps&logoColor=white" alt="Maps"/>
  <img src="https://img.shields.io/badge/Min%20SDK-24%20(Android%207.0)-brightgreen" alt="Min SDK"/>
  <img src="https://img.shields.io/badge/License-MIT-blue" alt="License"/>
</p>

---

## 📖 Hakkında

**GeoAlarm**, toplu taşımada, uzun yolculuklarda veya günlük hayatta belirlediğiniz bir konuma yaklaştığınızda sizi otomatik olarak uyaran bir Android uygulamasıdır. Artık durağınızı kaçırma veya dönüş noktanızı geçme derdi yok!

### Kullanım Senaryoları

| Senaryo | Açıklama |
|---|---|
| 🚌 **Otobüs / Metro** | İneceğiniz durağa yaklaşınca alarm çalar |
| 🚗 **Uzun Yolculuk** | Dinlenme tesisine veya varış noktasına yaklaşınca uyarır |
| 🏠 **Eve Dönüş** | Eve yaklaştığınızda hatırlatma alın |
| 📍 **Buluşma Noktası** | Belirtilen lokasyona yaklaşınca bildirim |

---

## ✨ Özellikler

- 🗺️ **Tam Ekran Harita** — Dark-styled Google Maps ile modern görünüm
- 📍 **Anlık Konum Takibi** — GPS ile yüksek doğrulukta konum belirleme
- 🎯 **Hedef Seçimi** — Haritada uzun basarak hedef belirleme
- 📏 **Ayarlanabilir Mesafe Eşiği** — 100m ile 5km arasında slider ile ayarlama
- ⭕ **Görsel Yarıçap** — Hedef etrafında seçilen mesafe kadar yarı-saydam daire
- 🔔 **Alarm Sistemi** — Eşik mesafeye girildiğinde ses + titreşim + bildirim
- 🔄 **Arkaplan Çalışma** — Foreground Service ile uygulama kapalıyken bile takip
- 📊 **Canlı Mesafe Bilgisi** — Anlık mesafe ve eşik değeri info kartlarında gösterilir
- 🎨 **Modern UI** — Glassmorphism, animasyonlar, gradient'ler ile premium tasarım

---

## 🏗️ Mimari & Teknoloji

```
GeoAlarm/
├── ui/theme/
│   ├── Color.kt          # Cyan/Blue dark renk paleti
│   ├── Theme.kt           # Material3 dark theme + edge-to-edge
│   └── Type.kt            # Özel tipografi tanımları
├── MainActivity.kt        # Ana ekran: Harita + BottomSheet + Slider + Kontroller
├── LocationService.kt     # Foreground Service: sürekli konum takibi
└── AlarmHelper.kt         # Alarm: ses, titreşim, bildirim yönetimi
```

| Teknoloji | Versiyon | Kullanım Amacı |
|---|---|---|
| **Kotlin** | 2.0.21 | Uygulama dili |
| **Jetpack Compose** | BOM 2024.09 | Deklaratif UI |
| **Material3** | — | Tasarım sistemi |
| **Google Maps Compose** | 8.1.0 | Harita görüntüleme |
| **Play Services Location** | 21.3.0 | GPS konum servisi |
| **Lifecycle Service** | 2.10.0 | Foreground service yönetimi |

---

## 🚀 Kurulum

### Gereksinimler

- Android Studio Ladybug (2024.2+) veya üzeri
- JDK 11+
- Android SDK 24+ (min) / 36 (target)
- Google Maps API Key

### Adımlar

1. **Repoyu klonlayın:**
   ```bash
   git clone https://github.com/Ibrahim-Taskiran/GeoAlarm.git
   cd GeoAlarm
   ```

2. **Google Maps API Key ayarlayın:**

   `app/src/main/AndroidManifest.xml` dosyasında kendi API Key'inizi girin:
   ```xml
   <meta-data
       android:name="com.google.android.geo.API_KEY"
       android:value="YOUR_API_KEY_HERE" />
   ```

   > API Key almak için: [Google Cloud Console](https://console.cloud.google.com/) → Maps SDK for Android

3. **Android Studio'da açın** ve Gradle Sync yapın.

4. **Run ▶️** butonuna basarak cihaz veya emülatöre yükleyin.

---

## 📱 Kullanım Kılavuzu

```
1. Uygulama açılır → Konum izni iste → "İzin Ver"
2. Sağ üst 📍 butonuna bas → Mevcut konumun haritada gösterilir
3. Haritada hedef noktaya UZUN BAS → Hedef marker + radius daire belirir
4. Alt paneldeki SLIDER ile alarm mesafesini ayarla (100m – 5km)
5. "Alarmı Başlat" butonuna bas → Takip başlar
6. Hedefe yaklaştığında → 🔔 Alarm çalar + 📳 Titreşim + 📩 Bildirim
7. "Alarmı Durdur" butonuna bas → Her şey durur
```

---

## 📋 İzinler

| İzin | Açıklama |
|---|---|
| `ACCESS_FINE_LOCATION` | GPS ile hassas konum |
| `ACCESS_COARSE_LOCATION` | Yaklaşık konum |
| `ACCESS_BACKGROUND_LOCATION` | Arkaplan konum takibi |
| `FOREGROUND_SERVICE` | Arkaplan servisi çalıştırma |
| `FOREGROUND_SERVICE_LOCATION` | Konum tabanlı servis |
| `POST_NOTIFICATIONS` | Bildirim gösterme (Android 13+) |
| `VIBRATE` | Titreşim |
| `INTERNET` | Harita tile'ları yükleme |

---

## 🤝 Katkıda Bulunma

1. Bu repoyu **fork** edin
2. Yeni bir branch oluşturun: `git checkout -b feature/yeni-ozellik`
3. Değişikliklerinizi commit edin: `git commit -m "Yeni özellik eklendi"`
4. Branch'inizi push edin: `git push origin feature/yeni-ozellik`
5. **Pull Request** açın

---

## 📄 Lisans

Bu proje [MIT Lisansı](LICENSE) ile lisanslanmıştır.

---

<p align="center">
  <b>⭐ Projeyi beğendiyseniz yıldız vermeyi unutmayın!</b>
</p>
