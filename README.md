# 워치 케어 커스터마이징

SMART-TIME PRO의 **원본 화면·기능 코드·워치 SDK를 유지하며 수정하는 Android 작업 공간**입니다. 현재 주 작업 버전은 **워치 케어 0.4.0 로컬 테스트판**입니다. 화면과 기능을 하나씩 새로 만드는 방식이 아니라, 기존 XML·이미지·Smali 코드를 재사용합니다.

**[최신 APK 다운로드](https://github.com/dlwlsrnjs/custom_smart-time-pro/releases/download/ui-v0.4.0/Watch_Care_0.4.0.apk)** · **[커스터마이징 작업 가이드](docs/WATCH_CARE_CUSTOMIZATION.md)** · **[현재 상태](docs/CURRENT_STATUS.md)** · **[Release와 체크섬](https://github.com/dlwlsrnjs/custom_smart-time-pro/releases/tag/ui-v0.4.0)**

## 지금 확인된 결과

- Galaxy S25 Android 16에서 업데이트 설치와 실행 확인.
- 원본 건강·운동·제품 설정·사용자 설정 화면 유지.
- 원본 SDK로 워치 등록·연결, 펌웨어 버전 확인.
- 실제 심박·활동 기록 동기화와 원본 건강 화면의 심박수·걸음수 표시 확인.
- 원본 검색 필터가 이미 등록된 워치를 숨기던 문제 수정.

파일 이름은 `Watch_Care_0.4.0.apk`, 휴대폰 앱 이름은 **워치 케어**, 앱 ID는 `com.diz.sw01.custom`, versionCode는 **10**입니다. 원본 앱 `com.sma.smartv3.pro`를 제거하지 않습니다. 이전 커스텀·센서 시험 앱은 같은 ID이므로 이 APK로 업데이트됩니다.

현재는 **서버 통신이 없는 로컬 시험판**입니다. 로그인·날씨·온라인 워치페이스·웹 콘텐츠는 연결되지 않습니다. 광고 초기화와 자동 진입점을 차단하고 민감 권한을 줄였지만, 의존성이 있는 SDK 코드가 남아 있습니다. 모든 광고 SDK 삭제, 모든 워치 지원, 전체 기능 회귀 시험이 완료된 것으로 해석하지 않습니다.

USB 설치 성공과 삼성·Google의 보안 판정 또는 파일관리자 재설치 성공은 별도입니다. 자세한 변경과 검증 범위는 [현재 빌드 설명](docs/ORIGINAL_UI_BUILD.md)에 있습니다.

## 무엇을 하려는지에 따라 시작하기

| 하려는 작업 | 먼저 읽을 문서 |
| --- | --- |
| APK를 설치해 현재 버전 사용 | [최신 Release](https://github.com/dlwlsrnjs/custom_smart-time-pro/releases/tag/ui-v0.4.0) |
| 원본 화면·문구·색·카드 크기 변경 | [작업 가이드 4절부터](docs/WATCH_CARE_CUSTOMIZATION.md#4-권장-시작-방법은-최신-apk를-작업-기준으로-풀기) |
| 기기 검색·센서·기능 로직 변경 | [작업 가이드 6절](docs/WATCH_CARE_CUSTOMIZATION.md#6-센서와-연결-기능을-수정하는-위치) |
| 수정본 빌드·서명·휴대폰 시험 | [작업 가이드 8절부터](docs/WATCH_CARE_CUSTOMIZATION.md#8-버전-변경과-빌드-및-서명) |
| 원본 기능별 수정 후보 확인 | [25개 영역별 안내](docs/CUSTOMIZATION.md) |
| 독립 센서 통신 검증 코드 확인 | [ble-minimal](ble-minimal/README.md) |
| 전체 분석본 복원과 과거 작업 확인 | [복원 범위](docs/RECOVERY.md), [기존 빌드 경로](docs/BUILD.md) |

## 권장 작업 흐름

1. 저장소를 복제하고 작업 브랜치를 만듭니다.
2. **최신 워치 케어 APK**를 Apktool로 풀어 작업 기준으로 사용합니다.
3. UI만 바꾸면 XML·이미지를, 기능도 바꾸면 전체 Smali를 수정합니다.
4. 바꾼 파일을 `patches/watch-care-ui`에 같은 상대 경로로 저장합니다.
5. `scripts/Build-WatchCare.ps1`로 버전 변경·빌드·정렬·서명·검사를 수행합니다.
6. 휴대폰에서 설치·원본 메뉴·기기 등록·센서 동기화를 시험하고 Release로 배포합니다.

전체 명령과 수정 예시, 필수 도구 경로, 서명키 유지 방법, 오류 대응은 **[커스터마이징 작업 가이드](docs/WATCH_CARE_CUSTOMIZATION.md)**에 정리했습니다.

## 저장소 구성

| 경로 | 용도 |
| --- | --- |
| `docs/` | 현재 작업 가이드, 변경 내용, 기능 지도, 과거 진단 |
| `patches/original-ui/` | 원본 UI 기반 로컬 빌드의 검색 수정 |
| `patches/base/` | 과거 원본 복원 경로의 Smali·리소스 오버레이 |
| `scripts/Build-WatchCare.ps1` | 최신 워치 케어 작업 공간 빌드·서명 |
| `scripts/UpdateWatchCareMetadata.py` | 앱 이름·버전 변경 |
| `scripts/PrepareOriginalUI.py` | 분석 작업 공간에서 로컬 UI 구성 재생성 |
| `reference-java/` | 구조를 읽기 위한 원본 Java 복원 자료 |
| `linear-reference-java/` | 단순 명령 흐름 보조 자료 |
| `analysis/` | 클래스·리소스·네이티브·복원 및 검사 자료 |
| `ble-minimal/` | 별도로 직접 빌드되는 센서 검증 앱 |
| `profiles/` | 기존 앱 식별자·버전 프로필 |

Java 복원본은 원 개발자의 Gradle 프로젝트가 아닙니다. 실행 로직은 Smali로 확인·수정하며, Java를 그대로 재컴파일해 전체 앱을 만드는 구조로 취급하지 않습니다. 전체 제3자 라이브러리와 리소스는 [분석 작업 공간 Release](https://github.com/dlwlsrnjs/custom_smart-time-pro/releases/tag/analysis-v2.1.3.4-baseline)에 있습니다.

## 버전별 역할

| 버전 | 역할과 상태 |
| --- | --- |
| **워치 케어 0.4.0** | 현재 주 작업 대상. 원본 UI·SDK 유지, 로컬 센싱 표시 실기기 확인 |
| 센서 0.3.0 | 통신 규격과 수신 해석을 검증한 독립 앱. 소스는 `ble-minimal`에 보존 |
| BLE 0.2.0 및 0.2.1 | 설치와 검색·연결·해제 범위를 좁혀 시험했던 앱 |
| Custom 0.1.0 및 초기 통합본 | 과거 재패키징 기준 자료. 삼성 설치 차단 보고가 있었던 버전 |

과거 보안 진단과 설치 비교는 [설치 점검](docs/INSTALLATION_AUDIT.md), [단말 진단](docs/DEVICE_BLOCK_DIAGNOSIS.md), [Mindrium 비교](docs/REFERENCE_INSTALL_COMPARISON.md)에 보존했습니다. 과거 APK를 최신 빌드로 혼동하지 마세요.
