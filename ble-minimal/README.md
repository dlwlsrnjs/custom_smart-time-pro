# 워치 센서 0.3.0

원본 SMART-TIME PRO의 BLE 규격을 분석해 만든 독립 센서 시험 앱입니다. 사용자의 최신 요구인 **원본 UI와 기능 유지** 버전은 [워치 케어 빌드](../docs/ORIGINAL_UI_BUILD.md)입니다. 이 디렉터리는 통신 검증과 후속 커스터마이징을 위해 보존합니다.

## 구현과 실기기 확인

- 검색, 등록된 기기 표시, 수동 연결, 서비스 탐색, 연결 해제.
- 원본 NUS 서비스 UUID, AB 메시지 헤더, CRC16, 응답과 ACK, 기기 등록 및 세션 절차 적용.
- 원본 형식의 심박 기록 6바이트와 활동 기록 16바이트 해석, 배터리 읽기.
- 실제 워치에서 제조사 등록·세션 연결, 심박 기록·활동 기록·배터리 응답 수신과 화면 표시 확인.
- 원본 `BleReadable.timeToMillis`에 맞춰 로컬 시각을 해석. 한국 시간에서 9시간 어긋나는 문제 수정.
- 심박 측정 시작·중지 요청과 워치 ACK 확인. **새 실시간 심박 알림 수신은 아직 확인하지 못함.** 표시된 심박 기록을 실시간 측정 성공으로 표현하지 않음.
- 걸음수는 오늘 수신한 활동 기록의 부분 합계. 워치 전체 일일 합계와 일치하는지는 추가 검증 필요. 읽은 기록을 워치에서 삭제하지 않음.
- 인터넷, 광고 SDK, 원본 DEX, 네이티브 라이브러리와 서버 통신 없음.

## 파일과 수정 위치

| 파일 | 역할 |
| --- | --- |
| `src/com/diz/sw01/custom/MainActivity.java` | 화면과 Bluetooth 권한·검색·연결·GATT 전송 큐·센서 표시 |
| `src/com/diz/sw01/custom/SmaProtocol.java` | 프레임 생성·CRC·분할 수신·센서 해석·로컬 시각 변환 |
| `tests/ProtocolTest.java` | CRC 기준값, 센서 레이아웃, 시간대, 모든 분할 경계, 손상·잘린 데이터 검증 |
| `AndroidManifest.xml` | 앱 ID·표시 이름과 권한 |
| `build.py` | javac와 D8, AAPT2, zipalign, apksigner 빌드 |

앱 ID는 `com.diz.sw01.custom`, versionCode 4, versionName 0.3.0, minSdk 26 / targetSdk 36입니다. 원본 UI 버전 0.4.0은 같은 앱 ID의 상위 버전이므로 설치하면 이 앱을 대체합니다. 기존 원본 앱 `com.sma.smartv3.pro`를 제거하지 않습니다.

## 테스트와 빌드

```powershell
javac -encoding UTF-8 -d <test_classes> src/com/diz/sw01/custom/SmaProtocol.java tests/ProtocolTest.java
java -cp <test_classes> com.diz.sw01.custom.ProtocolTest
python build.py --android-jar <android.jar> --aapt2 <aapt2.exe> --d8-jar <d8.jar> --zipalign <zipalign.exe> --apksigner-jar <apksigner.jar> --keystore <개인키.jks> --output <Watch_Sensors.apk> --build-dir <새_빌드_디렉터리>
```

기본 개발용 alias는 `androiddebugkey`, 기본 개발용 암호는 `android`이며 `DIZ_TEST_STOREPASS` 환경변수로 변경할 수 있습니다. 배포 APK와 업데이트 호환성을 유지하려면 같은 개인키를 사용해야 합니다. 키는 저장소나 소스 ZIP에 포함하지 않습니다.

Android 13 이상에서는 [Android 공식 BluetoothGatt API](https://developer.android.com/reference/android/bluetooth/BluetoothGatt)의 값 인자 방식으로 쓰며, 이전 OS에만 deprecated API를 사용합니다. 광고·민감 기능을 원본에서 무작정 가져와 설치를 보장하는 버전이 아닙니다. 서명·정렬 검사와 보안 서비스의 안전성 판정은 별도입니다.
