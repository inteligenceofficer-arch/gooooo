# TOEIC 실전 연습 (PWA)

- 앱 소스: `app/index.html` (문제 데이터 포함)
- 배포용 PWA: `docs/` (GitHub Pages가 이 폴더를 서비스합니다)
- 문제 수정 후: `python3 tools/build_pwa.py` → 커밋/푸시하면 설치된 앱이 다음 실행 때 자동 업데이트됩니다.

고정 주소: https://inteligenceofficer-arch.github.io/gooooo/
(Settings → Pages → Deploy from a branch → 브랜치 선택, 폴더 `/docs`)

## 스탬프캠 (날짜·장소 워터마크 카메라)
- 소스: `docs/camera/` (단일 `index.html` PWA) → https://inteligenceofficer-arch.github.io/gooooo/camera/

### Android 앱(APK)
- `android/` : 웹앱을 감싼 안드로이드 앱 소스. 푸시하면 GitHub Actions(`android.yml`)가 APK를 빌드해 Releases에 올립니다.
- 다운로드(항상 최신): https://github.com/inteligenceofficer-arch/gooooo/releases/latest/download/stampcam.apk
- 업데이트: 앱 실행 시 새 릴리스를 확인해 안내합니다. 화면(웹) 변경은 자동 반영, 앱 껍데기 변경은 새 APK 설치.
