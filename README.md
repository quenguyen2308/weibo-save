# WeiboSave

Android app tải ảnh full-resolution từ Weibo về thư mục `Pictures/WeiboSave` trên thiết bị.

---

## Tính năng

- **Nhập URL** — dán link bài Weibo bất kỳ (weibo.com hoặc m.weibo.cn)
- **Lọc ảnh** — chỉ định số thứ tự ảnh cần tải, ví dụ `1,4,7` hoặc `2,5-8`
- **Xem album** — lưới thumbnail 3 cột, hiển thị kích thước file gốc ngay dưới
- **Chọn ảnh** — long-press để vào selection mode; nút tròn toggle chọn tất cả
- **Preview** — tap vào ảnh để xem full-screen, vuốt ngang để chuyển ảnh
- **Tải xuống** — Foreground Service chạy nền, probe CDN (wx1–wx4) lấy chất lượng tốt nhất, chunk 8 ảnh/lần
- **Share intent** — nhận share link từ app Weibo, mở thẳng màn hình album
- **Đa ngôn ngữ** — Tiếng Anh (mặc định) + Tiếng Việt theo ngôn ngữ máy

---

## Yêu cầu

| Thứ | Phiên bản |
|-----|-----------|
| Android | 8.0+ (API 26) |
| JDK | 17 |
| Android SDK | API 35 |

---

## Build

### Nhanh (script)

```bash
# Debug APK (mặc định)
./build-apk.sh

# Release APK (đã sign)
./build-apk.sh release
```

APK output tại `WeiboSave_apk/WeiboSave_<type>_<timestamp>.apk`.

### Thủ công

```bash
./gradlew assembleDebug    # debug
./gradlew assembleRelease  # release
```

### Release signing

Thêm vào `local.properties` (file này bị gitignore):

```properties
RELEASE_STORE_FILE=keystore/weibosave-release.jks
RELEASE_STORE_PASSWORD=<password>
RELEASE_KEY_ALIAS=weibosave
RELEASE_KEY_PASSWORD=<password>
```

Nếu không có `local.properties` hoặc keystore, release build vẫn chạy nhưng ra file unsigned.

Tạo keystore mới:

```bash
keytool -genkeypair \
  -keystore keystore/weibosave-release.jks \
  -alias weibosave \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass '<password>' -keypass '<password>'
```

---

## Cấu trúc project

```
app/src/main/java/com/weibosave/
├── MainActivity.kt              # NavHost + Share Intent handler
├── data/
│   ├── WeiboApi.kt              # OkHttp → m.weibo.cn/statuses/show
│   ├── WeiboRepository.kt       # Singleton client + API + downloader
│   └── ImageDownloader.kt       # HEAD probe CDN, GET download, CDN fallback
├── model/
│   ├── WeiboPost.kt             # PostData, PicItem
│   └── DownloadState.kt         # Pending / Probing / Downloading / Done / Error
├── service/
│   ├── DownloadService.kt       # Foreground Service, notification progress
│   └── DownloadStateHolder.kt   # StateFlow chia sẻ trạng thái Service ↔ UI
├── ui/
│   ├── home/                    # URL input, index filter
│   ├── album/                   # Thumbnail grid, selection, preview
│   └── download/                # Danh sách tiến độ từng ảnh
└── util/
    ├── UrlExtractor.kt          # Regex extract post ID từ 5 dạng URL
    └── MediaStoreHelper.kt      # Lưu ảnh vào Pictures/WeiboSave (IS_PENDING)
```

---

## Cách dùng

### Từ app WeiboSave

1. Dán link bài Weibo vào ô **Weibo URL**
2. *(Tuỳ chọn)* Nhập số thứ tự ảnh vào ô **Số thứ tự ảnh** — ví dụ `1,3,5-7`
3. Nhấn **View & Download**
4. Trong màn hình album: long-press để chọn ảnh, hoặc nhấn **Download all**
5. Ảnh lưu vào `Pictures/WeiboSave` trong Gallery

### Share từ Weibo

Trong app Weibo → Share → WeiboSave → mở thẳng album bài đó.

---

## Kỹ thuật

### CDN probe

Mỗi ảnh được HEAD-request theo thứ tự ưu tiên: `large` → `orj1080` → `mw2000` → `orj480` → `orj360`.  
Nếu host `wx2` trả 403, tự động thử `wx1`, `wx3`, `wx4`.

### Download

8 ảnh/chunk, song song bằng `async/awaitAll` trong coroutine scope của Service.  
Retry 3 lần với exponential backoff (2s, 4s) khi lỗi mạng.

### Lưu ảnh

`MediaStore` với `IS_PENDING = 1` trong lúc ghi, đặt về `0` sau khi xong — tránh Gallery thấy file chưa hoàn chỉnh.

### Size display

Sau khi album load, probe HEAD song song cho tất cả ảnh để lấy `Content-Length`, hiển thị dần trên từng thumbnail khi có kết quả.
