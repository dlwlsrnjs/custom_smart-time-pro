# BLE 전용 최소 앱 0.2.0

사용자가 선택한 **검색·연결·연결 해제** 범위만 직접 구현한 독립 Android 앱입니다. 이전 대형 APK에서 Manifest만 지운 버전이 아니며, 이전 앱의 DEX·광고 SDK·네이티브·서버·리소스를 포함하지 않습니다. 공급사 SDK를 유지한 제조사 앱의 기능 축소판이 아니라 Android 표준 BLE API만 사용하는 테스트입니다.

- 앱 ID는 `com.diz.sw01.custom`, 이전 커스텀 버전과 같은 테스트 서명 키를 유지합니다.
- 0.2.0 / versionCode 2, minSdk 26 / targetSdk 36.
- Android 12 이상에서는 BLUETOOTH_SCAN·BLUETOOTH_CONNECT만 런타임 요청합니다.
- Android 11 이하의 BLE 검색에 필요한 기존 Bluetooth·위치 권한은 maxSdkVersion 30으로 제한했습니다.
- 인터넷·광고·SMS·연락처·통화·마이크·카메라·알림 접근·접근성·계정·서버 통신·백그라운드 서비스가 없습니다.
- 기기 선택은 사용자가 직접 합니다. 연결 후 GATT 서비스를 조회하며 특성 쓰기·펌웨어 전송은 하지 않습니다.
- 제조사 인증·바인딩·암호화 명령·건강 기록 동기화는 구현하지 않았습니다. GATT 연결 성공을 Diz-SW01의 전체 연동 성공으로 표시하지 않습니다.
- 검색 결과는 메모리에만 보관하고 서버로 보내지 않습니다. 검색은 20초 후 또는 앱이 가려지면 중지하며 Activity 종료 시 연결을 정리합니다.

## 설치 검증 목적

발행자 인증서·앱 ID·targetSdk를 이전 커스텀 앱과 같게 유지하면서 제품 기능을 BLE 시험으로 축소했습니다. 설치가 허용되면 기존 인증서가 기기에서 일괄 차단되는 상황이라는 설명은 약해집니다. 그러나 여러 구성 요소가 바뀌었으므로 어떤 권한/SDK가 원인이었는지 단독으로 입증하지는 않습니다. 같은 휴대폰에서 사용자가 정상 설치 UI로 시험하며, 보안 차단을 무시하거나 ADB 강제 설치하는 절차는 포함하지 않습니다.

## 빌드

Android API 36 android.jar, AAPT2, D8, zipalign, apksigner와 해당 도구를 실행할 JDK가 필요합니다. `build.py --help`로 인자를 확인하세요. 예시(경로는 자신의 SDK에 맞춥니다):

```powershell
python ble-minimal/build.py --android-jar "$env:ANDROID_HOME/platforms/android-36/android.jar" --aapt2 "$env:ANDROID_HOME/build-tools/37.0.0/aapt2.exe" --d8-jar "$env:ANDROID_HOME/build-tools/37.0.0/lib/d8.jar" --zipalign "$env:ANDROID_HOME/build-tools/37.0.0/zipalign.exe" --apksigner-jar "$env:ANDROID_HOME/build-tools/37.0.0/lib/apksigner.jar" --keystore "<본인의 개발용 키 경로>" --output "dist/Diz_SW01_BLE_0.2.0_TEST.apk" --build-dir "work/ble-build"
```

기본 개발용 alias는 androiddebugkey, 개발용 암호는 android입니다. `DIZ_TEST_STOREPASS` 환경변수로 암호를 전달할 수 있습니다. 운영용 키와 배포 절차는 별도로 관리해야 합니다. 공개 저장소에는 개인키가 없습니다. 게시 APK는 Java 11, API 36, Apktool 번들 AAPT2, JADX 번들 D8 9.1.31, 공식 SDK 37 zipalign·apksigner로 실제 빌드했습니다.
