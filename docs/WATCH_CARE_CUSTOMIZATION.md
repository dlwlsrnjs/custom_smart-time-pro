# 워치 케어 커스터마이징 작업 가이드

현재 주 작업 대상은 **원본 UI와 워치 SDK를 유지한 워치 케어 0.4.0**입니다. 기능을 하나씩 새로 만드는 대신, 기존 화면·코드·통신 흐름을 보존하고 필요한 부분을 수정합니다. `ble-minimal`은 통신 규격을 검증한 독립 센서 앱이며 최종 UI의 기준이 아닙니다.

## 1 현재 결과와 다운로드

- [최신 테스트 APK와 패치](https://github.com/dlwlsrnjs/custom_smart-time-pro/releases/tag/ui-v0.4.0)
- [APK 바로 받기](https://github.com/dlwlsrnjs/custom_smart-time-pro/releases/download/ui-v0.4.0/Watch_Care_0.4.0.apk)
- [최신 빌드의 변경 내용](ORIGINAL_UI_BUILD.md)
- [전체 분석 작업 공간](https://github.com/dlwlsrnjs/custom_smart-time-pro/releases/tag/analysis-v2.1.3.4-baseline)

파일 이름은 `Watch_Care_0.4.0.apk`, 휴대폰의 앱 이름은 **워치 케어**, 앱 ID는 `com.diz.sw01.custom`입니다. 기존 SMART-TIME PRO 앱의 ID와 다르며 기존 원본 앱을 삭제하지 않습니다. 이전 커스텀·센서 시험 버전과는 같은 ID를 사용하므로 업데이트 시 그 앱이 워치 케어로 바뀝니다.

확인한 내용은 Galaxy S25 Android 16에서의 USB 설치와 실행, 원본 건강·운동·제품 설정·사용자 설정 UI 진입, 원본 SDK의 워치 등록, 심박 및 걸음수 데이터 동기화와 표시입니다. 표시된 모든 건강 항목의 측정과 정확도를 확인한 것은 아닙니다.

현재 인터넷 권한이 없는 **로컬 시험 구성**입니다. 계정 로그인·온라인 워치페이스·날씨·웹 콘텐츠는 연결되지 않습니다. 광고 초기화와 Manifest 진입점을 비활성화했지만, 의존성을 가진 광고 SDK의 모든 DEX를 삭제한 상태는 아닙니다. 코드 존재·메뉴 표시·기기 지원·실제 시험 완료를 구분해 관리합니다.

## 2 작업자가 먼저 알아야 하는 구조

| 위치 | 쓰임새 |
| --- | --- |
| `docs/WATCH_CARE_CUSTOMIZATION.md` | 현재 버전으로 작업을 시작하는 절차와 수정 예시 |
| `docs/ORIGINAL_UI_BUILD.md` | 원본을 유지하면서 실제로 바꾼 부분 |
| `docs/CUSTOMIZATION.md` | 원본의 기능 영역별 수정 후보와 제약 |
| `patches/base/` | 과거 원본 분석·복원 경로의 오버레이 |
| `patches/original-ui/` | 현재 원본 UI 버전의 기기 검색 필터 수정 |
| `patches/watch-care-ui/` | 앞으로 추가할 화면·기능 수정 파일을 보관할 권장 경로 |
| `reference-java/` | 원본 구조를 읽기 위한 Java 복원 자료 |
| `linear-reference-java/` | 단순 명령 흐름을 확인하는 보조 Java 자료 |
| `ble-minimal/` | 직접 빌드되는 독립 BLE 센서 검증 코드 |
| `scripts/Build-WatchCare.ps1` | 수정된 워치 케어를 빌드·서명·검사하는 도구 |
| `scripts/UpdateWatchCareMetadata.py` | 표시 이름과 버전을 함께 바꾸는 도구 |
| `scripts/PrepareOriginalUI.py` | 기존 전체 분석 작업 공간에서 로컬 UI 판을 다시 만드는 고급 경로 |

Java 복원본을 Android Studio에 넣어 그대로 빌드하는 구조는 아닙니다. 난독화된 이름과 잘못 복원된 메서드가 남아 있으므로 실행 로직 수정은 실제 **Smali**와 대조합니다. 원래 개발자의 변수명·주석·Gradle 프로젝트가 완전히 복원된 상태로 취급하지 않습니다.

```mermaid
flowchart LR
    A[최신 검증 APK] --> B[Apktool로 작업 공간 생성]
    B --> C[XML과 이미지 및 Smali 수정]
    C --> D[오버레이와 변경 설명 저장]
    D --> E[버전 변경과 빌드]
    E --> F[정렬과 동일 키 서명]
    F --> G[설치와 워치 연결 시험]
    G --> H[GitHub 코드와 Release 배포]
```

## 3 환경 준비

PowerShell 7, Java 11 이상, Python 3.10 이상, Apktool, Android SDK의 `zipalign`·`apksigner`, USB 시험용 `adb`를 기준으로 작업합니다. Git과 GitHub CLI는 코드 관리와 다운로드에 사용합니다. 이번 실제 빌드는 Apktool 3.0.3과 Android SDK Build Tools 37의 서명·정렬 도구로 진행했습니다.

```powershell
git clone https://github.com/dlwlsrnjs/custom_smart-time-pro.git
cd custom_smart-time-pro
git switch -c feature/watch-care-custom
java -version
python --version
```

전체 작업 공간이 필요한 경우 저장소의 `scripts/Restore-Workspace.ps1`을 사용합니다. 분석 ZIP은 파일 수가 많으므로 다운로드·압축 해제에 시간이 걸릴 수 있습니다. 이미 작업 공간이 있으면 다시 복원하지 않습니다. 이 ZIP에는 과거 분석본이 들어 있으며 **최신 워치 케어 변경을 모두 적용한 작업 공간은 아닙니다.**

```powershell
.\scripts\Restore-Workspace.ps1
```

Apktool은 위 작업 공간의 `tools/apktool.jar`를 사용할 수 있습니다. `zipalign`과 `apksigner.jar`는 Android SDK Build Tools 경로를 지정합니다. 빌드 스크립트의 `-P 16` 정렬 옵션을 지원하는 도구를 사용해야 합니다. 프로젝트 폴더를 공백이 적고 짧은 경로에 두면 Windows의 경로 길이 문제를 줄일 수 있습니다.

## 4 권장 시작 방법은 최신 APK를 작업 기준으로 풀기

최신 APK에 이미 반영된 광고 초기화 차단과 로컬 구성 및 검색 수정이 유지되는 경로입니다. 과거 기본 빌드 스크립트만 실행해 최신 변경이 자동으로 따라올 것으로 가정하지 않습니다.

```powershell
New-Item -ItemType Directory -Force .downloads,workspace | Out-Null
gh release download ui-v0.4.0 --repo dlwlsrnjs/custom_smart-time-pro --pattern Watch_Care_0.4.0.apk --dir .downloads
$apktool = (Resolve-Path .\workspace\SMART_TIME_PRO_customization\tools\apktool.jar).Path
```

GitHub CLI 대신 Release 페이지에서 APK를 내려받아 `.downloads`에 넣어도 됩니다. 이미 같은 파일이 있으면 기존 파일을 보존하고 경로를 확인합니다.

### 화면과 문구만 바꿀 경우

```powershell
java -Xmx4g -jar $apktool d --no-src -o .\workspace\care-ui .\.downloads\Watch_Care_0.4.0.apk
```

이 작업 공간에는 XML·이미지·폰트와 원본 DEX가 들어 있습니다. 화면을 수정하면서 원본 실행 코드는 그대로 재사용할 수 있습니다. **이 경로에 Smali 한 파일만 새 폴더로 만들어 넣지 마세요.** 일부 클래스만 재조립하면 나머지 코드를 누락하거나 수정이 적용되지 않을 수 있습니다.

### 연결 로직과 기능도 바꿀 경우

```powershell
java -Xmx4g -jar $apktool d -o .\workspace\care-code .\.downloads\Watch_Care_0.4.0.apk
```

이 경로는 `smali`, `smali_classes2` 등의 실제 실행 코드도 풉니다. 파일 수와 디스크 사용량이 커집니다. 이미 존재하는 출력 디렉터리를 강제로 지우지 말고 새로운 디렉터리명을 사용하거나 기존 작업을 이어갑니다.

이후 예시의 `care-ui`는 자신이 선택한 `care-code`로 바꿔도 됩니다. 원본 UI 0.4.0 APK를 입력으로 사용했다면 검색 필터 수정은 이미 포함되어 있으므로 다시 덮을 필요가 없습니다.

## 5 바꾸려는 항목과 실제 파일 찾기

| 변경 목표 | 리소스의 시작 위치 | 함께 볼 원본 코드 |
| --- | --- | --- |
| 앱 이름과 안내 | `res/values/strings.xml`, `res/values-ko/strings.xml`, Manifest | `ui/launcher/LauncherActivity` |
| 하단 탭과 첫 화면 | `res/layout/activity_main.xml`, `res/menu/bottom_nav.xml` | `ui/main/MainActivity`, `BaseMainActivity` |
| 걸음수 카드 | `v2_status_item_step.xml`, `v2_status_item_step_transverse.xml`, `status_item_step.xml` | `ui/status/item/MainStep2Item` |
| 심박수 카드 | `v2_status_item_heart_rate.xml`, `status_item_heart_rate.xml` | `ui/status/item/HeartRate2Item`, `HeartRateItem` |
| 심박 상세 | `fragment_status_hr_day.xml` 및 관련 레이아웃 | `ui/status/HeartRateActivity` |
| 걸음수 상세 | 관련 status day/week/month 레이아웃 | `ui/status/StepActivity` |
| 기기 검색 화면 | `activity_add_device.xml`, `item_device.xml` | `ui/device/AddDeviceActivity`와 내부 필터 클래스 |
| 제품 설정 메뉴 | `device_item_*.xml` 및 관련 레이아웃 | `ui/device/DeviceFragment` |
| 사용자 설정 | `main_me_item_bottom.xml` 및 관련 레이아웃 | `ui/me/MainMeFragment` |
| 공통 색과 글꼴 | `res/values/colors.xml`, `styles.xml`, `res/font`, `assets` | `com.sma.smartv3.view`의 사용자 정의 View |

표의 리소스는 작업 공간의 `res/layout` 등을 기준으로 합니다. 코드 이름은 `com.sma.smartv3` 아래의 실제 클래스입니다. 복원 Java의 경로에는 `ui` 대신 `p610ui`처럼 JADX가 붙인 이름이 있을 수 있습니다. Smali에서는 실제 패키지명을 확인합니다.

원본은 기기 능력과 설정에 따라 서로 다른 카드 레이아웃을 선택합니다. 파일 이름이 비슷하다고 한 파일만 고쳐 모든 화면이 바뀐다고 가정하지 않습니다.

```powershell
rg -n 'bottom_health|bottom_device|심박' .\workspace\care-ui\res\values-ko\strings.xml
rg -n 'tvStep|tv_no_data|bottomNav' .\workspace\care-ui\res\layout
rg -n 'v2_status_item_step|v2_status_item_heart_rate' .\linear-reference-java\com\sma\smartv3\ui\status
```

### 예시 A 건강 탭 이름 바꾸기

`res/values-ko/strings.xml`의 `bottom_health` 값을 원하는 표현으로 바꿉니다. 이 키를 쓰는 홈 제목과 하단 탭이 함께 바뀔 수 있습니다. `%s`, `%d`, `%1$s` 같은 서식 기호와 XML 엔티티는 유지합니다. 언어를 바꿔도 적용하려면 기본 `values/strings.xml`과 필요한 번역 파일도 확인합니다.

### 예시 B 고령자를 위한 카드 가독성 개선

실제로 사용되는 걸음수·심박수 카드에서 글자 크기, 내부 여백, 배경 대비를 조정합니다. 텍스트 크기는 `sp`, 터치 영역과 여백은 `dp`를 기준으로 검토합니다. `tvStep`, `tv_no_data` 등 기존 ID와 View 타입은 유지합니다. 큰 시스템 글꼴에서 숫자와 단위가 겹치지 않는지 확인합니다.

XML에 적힌 `32266`, `26°C` 같은 숫자는 초기·미리보기 값일 수 있습니다. 센서 값 변경은 XML의 숫자를 바꾸는 일이 아닙니다. 실제 화면 바인딩과 원본 DB 데이터가 어떤 값을 쓰는지 확인해야 합니다.

### 예시 C 앱 이름과 아이콘 변경

이름은 아래 빌드 스크립트의 `-AppLabel`로 바꿀 수 있습니다. 아이콘은 Manifest의 `android:icon`과 `roundIcon`에서 참조하는 `mipmap` 리소스를 찾고 해상도별·적응형 아이콘 구성을 함께 수정합니다. DEX의 `com.sma.smartv3` 클래스명을 한꺼번에 치환하지 않습니다. JNI와 리플렉션 및 SDK 참조가 깨질 수 있습니다.

### 예시 D 메뉴 숨기기와 기능 제거

탭·카드를 숨기는 것은 기능 제거와 다릅니다. `bottom_nav.xml`의 visible 값을 코드가 다시 설정할 수 있으므로 MainActivity와 기기 능력 조건을 함께 봅니다. 광고나 음성 기능을 제거할 때에는 초기화, 자동 Provider, 서비스, 메뉴, 호출부, 데이터 흐름까지 확인합니다. 클래스 디렉터리만 일괄 삭제하지 않습니다.

## 6 센서와 연결 기능을 수정하는 위치

```mermaid
flowchart LR
    A[워치] --> B[원본 BLE SDK]
    B --> C[메시지 검사와 센서 엔티티 해석]
    C --> D[BleInitializer 콜백]
    D --> E[로컬 데이터베이스]
    E --> F[건강 카드와 상세 화면]
```

| 역할 | 원본 코드 |
| --- | --- |
| 검색·사용자 선택 | `com.sma.smartv3.ui.device.AddDeviceActivity` |
| BLE 연결과 세션 | `com.szabh.smable3.component.BaseBleConnector` |
| 프레임·CRC | `com.szabh.smable3.component.MessageFactory` |
| 알림 분할 수신 | `com.szabh.smable3.component.BleParser` |
| 심박·활동 형식 | `com.szabh.smable3.entity.BleHeartRate`, `BleActivity` |
| 로컬 시간 변환 | `com.bestmafen.baseble.data.BleReadable`의 `timeToMillis` |
| 센서 수신과 앱 반영 | `com.sma.smartv3.initializer.BleInitializer` |
| 원본 DB | `com.sma.smartv3.db.MyDb` |

독립 센서 앱의 `SmaProtocol.java`에는 같은 규격을 읽기 쉽게 정리해 두었습니다. NUS 서비스 UUID, AB 프레임, CRC16, 심박 6바이트·활동 16바이트 레코드와 로컬 시각 변환을 확인하는 자료입니다. 원본 UI를 유지할 때 원본 SDK를 이 코드로 무조건 대체할 필요는 없습니다.

연결은 **휴대폰 Bluetooth 연결 → 앱 BLE 연결 → 제조사 등록·세션 → 센서 동기화 → 화면 표시**를 각각 확인합니다. 통화용 Bluetooth 연결만으로 센서 수신 성공을 판단하지 않습니다.

0.4.0에서 신호가 로그에 잡히지만 목록에 안 보이던 원인은 원본 지원 기기 이름 필터였습니다. 수정은 `patches/original-ui/.../AddDeviceActivity$O00.smali`에 있습니다. 이름·신호 조건은 유지하고 사용자 본인이 이미 등록한 기기도 목록에 포함합니다. 모든 시중 스마트워치의 프로토콜을 지원한다는 의미는 아닙니다.

원본 SDK의 동기화에는 읽은 기록을 워치에서 지우는 경로가 있습니다. 데이터 저장·동기화 코드를 바꿀 때에는 앱 DB 반영과 다음 기록 요청까지 함께 확인해야 합니다. 기록을 삭제하는 명령이나 공장 초기화·펌웨어 명령을 센서 읽기 시험에 섞지 않습니다.

## 7 변경 파일을 오버레이로 남기기

작업 공간 전체를 Git에 올리지 않고 바꾼 파일만 저장합니다. 최신 APK에서 다시 풀었을 때 오버레이를 적용하면 수정 내용을 재현할 수 있도록 합니다.

```powershell
New-Item -ItemType Directory -Force .\patches\watch-care-ui\res\layout | Out-Null
Copy-Item -LiteralPath .\workspace\care-ui\res\layout\v2_status_item_step.xml -Destination .\patches\watch-care-ui\res\layout\v2_status_item_step.xml
```

예를 들어 `res/values-ko/strings.xml`을 바꿨다면 같은 상대 경로로 복사합니다. Smali 오버레이는 해당 DEX 전체가 디코드된 `care-code`에서만 적용합니다. 과거 분석용 `patches/base`와 현재 UI 작업용 `patches/watch-care-ui`를 구분합니다.

변경 설명에는 대상 화면, 바꾼 파일, 기대 동작, 실제 확인 결과를 적습니다. 개인키, 비밀번호, GitHub 토큰, 사용자 DB, Bluetooth 진단 원문과 건강 측정값은 공개 커밋에 넣지 않습니다.

## 8 버전 변경과 빌드 및 서명

현재 설치된 APK는 versionCode **10**입니다. 다음 배포는 예를 들어 **11 / 0.4.1**로 올립니다. 아직 배포하지 않은 실패 빌드를 같은 코드로 다시 빌드하는 것은 가능하지만, 배포 후에는 새 버전 코드를 사용합니다.

현재 설치본을 업데이트하려면 **같은 앱 ID와 같은 서명키**가 필요합니다. 새 PC에서 새 키를 만들어 기존 APK와 자동으로 호환될 것으로 가정하지 않습니다. 원 제작자나 Mindrium의 개인키를 복사하는 방식이 아닙니다. 현재 사용한 개발용 키는 공개 저장소에 넣지 않습니다.

다음 예시는 본인의 Android SDK와 개인키 경로로 바꿔 실행합니다. 비밀번호는 명령 인자에 직접 쓰는 대신 환경변수로 전달합니다.

```powershell
$env:WATCHCARE_STOREPASS = '본인_개인키_암호'
.\scripts\Build-WatchCare.ps1 `
  -DecodedDirectory .\workspace\care-ui `
  -Keystore C:\private\watch-care.jks `
  -KeyAlias androiddebugkey `
  -ApktoolJar $apktool `
  -Zipalign C:\Android\Sdk\build-tools\37.0.0\zipalign.exe `
  -ApksignerJar C:\Android\Sdk\build-tools\37.0.0\lib\apksigner.jar `
  -VersionCode 11 `
  -VersionName 0.4.1 `
  -AppLabel '워치 케어' `
  -OutputDirectory .\dist
```

키의 alias가 다르면 `-KeyAlias`를 맞춥니다. 키 비밀번호와 저장소 비밀번호가 다르면 별도 환경변수를 만들고 `-KeyPasswordEnv`를 지정합니다. 스크립트는 없는 키를 임의로 생성하지 않습니다.

다른 새 디코드 작업 공간에 저장한 수정 파일을 적용하려면 `-OverlayDirectory .\patches\watch-care-ui`를 추가합니다. APK 안의 Smali와 raw DEX를 부분적으로 섞는 방식은 피합니다.

스크립트는 메타데이터 변경, Apktool 빌드, ZIP 정렬, 서명, 서명 검사, 정렬 재검사, SHA256 기록 순서로 진행합니다. 결과는 `dist/Watch_Care_0.4.1.apk`와 `.sha256` 파일입니다. 중간 빌드는 `work/care-build-*`에 보관하며 기존 작업 폴더를 삭제하지 않습니다.

`scripts/Build.ps1`은 과거 전체 원본 복원 경로입니다. 이 스크립트만 실행하면 이전 프로필이 적용될 수 있으므로, 현재 화면 커스터마이징은 `Build-WatchCare.ps1` 경로를 사용합니다.

## 9 휴대폰 설치와 회귀 시험

```powershell
adb devices -l
adb -s <본인_휴대폰_serial> install -r .\dist\Watch_Care_0.4.1.apk
adb -s <본인_휴대폰_serial> shell am start -W -n com.diz.sw01.custom/com.sma.smartv3.ui.launcher.LauncherActivity
adb -s <본인_휴대폰_serial> push .\dist\Watch_Care_0.4.1.apk /sdcard/Download/Watch_Care_0.4.1.apk
```

설치 성공과 앱 실행, 기기 추가, 제조사 등록, 센서 수신, 홈·상세 화면 표시를 차례로 확인합니다. 심박은 수신 시각을 확인하고, 걸음수는 워치 화면의 같은 날짜·동기화 범위와 비교합니다. 연결이 끊긴 상태의 마지막 값도 새 측정값처럼 표시되지 않는지 봅니다.

화면 변경 후에는 큰 시스템 글꼴과 화면 스크롤, 하단 탭, 연결 해제·재연결, 재실행 후 DB 기록을 확인합니다. 실제 기기가 지원하지 않는 산소포화도·혈압 등의 빈 카드를 데이터 수신 성공으로 기록하지 않습니다.

| 증상 | 먼저 확인할 부분 |
| --- | --- |
| 신호는 잡히는데 검색 목록에 안 뜸 | 원본 이름 필터, 서버 지원 목록, Bluetooth 등록 여부 |
| Bluetooth는 연결됐는데 데이터가 없음 | BLE 서비스, 제조사 세션, 워치의 등록 수락, SDK 콜백 |
| 심박 기록 시간이 9시간 밀림 | 원본의 로컬 벽시계 시각 변환 |
| 업데이트 서명이 다르다고 나옴 | 같은 앱 ID와 개인키로 서명했는지 확인 |
| 버전 다운그레이드 오류 | 설치된 versionCode보다 높은지 확인 |
| XML 수정 뒤 바로 종료 | 기존 View ID·타입·리소스 ID와 코드 참조 확인 |
| 로그인·날씨·온라인 다이얼이 안 됨 | 현재 로컬 빌드의 비활성 범위인지 확인 |
| 삼성 또는 Play Protect 차단 | 경고와 APK 해시를 기록하고 공식 판정 사유를 확인 |

보안 차단이 있으면 APK 이름·패키지·키를 바꿔 검사를 피하는 방법으로 처리하지 않습니다. USB 설치 성공, 파일관리자 설치 성공, 서명 무결성 검사, 보안 서비스의 안전성 판정은 서로 다른 검증입니다.

## 10 코드 반영과 APK 게시

```powershell
git status --short
git diff --check
git add patches/watch-care-ui docs
git commit -m 'Update watch health screen'
git push origin feature/watch-care-custom
```

수정 코드를 검토한 뒤 병합하고 해당 커밋을 대상으로 Release를 만듭니다. APK 같은 바이너리는 Git 이력 대신 Release assets에 올립니다. Release 설명에는 원본 유지 범위, 변경한 기능, 비활성 기능, 실제 시험한 기기·OS, 미검증 항목과 체크섬을 적습니다. 개인 건강 화면은 동의 없이 공개 첨부하지 않습니다.

```powershell
gh release create ui-v0.4.1 .\dist\Watch_Care_0.4.1.apk .\dist\Watch_Care_0.4.1.apk.sha256 --repo dlwlsrnjs/custom_smart-time-pro --target <검증한_커밋_SHA> --title 'Watch Care 0.4.1' --notes-file .\release-notes.txt --prerelease
```

## 11 다음 작업의 권장 순서

1. 현재 원본 UI에서 문구·카드 크기·대비부터 조정하고 연결과 센싱 흐름이 유지되는지 확인합니다.
2. 사용자에게 필요하지 않은 메뉴를 정리하면서 메뉴·호출부·서비스·권한의 대응 관계를 기록합니다.
3. 광고 SDK 전체 삭제는 클래스와 리소스 의존성을 추적하며 별도 변경으로 진행합니다. 초기화 차단과 완전 제거를 구분합니다.
4. 온라인 기능을 다시 쓸 때에는 필요한 API와 앱 ID·서명 등록, 계정·데이터 처리 범위를 정한 뒤 항목별로 연결합니다. INTERNET 권한만 추가해 모든 기능이 돌아온다고 가정하지 않습니다.
5. 자체 서버·게임 콘텐츠·추가 센서를 연결하는 작업은 안정화된 UI 및 워치 통신 위에서 진행합니다. 기기의 지원 여부와 실제 수신을 확인한 뒤 화면에 노출합니다.

## 12 전체 분석본에서 다시 조립하는 고급 경로

과거 분석 ZIP에는 `diz-custom` 디렉터리가 들어 있지 않습니다. 따라서 새로 ZIP을 푼 다음 `PrepareOriginalUI.py`만 바로 실행하면 필요한 입력이 없습니다. 아래는 재구성 순서이며, 현재 APK에서 디코드하는 권장 경로보다 준비가 많습니다.

1. `Restore-Workspace.ps1`로 전체 분석본을 복원합니다.
2. 기존 `scripts/Build.ps1`로 기본 재조립을 수행합니다. 이 단계의 옛 테스트 APK를 최신 결과로 배포하지 않습니다.
3. 저장소의 최신 `PrepareStandalone.py`와 `ApplyIdentity.py`를 분석 작업 공간에 복사합니다.
4. 분석 작업 공간에서 `python PrepareStandalone.py diz-custom`으로 ARM64와 화면 리소스를 합칩니다. 출력이 이미 있으면 보존하고 다른 작업 공간을 준비합니다.
5. `python ApplyIdentity.py --decoded diz-custom --profile <저장소/profiles/diz-sw01-custom.json> --report inventory/custom_identity.json`을 실행합니다.
6. 저장소에서 `python scripts/PrepareOriginalUI.py --workspace <분석_작업_공간> --output-dir <새_로컬_UI_작업_공간>`을 실행합니다. 여기서 현재 Manifest·초기화·검색 수정을 적용합니다.
7. 만들어진 작업 공간에서 원하는 추가 수정을 한 뒤 `Build-WatchCare.ps1`로 더 높은 버전을 빌드합니다.

과거 분석본과 최신 APK 사이에는 구성 차이가 있을 수 있습니다. 원본 SDK 전체 재복원 연구가 목적이 아니라면 **현재 검증한 APK를 기준으로 수정하는 4절 경로가 우선**입니다.
