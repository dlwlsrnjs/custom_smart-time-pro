# 원본 UI와 기능을 유지하는 워치 케어

0.4.0은 SMART-TIME PRO의 화면과 기능을 새로 그린 앱이 아닙니다. 기존 재패키징 작업 공간의 XML·이미지·폰트·앱 코드·워치 SDK를 유지하고, 선택 기능의 초기화와 Manifest를 수정한 ARM64 단일 APK입니다. `ble-minimal`의 독립 센서 앱과 구분합니다.

## 현재 변경

- 원본 화면 리소스와 워치 SDK 유지. 앱 표시 이름은 `워치 케어`, 앱 ID는 `com.diz.sw01.custom`, versionCode 10 / versionName 0.4.0.
- 원본 기기 추가 화면의 지원 기기 이름 필터에 사용자 본인이 Bluetooth 설정에서 등록한 기기도 포함. 이름과 신호 세기 조건은 유지하며, 사용자가 선택한 기기에만 원본 SDK로 연결. 서버 지원 목록이 없는 로컬 모드에서 이미 등록된 워치가 숨겨지던 문제에 대응.
- 이전 테스트 앱과 같은 개발용 서명키 사용. 원 제작자의 서명키나 Mindrium의 개인키를 복제하지 않음.
- 광고 SDK Manifest 진입점 67개 제거. 원본의 `AdInitializer` SDK 시작 경로 4개와 음성·오디오 콘텐츠 초기화 경로 각 1개를 비활성화.
- Audience Network 실행 assets와 광고 viewer assets 제거. **의존성 때문에 광고 SDK의 DEX 코드는 아직 남아 있음. 전체 SDK 삭제 완료를 의미하지 않음.**
- SMS·통화기록·연락처·녹음·카메라·특권 권한 등을 제거. 해당 부가 기능은 사용할 수 없음.
- 첫 실기기 시험은 로컬 모드. INTERNET 권한을 제거하여 원 공급자 서버에 데이터를 전송하지 않음. 계정, 날씨, 온라인 워치페이스, 웹 콘텐츠 기능은 연결되지 않음.
- 시작 화면의 데이터 처리 안내를 현재 로컬 테스트 구성에 맞게 수정. 로그인이나 결제·유료 기능 인증을 우회하지 않음.

## 직접 재빌드

전체 분석 작업 공간의 `diz-custom` 디렉터리와 `editable/base/smali_classes18`, `editable/base/smali_classes19`가 필요합니다. Git 저장소만으로는 전체 제3자 SDK와 바이너리가 제공되지 않습니다.

```powershell
python scripts/PrepareOriginalUI.py --workspace <SMART_TIME_PRO_customization> --output-dir <새로운_빌드_디렉터리>
java -Xmx4g -jar <apktool.jar> b -j 6 -p <framework_cache> -o <unsigned.apk> <새로운_빌드_디렉터리>
zipalign -f -P 16 4 <unsigned.apk> <aligned.apk>
java -jar <apksigner.jar> sign --ks <개인키_경로> --out <Watch_Care.apk> <aligned.apk>
java -jar <apksigner.jar> verify --verbose <Watch_Care.apk>
```

원본 앱 클래스의 패키지명은 JNI와 SDK 참조를 유지하기 위해 바꾸지 않습니다. 설치 식별자인 application ID와 Provider authority는 이미 분리된 `diz-custom` 작업 공간을 사용합니다. `PrepareOriginalUI.py`는 기존 원본을 덮어쓰지 않고 새 출력 디렉터리를 생성합니다.

## 검증을 구분할 것

APK 서명 검증, USB 설치, 일반 파일관리자 설치, 시작 화면 표시, 워치 등록, 실제 건강 데이터 수신은 각각 확인해야 합니다. USB 설치 성공을 삼성 또는 Google의 안전성 인증으로 표현하지 않습니다. 전체 원본 기능이 들어 있다는 것과 각 기능의 실제 동작이 검증됐다는 것은 다릅니다.

Mindrium 비교 APK는 Android Debug 인증서의 v2 서명이었습니다. 별도 공인 인증서 등록이 있어야만 APK를 설치할 수 있는 형태가 아니었습니다. 현재 자체 서명키를 유지하고 Android 표준 서명·버전·권한 구성을 검증합니다. 보안 판정의 원인을 인증서 차이 하나로 확정하지 않습니다.
